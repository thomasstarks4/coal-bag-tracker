/*
 * Copyright (c) 2026, Zeno
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.zeno.coalbagtracker;

import net.runelite.api.Item;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class CoalBagTrackerTest
{
	private final CoalBagTracker tracker = new CoalBagTracker();

	/** Each id is one occupied inventory slot. */
	private static BagInventory inventory(int... ids)
	{
		Item[] items = new Item[ids.length];
		for (int i = 0; i < ids.length; i++)
		{
			items[i] = new Item(ids[i], 1);
		}
		return new BagInventory(items);
	}

	private void checkBag(int amount, BagInventory inv)
	{
		tracker.onMessage("The coal bag contains " + amount + " pieces of coal.");
		tracker.finishTick(null, inv, false, 27);
	}

	@Test
	public void superheatUsesLooseCoalFirstThenOnlyTakesTheShortfall()
	{
		BagInventory before = inventory(ItemID.COAL_BAG, ItemID.MITHRIL_ORE, ItemID.COAL);
		BagInventory after = inventory(ItemID.COAL_BAG, ItemID.MITHRIL_BAR);
		checkBag(10, before);
		tracker.finishTick(before, after, true, 27);
		assertEquals(7, tracker.getAmount());
		tracker.finishTick(after, after, true, 27);
		assertEquals(7, tracker.getAmount());
	}

	@Test
	public void entirelyInventoryFundedBarAndFailedCastLeaveBagAlone()
	{
		BagInventory before = inventory(ItemID.COAL_BAG_OPEN, ItemID.IRON_ORE, ItemID.COAL, ItemID.COAL);
		checkBag(9, before);
		tracker.finishTick(before, before, true, 27);
		assertEquals(9, tracker.getAmount());
		tracker.finishTick(before, inventory(ItemID.COAL_BAG_OPEN, ItemID.STEEL_BAR), true, 27);
		assertEquals(9, tracker.getAmount());
	}

	@Test
	public void furnaceBatchCanCrossFromLooseCoalIntoTheBagAndEndAtZero()
	{
		BagInventory before = inventory(ItemID.COAL_BAG, ItemID.ADAMANTITE_ORE, ItemID.ADAMANTITE_ORE, ItemID.COAL);
		BagInventory first = inventory(ItemID.COAL_BAG, ItemID.ADAMANTITE_ORE, ItemID.ADAMANTITE_BAR);
		BagInventory second = inventory(ItemID.COAL_BAG, ItemID.ADAMANTITE_BAR, ItemID.ADAMANTITE_BAR);
		checkBag(11, before);
		tracker.finishTick(before, first, true, 27);
		assertEquals(6, tracker.getAmount());
		tracker.finishTick(first, second, true, 27);
		assertEquals(0, tracker.getAmount());
	}

	@Test
	public void eachSupportedRecipeUsesItsOwnCoalCost()
	{
		int[][] recipes = {
			{ItemID.IRON_ORE, ItemID.STEEL_BAR, 2},
			{ItemID.LOVAKITE_ORE, ItemID.LOVAKITE_BAR, 2},
			{ItemID.MITHRIL_ORE, ItemID.MITHRIL_BAR, 4},
			{ItemID.ADAMANTITE_ORE, ItemID.ADAMANTITE_BAR, 6},
			{ItemID.RUNITE_ORE, ItemID.RUNITE_BAR, 8},
			{ItemID.ELEMENTAL_WORKSHOP_ORE, ItemID.ELEMENTAL_WORKSHOP_BAR, 4}
		};
		for (int[] recipe : recipes)
		{
			BagInventory before = inventory(ItemID.COAL_BAG, recipe[0]);
			checkBag(20, before);
			tracker.finishTick(before, inventory(ItemID.COAL_BAG, recipe[1]), true, 27);
			assertEquals("Bar " + recipe[1], 20 - recipe[2], tracker.getAmount());
		}
	}

	@Test
	public void catalystOnlyDiscountsBarsForWhichOneWasConsumed()
	{
		BagInventory before = inventory(ItemID.COAL_BAG, ItemID.RUNITE_ORE, ItemID.RUNITE_ORE,
			ItemID.SMITHING_CATALYST, ItemID.COAL);
		BagInventory after = inventory(ItemID.COAL_BAG, ItemID.RUNITE_BAR, ItemID.RUNITE_BAR);
		checkBag(15, before);
		tracker.finishTick(before, after, true, 27);
		assertEquals(4, tracker.getAmount());
	}

	@Test
	public void carryingAnUnusedCatalystGivesNoDiscount()
	{
		BagInventory before = inventory(ItemID.COAL_BAG, ItemID.MITHRIL_ORE, ItemID.SMITHING_CATALYST);
		checkBag(10, before);
		tracker.finishTick(before, inventory(ItemID.COAL_BAG, ItemID.MITHRIL_BAR, ItemID.SMITHING_CATALYST), true, 27);
		assertEquals(6, tracker.getAmount());
	}

	@Test
	public void coalFreeIronBarIsDistinctFromSteel()
	{
		BagInventory before = inventory(ItemID.COAL_BAG, ItemID.IRON_ORE);
		checkBag(7, before);
		tracker.finishTick(before, inventory(ItemID.COAL_BAG, ItemID.IRON_BAR), true, 27);
		assertEquals(7, tracker.getAmount());
	}

	@Test
	public void blastFurnaceDepositAndCollectionDoNotConsumeBagCoal()
	{
		BagInventory before = inventory(ItemID.COAL_BAG, ItemID.MITHRIL_ORE, ItemID.COAL);
		BagInventory deposited = inventory(ItemID.COAL_BAG);
		checkBag(16, before);
		tracker.finishTick(before, deposited, false, 27);
		tracker.finishTick(deposited, inventory(ItemID.COAL_BAG, ItemID.MITHRIL_BAR), true, 27);
		assertEquals(16, tracker.getAmount());
	}

	@Test
	public void bankExchangeOfOreForBarsIsNotSmelting()
	{
		BagInventory before = inventory(ItemID.COAL_BAG, ItemID.MITHRIL_ORE);
		checkBag(10, before);
		tracker.finishTick(before, inventory(ItemID.COAL_BAG, ItemID.MITHRIL_BAR), false, 27);
		assertEquals(10, tracker.getAmount());
	}

	@Test
	public void partialEmptyAndAnExactCheckOverrideInventoryInference()
	{
		BagInventory before = inventory(ItemID.COAL_BAG, ItemID.MITHRIL_ORE);
		checkBag(8, before);
		tracker.onMessage("The coal bag still contains 4 pieces of coal.");
		tracker.finishTick(before, inventory(ItemID.COAL_BAG, ItemID.MITHRIL_BAR), true, 27);
		assertEquals(4, tracker.getAmount());
		tracker.onMessage("The coal bag still contains one piece of coal.");
		assertEquals(1, tracker.getAmount());
		tracker.onMessage("You empty all of your containers into the bank.");
		assertEquals(0, tracker.getAmount());
	}

	@Test
	public void bonusAndBaseMiningMessagesCountInEitherOrderAndExpireTogether()
	{
		BagInventory open = inventory(ItemID.COAL_BAG_OPEN);
		checkBag(10, open);
		tracker.onMessage("Your Celestial ring allows you to mine an additional ore.");
		tracker.onMessage("You manage to mine some coal.");
		tracker.onMessage("The Varrock platebody enabled you to mine an additional ore.");
		tracker.finishTick(open, open, false, 27);
		assertEquals(13, tracker.getAmount());
		tracker.onMessage("The Varrock platebody enabled you to mine an additional ore.");
		tracker.finishTick(open, open, false, 27);
		assertEquals(13, tracker.getAmount());
	}

	@Test
	public void onlyCoalThatEntersTheOpenBagIncreasesTheCount()
	{
		BagInventory open = inventory(ItemID.COAL_BAG_OPEN);
		BagInventory overflow = inventory(ItemID.COAL_BAG_OPEN, ItemID.COAL);
		checkBag(26, open);
		tracker.onMessage("You manage to mine some coal.");
		tracker.onMessage("Your Celestial ring allows you to mine an additional ore.");
		tracker.finishTick(open, overflow, false, 27);
		assertEquals(27, tracker.getAmount());
		tracker.onMessage("You manage to mine some coal.");
		tracker.finishTick(overflow, inventory(ItemID.COAL_BAG_OPEN, ItemID.COAL, ItemID.COAL), false, 27);
		assertEquals(27, tracker.getAmount());
	}

	@Test
	public void miningWithClosedBagAndMiningOtherOresDoNotAddCoal()
	{
		BagInventory closed = inventory(ItemID.COAL_BAG);
		checkBag(8, closed);
		tracker.onMessage("You manage to mine some coal.");
		tracker.finishTick(closed, inventory(ItemID.COAL_BAG, ItemID.COAL), false, 27);
		assertEquals(8, tracker.getAmount());
		BagInventory open = inventory(ItemID.COAL_BAG_OPEN);
		tracker.onMessage("You manage to mine some mithril.");
		tracker.onMessage("Your Celestial ring allows you to mine an additional ore.");
		tracker.finishTick(open, open, false, 27);
		assertEquals(8, tracker.getAmount());
	}

	@Test
	public void capeRaisesCapacityAndRemovingItDoesNotRemoveExistingCoal()
	{
		BagInventory open = inventory(ItemID.COAL_BAG_OPEN);
		checkBag(35, open);
		tracker.onMessage("You manage to mine some coal.");
		tracker.finishTick(open, open, false, 36);
		assertEquals(36, tracker.getAmount());
		tracker.onMessage("You manage to mine some coal.");
		tracker.finishTick(open, open, false, 27);
		assertEquals(36, tracker.getAmount());
	}

	@Test
	public void resetAndBagRemovalRequireAFreshCount()
	{
		BagInventory before = inventory(ItemID.COAL_BAG_OPEN, ItemID.RUNITE_ORE);
		checkBag(20, before);
		tracker.reset();
		tracker.onMessage("You manage to mine some coal.");
		tracker.finishTick(before, inventory(ItemID.COAL_BAG_OPEN, ItemID.RUNITE_BAR), true, 36);
		assertEquals(CoalBagTracker.UNKNOWN, tracker.getAmount());
		checkBag(10, before);
		tracker.finishTick(before, inventory(), false, 27);
		assertEquals(CoalBagTracker.UNKNOWN, tracker.getAmount());
	}

	@Test
	public void impossibleSpendFallsBackToCheck()
	{
		BagInventory before = inventory(ItemID.COAL_BAG, ItemID.RUNITE_ORE);
		checkBag(3, before);
		tracker.finishTick(before, inventory(ItemID.COAL_BAG, ItemID.RUNITE_BAR), true, 27);
		assertEquals(CoalBagTracker.UNKNOWN, tracker.getAmount());
		tracker.onMessage("The coal bag is now empty.");
		assertEquals(0, tracker.getAmount());
	}

	@Test
	public void miningAndSuperheatingInOneTickShareTheSameLooseCoalBalance()
	{
		BagInventory before = inventory(ItemID.COAL_BAG_OPEN, ItemID.COAL, ItemID.MITHRIL_ORE);
		BagInventory after = inventory(ItemID.COAL_BAG_OPEN, ItemID.MITHRIL_BAR);
		checkBag(27, before);
		tracker.onMessage("You manage to mine some coal.");
		tracker.finishTick(before, after, true, 27);
		assertEquals(25, tracker.getAmount());
	}
}
