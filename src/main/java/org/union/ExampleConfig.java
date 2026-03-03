package org.union;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("example")
public interface ExampleConfig extends Config
{
	@ConfigItem(
		keyName = "greeting",
		name = "Welcome Greeting wuhu",
		description = "The message to show to the user when they login"
	)
	default String greeting()
	{
		return "Hello Seb1";
	}
}
