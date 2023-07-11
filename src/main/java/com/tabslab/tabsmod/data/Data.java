package com.tabslab.tabsmod.data;

import com.tabslab.tabsmod.exp.Timer;
import com.tabslab.tabsmod.init.BlockInit;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Data {

    private static final ArrayList<Event> evts = new ArrayList<>();
    private static String playerName;

    private static final Map<String, BlockPos> blockPositions = new HashMap<>();
    private static Entity playerEntity;

    public static void setPlayerEntity(Entity entity) {
        playerEntity = entity;
        System.out.println("Entity Set");
        System.out.println("Block Position:");
        System.out.println(entity.blockPosition().toShortString());
        System.out.println("Position:");
        System.out.println(entity.position());
        System.out.println("GetOnPos:");
        System.out.println(entity.getOnPos());
        System.out.println("X: " + entity.getX() + ", Y: " + entity.getY() + ", Z: " + entity.getZ());
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

    public static void respawnBlocks(Level lvl, boolean initialSpawn) {

        // First, remove old blocks if it isn't the initial level

        if (!initialSpawn) {
            BlockPos block_a_pos = blockPositions.get("block_a");
            BlockPos block_b_pos = blockPositions.get("block_b");

            Block block_a = lvl.getBlockState(block_a_pos).getBlock();
            Block block_b = lvl.getBlockState(block_b_pos).getBlock();

            if (block_a.equals(BlockInit.BLOCK_A.get())) {
                // If block at position is a BlockA...
                lvl.removeBlock(block_a_pos, false);
            }

            if (block_b.equals(BlockInit.BLOCK_B.get())) {
                // If block at position is a BlockB...
                lvl.removeBlock(block_b_pos, false);
            }
        }

        // Next, respawn them in new random position, equidistant from player
        BlockPos playerPos = playerEntity.getOnPos();
        BlockPos block_a_pos_new = new BlockPos(playerPos.getX() + 3, playerPos.getY() + 3, playerPos.getZ() + 1);
        BlockPos block_b_pos_new = new BlockPos(playerPos.getX() - 3, playerPos.getY() - 3, playerPos.getZ() + 1);
        boolean set_a = lvl.setBlock(block_a_pos_new, BlockInit.BLOCK_A.get().defaultBlockState(), 1);
        boolean set_b = lvl.setBlock(block_b_pos_new, BlockInit.BLOCK_B.get().defaultBlockState(), 1);

        // Log as event
        Map<String, Object> data = new HashMap<>();
        data.put("block_a_spawn", block_a_pos_new);
        data.put("block_a_set", set_a);
        data.put("block_b_spawn", block_b_pos_new);
        data.put("block_b_set", set_b);
        if (initialSpawn) {
            addEvent("blocks_spawn_initial", 0, data);
        } else {
            long time = Timer.timeElapsed();
            addEvent("blocks_spawn", time, data);
        }

        // Update new block positions
        blockPositions.clear();
        blockPositions.put("block_a", block_a_pos_new);
        blockPositions.put("block_b", block_b_pos_new);
    }


    public static void addEvent(String type, long time, Map<String, Object> data) {
        Event evt = new Event(type, time, data);

        // Print to log
        System.out.println("-----------------------------------------");
        System.out.println("Event Type: " + evt.getType());
        System.out.println("Time: " + evt.getTime());
        System.out.println("Data: " + evt.getDataString());
        System.out.println("-----------------------------------------");

        evts.add(evt);
    }

    public static void addEvent(String type, long time) {
        Event evt = new Event(type, time);

        // Print to log
        System.out.println("-----------------------------------------");
        System.out.println("Event Type: " + evt.getType());
        System.out.println("Time: " + evt.getTime());
        System.out.println("-----------------------------------------");

        evts.add(new Event(type, time));
    }

    public static void setName(String name) {
        playerName = name;
    }

    public static void printSummary() {
        System.out.println("-----------------------------------------");
        System.out.println("Event Summary");
        System.out.println(evts);
        System.out.println("-----------------------------------------");
    }

    public static void endSession() {
        writeToCSV();
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

            // Add headers
            pw.println("Player Name: " + playerName);

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
