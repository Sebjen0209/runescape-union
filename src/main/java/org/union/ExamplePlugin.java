package org.union;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;

import net.runelite.api.Client;
import net.runelite.api.ChatMessageType;
import net.runelite.api.GameState;
import net.runelite.api.NPC;
import net.runelite.api.NpcID;
import net.runelite.api.Player;
import net.runelite.api.events.ActorDeath;
import net.runelite.api.events.GameStateChanged;

import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Slf4j
@PluginDescriptor(
		name = "Union Ting"
)
public class ExamplePlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private ExampleConfig config;

	@Inject
	private ConfigManager configManager;

	// ---- Killcount persistence keys ----
	private static final String CONFIG_GROUP = "unionting";
	private static final String CONFIG_KEY_PREFIX = "kc_";

	// ---- Which NPCs to track (IDs are best) ----
	// Add/remove IDs here. Use NpcID constants where possible.
	private static final Set<Integer> TRACKED_NPC_IDS = new HashSet<Integer>()
	{{
		add(NpcID.GOBLIN);
		add(NpcID.GOBLIN_3029);
		add(NpcID.GOBLIN_3030);
		add(NpcID.GOBLIN_3031);
		add(NpcID.GOBLIN_3032);
		// add(NpcID.GOBLIN_XXXX); // add variants you care about
	}};

	// In-memory counts: npcId -> killcount
	private final Map<Integer, Integer> killCountsById = new HashMap<>();

	@Override
	protected void startUp()
	{
		log.debug("Union Ting started!");

		// Load saved counts for tracked NPC IDs
		for (int id : TRACKED_NPC_IDS)
		{
			Integer saved = configManager.getConfiguration(CONFIG_GROUP, CONFIG_KEY_PREFIX + id, int.class);
			if (saved != null)
			{
				killCountsById.put(id, saved);
			}
		}
	}

	@Override
	protected void shutDown()
	{
		log.debug("Union Ting stopped!");
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged e)
	{
		if (e.getGameState() == GameState.LOGGED_IN)
		{
			client.addChatMessage(
					ChatMessageType.GAMEMESSAGE,
					"",
					"Example says " + config.greeting(),
					null
			);
		}
	}

	@Subscribe
	public void onActorDeath(ActorDeath event)
	{
		if (!(event.getActor() instanceof NPC))
		{
			return;
		}

		NPC npc = (NPC) event.getActor();
		int id = npc.getId();

		// Only track specific NPC IDs
		if (!TRACKED_NPC_IDS.contains(id))
		{
			return;
		}

		// Best-effort attribution: only count if it was interacting with you when it died
		if (!isLikelyMyKill(npc))
		{
			return;
		}

		int newCount = killCountsById.getOrDefault(id, 0) + 1;
		killCountsById.put(id, newCount);

		// Persist it (survives restart)
		configManager.setConfiguration(CONFIG_GROUP, CONFIG_KEY_PREFIX + id, newCount);

		String name = npc.getName() != null ? npc.getName() : ("NPC " + id);
		client.addChatMessage(
				ChatMessageType.GAMEMESSAGE,
				"",
				"Killcount: " + name + " (id " + id + ") = " + newCount,
				null
		);
	}

	private boolean isLikelyMyKill(NPC npc)
	{
		Player me = client.getLocalPlayer();
		return me != null && npc.getInteracting() == me;
	}

	@Provides
	ExampleConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(ExampleConfig.class);
	}
}