package com.cappleapple.needsnotnecessities.api.provider;

import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

@FunctionalInterface
public interface ComfortProvider {
    List<Contribution> provide(ServerPlayer player);

    record Contribution(ResourceLocation id, String type, double comfort) {
        public Contribution {
            if (id == null || type == null || type.isBlank() || !Double.isFinite(comfort) || comfort <= 0.0D) {
                throw new IllegalArgumentException("Comfort provider contributions need an ID, type and positive value");
            }
        }
    }
}
