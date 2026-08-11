package com.cappleapple.needsnotnecessities.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cappleapple.needsnotnecessities.modifier.ModifierOperation;
import com.cappleapple.needsnotnecessities.modifier.SurvivalModifier;
import com.cappleapple.needsnotnecessities.modifier.SurvivalModifierService;
import com.cappleapple.needsnotnecessities.survival.state.SurvivalStateDefinition;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class SurvivalSnapshotServiceTest {
    @Test
    void stateHoverEffectsIncludeGenericAndPassiveRegenerationModifiers() {
        SurvivalModifier armor = new SurvivalModifier(
                id("state/armor"),
                ResourceLocation.withDefaultNamespace("generic.armor"),
                2.0D,
                ModifierOperation.ADD);
        SurvivalStateDefinition state = new SurvivalStateDefinition(
                id("satiated"),
                "Satiated",
                4,
                8.0D,
                1.15D,
                List.of(armor),
                List.of(),
                "");

        List<SurvivalModifier> effects = SurvivalSnapshotService.stateEffectModifiers(state, true);
        assertEquals(2, effects.size());
        assertTrue(effects.stream().anyMatch(effect -> effect.target().equals(armor.target())));
        assertEquals(0.15D, effects.stream()
                .filter(effect -> effect.target().equals(SurvivalModifierService.PASSIVE_REGENERATION))
                .findFirst()
                .orElseThrow()
                .amount(), 1.0E-9D);
        assertEquals(List.of(armor), SurvivalSnapshotService.stateEffectModifiers(state, false));
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("test", path);
    }
}
