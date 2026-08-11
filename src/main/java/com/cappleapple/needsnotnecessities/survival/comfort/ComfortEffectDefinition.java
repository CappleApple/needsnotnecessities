package com.cappleapple.needsnotnecessities.survival.comfort;

import com.cappleapple.needsnotnecessities.modifier.SurvivalModifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.resources.ResourceLocation;

public record ComfortEffectDefinition(
        ResourceLocation id,
        double threshold,
        boolean repeat,
        List<SurvivalModifier> modifiers) {

    public ComfortEffectDefinition {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(modifiers, "modifiers");
        if (!Double.isFinite(threshold) || threshold <= 0.0D || modifiers.isEmpty()) {
            throw new IllegalArgumentException("Comfort effect needs a positive threshold and modifiers: " + id);
        }
        modifiers = List.copyOf(modifiers);
    }

    public List<SurvivalModifier> modifiersAt(double comfort) {
        int applications = repeat ? (int) Math.floor(comfort / threshold) : comfort >= threshold ? 1 : 0;
        if (applications <= 0) {
            return List.of();
        }
        List<SurvivalModifier> result = new ArrayList<>(modifiers.size() * applications);
        for (int application = 0; application < applications; application++) {
            for (SurvivalModifier modifier : modifiers) {
                ResourceLocation appliedId = ResourceLocation.fromNamespaceAndPath(
                        modifier.id().getNamespace(), modifier.id().getPath() + "/" + application);
                result.add(new SurvivalModifier(appliedId, modifier.target(), modifier.amount(), modifier.operation()));
            }
        }
        return List.copyOf(result);
    }
}
