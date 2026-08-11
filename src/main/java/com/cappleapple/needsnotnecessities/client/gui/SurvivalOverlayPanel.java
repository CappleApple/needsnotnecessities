package com.cappleapple.needsnotnecessities.client.gui;

import com.cappleapple.needsnotnecessities.NeedsNotNecessities;
import com.cappleapple.needsnotnecessities.client.ModifierTextColor;
import com.cappleapple.needsnotnecessities.client.ClientSurvivalCache;
import com.cappleapple.needsnotnecessities.config.ClientConfig;
import com.cappleapple.needsnotnecessities.survival.SurvivalModule;
import com.cappleapple.panelsnotscreens.api.panel.DockSide;
import com.cappleapple.panelsnotscreens.api.panel.Panel;
import com.cappleapple.panelsnotscreens.api.panel.PanelBounds;
import com.cappleapple.panelsnotscreens.api.panel.PanelBuilder;
import com.cappleapple.panelsnotscreens.api.panel.PanelContent;
import com.cappleapple.panelsnotscreens.api.panel.PanelContext;
import com.cappleapple.panelsnotscreens.api.panel.PanelManager;
import com.cappleapple.panelsnotscreens.api.panel.PanelState;
import com.cappleapple.panelsnotscreens.api.panel.PanelStateStore;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.ScreenEvent;

public final class SurvivalOverlayPanel {
    private static final ResourceLocation PANEL_ID = NeedsNotNecessities.id("survival_status");
    private static final ResourceLocation DEFAULT_PANEL_ICON = ResourceLocation.withDefaultNamespace("item/carrot");
    private static final int WIDTH = 174;
    private static final int LINE_HEIGHT = 13;
    private static final PanelStateStore STATE_STORE = new ConfigPanelStateStore();

    private static InventoryScreen activeScreen;
    private static PanelManager manager;
    private static Panel panel;
    private static boolean pointerCaptured;
    private static List<Component> hoverTooltip = List.of();
    private static int hoverX;
    private static int hoverY;

