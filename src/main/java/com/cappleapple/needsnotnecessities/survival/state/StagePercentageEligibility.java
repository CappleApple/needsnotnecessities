package com.cappleapple.needsnotnecessities.survival.state;

public final class StagePercentageEligibility {
    private StagePercentageEligibility() {
    }

    public static boolean isBelowConfiguredPercentage(
            StateTimeline timeline,
            double positionHours,
            double percentage) {
        int stateCount = timeline.states().size();
        int eligibleCount = eligibleStageCount(stateCount, percentage);
        int currentIndex = timeline.states().indexOf(timeline.stateAt(positionHours));
        return currentIndex >= 0 && currentIndex < eligibleCount;
    }

    public static int eligibleStageCount(int stateCount, double percentage) {
        if (stateCount <= 0 || !Double.isFinite(percentage) || percentage < 0.0D || percentage > 100.0D) {
            throw new IllegalArgumentException("Stage count must be positive and percentage must be between 0 and 100");
        }
        if (percentage <= 0.0D) {
            return 0;
        }
        if (percentage >= 100.0D) {
            return stateCount;
        }
        return Math.max(1, (int) Math.floor(stateCount * percentage / 100.0D));
    }
}
