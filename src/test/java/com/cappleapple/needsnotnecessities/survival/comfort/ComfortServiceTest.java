package com.cappleapple.needsnotnecessities.survival.comfort;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.cappleapple.needsnotnecessities.modifier.ModifierOperation;
import com.cappleapple.needsnotnecessities.modifier.SurvivalModifier;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class ComfortServiceTest {
    @Test
    void identicalSourcesUseGeometricDiminishingReturns() {
        List<ComfortService.ComfortContributor> tables = new ArrayList<>();
        for (int index = 0; index < 5; index++) {
            tables.add(contributor("table", 5.0D, index));
        }
        var result = ComfortService.calculate(Map.of("table", tables), 0.5D);
        assertEquals(9.6875D, result.totalComfort(), 1.0E-10D);
    }

    @Test
    void differentComfortTypesStackNormally() {
        Map<String, List<ComfortService.ComfortContributor>> grouped = new LinkedHashMap<>();
        grouped.put("table", List.of(contributor("table", 5.0D, 0)));
        grouped.put("hearth", List.of(contributor("hearth", 8.0D, 1)));
        assertEquals(13.0D, ComfortService.calculate(grouped, 0.5D).totalComfort());
    }

    @Test
    void repeatingThresholdProducesOneStableModifierPerApplication() {
        ResourceLocation effectId = id("comfort_effect");
        SurvivalModifier base = new SurvivalModifier(id("comfort/health"), id("attribute"), 2.0D, ModifierOperation.ADD);
        ComfortEffectDefinition definition = new ComfortEffectDefinition(effectId, 10.0D, true, List.of(base));
        assertEquals(0, definition.modifiersAt(9.99D).size());
        assertEquals(3, definition.modifiersAt(35.0D).size());
        assertEquals(6.0D, definition.modifiersAt(35.0D).stream().mapToDouble(SurvivalModifier::amount).sum());
    }

    private static ComfortService.ComfortContributor contributor(String type, double amount, int coordinate) {
        return new ComfortService.ComfortContributor(
                new BlockPos(coordinate, 0, 0), id("source/" + type), type, amount, 0.0D);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("test", path);
    }
}
