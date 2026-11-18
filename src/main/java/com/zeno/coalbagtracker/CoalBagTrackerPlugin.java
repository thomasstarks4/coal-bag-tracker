/*
 * Copyright (c) 2019 Adam <Adam@sigterm.info>
 * Copyright (c) 2021 Nick Wolff <nick@wolff.tech>
 * Copyright (c) 2022 TicTac7x
 * Copyright (c) 2026, Zeno
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.zeno.coalbagtracker;

import com.google.inject.Provides;
import javax.inject.Inject;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.ItemContainer;
import net.runelite.api.Player;
import net.runelite.api.Skill;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.ClientTick;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.gameval.AnimationID;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.Text;

@PluginDescriptor(
	name = "Coal Bag Tracker",
	description = "Tracks coal in your bag as you mine, smelt bars, and cast Superheat Item",
	tags = {"coal", "bag", "mining", "smithing", "smelting", "superheat"}
)
public class CoalBagTrackerPlugin extends Plugin
{
	@Inject
	private Client client;
	@Inject
	private OverlayManager overlayManager;
	@Inject
	private CoalBagOverlay overlay;
	@Inject
	private CoalBagTracker tracker;

	private BagInventory previousInventory;
	private boolean inventoryDirty;
	private boolean equipmentDirty;
	private int capacity;
	private int previousSmithingXp;
	private String lastDialog;

	@Provides
	CoalBagConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(CoalBagConfig.class);
	}

	@Override
	protected void startUp()
	{
		reset();
		overlayManager.add(overlay);
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(overlay);
		reset();
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		switch (event.getGameState())
		{
			case LOGGING_IN:
			case LOGIN_SCREEN:
			case HOPPING:
			case CONNECTION_LOST:
				reset();
				break;
			default:
				break;
		}
	}

	private void reset()
	{
		tracker.reset();
		previousInventory = null;
		previousSmithingXp = -1;
		inventoryDirty = true;
		equipmentDirty = true;
		lastDialog = null;
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		if (event.getContainerId() == InventoryID.INV)
		{
			inventoryDirty = true;
		}
		else if (event.getContainerId() == InventoryID.WORN)
		{
			equipmentDirty = true;
		}
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}
		BagInventory current = previousInventory;
		if (inventoryDirty)
		{
			ItemContainer inventory = client.getItemContainer(InventoryID.INV);
			current = inventory == null ? null : new BagInventory(inventory.getItems());
			inventoryDirty = inventory == null;
		}

		if (equipmentDirty)
		{
			capacity = bagCapacity();
			equipmentDirty = false;
		}

		int xp = client.getSkillExperience(Skill.SMITHING);
		// Animations also cover players at the XP cap. Both paths still require an ore-to-bar change.
		boolean smithing = (previousSmithingXp >= 0 && xp > previousSmithingXp)
			|| (current != previousInventory && usingFurnaceOrSuperheat());
		tracker.finishTick(previousInventory, current, smithing, capacity);
		previousInventory = current;
		previousSmithingXp = xp;
	}

	private boolean usingFurnaceOrSuperheat()
	{
		Player player = client.getLocalPlayer();
		if (player == null)
		{
			return false;
		}
		switch (player.getAnimation())
		{
			case AnimationID.HUMAN_CASTSUPERHEATITEM:
			case AnimationID.SUPERHEATITEM_CASTING:
			case AnimationID.HUMAN_FURNACE:
			case AnimationID.HUMAN_FURNACE_NOSTALL:
				return true;
			default:
				return false;
		}
	}

	private int bagCapacity()
	{
		ItemContainer equipment = client.getItemContainer(InventoryID.WORN);
		boolean cape = equipment != null && (equipment.contains(ItemID.SKILLCAPE_SMITHING)
			|| equipment.contains(ItemID.SKILLCAPE_SMITHING_TRIMMED)
			|| equipment.contains(ItemID.SKILLCAPE_MAX)
			|| equipment.contains(ItemID.SKILLCAPE_MAX_WORN));
		return cape ? 36 : 27;
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		if (event.getType() == ChatMessageType.GAMEMESSAGE || event.getType() == ChatMessageType.SPAM)
		{
			readMessage(event.getMessage());
		}
	}

	@Subscribe
	public void onClientTick(ClientTick event)
	{
		// Some partial empties use a short-lived object dialogue instead of a chat line.
		Widget widget = client.getWidget(InterfaceID.Objectbox.TEXT);
		String text = widget == null || widget.isHidden() ? null : widget.getText();
		if (text != null && !text.equals(lastDialog))
		{
			readMessage(text);
		}
		lastDialog = text;
	}

	private void readMessage(String text)
	{
		ItemContainer inventory = client.getItemContainer(InventoryID.INV);
		if (text != null && inventory != null && (inventory.contains(ItemID.COAL_BAG)
			|| inventory.contains(ItemID.COAL_BAG_OPEN)))
		{
			tracker.onMessage(Text.removeTags(text.replace("<br>", " ")).replace('\u00a0', ' ').trim());
		}
	}
}
