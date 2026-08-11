package com.cappleapple.needsnotnecessities.survival.health;

import com.cappleapple.needsnotnecessities.NeedsNotNecessities;
import com.cappleapple.needsnotnecessities.config.BaseHealthMode;
import com.cappleapple.needsnotnecessities.config.ServerConfig;
import com.cappleapple.needsnotnecessities.modifier.ModifierOperation;
import com.cappleapple.needsnotnecessities.modifier.SurvivalModifier;
import com.cappleapple.needsnotnecessities.survival.SurvivalModule;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

public final class BaseHealthService {
    public static final ResourceLocation MODIFIER_ID = NeedsNotNecessities.id("base_health");
    public static final ResourceLocation MAX_HEALTH_ATTRIBUTE = ResourceLocation.withDefaultNamespace("generic.max_health");

    private BaseHealthService() {
    }

    public static Optional<SurvivalModifier> configuredModifier() {
        ServerConfig config = ServerConfig.INSTANCE;
        if (!config.isEnabled(SurvivalModule.BASE_HEALTH)) {
            return Optional.empty();
        }
        return Optional.of(createModifier(config.baseHealthMode.get(), config.baseHealthAmount.getAsDouble()));
    }

    public static SurvivalModifier createModifier(BaseHealthMode mode, double configuredAmount) {
        return switch (mode) {
            case ADD -> new SurvivalModifier(
                    MODIFIER_ID,
                    MAX_HEALTH_ATTRIBUTE,
                    configuredAmount,
                    ModifierOperation.ADD);
            case MULTIPLY -> new SurvivalModifier(
                    MODIFIER_ID,
                    MAX_HEALTH_ATTRIBUTE,
                    configuredAmount - 1.0D,
                    ModifierOperation.MULTIPLY_BASE);
        };
    }
}
