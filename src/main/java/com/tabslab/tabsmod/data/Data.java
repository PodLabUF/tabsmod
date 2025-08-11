package com.tabslab.tabsmod.data;

import com.tabslab.tabsmod.TabsMod;
import com.tabslab.tabsmod.exp.ExpHud;
import com.tabslab.tabsmod.exp.Timer;
import com.tabslab.tabsmod.init.BlockInit;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

public class Data {
    public static List<Long> storedViIntervals = new ArrayList<>();
    private static final ArrayList<Event> evts = new ArrayList<>();
    private static String playerName;
    public static final Map<String, BlockPos> blockPositions = new HashMap<>();
    private static Entity playerEntity;
    public static long sessionStartTime = 0;
    private static long sessionEndTime = 0;
    private static double meanIntervalValue = 2000.0; // mean interval value in ms (2s)
    private static int numberOfSteps = 10; // total number of intervals (steps)
    private static double probability = .5; // probability of reinforcement

    //AMV for timing of pickupCoinPrompt
    public static int coinToken = 0;
    public static int getCoinToken(){
        return coinToken;
    }
    public static void nextCoinToken(){
        coinToken++;
    }

    // Parameters for Flesher-Hoffman interval generation
    public static void setParameters(double sec, int steps, double prob) {
        Data.meanIntervalValue = sec;
        Data.numberOfSteps = steps;
        Data.probability = prob;
    }

    public static List<Long> generateIntervals() {
        // get the current time
        long baseTime = Timer.timeElapsed();

        // list to store end times of each interval
        List<Long> intervalDurations = new ArrayList<>();

        // constant factor
        double factor = -1.0 / Math.log(1 - probability);

        // random generator for variability
        Random random = new Random();

        System.out.println("Generated Intervals:");

        // iterate to generate interval
        for (int n = 1; n <= numberOfSteps; n++) {
            double t_n;

            // the last interval
            if (n == numberOfSteps) {
                // calculate the last interval to ensure it is valid
                t_n = factor * (1 + Math.log(numberOfSteps));
            } else {
                // Fleshler-Hoffman
                t_n = factor * (
                        1 + Math.log(numberOfSteps) +
                                (numberOfSteps - n) * Math.log(numberOfSteps - n) -
                                (numberOfSteps - n + 1) * Math.log(numberOfSteps - n + 1)
                );
            }

            // introduce variability: random multiplier between 0.7 and 1.0
            double randomFactor = 0.7 + (0.3 * random.nextDouble());
            long intervalDuration = (long) (t_n * meanIntervalValue * randomFactor);

            // store the end time in the list
            intervalDurations.add(intervalDuration);

            // calculate end time of the interval
            long endTime = baseTime + intervalDuration;

            // update baseTime to the end of the current interval for the next iteration
            baseTime = endTime;
        }

        //AMV To Ensure always 2 seconds
        long totalDuration_vi = 0;
        for (long duration : intervalDurations) {
            totalDuration_vi += duration;
        }
        if(totalDuration_vi > 20000){
            long excess = totalDuration_vi - 20000;
            int lastIndex = intervalDurations.size() - 1;
            long Last_Val = intervalDurations.get(lastIndex);
            Long New_Last_Val = Math.max(0, Last_Val - excess);
            intervalDurations.set(lastIndex, New_Last_Val);
        } else if (totalDuration_vi < 20000) {
            long excess = 20000 - totalDuration_vi;
            int lastIndex = intervalDurations.size() - 1;
            long Last_Val = intervalDurations.get(lastIndex);
            Long New_Last_Val = Math.max(0, Last_Val + excess);
            intervalDurations.set(lastIndex, New_Last_Val);
        }

        // shuffle list
        Collections.shuffle(intervalDurations);

        storedViIntervals = new ArrayList<>(intervalDurations);
        for (int i = 0; i < storedViIntervals.size(); i++) {
            Map<String, Object> data = new HashMap<>();
            data.put("interval_index", i);
            data.put("interval_duration_ms", storedViIntervals.get(i));
            Data.addEvent("interval_generated", Timer.timeElapsed(), data);
        }


        // return the list of total interval times
        return intervalDurations;
    }

    public static void setPlayerEntity(Entity entity) {
        playerEntity = entity;
        System.out.println("Entity Set");
        System.out.println("Position:");
        System.out.println(entity.position());

        System.out.println("Player Chunk: ");
        System.out.println(entity.chunkPosition());
    }

