package org.union;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;

import net.runelite.api.Client;
import net.runelite.api.ChatMessageType;
import net.runelite.api.GameState;
import net.runelite.api.NPC;
import net.runelite.api.events.ActorDeath;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.InteractingChanged;

import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.api.events.HitsplatApplied;

import java.util.*;

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

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private UnionOverlay overlay;

	//API related
	private UnionApiClient unionApiClient;
	private static final int MISSION_KILL_GOAL = 4; // "kill 200 goblins" daily mission
	private static final String API_BASE_URL = "https://kjfuh3gwz5.execute-api.eu-north-1.amazonaws.com/prod";
	private boolean missionCompleted = false;

	// ---- Killcount persistence keys ----
	private static final String CONFIG_GROUP = "unionting";
	private static final String CONFIG_KEY_PREFIX = "kc_";

	// ---- Which NPCs to track (by in-game name) ----
	private static final Set<String> TRACKED_NPC_NAMES = new HashSet<>(Arrays.asList(
			"Goblin"
	));

	// In-memory counts: npcName -> killcount
	private final Map<String, Integer> killCountsByName = new HashMap<>();
	private final Set<Integer> recentlyAttackedNpcIndexes = new HashSet<>();

	@Override
	protected void startUp()
	{
		log.debug("Union Ting started!");

		//API Related
		unionApiClient = new UnionApiClient(API_BASE_URL);

		//overlay
		overlayManager.add(overlay);

		// ---- TEMPORARY for testing: wipe saved counts on every startup
		resetKillCounts();


		// Load saved counts for tracked NPC names
		for (String name : TRACKED_NPC_NAMES)
		{
			Integer saved = configManager.getConfiguration(CONFIG_GROUP, CONFIG_KEY_PREFIX + name, int.class);
			if (saved != null)
			{
				killCountsByName.put(name, saved);
			}
		}
	}

	@Override
	protected void shutDown()
	{
		unionApiClient.shutdown();
		overlayManager.remove(overlay);
		recentlyAttackedNpcIndexes.clear();
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
	public void onHitsplatApplied(HitsplatApplied event)
	{
		// Only register if YOU dealt a hitsplat on a tracked NPC
		if (!(event.getActor() instanceof NPC))
		{
			return;
		}

		NPC npc = (NPC) event.getActor();
		if (npc.getName() == null || !TRACKED_NPC_NAMES.contains(npc.getName()))
		{
			return;
		}

		// Check the hitsplat was dealt by the local player
		if (event.getHitsplat().isMine())
		{
			recentlyAttackedNpcIndexes.add(npc.getIndex());
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
		String name = npc.getName();

		// Only track NPCs whose name is in our set
		if (name == null || !TRACKED_NPC_NAMES.contains(name))
		{
			return;
		}

		// Only count if we recently attacked this NPC
		if (!isLikelyMyKill(npc))
		{
			return;
		}

		int newCount = killCountsByName.getOrDefault(name, 0) + 1;
		killCountsByName.put(name, newCount);

		// Persist it (survives restart)
		configManager.setConfiguration(CONFIG_GROUP, CONFIG_KEY_PREFIX + name, newCount);

		client.addChatMessage(
				ChatMessageType.GAMEMESSAGE,
				"",
				"Killcount: " + name + " = " + newCount,
				null
		);
		int total = killCountsByName.values().stream().mapToInt(Integer::intValue).sum();
		if (!missionCompleted && total >= MISSION_KILL_GOAL)
		{
			missionCompleted = true;
			unionApiClient.recordContribution(
					config.userId(),      // from config
					config.unionId(),     // from config
					total,                // delta_points = goblin kills
					1                     // delta_missions = 1 completed mission
			);
			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "",
					"Mission complete! Contribution sent to union.", null);
		}
	}

	public Map<String, Integer> getKillCountsByName()
	{
		return Collections.unmodifiableMap(killCountsByName);
	}

	private boolean isLikelyMyKill(NPC npc)
	{
		return recentlyAttackedNpcIndexes.remove(npc.getIndex());
	}

	public void resetKillCounts()
	{
		for (String name : TRACKED_NPC_NAMES)
		{
			configManager.unsetConfiguration(CONFIG_GROUP, CONFIG_KEY_PREFIX + name);
		}
		killCountsByName.clear();
		missionCompleted = false;
		log.debug("Kill counts and mission reset!");
	}

	@Provides
	ExampleConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(ExampleConfig.class);
	}
}