package com.cappleapple.needsnotnecessities.gametest;

import com.cappleapple.needsnotnecessities.NeedsNotNecessities;
import com.cappleapple.needsnotnecessities.config.ServerConfig;
import com.cappleapple.needsnotnecessities.data.ModAttachments;
import com.cappleapple.needsnotnecessities.data.PlayerSurvivalData;
import com.cappleapple.needsnotnecessities.survival.state.StateDefinitionManager;
import com.cappleapple.needsnotnecessities.survival.state.StateTimeline;
import com.cappleapple.needsnotnecessities.survival.state.StateTrackService;
import com.cappleapple.needsnotnecessities.survival.state.SurvivalStateIds;
import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.stats.Stats;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.PhantomSpawner;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerSpawnPhantomsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerSpawnPhantomsEvent.Result;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(NeedsNotNecessities.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PhantomSpawnGameTests {
    private static final String EMPTY_TEMPLATE = "bastion/mobs/empty";

    private PhantomSpawnGameTests() {
    }

    @GameTest(templateNamespace = "minecraft", template = EMPTY_TEMPLATE)
    public static void lowestRestSpawnsPhantomsAndRecoveryStopsNewSpawns(GameTestHelper helper) {
        try (Fixture fixture = new Fixture(helper)) {
            fixture.setInsomnia(0);
            // Even the very start of Exhausted qualifies, before the meter reaches zero.
            double nextStage = fixture.timeline.rangeOf(fixture.timeline.states().get(1).id()).startHours();
            fixture.setRest(Math.nextDown(nextStage));
            helper.assertTrue(fixture.attemptSpawns(true) > 0,
                    "Exhausted players with no vanilla insomnia must spawn actual phantoms");
            helper.assertTrue(!fixture.level.getEntitiesOfClass(Phantom.class, fixture.area).isEmpty(),
                    "The spawner must add phantom entities to the server level");
            helper.assertTrue(fixture.insomnia() == 0, "Rest must not rewrite the insomnia statistic");

            fixture.setInsomnia(240_000);
            for (var state : fixture.timeline.states().subList(1, fixture.timeline.states().size())) {
                fixture.setRest(fixture.timeline.rangeOf(state.id()).startHours());
                helper.assertTrue(fixture.attemptSpawns(true) == 0,
                        "Rest stage " + state.id() + " must prevent new phantom spawns even with vanilla insomnia");
            }
            fixture.setRest(0.0D);
            helper.assertTrue(fixture.attemptSpawns(true) > 0, "Returning to Exhausted must enable spawning again");
            helper.assertTrue(fixture.insomnia() == 240_000, "The existing insomnia statistic must remain intact");
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = "minecraft", template = EMPTY_TEMPLATE)
    public static void disablingRestRestoresVanillaInsomnia(GameTestHelper helper) {
        try (Fixture fixture = new Fixture(helper)) {
            fixture.restEnabled.set(false);
            fixture.setRest(fixture.timeline.bestPosition());
            fixture.setInsomnia(240_000);
            helper.assertTrue(fixture.attemptSpawns(true) > 0,
                    "Disabling Rest must allow vanilla insomnia even at maximum Rest");
            fixture.setRest(0.0D);
            fixture.setInsomnia(0);
            helper.assertTrue(fixture.attemptSpawns(true) == 0,
                    "Disabling Rest must retain vanilla's minimum insomnia requirement");
            fixture.restEnabled.set(true);
            helper.assertTrue(fixture.attemptSpawns(true) > 0, "Re-enabling Rest must use Exhausted immediately");
            helper.assertTrue(fixture.insomnia() == 0, "Toggling Rest must not alter saved insomnia");
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = "minecraft", template = EMPTY_TEMPLATE)
    public static void restSpawnsRetainVanillaEnvironmentalRulesAndInterval(GameTestHelper helper) {
        try (Fixture fixture = new Fixture(helper)) {
            fixture.setRest(0.0D);
            fixture.setInsomnia(0);
            helper.assertTrue(fixture.attemptSpawns(true) > 0, "The nighttime fixture must permit phantom spawning");
            PhantomSpawner timedSpawner = new PhantomSpawner();
            timedSpawner.tick(fixture.level, true, true);
            helper.assertTrue(timedSpawner.tick(fixture.level, true, true) == 0,
                    "The vanilla spawn interval must prevent consecutive attempts");

            fixture.level.getGameRules().getRule(GameRules.RULE_DOINSOMNIA).set(false, fixture.player.server);
            helper.assertTrue(fixture.attemptSpawns(true) == 0, "doInsomnia=false must still block spawning");
            fixture.level.getGameRules().getRule(GameRules.RULE_DOINSOMNIA).set(true, fixture.player.server);
            helper.assertTrue(fixture.attemptSpawns(false) == 0, "Disabled monster spawning must still block spawning");

            fixture.setTime(6_000L);
            helper.assertTrue(fixture.attemptSpawns(true) == 0, "Daylight must still block phantom spawning");
            fixture.setTime(18_000L);

            BlockPos roof = fixture.player.blockPosition().above(2);
            var originalRoof = fixture.level.getBlockState(roof);
            try {
                fixture.level.setBlockAndUpdate(roof, Blocks.STONE.defaultBlockState());
                fixture.flushLighting(roof);
                helper.assertTrue(!fixture.level.canSeeSky(fixture.player.blockPosition()),
                        "The roof fixture must finish updating skylight before testing spawns");
                helper.assertTrue(fixture.attemptSpawns(true) == 0, "A roof must still block phantom spawning");
            } finally {
                fixture.level.setBlockAndUpdate(roof, originalRoof);
                fixture.flushLighting(roof);
            }

            double originalY = fixture.player.getY();
            fixture.player.setPos(fixture.player.getX(), fixture.level.getSeaLevel() - 1, fixture.player.getZ());
            helper.assertTrue(fixture.attemptSpawns(true) == 0, "Being below sea level must still block phantom spawning");
            fixture.player.setPos(fixture.player.getX(), originalY, fixture.player.getZ());

            fixture.player.server.setDifficulty(Difficulty.PEACEFUL, true);
            helper.assertTrue(fixture.attemptSpawns(true) == 0, "Peaceful local difficulty must still block spawning");
            fixture.player.server.setDifficulty(Difficulty.HARD, true);
            fixture.player.setGameMode(GameType.CREATIVE);
            helper.assertTrue(fixture.attemptSpawns(true) == 0, "Creative players must not trigger Rest phantoms");
            fixture.player.setGameMode(GameType.SPECTATOR);
            helper.assertTrue(fixture.attemptSpawns(true) == 0, "Spectators must not trigger Rest phantoms");
            fixture.player.setGameMode(GameType.ADVENTURE);
            helper.assertTrue(fixture.attemptSpawns(true) > 0, "Adventure players must use Rest-based spawning");
            fixture.player.setHealth(0.0F);
            helper.assertTrue(fixture.attemptSpawns(true) == 0, "Dead players must not trigger Rest phantoms");
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = "minecraft", template = EMPTY_TEMPLATE)
    public static void restSpawnHookPreservesOtherDenialsAndInitializesMissingRest(GameTestHelper helper) {
        try (Fixture fixture = new Fixture(helper)) {
            fixture.setRest(0.0D);
            PlayerSpawnPhantomsEvent denied = new PlayerSpawnPhantomsEvent(fixture.player, 3);
            denied.setResult(Result.DENY);
            NeoForge.EVENT_BUS.post(denied);
            helper.assertTrue(denied.getResult() == Result.DENY, "An existing mod denial must be preserved");
            helper.assertTrue(denied.getPhantomsToSpawn() == 3, "The configured phantom group size must be preserved");

            fixture.player.setData(ModAttachments.PLAYER_SURVIVAL, new PlayerSurvivalData());
            PlayerSpawnPhantomsEvent uninitialized = NeoForge.EVENT_BUS.post(new PlayerSpawnPhantomsEvent(fixture.player, 2));
            helper.assertTrue(uninitialized.getResult() == Result.DENY,
                    "Missing Rest data must initialize to Neutral rather than count as Exhausted");
            helper.assertTrue(fixture.player.getData(ModAttachments.PLAYER_SURVIVAL).statePosition(SurvivalStateIds.REST)
                            == fixture.timeline.neutralPosition(),
                    "The spawn hook must use the same initial Rest state as normal player initialization");

            fixture.restEnabled.set(false);
            PlayerSpawnPhantomsEvent vanilla = NeoForge.EVENT_BUS.post(new PlayerSpawnPhantomsEvent(fixture.player, 2));
            helper.assertTrue(vanilla.getResult() == Result.DEFAULT, "Disabled Rest must leave the event untouched");
        }
        helper.succeed();
    }

    private static final class Fixture implements AutoCloseable {
        private final ServerLevel level;
        private final ServerPlayer player;
        private final EmbeddedChannel channel;
        private final StateTimeline timeline = StateDefinitionManager.INSTANCE.require(SurvivalStateIds.REST);
        private final ModConfigSpec.BooleanValue restEnabled = ServerConfig.SPEC.getValues().get(List.of("modules", "rest"));
        private final boolean previousRestEnabled = restEnabled.getAsBoolean();
        private final Difficulty previousDifficulty;
        private final long previousDayTime;
        private final boolean previousInsomniaRule;
        private final AABB area;

        private Fixture(GameTestHelper helper) {
            level = helper.getLevel();
            previousDifficulty = level.getDifficulty();
            previousDayTime = level.getDayTime();
            previousInsomniaRule = level.getGameRules().getBoolean(GameRules.RULE_DOINSOMNIA);
            restEnabled.set(true);
            level.getServer().setDifficulty(Difficulty.HARD, true);
            level.getGameRules().getRule(GameRules.RULE_DOINSOMNIA).set(true, level.getServer());
            setTime(18_000L);

            UUID id = UUID.randomUUID();
            player = new ServerPlayer(level.getServer(), level,
                    new GameProfile(id, "phantom_" + id.toString().substring(0, 8)), ClientInformation.createDefault());
            Connection connection = new Connection(PacketFlow.SERVERBOUND);
            channel = new EmbeddedChannel(connection);
            new ServerGamePacketListenerImpl(player.server, connection, player,
                    CommonListenerCookie.createInitial(player.getGameProfile(), false)) {
                @Override
                public void send(Packet<?> packet, PacketSendListener listener) {
                }
            };
            BlockPos origin = helper.absolutePos(BlockPos.ZERO);
            player.moveTo(origin.getX() + 0.5D, Math.max(origin.getY() + 4, level.getSeaLevel() + 16), origin.getZ() + 0.5D);
            player.setGameMode(GameType.SURVIVAL);
            StateTrackService.initializeMissingTracks(player.getData(ModAttachments.PLAYER_SURVIVAL));
            area = player.getBoundingBox().inflate(48.0D);
            level.addNewPlayer(player);
            helper.assertTrue(level.players().contains(player), "The test player must be visible to PhantomSpawner");
            helper.assertTrue(level.canSeeSky(player.blockPosition()), "The fixture must start under open sky");
        }

        private void setRest(double position) {
            player.getData(ModAttachments.PLAYER_SURVIVAL).setStatePosition(SurvivalStateIds.REST, position);
        }

        private void setInsomnia(int ticks) {
            player.getStats().setValue(player, Stats.CUSTOM.get(Stats.TIME_SINCE_REST), ticks);
        }

        private int insomnia() {
            return player.getStats().getValue(Stats.CUSTOM.get(Stats.TIME_SINCE_REST));
        }

        private void setTime(long ticks) {
            level.setDayTime(ticks);
            level.updateSkyBrightness();
        }

        private void flushLighting(BlockPos position) {
            var lightEngine = level.getChunkSource().getLightEngine();
            var pending = lightEngine.waitForPendingTasks(position.getX() >> 4, position.getZ() >> 4);
            level.getServer().managedBlock(() -> {
                lightEngine.tryScheduleUpdate();
                return pending.isDone();
            });
            pending.join();
        }

        private int attemptSpawns(boolean spawnMonsters) {
            // Fixed seeds cover the vanilla random difficulty/position checks without waiting minutes.
            for (int seed = 0; seed < 32; seed++) {
                level.random.setSeed(seed);
                int spawned = new PhantomSpawner().tick(level, spawnMonsters, true);
                if (spawned > 0) {
                    return spawned;
                }
            }
            return 0;
        }

        @Override
        public void close() {
            try {
                level.getEntitiesOfClass(Phantom.class, area).forEach(Entity::discard);
                level.removePlayerImmediately(player, Entity.RemovalReason.DISCARDED);
            } finally {
                restEnabled.set(previousRestEnabled);
                level.getServer().setDifficulty(previousDifficulty, true);
                level.getGameRules().getRule(GameRules.RULE_DOINSOMNIA).set(previousInsomniaRule, level.getServer());
                setTime(previousDayTime);
                channel.finishAndReleaseAll();
            }
        }
    }
}
