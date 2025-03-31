package com.tabslab.tabsmod.exp;

import com.tabslab.tabsmod.data.Data;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

import java.sql.Time;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Timer {
    private static long startTime = 0;

    // *** adjust phase length if necessary ***
    private static long phaseLength = 300000; // Milliseconds
    private static int totalPhases = 3;
    private static boolean timerStarted = false;
    private static long[] intervals = new long[0];
    private static int currentInterval = 0; // Track the current interval
    private static long pauseTime = 0; // Track when the timer was paused
    private static boolean timerPaused = false;
    private static int lastPhase = -1;
    private static long viElapsedBeforePause = 0;
    // vi Timer
    private static List<Long> viIntervals;
    public static int viIndex = -1;
    private static long viStartTime = 0;
    private static boolean viTimerRunning = false;
    private static boolean viTimerPaused = false;

    public static void pauseTimer() {
        if (!timerPaused) {
            pauseTime = System.currentTimeMillis();
            timerPaused = true;
        }
        // Pause viTimer
        if (viTimerRunning && !viTimerPaused) {
            viElapsedBeforePause = System.currentTimeMillis() - viStartTime;
            viTimerPaused = true;
        }
    }

    public static void resumeTimer() {
        if (timerPaused) {
            long pausedDuration = System.currentTimeMillis() - pauseTime;
            startTime += pausedDuration; // Adjust start time by the paused duration
            timerPaused = false;
        }

        if (viTimerPaused) {
            long pausedDuration = System.currentTimeMillis() - pauseTime;
            viStartTime = System.currentTimeMillis() - viElapsedBeforePause;
            viTimerPaused = false;
        }
    }

    public static long timeElapsed() {
        if (!timerStarted || timerPaused) {
            return pauseTime - startTime; // Return the time before pausing
        }
        return System.currentTimeMillis() - startTime;
    }

    // Check if the stimulus point for the current interval has been reached
    public static boolean isStimulusReached() {
        long elapsedTime = timeElapsed();

        // If all intervals are completed, return false
        if (currentInterval >= intervals.length) return false;

        // Check if elapsed time has passed the stimulus point of the current interval
        if (elapsedTime >= intervals[currentInterval]) {
            currentInterval++; // Move to the next interval
            return true;       // Stimulus point reached
        }
        return false;
    }

    public static int getTotalPhases() {
        return totalPhases;
    }

    public static void startTimer() {
        if (!timerStarted) {
            startTime = System.currentTimeMillis();
            timerStarted = true;  // Set this to true when the timer starts
        }

    }

    public static boolean timerStarted() {
        return timerStarted;  // Getter method to check if the timer has started
    }

    public static void scheduleDelayedTask(Runnable task, long delayMillis) {
        new Thread(() -> {
            try {
                Thread.sleep(delayMillis);
                Minecraft.getInstance().execute(task);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    public static String timeString() {
        long millis = timeElapsed();
        String mins = String.format("%02d", (millis / 1000) / 60);
        String secs = String.format("%02d", (millis / 1000) % 60);

        return mins + ":" + secs;
    }

    public static void setPhase(int phase) {
        int oldPhase = Timer.currentPhase();
        startTime = System.currentTimeMillis() - (phaseLength * (phase - 1));

        String evt_type = "phase_set_" + phase;
        Map<String, Object> data = new HashMap<>();
        data.put("old_phase", oldPhase);
        data.put("new_phase", phase);
        Data.addEvent(evt_type, Timer.timeElapsed(), data);
    }

    public static int currentPhase() {
        // This is where the package will determine what phase the experiment is in
        // when called.
        long elapsed = timeElapsed();
        return (int) Math.floorDiv(elapsed, phaseLength) + 1;
    }

    public static String currentPhaseString() {
        return String.valueOf(currentPhase());
    }

    public static void endSession() {
        setPhase(totalPhases + 1);
    }


    public static void startViTimer(int index) {
        if (viIntervals == null || viIntervals.isEmpty()) {
            viIntervals = Data.generateIntervals(); // Generate intervals from Data.java
        }
        viIndex = index;
        viStartTime = System.currentTimeMillis();
        viTimerRunning = true;
    }

    public static List<Long> getViIntervals() {
        return viIntervals;
    }

    public static long viTimeRemaining() {
        if (!viTimerRunning || viIndex < 0 || viIndex >= viIntervals.size()) {
            return 0;
        }

        long elapsed;
        if (viTimerPaused) {
            elapsed = viElapsedBeforePause;
        } else {
            elapsed = System.currentTimeMillis() - viStartTime;
        }

        long remaining = viIntervals.get(viIndex) - elapsed;
        return Math.max(remaining, 0);
    }

    public static void nextViInterval() {
        if (viIntervals == null || viIntervals.isEmpty()) {
            viIntervals = Data.generateIntervals();
            viIndex = 0;
        } else if (viIndex >= viIntervals.size() - 1) {
            viIntervals = Data.generateIntervals();
            viIndex = 0;
        } else {
            viIndex++;
        }

        viStartTime = System.currentTimeMillis();
        viElapsedBeforePause = 0; // reset pause tracking
    }

    public static boolean isViRunning() {
        return viTimerRunning && viTimeRemaining() > 0;
    }

    public static boolean hasPhaseChanged() {
        int currentPhase = currentPhase();
        if (currentPhase != lastPhase) {
            lastPhase = currentPhase; // update last phase
            return true; // phase has changed
        }
        return false; // no phase change
    }
    public static void resetViState() {
        viIntervals = Data.generateIntervals();
        viIndex = -1;
        viStartTime = 0;
        viElapsedBeforePause = 0;
        viTimerRunning = false;
        viTimerPaused = false;
    }
}


