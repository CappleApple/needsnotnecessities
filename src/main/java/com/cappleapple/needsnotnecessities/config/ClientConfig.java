package com.cappleapple.needsnotnecessities.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class ClientConfig {
    public static final ClientConfig INSTANCE;
    public static final ModConfigSpec SPEC;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        INSTANCE = new ClientConfig(builder);
        SPEC = builder.build();
    }

    public final ModConfigSpec.BooleanValue rememberPanelPosition;
    public final ModConfigSpec.IntValue defaultPanelX;
    public final ModConfigSpec.IntValue defaultPanelY;
    public final ModConfigSpec.BooleanValue panelStateSaved;
    public final ModConfigSpec.IntValue panelHandleX;
    public final ModConfigSpec.IntValue panelHandleY;
    public final ModConfigSpec.ConfigValue<String> panelDockSide;
    public final ModConfigSpec.BooleanValue panelExpanded;
    public final ModConfigSpec.ConfigValue<String> panelIconSprite;

    private ClientConfig(ModConfigSpec.Builder builder) {
        builder.push("inventory_overlay");
        rememberPanelPosition = builder
                .comment("Remember the draggable inventory status panel position on this client.")
                .translation("needs_not_necessities.configuration.remember_panel_position")
                .define("remember_panel_position", true);
        defaultPanelX = builder
                .comment("Default horizontal panel offset before the player moves it.")
                .translation("needs_not_necessities.configuration.default_panel_x")
                .defineInRange("default_panel_x", 0, -10000, 10000);
        defaultPanelY = builder
                .comment("Default vertical panel offset before the player moves it.")
                .translation("needs_not_necessities.configuration.default_panel_y")
                .defineInRange("default_panel_y", 0, -10000, 10000);
        panelStateSaved = builder
                .comment("Internal marker indicating that the Panels Not Screens handle has been positioned.")
                .define("panel_state_saved", false);
        panelHandleX = builder
                .comment("Saved horizontal position of the draggable panel opener/handle.")
                .translation("needs_not_necessities.configuration.panel_handle_x")
                .defineInRange("panel_handle_x", 0, -10000, 10000);
        panelHandleY = builder
                .comment("Saved vertical position of the draggable panel opener/handle.")
                .translation("needs_not_necessities.configuration.panel_handle_y")
                .defineInRange("panel_handle_y", 0, -10000, 10000);
        panelDockSide = builder
                .comment("Side from which the status panel expands: LEFT, RIGHT, TOP, or BOTTOM.")
                .translation("needs_not_necessities.configuration.panel_dock_side")
                .define("panel_dock_side", "RIGHT");
        panelExpanded = builder
                .comment("Remember whether the status panel was expanded or collapsed.")
                .translation("needs_not_necessities.configuration.panel_expanded")
                .define("panel_expanded", false);
        panelIconSprite = builder
                .comment("GUI sprite or item/block texture resource location rendered on the draggable panel handle.")
                .translation("needs_not_necessities.configuration.panel_icon_sprite")
                .define("panel_icon_sprite", "minecraft:item/carrot");
        builder.pop();
    }
}
