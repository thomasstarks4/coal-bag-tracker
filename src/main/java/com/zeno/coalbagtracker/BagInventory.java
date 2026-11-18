/*
 * Copyright (c) 2026, Zeno
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.zeno.coalbagtracker;

import java.util.HashMap;
import java.util.Map;
import net.runelite.api.Item;
import net.runelite.api.gameval.ItemID;

/** ItemContainer is mutable. Keep independent counts at the end of each server tick. */
final class BagInventory
{
	private enum Recipe
	{
		STEEL(ItemID.IRON_ORE, ItemID.STEEL_BAR, 2),
		LOVAKITE(ItemID.LOVAKITE_ORE, ItemID.LOVAKITE_BAR, 2),
		MITHRIL(ItemID.MITHRIL_ORE, ItemID.MITHRIL_BAR, 4),
		ADAMANTITE(ItemID.ADAMANTITE_ORE, ItemID.ADAMANTITE_BAR, 6),
		RUNITE(ItemID.RUNITE_ORE, ItemID.RUNITE_BAR, 8),
		ELEMENTAL(ItemID.ELEMENTAL_WORKSHOP_ORE, ItemID.ELEMENTAL_WORKSHOP_BAR, 4);

		private final int ore;
		private final int bar;
		private final int coal;

		Recipe(int ore, int bar, int coal)
		{
			this.ore = ore;
			this.bar = bar;
			this.coal = coal;
		}
	}

	private final Map<Integer, Integer> counts = new HashMap<>();

	BagInventory(Item[] items)
	{
		for (Item item : items)
		{
			if (item != null && item.getId() >= 0)
			{
				counts.merge(item.getId(), item.getQuantity(), Integer::sum);
			}
		}
	}

	int count(int id)
	{
		return counts.getOrDefault(id, 0);
	}

	boolean hasBag()
	{
		return count(ItemID.COAL_BAG) > 0 || count(ItemID.COAL_BAG_OPEN) > 0;
	}

	int coalSpentSince(BagInventory before)
	{
		int catalystsUsed = Math.max(0, before.count(ItemID.SMITHING_CATALYST) - count(ItemID.SMITHING_CATALYST));
		int coal = 0;
		for (Recipe recipe : Recipe.values())
		{
			int oreUsed = before.count(recipe.ore) - count(recipe.ore);
			int barsGained = count(recipe.bar) - before.count(recipe.bar);
			int smelted = Math.max(0, Math.min(oreUsed, barsGained));
			int discounted = Math.min(smelted, catalystsUsed);
			coal += smelted * recipe.coal - discounted * (recipe.coal / 2);
			catalystsUsed -= discounted;
		}
		return coal;
	}
}