    public static void teleportPlayer(double x, double y, double z) {
        playerEntity.moveTo(x, y, z);
        //playerEntity.setPositionAndUpdate(x, y, z);
    }

    public static void setBlockPositions(Map<String, BlockPos> positions) {
        blockPositions.put("block_a", positions.get("block_a"));
        blockPositions.put("block_b", positions.get("block_b"));
    }

    public static BlockPos getBlockAPos() {
        return blockPositions.get("block_a");
    }

    public static BlockPos getBlockBPos() {
        return blockPositions.get("block_b");
    }

    public static void handleBlocksBreak(Level lvl, BlockBroken blockBroken, boolean initialBlockBreak) {
        BlockPos block_a_pos = blockPositions.get("block_a");
        BlockPos block_b_pos = blockPositions.get("block_b");

        if (block_a_pos != null && block_b_pos != null) {
            int phase = Timer.currentPhase();

            boolean isCorrect =
                    (phase == 1 && blockBroken == BlockBroken.BlockA) ||
                            (phase == 2 && blockBroken == BlockBroken.BlockB);

            if (isCorrect) {
                // Check eligibility to drop coin
                boolean dropCoin = initialBlockBreak || Timer.viTimeRemaining() == 0;

                // Destroy both blocks - only correct one drops
                lvl.destroyBlock(block_a_pos, dropCoin && phase == 1);
                lvl.destroyBlock(block_b_pos, dropCoin && phase == 2);

                if (dropCoin) {
                    // Coin drop logic
                    Timer.pauseTimer();
                    ExpHud.setCoinAvailable(true);

                    Map<String, Object> data = new HashMap<>();
                    data.put("phase", Timer.currentPhase());
                    Data.addEvent("reinforcer", Timer.timeElapsed(), data);

                    //AMV Timing of pickup Prompt
                    ExpHud.setShowPickupPrompt(false);
                    nextCoinToken();
                    int currentToken = getCoinToken();

                    Timer.scheduleDelayedTask(() -> {
                        if (ExpHud.isCoinAvailable() && getCoinToken() == currentToken) {
                            ExpHud.setShowPickupPrompt(true);
                        }
                    }, 5000);

                    if (!initialBlockBreak) {
                        Timer.nextViInterval();
                    }
                } else {
                    // Not eligible - immediately respawn
                    Data.respawnBlocks(lvl, false);
                }

            } else {
                // Wrong block - destroy both without drops and respawn
                lvl.destroyBlock(block_a_pos, false);
                lvl.destroyBlock(block_b_pos, false);
                Data.respawnBlocks(lvl, false);
            }
        }
    }

    public static void respawnBlocks(Level lvl, boolean initialSpawn) {
        // Gets player position
        BlockPos playerPos = playerEntity.getOnPos();
        int xPos = playerPos.getX();
        int yPos = playerPos.getY();
        int zPos = playerPos.getZ();

        // Get direction the player is facing
        Vec3 lookVec = playerEntity.getLookAngle().normalize();

        double sideOffset = 3.0;      // Left/right distance
        double forwardOffset = 5.0; // Forward distance
        double heightOffset = 1.0;  // Height

        // Forward offset
        double fx = lookVec.x * forwardOffset;
        double fz = lookVec.z * forwardOffset;

        // Right vector (90° rotated horizontal vector)
        Vec3 rightVec = new Vec3(-lookVec.z, 0, lookVec.x).normalize();

        // Block A on left
        BlockPos newBlockAPos = new BlockPos(
                xPos + fx - rightVec.x * sideOffset,
                yPos + heightOffset,
                zPos + fz - rightVec.z * sideOffset
        );

        // Block B on right
        BlockPos newBlockBPos = new BlockPos(
                xPos + fx + rightVec.x * sideOffset,
                yPos + heightOffset,
                zPos + fz + rightVec.z * sideOffset
        );

        // Place the blocks
        BlockState blockStateA = BlockInit.BLOCK_A.get().defaultBlockState();
        BlockState blockStateB = BlockInit.BLOCK_B.get().defaultBlockState();
        boolean setA = lvl.setBlockAndUpdate(newBlockAPos, blockStateA);
        boolean setB = lvl.setBlockAndUpdate(newBlockBPos, blockStateB);

        // Log event
        Map<String, Object> data = new HashMap<>();
        data.put("block_a_spawn", newBlockAPos);
        data.put("block_a_set", setA);
        data.put("block_b_spawn", newBlockBPos);
        data.put("block_b_set", setB);

        if (initialSpawn) {
            addEvent("blocks_spawn_initial", 0, data);
        } else {
            long time = Timer.timeElapsed();
            addEvent("blocks_spawn", time, data);
        }

        // Update positions
        blockPositions.clear();
        blockPositions.put("block_a", newBlockAPos);
        blockPositions.put("block_b", newBlockBPos);
    }

