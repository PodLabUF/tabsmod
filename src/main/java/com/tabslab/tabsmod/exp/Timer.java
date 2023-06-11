package com.tabslab.tabsmod.exp;

public class Timer {
    private static long startTime = 0;

    public static void startTimer() {
        startTime = System.currentTimeMillis();
    }

    public static long timeElapsed() {
        return System.currentTimeMillis() - startTime;
    }

    public static String timeString() {
        long millis = timeElapsed();
        String mins = String.format("%02d", (millis / 1000) / 60);
        String secs = String.format("%02d", (millis / 1000) % 60);

        return mins + ":" + secs;
    }

    public static int currentPhase() {
        // This is where the package will determine what phase the experiment is in
        // when called.
        long phaseLength = 300000; // Milliseconds
        long elapsed = timeElapsed();
        return (int) Math.floorDiv(elapsed, phaseLength) + 1;
    }

    public static String currentPhaseString() {
        return String.valueOf(currentPhase());
    }

    public static void endSession() {
        startTime = 0;
    }

}
