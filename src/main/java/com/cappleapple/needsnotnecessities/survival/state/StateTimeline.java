package com.cappleapple.needsnotnecessities.survival.state;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;

public final class StateTimeline {
    private final ResourceLocation id;
    private final ResourceLocation neutralStateId;
    private final List<SurvivalStateDefinition> states;
    private final Map<ResourceLocation, StateRange> ranges;
    private final double totalHours;

    public StateTimeline(
            ResourceLocation id,
            ResourceLocation neutralStateId,
            List<SurvivalStateDefinition> definitions) {
        this.id = Objects.requireNonNull(id, "id");
        this.neutralStateId = Objects.requireNonNull(neutralStateId, "neutralStateId");
        if (definitions.isEmpty()) {
            throw new IllegalArgumentException("State timeline cannot be empty: " + id);
        }

        List<SurvivalStateDefinition> sorted = new ArrayList<>(definitions);
        sorted.sort(Comparator.comparingInt(SurvivalStateDefinition::order));
        Set<Integer> orders = new HashSet<>();
        Set<ResourceLocation> stateIds = new HashSet<>();
        Map<ResourceLocation, StateRange> builtRanges = new HashMap<>();
        double cursor = 0.0D;
        for (SurvivalStateDefinition state : sorted) {
            if (!orders.add(state.order())) {
                throw new IllegalArgumentException("Duplicate state order " + state.order() + " in " + id);
            }
            if (!stateIds.add(state.id())) {
                throw new IllegalArgumentException("Duplicate state ID " + state.id() + " in " + id);
            }
            double end = cursor + state.durationHours();
            builtRanges.put(state.id(), new StateRange(cursor, end));
            cursor = end;
        }
        if (!stateIds.contains(neutralStateId)) {
            throw new IllegalArgumentException("Neutral state " + neutralStateId + " does not exist in " + id);
        }

        this.states = List.copyOf(sorted);
        this.ranges = Map.copyOf(builtRanges);
        this.totalHours = cursor;
    }

    public ResourceLocation id() {
        return id;
    }

    public List<SurvivalStateDefinition> states() {
        return states;
    }

    public ResourceLocation neutralStateId() {
        return neutralStateId;
    }

    public double totalHours() {
        return totalHours;
    }

    public double clamp(double positionHours) {
        if (Double.isNaN(positionHours)) {
            return neutralPosition();
        }
        if (positionHours == Double.POSITIVE_INFINITY) {
            return totalHours;
        }
        if (positionHours == Double.NEGATIVE_INFINITY) {
            return 0.0D;
        }
        return Math.clamp(positionHours, 0.0D, totalHours);
    }

    public SurvivalStateDefinition stateAt(double positionHours) {
        double clamped = clamp(positionHours);
        if (clamped >= totalHours) {
            return states.getLast();
        }
        for (SurvivalStateDefinition state : states) {
            if (clamped < ranges.get(state.id()).endHours()) {
                return state;
            }
        }
        return states.getLast();
    }

    public StateRange rangeOf(ResourceLocation stateId) {
        StateRange range = ranges.get(stateId);
        if (range == null) {
            throw new IllegalArgumentException("Unknown state " + stateId + " in " + id);
        }
        return range;
    }

    public double neutralPosition() {
        return positionForState(neutralStateId, PositionAnchor.MIDDLE);
    }

    public double worstPosition() {
        return 0.0D;
    }

    public double bestPosition() {
        return totalHours;
    }

    public double positionForState(ResourceLocation stateId, PositionAnchor anchor) {
        StateRange range = rangeOf(stateId);
        return switch (anchor) {
            case START -> range.startHours();
            case MIDDLE -> range.startHours() + range.durationHours() / 2.0D;
            case END -> range.endHours();
        };
    }

    public double add(double positionHours, double deltaHours) {
        if (!Double.isFinite(deltaHours)) {
            throw new IllegalArgumentException("State delta must be finite");
        }
        double result = clamp(positionHours) + deltaHours;
        return clamp(result);
    }

    public double moveLevels(double positionHours, double levelDelta) {
        if (!Double.isFinite(levelDelta)) {
            throw new IllegalArgumentException("State level delta must be finite");
        }
        return positionFromLevelCoordinate(Math.clamp(levelCoordinate(positionHours) + levelDelta, 0.0D, states.size()));
    }

    public double levelCoordinate(double positionHours) {
        double clamped = clamp(positionHours);
        if (clamped >= totalHours) {
            return states.size();
        }
        SurvivalStateDefinition state = stateAt(clamped);
        int index = states.indexOf(state);
        StateRange range = ranges.get(state.id());
        return index + (clamped - range.startHours()) / range.durationHours();
    }

    public double positionFromLevelCoordinate(double coordinate) {
        if (Double.isNaN(coordinate)) {
            return neutralPosition();
        }
        double clamped = Math.clamp(coordinate, 0.0D, states.size());
        if (clamped >= states.size()) {
            return totalHours;
        }
        int index = (int) Math.floor(clamped);
        double fraction = clamped - index;
        StateRange range = ranges.get(states.get(index).id());
        return range.startHours() + range.durationHours() * fraction;
    }

    public enum PositionAnchor {
        START,
        MIDDLE,
        END
    }

    public record StateRange(double startHours, double endHours) {
        public double durationHours() {
            return endHours - startHours;
        }

        public boolean contains(double positionHours) {
            return positionHours >= startHours && positionHours < endHours;
        }
    }
}
