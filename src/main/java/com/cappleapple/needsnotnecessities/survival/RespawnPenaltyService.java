package com.cappleapple.needsnotnecessities.survival;

public final class RespawnPenaltyService {
    private RespawnPenaltyService() {
    }

    public static boolean shouldSendMessage(
            double previousHungerHours,
            double respawnHungerHours,
            double previousThirstHours,
            double respawnThirstHours,
            boolean hungerEnabled,
            boolean thirstEnabled) {
        return hungerEnabled
                && thirstEnabled
                && previousHungerHours > respawnHungerHours
                && previousThirstHours > respawnThirstHours;
    }
}