    private SurvivalOverlayPanel() {
    }

    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof InventoryScreen screen)) {
            clearScreenState();
            return;
        }
        activeScreen = screen;
        pointerCaptured = false;
        int initialX = screen.width / 2 + 90 + ClientConfig.INSTANCE.defaultPanelX.getAsInt();
        int initialY = screen.height / 2 - 84 + ClientConfig.INSTANCE.defaultPanelY.getAsInt();
        panel = PanelBuilder.create(PANEL_ID)
                .size(WIDTH, 96)
                .minimumSize(WIDTH, 32)
                .maximumSize(WIDTH, 512)
                .position(initialX, initialY)
                .handleSize(18, 18)
                .contentPadding(5)
                .panelGap(2)
                .screenMargin(2)
                .dockSide(DockSide.RIGHT)
                .automaticDocking(true)
                .expanded(false)
                .stateStore(STATE_STORE)
                .proceduralStyle()
                .content(new StatusContent())
                .build();
        manager = new PanelManager();
        manager.add(panel);
    }

    public static void onRender(ScreenEvent.Render.Post event) {
        if (manager == null || panel == null || event.getScreen() != activeScreen) {
            return;
        }
        List<StatusLine> lines = statusLines(Minecraft.getInstance().options.advancedItemTooltips);
        panel.setPanelSize(WIDTH, 10 + Math.max(1, lines.size()) * LINE_HEIGHT);
        hoverTooltip = List.of();
        manager.render(
                event.getScreen(),
                event.getGuiGraphics(),
                event.getMouseX(),
                event.getMouseY(),
                0.0F);
        if (!hoverTooltip.isEmpty()) {
            event.getGuiGraphics().renderComponentTooltip(
                    Minecraft.getInstance().font, hoverTooltip, hoverX, hoverY);
        }
    }

    public static void onMousePressed(ScreenEvent.MouseButtonPressed.Pre event) {
        if (manager != null
                && event.getScreen() == activeScreen
                && manager.mouseClicked(event.getScreen(), event.getMouseX(), event.getMouseY(), event.getButton())) {
            pointerCaptured = true;
            event.setCanceled(true);
        }
    }

    public static void onMouseDragged(ScreenEvent.MouseDragged.Pre event) {
        if (pointerCaptured && event.getScreen() == activeScreen) {
            event.setCanceled(true);
        }
    }

    public static void onMouseReleased(ScreenEvent.MouseButtonReleased.Pre event) {
        if (manager != null
                && event.getScreen() == activeScreen
                && manager.mouseReleased(event.getScreen(), event.getMouseX(), event.getMouseY(), event.getButton())) {
            event.setCanceled(true);
        }
        pointerCaptured = false;
    }

    public static void onMouseScrolled(ScreenEvent.MouseScrolled.Pre event) {
        if (manager != null
                && event.getScreen() == activeScreen
                && manager.mouseScrolled(
                        event.getScreen(), event.getMouseX(), event.getMouseY(), event.getScrollDeltaY())) {
            event.setCanceled(true);
        }
    }

    private static void clearScreenState() {
        activeScreen = null;
        manager = null;
        panel = null;
        pointerCaptured = false;
        hoverTooltip = List.of();
    }

    private static List<StatusLine> statusLines(boolean advanced) {
        CompoundTag snapshot = ClientSurvivalCache.snapshot();
        List<StatusLine> lines = new ArrayList<>();
        for (CompoundTag state : ClientSurvivalCache.list("states")) {
            List<Component> hover = new ArrayList<>();
            if (!state.getString("description").isBlank()) {
                hover.add(Component.literal(state.getString("description")).withStyle(ChatFormatting.GRAY));
            }
            List<Component> effects = modifierLines(state.getList("modifiers", Tag.TAG_COMPOUND));
            if (effects.isEmpty()) {
                hover.add(Component.literal("No effects").withStyle(ChatFormatting.GRAY));
            } else {
                hover.add(Component.literal("Effects").withStyle(ChatFormatting.GOLD));
                hover.addAll(effects);
            }
            if (advanced) {
                hover.add(debugLine(String.format(Locale.ROOT, "Position: %.3f biological hours", state.getDouble("position_hours"))));
                hover.add(debugLine(String.format(Locale.ROOT, "Tier range: %.3f - %.3f", state.getDouble("range_start"), state.getDouble("range_end"))));
                hover.add(debugLine(String.format(Locale.ROOT, "Until worse tier: %.3f hours", state.getDouble("hours_to_worse_state"))));
                if (state.getString("label").equals("Hunger")) {
                    hover.add(debugLine(String.format(Locale.ROOT, "Conversion: %.3f h/nutrition + %.3f h/saturation",
                            snapshot.getDouble("hours_per_hunger_point"), snapshot.getDouble("hours_per_saturation_point"))));
                }
            }
            lines.add(new StatusLine(
                    state.getString("label") + ": " + state.getString("state_name"),
                    0xE8E8E8,
                    List.copyOf(hover)));
        }

        if (ClientSurvivalCache.enabled(SurvivalModule.COMFORT)) {
            CompoundTag comfort = snapshot.getCompound("comfort");
            List<Component> hover = new ArrayList<>();
            hover.add(Component.literal("Remaining: " + formatSeconds(comfort.getLong("retention_ticks") / 20.0D)));
            hover.addAll(modifierLines(comfort.getList("modifiers", Tag.TAG_COMPOUND)));
            if (advanced) {
                hover.add(debugLine("Scan radius: " + comfort.getInt("radius")));
                hover.add(debugLine(String.format(Locale.ROOT, "Diminishing factor: %.3f", comfort.getDouble("diminishing_factor"))));
            }
            lines.add(new StatusLine(
                    "Comfort: " + Math.round(comfort.getDouble("value")),
                    0x9FE3B0,
                    List.copyOf(hover)));
        }

        if (ClientSurvivalCache.enabled(SurvivalModule.ACTIVE_MEAL)) {
            if (snapshot.contains("active_meal", Tag.TAG_COMPOUND)) {
                CompoundTag meal = snapshot.getCompound("active_meal");
                double realSeconds = meal.getDouble("remaining_biological_hours") / 24.0D
                        * snapshot.getDouble("day_length_minutes") * 60.0D;
                List<Component> hover = new ArrayList<>();
                hover.add(Component.literal("Remaining: " + formatSeconds(realSeconds)));
                hover.addAll(modifierLines(meal.getList("modifiers", Tag.TAG_COMPOUND)));
                if (advanced) {
                    hover.add(debugLine(String.format(Locale.ROOT, "Meal score: %.3f", meal.getDouble("score"))));
                    hover.add(debugLine("Recipe complexity: " + meal.getInt("recipe_complexity")));
                    hover.add(debugLine("Traits: " + traitSummary(meal)));
                    hover.add(debugLine(String.format(Locale.ROOT, "Quality value: %.3f", meal.getDouble("quality_value"))));
                }
                lines.add(new StatusLine(
                        "Active Meal: " + meal.getString("display_name"),
                        0xFFE09A,
                        List.copyOf(hover)));
            } else {
                lines.add(new StatusLine("Active Meal: None", 0xA0A0A0, List.of()));
            }
        }
        return List.copyOf(lines);
    }

    private static List<Component> modifierLines(net.minecraft.nbt.ListTag modifiers) {
        List<Component> result = new ArrayList<>();
        for (Tag element : modifiers) {
            CompoundTag modifier = (CompoundTag) element;
            result.add(Component.literal(formatModifier(modifier))
                    .withStyle(ModifierTextColor.forAmount(modifier.getDouble("amount"))));
        }
        return result;
    }

    private static String formatModifier(CompoundTag modifier) {
        String target = modifier.getString("target");
        String path = target.contains(":") ? target.substring(target.indexOf(':') + 1) : target;
        path = titleCase(path.replace("generic.", "").replace("player.", "").replace('_', ' '));
        double amount = modifier.getDouble("amount");
        String operation = modifier.getString("operation");
        if (operation.startsWith("MULTIPLY") || target.endsWith("passive_regeneration")) {
            return String.format(Locale.ROOT, "%+.1f%% %s", amount * 100.0D, path);
        }
        return String.format(Locale.ROOT, "%+.2f %s", amount, path);
    }

    private static String traitSummary(CompoundTag meal) {
        List<String> traits = new ArrayList<>();
        for (Tag element : meal.getList("traits", Tag.TAG_COMPOUND)) {
            CompoundTag trait = (CompoundTag) element;
            traits.add(String.format(Locale.ROOT, "%s %.2f", trait.getString("trait"), trait.getDouble("value")));
        }
        return traits.isEmpty() ? "none" : String.join(", ", traits);
    }

    private static Component debugLine(String text) {
        return Component.literal(text).withStyle(ChatFormatting.DARK_GRAY);
    }

    private static String formatSeconds(double seconds) {
        long total = Math.max(0L, Math.round(seconds));
        return String.format(Locale.ROOT, "%02d:%02d", total / 60L, total % 60L);
    }

    private static String titleCase(String value) {
        StringBuilder result = new StringBuilder();
        for (String word : value.split(" ")) {
            if (!word.isBlank()) {
                if (!result.isEmpty()) {
                    result.append(' ');
                }
                result.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
            }
        }
        return result.toString();
    }

    private static final class StatusContent implements PanelContent {
        @Override
        public void render(PanelContext context, GuiGraphics graphics, int mouseX, int mouseY) {
            Minecraft minecraft = Minecraft.getInstance();
            PanelBounds content = context.layout().content();
            if (!ClientSurvivalCache.available()) {
                graphics.drawString(
                        minecraft.font,
                        "Waiting for server data...",
                        content.x(),
                        content.y(),
                        0xA0A0A0,
                        false);
                return;
            }
            int lineY = content.y();
            for (StatusLine line : statusLines(minecraft.options.advancedItemTooltips)) {
                graphics.drawString(minecraft.font, line.text(), content.x(), lineY, line.color(), false);
                if (new PanelBounds(content.x(), lineY - 1, content.width(), LINE_HEIGHT).contains(mouseX, mouseY)
                        && !line.hover().isEmpty()) {
                    hoverTooltip = line.hover();
                    hoverX = mouseX;
                    hoverY = mouseY;
                }
                lineY += LINE_HEIGHT;
            }
        }

        @Override
        public void renderHandle(PanelContext context, GuiGraphics graphics, int mouseX, int mouseY) {
            PanelBounds handle = context.layout().handle();
            ResourceLocation icon = ResourceLocation.tryParse(ClientConfig.INSTANCE.panelIconSprite.get());
            if (icon == null) {
                icon = DEFAULT_PANEL_ICON;
            }
            int iconSize = 14;
            int iconX = handle.x() + (handle.width() - iconSize) / 2;
            int iconY = handle.y() + (handle.height() - iconSize) / 2;
            if (isDirectTexture(icon)) {
                graphics.blit(textureLocation(icon), iconX, iconY, 0.0F, 0.0F, iconSize, iconSize, 16, 16);
            } else {
                graphics.blitSprite(icon, iconX, iconY, iconSize, iconSize);
            }
        }

        private static boolean isDirectTexture(ResourceLocation icon) {
            String path = icon.getPath();
            return path.startsWith("item/")
                    || path.startsWith("block/")
                    || path.startsWith("textures/")
                    || path.endsWith(".png");
        }

        private static ResourceLocation textureLocation(ResourceLocation icon) {
            String path = icon.getPath();
            if (!path.startsWith("textures/")) {
                path = "textures/" + path;
            }
            if (!path.endsWith(".png")) {
                path += ".png";
            }
            return ResourceLocation.fromNamespaceAndPath(icon.getNamespace(), path);
        }
    }

    private static final class ConfigPanelStateStore implements PanelStateStore {
        @Override
        public Optional<PanelState> load(ResourceLocation panelId) {
            ClientConfig config = ClientConfig.INSTANCE;
            if (!PANEL_ID.equals(panelId)
                    || !config.rememberPanelPosition.getAsBoolean()
                    || !config.panelStateSaved.getAsBoolean()) {
                return Optional.empty();
            }
            DockSide side;
            try {
                side = DockSide.valueOf(config.panelDockSide.get().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException exception) {
                side = DockSide.RIGHT;
            }
            return Optional.of(new PanelState(
                    config.panelHandleX.getAsInt(),
                    config.panelHandleY.getAsInt(),
                    side,
                    config.panelExpanded.getAsBoolean(),
                    true));
        }

        @Override
        public void save(ResourceLocation panelId, PanelState state) {
            ClientConfig config = ClientConfig.INSTANCE;
            if (!PANEL_ID.equals(panelId) || !config.rememberPanelPosition.getAsBoolean()) {
                return;
            }
            config.panelStateSaved.set(true);
            config.panelHandleX.set(state.handleX());
            config.panelHandleY.set(state.handleY());
            config.panelDockSide.set(state.dockSide().name());
            config.panelExpanded.set(state.expanded());
            ClientConfig.SPEC.save();
        }
    }

    private record StatusLine(String text, int color, List<Component> hover) {
    }
}
