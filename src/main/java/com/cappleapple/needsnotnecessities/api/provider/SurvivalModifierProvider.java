package com.cappleapple.needsnotnecessities.api.provider;

import com.cappleapple.needsnotnecessities.modifier.SurvivalModifier;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;

@FunctionalInterface
public interface SurvivalModifierProvider {
    List<SurvivalModifier> gather(ServerPlayer player);
}