    private static void removeAllBlocks(Level lvl, Block targetBlock) {
        for (BlockPos pos : BlockPos.betweenClosed(lvl.getMinBuildHeight(), 0, lvl.getMinBuildHeight(), lvl.getMaxBuildHeight(), 255, lvl.getMaxBuildHeight())) {
            if (lvl.getBlockState(pos).getBlock() == targetBlock) {
                lvl.destroyBlock(pos, true); // Drop the block as an item
            }
        }
    }

    public static void addEvent(String type, long time, Map<String, Object> data) {
        boolean dev = TabsMod.getDev();
        if (!dev) {
            Event evt = new Event(type, time, data);

            // Print to log
            System.out.println("-----------------------------------------");
            System.out.println("Event Type: " + evt.getType());
            System.out.println("Time: " + evt.getTime());
            System.out.println("Data: " + evt.getDataString());
            System.out.println("-----------------------------------------");

            evts.add(evt);
        }
    }

    public static void addEvent(String type, long time) {
        boolean dev = TabsMod.getDev();
        if (!dev) {
            Event evt = new Event(type, time);

            // Print to log
            System.out.println("-----------------------------------------");
            System.out.println("Event Type: " + evt.getType());
            System.out.println("Time: " + evt.getTime());
            System.out.println("-----------------------------------------");

            evts.add(new Event(type, time));
        }
    }

    public static void setName(String name) {
        playerName = name;
    }

    public static void printSummary() {
        boolean dev = TabsMod.getDev();
        if (!dev) {
            System.out.println("-----------------------------------------");
            System.out.println("Event Summary");
            System.out.println(evts);
            System.out.println("-----------------------------------------");
        }
    }

    public static void endSession() {
        boolean dev = TabsMod.getDev();
        sessionEndTime = System.currentTimeMillis();
        if (!dev) {
            writeToCSV();
        }
        playerName = null;
        blockPositions.clear();
        evts.clear();
    }

    public static void writeToCSV() {

        System.out.println("-----------------------------------------");
        System.out.println("Creating csv file...");
        System.out.println("-----------------------------------------");

        File file = new File(playerName + ".csv");

        try(PrintWriter pw = new PrintWriter(file)) {
            // Get date, start time, and end time
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
            dateFormat.setTimeZone(TimeZone.getDefault());
            String startTimeStr = dateFormat.format(new Date(sessionStartTime));
            String endTimeStr = dateFormat.format(new Date(sessionEndTime));
            String timeZoneStr = new SimpleDateFormat("HH:mm:ss z").format(new Date(sessionEndTime));

            // Write session info at the beginning of CSV
            pw.println("Start: " + startTimeStr);
            pw.println("End: " + endTimeStr);
            pw.println(timeZoneStr);
            pw.println();

            // Add headers
            pw.println("Player Name: " + playerName);

            if (storedViIntervals != null && !storedViIntervals.isEmpty()) {
                pw.println("Generated Intervals (ms):");
                for (int i = 0; i < storedViIntervals.size(); i++) {
                    pw.println("Interval " + (i + 1) + ": " + storedViIntervals.get(i));
                }
                pw.println(); // newline for clarity
            }

            // Write events
            String[] cols = { "Time", "Type", "Other Data" };
            pw.println(String.join(",", cols));

            for (Event evt : evts) {
                pw.println(evt.toCSV());
            }

            // Close connection
            pw.close();

            System.out.println("-----------------------------------------");
            System.out.println("Data File Created!");
            System.out.println("Absolute Path: " + file.getAbsolutePath());
            System.out.println("-----------------------------------------");

        } catch (IOException e) {

            System.out.println("-----------------------------------------");
            System.out.println("Exception during file creation:");
            System.out.println(e.getMessage());
            System.out.println("-----------------------------------------");
        }

    }
}
