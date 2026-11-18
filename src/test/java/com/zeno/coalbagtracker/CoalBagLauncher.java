/*
 * Copyright (c) 2026, Zeno
 * All rights reserved.
 */
package com.zeno.coalbagtracker;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class CoalBagLauncher
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(CoalBagTrackerPlugin.class);
		RuneLite.main(args);
	}
}
