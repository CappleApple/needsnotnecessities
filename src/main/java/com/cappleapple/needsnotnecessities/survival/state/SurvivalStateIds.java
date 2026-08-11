package com.cappleapple.needsnotnecessities.survival.state;

import com.cappleapple.needsnotnecessities.NeedsNotNecessities;
import com.cappleapple.needsnotnecessities.survival.SurvivalModule;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

public final class SurvivalStateIds {
    public static final ResourceLocation HUNGER = NeedsNotNecessities.id("hunger");
    public static final ResourceLocation THIRST = NeedsNotNecessities.id("thirst");
    public static final ResourceLocation REST = NeedsNotNecessities.id("rest");

    private static final Map<ResourceLocation, SurvivalModule> MODULES = Map.of(
            HUNGER, SurvivalModule.HUNGER,
            THIRST, SurvivalModule.THIRST,
            REST, SurvivalModule.REST);

    private SurvivalStateIds() {
    }

    public static Optional<SurvivalModule> moduleFor(ResourceLocation stateSystem) {
        return Optional.ofNullable(MODULES.get(stateSystem));
    }
}
