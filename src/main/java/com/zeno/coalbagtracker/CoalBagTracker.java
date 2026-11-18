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

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Singleton;
import net.runelite.api.gameval.ItemID;

@Singleton
class CoalBagTracker
{
	static final int UNKNOWN = -1;
	private static final Pattern CONTENTS = Pattern.compile(
		"The coal bag (?:still )?contains (one|\\d{1,2}) pieces? of coal\\.");
	private static final String MINED = "You manage to mine some coal.";
	private static final String RING_BONUS = "Your Celestial ring allows you to mine an additional ore.";
	private static final String ARMOUR_BONUS = "The Varrock platebody enabled you to mine an additional ore.";

	private int amount = UNKNOWN;
	private int mined;
	private int bonus;
	private boolean confirmed;

	int getAmount()
	{
		return amount;
	}

	boolean isUnknown()
	{
		return amount == UNKNOWN;
	}

	boolean isEmpty()
	{
		return amount == 0;
	}

	void reset()
	{
		amount = UNKNOWN;
		clearTick();
	}

	void onMessage(String message)
	{
		switch (message)
		{
			case MINED:
				mined++;
				return;
			case RING_BONUS:
			case ARMOUR_BONUS:
				bonus++;
				return;
			case "The coal bag is empty.":
			case "The coal bag is now empty.":
			case "You empty all of your containers into the bank.":
				confirm(0);
				return;
			default:
				Matcher match = CONTENTS.matcher(message);
				if (match.matches())
				{
					String count = match.group(1);
					confirm("one".equals(count) ? 1 : Integer.parseInt(count));
				}
		}
	}

	private void confirm(int count)
	{
		amount = count;
		confirmed = true;
		mined = 0;
		bonus = 0;
	}

	void finishTick(BagInventory before, BagInventory after, boolean smithing, int capacity)
	{
		if (after == null || !after.hasBag())
		{
			reset();
			return;
		}
		if (!isUnknown() && before != null && before.hasBag())
		{
			int minedCoal = mined > 0 ? mined + bonus : 0;
			// A contents report already includes the corresponding inventory change.
			int required = !confirmed && smithing && before != after ? after.coalSpentSince(before) : 0;
			int looseChange = after.count(ItemID.COAL) - before.count(ItemID.COAL);

			// Coal made or spent this tick must end up either loose or in the bag.
			// Limit each adjustment to its observed action, so an unrelated transfer cannot move the counter.
			int bagChange = minedCoal - required - looseChange;
			if (bagChange > 0 && after.count(ItemID.COAL_BAG_OPEN) > 0)
			{
				int freeSpace = Math.max(0, capacity - amount);
				amount += Math.min(bagChange, Math.min(minedCoal, freeSpace));
			}
			else if (bagChange < 0)
			{
				int spent = Math.min(-bagChange, required);
				amount = spent <= amount ? amount - spent : UNKNOWN;
			}
		}
		clearTick();
	}

	private void clearTick()
	{
		mined = 0;
		bonus = 0;
		confirmed = false;
	}
}
