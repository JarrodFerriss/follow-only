package com.followonly;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class FollowOnlyPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(FollowOnlyPlugin.class);
		RuneLite.main(args);
	}
}