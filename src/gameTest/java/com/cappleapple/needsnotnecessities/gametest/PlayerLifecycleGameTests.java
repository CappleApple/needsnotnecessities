package com.cappleapple.needsnotnecessities.gametest;

import com.cappleapple.needsnotnecessities.NeedsNotNecessities;
import com.cappleapple.needsnotnecessities.config.ServerConfig;
import com.cappleapple.needsnotnecessities.data.ModAttachments;
import com.cappleapple.needsnotnecessities.survival.state.StateDefinitionManager;
import com.cappleapple.needsnotnecessities.survival.state.StateTrackService;
import com.cappleapple.needsnotnecessities.survival.state.SurvivalStateIds;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ClientboundSetHealthPacket;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(NeedsNotNecessities.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PlayerLifecycleGameTests {
    private static final String EMPTY_TEMPLATE = "bastion/mobs/empty";

    private PlayerLifecycleGameTests() {
    }

    @GameTest(templateNamespace = "minecraft", template = EMPTY_TEMPLATE)
    public static void resetDefaultsToCommandSender(GameTestHelper helper) {
        try (TestPlayer sender = new TestPlayer(helper)) {
            for (String root : new String[]{"nnn", NeedsNotNecessities.MOD_ID}) {
                var data = sender.player.getData(ModAttachments.PLAYER_SURVIVAL);
                data.setStatePosition(SurvivalStateIds.HUNGER, 0.0D);
                data.setStatePosition(SurvivalStateIds.THIRST, 0.0D);
                data.setStatePosition(SurvivalStateIds.REST, 0.0D);
                execute(helper, sender.source(), root + " reset");
                assertNeutral(helper, sender.player);
            }
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = "minecraft", template = EMPTY_TEMPLATE)
    public static void resetStillAcceptsAnotherPlayer(GameTestHelper helper) {
        try (TestPlayer sender = new TestPlayer(helper); TestPlayer target = new TestPlayer(helper)) {
            sender.player.getData(ModAttachments.PLAYER_SURVIVAL).setStatePosition(SurvivalStateIds.HUNGER, 0.0D);
            target.player.getData(ModAttachments.PLAYER_SURVIVAL).setStatePosition(SurvivalStateIds.HUNGER, 0.0D);
            execute(helper, sender.source(), "nnn reset " + target.player.getGameProfile().getName());
            assertNeutral(helper, target.player);
            assertClose(helper, 0.0D, sender.player.getData(ModAttachments.PLAYER_SURVIVAL).statePosition(SurvivalStateIds.HUNGER),
                    "An explicit target must not reset the sender");
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = "minecraft", template = EMPTY_TEMPLATE)
    public static void consoleResetStillRequiresATarget(GameTestHelper helper) {
        var dispatcher = helper.getLevel().getServer().getCommands().getDispatcher();
        var source = helper.getLevel().getServer().createCommandSourceStack().withSuppressedOutput();
        try {
            dispatcher.execute("nnn reset", source);
            helper.fail("A console source without a target must not reset a player");
        } catch (CommandSyntaxException expected) {
            helper.assertTrue(expected.getType() == CommandSourceStack.ERROR_NOT_PLAYER,
                    "A bare reset must resolve the sender, not fail as an incomplete command");
        }
        try (TestPlayer target = new TestPlayer(helper)) {
            target.player.getData(ModAttachments.PLAYER_SURVIVAL).setStatePosition(SurvivalStateIds.HUNGER, 0.0D);
            execute(helper, source, "nnn reset " + target.player.getGameProfile().getName());
            assertNeutral(helper, target.player);
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = "minecraft", template = EMPTY_TEMPLATE)
    public static void deathHealthAppliesAfterRespawnSetupAndOnlyOnce(GameTestHelper helper) {
        try (TestPlayer fixture = new TestPlayer(helper)) {
            ServerPlayer original = fixture.player;
            original.getAttribute(Attributes.MAX_HEALTH).setBaseValue(40.0D);
            original.setHealth(0.0F);
            // Emulate another mod finalizing max health and filling health after our respawn listener.
            Consumer<PlayerEvent.PlayerRespawnEvent> lateRespawnHandler = event -> {
                if (event.getEntity().getUUID().equals(original.getUUID())) {
                    event.getEntity().getAttribute(Attributes.MAX_HEALTH).addTransientModifier(new AttributeModifier(
                            NeedsNotNecessities.id("gametest_respawn_bonus"), 0.5D,
                            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
                    event.getEntity().setHealth(event.getEntity().getMaxHealth());
                }
            };
            NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, lateRespawnHandler);
            try {
                fixture.respawn(false);
            } finally {
                NeoForge.EVENT_BUS.unregister(lateRespawnHandler);
            }
            assertClose(helper, 0.0D, original.getHealth(), "The dead original must not be healed");
            helper.assertTrue(fixture.player.getData(ModAttachments.PLAYER_SURVIVAL).pendingDeathHealthReset(),
                    "Health must remain queued until the replacement player's first live tick");
            fixture.player.doTick();
            double expected = fixture.player.getMaxHealth() * ServerConfig.INSTANCE.respawnHealthPercentage.getAsDouble();
            assertClose(helper, expected, fixture.player.getHealth(), "Respawn health must use the finalized max health");
            var healthPacket = fixture.sentPackets.stream().filter(ClientboundSetHealthPacket.class::isInstance)
                    .map(ClientboundSetHealthPacket.class::cast).reduce((first, last) -> last).orElseThrow();
            assertClose(helper, expected, healthPacket.getHealth(), "The client health packet must contain the respawn percentage");
            helper.assertTrue(!fixture.player.getData(ModAttachments.PLAYER_SURVIVAL).pendingDeathHealthReset(),
                    "The pending health adjustment must be consumed");
            fixture.player.setHealth(fixture.player.getHealth() + 2.0F);
            fixture.player.tickCount = 1;
            fixture.player.doTick();
            assertClose(helper, expected + 2.0D, fixture.player.getHealth(), "Later healing must not be overwritten");
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = "minecraft", template = EMPTY_TEMPLATE)
    public static void deathHealthDoesNotRunDuringCloneOrOnDeadPlayers(GameTestHelper helper) {
        try (TestPlayer original = new TestPlayer(helper); TestPlayer replacement = new TestPlayer(helper)) {
            original.player.setHealth(0.0F);
            NeoForge.EVENT_BUS.post(new PlayerEvent.Clone(replacement.player, original.player, true));
            helper.assertTrue(!replacement.player.getData(ModAttachments.PLAYER_SURVIVAL).pendingDeathHealthReset(),
                    "Cloning alone must not queue the health adjustment before respawn");
            NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerRespawnEvent(replacement.player, false));
            replacement.player.setHealth(0.0F);
            replacement.player.doTick();
            assertClose(helper, 0.0D, replacement.player.getHealth(), "A pending adjustment must never revive a dead player");
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = "minecraft", template = EMPTY_TEMPLATE)
    public static void endReturnDoesNotApplyDeathHealth(GameTestHelper helper) {
        try (TestPlayer fixture = new TestPlayer(helper)) {
            fixture.player.setHealth(7.0F);
            fixture.respawn(true);
            fixture.player.tickCount = 1;
            fixture.player.doTick();
            assertClose(helper, 7.0D, fixture.player.getHealth(), "Non-death End return must retain health");
            helper.assertTrue(!fixture.player.getData(ModAttachments.PLAYER_SURVIVAL).pendingDeathHealthReset(),
                    "End return must not queue a death penalty");
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = "minecraft", template = EMPTY_TEMPLATE)
    public static void disabledDeathPenaltiesLeaveRespawnHealthAlone(GameTestHelper helper) {
        boolean enabled = ServerConfig.INSTANCE.deathPenaltiesEnabled.getAsBoolean();
        ServerConfig.INSTANCE.deathPenaltiesEnabled.set(false);
        try (TestPlayer fixture = new TestPlayer(helper)) {
            fixture.player.setHealth(0.0F);
            fixture.respawn(false);
            float health = fixture.player.getHealth();
            fixture.player.tickCount = 1;
            fixture.player.doTick();
            assertClose(helper, health, fixture.player.getHealth(), "Disabled death penalties must not change respawn health");
            helper.assertTrue(!fixture.player.getData(ModAttachments.PLAYER_SURVIVAL).pendingDeathHealthReset(),
                    "Disabled death penalties must not queue a health adjustment");
        } finally {
            ServerConfig.INSTANCE.deathPenaltiesEnabled.set(enabled);
        }
        helper.succeed();
    }

    private static void execute(GameTestHelper helper, CommandSourceStack source, String command) {
        try {
            helper.assertTrue(helper.getLevel().getServer().getCommands().getDispatcher().execute(command, source) == 1,
                    "Command did not succeed: " + command);
        } catch (CommandSyntaxException exception) {
            helper.fail("Command failed: " + command + ": " + exception.getMessage());
        }
    }

    private static void assertNeutral(GameTestHelper helper, ServerPlayer player) {
        for (var id : new net.minecraft.resources.ResourceLocation[]{
                SurvivalStateIds.HUNGER, SurvivalStateIds.THIRST, SurvivalStateIds.REST}) {
            assertClose(helper, StateDefinitionManager.INSTANCE.require(id).neutralPosition(),
                    player.getData(ModAttachments.PLAYER_SURVIVAL).statePosition(id), "Reset did not restore " + id);
        }
    }

    private static void assertClose(GameTestHelper helper, double expected, double actual, String message) {
        helper.assertTrue(Math.abs(expected - actual) < 0.0001D, message + ": expected " + expected + ", got " + actual);
    }

    private static final class TestPlayer implements AutoCloseable {
        private final EmbeddedChannel channel;
        private final List<Packet<?>> sentPackets = new ArrayList<>();
        private ServerPlayer player;

        private TestPlayer(GameTestHelper helper) {
            UUID id = UUID.randomUUID();
            player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(),
                    new GameProfile(id, "nnn_" + id.toString().substring(0, 8)), ClientInformation.createDefault());
            Connection connection = new Connection(PacketFlow.SERVERBOUND);
            channel = new EmbeddedChannel(connection);
            new ServerGamePacketListenerImpl(player.server, connection, player,
                    CommonListenerCookie.createInitial(player.getGameProfile(), false)) {
                @Override
                public void send(Packet<?> packet, PacketSendListener listener) {
                    sentPackets.add(packet);
                }
            };
            BlockPos position = helper.absolutePos(new BlockPos(0, 2, 0));
            player.moveTo(position.getX() + 0.5D, position.getY(), position.getZ() + 0.5D);
            player.setRespawnPosition(helper.getLevel().dimension(), position, 0.0F, true, false);
            StateTrackService.initializeMissingTracks(player.getData(ModAttachments.PLAYER_SURVIVAL));
            // Use vanilla's replacement path to register the player with an in-memory connection.
            respawn(true);
        }

        private CommandSourceStack source() {
            return player.createCommandSourceStack().withPermission(2).withSuppressedOutput();
        }

        private void respawn(boolean endConquered) {
            player = player.server.getPlayerList().respawn(player, endConquered,
                    endConquered ? Entity.RemovalReason.CHANGED_DIMENSION : Entity.RemovalReason.KILLED);
            player.connection.player = player;
        }

        @Override
        public void close() {
            try {
                player.server.getPlayerList().remove(player);
            } finally {
                channel.finishAndReleaseAll();
            }
        }
    }
}
