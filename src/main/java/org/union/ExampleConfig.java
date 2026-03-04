package org.union;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("unionting")
public interface ExampleConfig extends Config
{
	@ConfigItem(
			keyName = "greeting",
			name = "Welcome Greeting",
			description = "The message to show to the user when they login"
	)
	default String greeting() { return "Hello Seb!"; }

	@ConfigItem(
			keyName = "userId",
			name = "User ID",
			description = "Your union service user ID"
	)
	default String userId() { return ""; }

	@ConfigItem(
			keyName = "unionId",
			name = "Union ID",
			description = "Your union ID"
	)
	default String unionId() { return ""; }

	@ConfigItem(
			keyName = "missionKillGoal",
			name = "Mission Kill Goal",
			description = "How many goblin kills count as one completed mission"
	)
	default int missionKillGoal() { return 200; }
}