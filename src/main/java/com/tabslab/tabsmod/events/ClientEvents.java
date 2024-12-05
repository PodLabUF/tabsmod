package com.tabslab.tabsmod.events;

import com.google.gson.GsonBuilder;
import com.tabslab.tabsmod.TabsMod;
import com.tabslab.tabsmod.blocks.BlockA;
import com.tabslab.tabsmod.blocks.BlockB;
import com.tabslab.tabsmod.commands.Session;
import com.tabslab.tabsmod.data.BlockBroken;
import com.tabslab.tabsmod.data.Data;
import com.tabslab.tabsmod.exp.ExpHud;
import com.tabslab.tabsmod.exp.Timer;
import com.tabslab.tabsmod.init.BlockInit;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent.BreakEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;


import java.util.HashMap;
import java.util.Map;

public class ClientEvents {
    private static boolean initialBlockBreak;
    private static boolean intervalStart = false;

    @Mod.EventBusSubscriber(bus=Mod.EventBusSubscriber.Bus.MOD, modid=TabsMod.MODID, value=Dist.CLIENT)
    public static class ClientModBusEvents {
        @SubscribeEvent
        public static void registerGuiOverlays(RegisterGuiOverlaysEvent event) {
            event.registerAboveAll("points", ExpHud.HUD);
        }
    }

    @Mod.EventBusSubscriber(bus= Mod.EventBusSubscriber.Bus.FORGE, modid=TabsMod.MODID, value=Dist.CLIENT)
    public static class ForgeEvents {

        // Register custom commands
        @SubscribeEvent
        public static void registerCommands(RegisterCommandsEvent event) {
            Session.register(event.getDispatcher());
        }

        // Tick is unit of time within game's update cycle
        @SubscribeEvent
        public static void onTicks(TickEvent.PlayerTickEvent event) {
            // if at the end of the tick event
            if (event.phase == TickEvent.Phase.END) {
                if (initialBlockBreak && !intervalStart) { // if first block is broken and interval hasnt started, but needs to add if first reinforced block is broken
                    System.out.printf("Interval started\n");
                    Data.generateIntervals();
                    intervalStart = true;
                }
            }
        }

        // Will be run when a block is broken
        @SubscribeEvent
        public static void onBlockBreak(BreakEvent event) {
            Block block = event.getState().getBlock();
            if (block.equals(BlockInit.BLOCK_A.get())) {
                BlockA.broken(event);
                Data.respawnBlocks(event.getPlayer().getLevel(), false, BlockBroken.BlockA);
                initialBlockBreak = true;
            } else if (block.equals(BlockInit.BLOCK_B.get())) {
                BlockB.broken(event);
                Data.respawnBlocks(event.getPlayer().getLevel(), false, BlockBroken.BlockB);
                initialBlockBreak = true;
            }

        }

        @SubscribeEvent
        public static void onPlayerLeave(PlayerEvent.PlayerLoggedOutEvent event) {

            // Get event data
            long time = Timer.timeElapsed();

            // Leave level event
            Map<String, Object> data = new HashMap<>();
            Vec3 pos = event.getEntity().position();
            String name = event.getEntity().getName().getString();
            data.put("position", pos);
            data.put("name", name);

            Data.addEvent("player_leave_level", time, data);

            TabsMod.endSession();
        }

        @SubscribeEvent
        public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {

            // Set entity
            Data.setPlayerEntity(event.getEntity());

            // First, spawn block_a and block_b equidistant from player
            Data.respawnBlocks(event.getEntity().getLevel(), true, BlockBroken.Neither);

            // Flag for interval
            initialBlockBreak = false;

            // Begin timer
            Timer.startTimer();

            // Get event data
            long time = Timer.timeElapsed();

            // Join level event
            Map<String, Object> data = new HashMap<>();
            String name = event.getEntity().getName().getString();

            // Get level data
            ClientLevel.ClientLevelData lvl = Minecraft.getInstance().level.getLevelData();
            data.put("day_time", lvl.getDayTime());
            data.put("game_time", lvl.getGameTime());
            data.put("difficulty", lvl.getDifficulty().getKey());
            data.put("spawn_angle", lvl.getSpawnAngle());
            data.put("spawn_position", String.join("`", new GsonBuilder().create().toJson(Map.of(
                    "x", lvl.getXSpawn(),
                    "y", lvl.getYSpawn(),
                    "z", lvl.getZSpawn()
            ))));
            data.put("is_hardcore", lvl.isHardcore());
            lvl.setRaining(false);
            data.put("is_raining", lvl.isRaining());
            data.put("is_thundering", lvl.isThundering());

            // Set name
            Data.setName(name);

            // Phase 1 start event
            Data.addEvent("phase_1_start", time);

            Data.addEvent("player_join_level", time, data);
        }

        @SubscribeEvent
        public static void playerEntityInteract(PlayerInteractEvent.EntityInteract event) {
            if (event.getLevel().isClientSide) {

                // Get interaction data
                long time = Timer.timeElapsed();
                BlockPos pos = event.getPos();
                InteractionHand hand = event.getHand();
                Event.Result res = event.getResult();

                // Add to event list
                Map<String, Object> data = new HashMap<>();
                data.put("position", pos);
                data.put("hand", hand);
                data.put("result", res);
                Data.addEvent("entity_interact", time, data);
            }
        }

        @SubscribeEvent
        public static void playerEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
            if (event.getLevel().isClientSide) {

                // Get interaction data
                long time = Timer.timeElapsed();
                BlockPos pos = event.getPos();
                InteractionHand hand = event.getHand();
                Event.Result res = event.getResult();

                // Add to event list
                Map<String, Object> data = new HashMap<>();
                data.put("position", pos);
                data.put("hand", hand);
                data.put("result", res);
                Data.addEvent("entity_interact_specific", time, data);
            }
        }

        @SubscribeEvent
        public static void playerLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
            if (event.getLevel().isClientSide) {

                // Get interaction data
                long time = Timer.timeElapsed();
                BlockPos pos = event.getPos();
                InteractionHand hand = event.getHand();
                Event.Result res = event.getResult();
                Event.Result useBlock = event.getUseBlock();

                // Add to event list
                Map<String, Object> data = new HashMap<>();
                data.put("position", pos);
                data.put("hand", hand);
                data.put("result", res);
                data.put("useBlock", useBlock);
                Data.addEvent("left_click_block", time, data);
            }
        }

        @SubscribeEvent
        public static void playerRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
            InteractionHand hand = event.getHand();
            if (event.getLevel().isClientSide && hand == InteractionHand.OFF_HAND) {

                // Get interaction data
                long time = Timer.timeElapsed();
                BlockPos pos = event.getPos();
                Event.Result res = event.getResult();
                Event.Result useBlock = event.getUseBlock();

                // Add to event list
                Map<String, Object> data = new HashMap<>();
                data.put("position", pos);
                data.put("hand", hand);
                data.put("result", res);
                data.put("useBlock", useBlock);
                Data.addEvent("right_click_block", time, data);
            }
        }

        @SubscribeEvent
        public static void playerRightClickEmpty(PlayerInteractEvent.RightClickEmpty event) {
            if (event.getLevel().isClientSide) {

                // Get interaction data
                long time = Timer.timeElapsed();
                BlockPos pos = event.getPos();
                InteractionHand hand = event.getHand();
                Event.Result res = event.getResult();

                // Add to event list
                Map<String, Object> data = new HashMap<>();
                data.put("position", pos);
                data.put("hand", hand);
                data.put("result", res);
                Data.addEvent("right_click_empty", time, data);
            }
        }

        @SubscribeEvent
        public static void playerRightClickItem(PlayerInteractEvent.RightClickItem event) {
            if (event.getLevel().isClientSide) {

                // Get interaction data
                long time = Timer.timeElapsed();
                BlockPos pos = event.getPos();
                InteractionHand hand = event.getHand();
                Event.Result res = event.getResult();

                // Add to event list
                Map<String, Object> data = new HashMap<>();
                data.put("position", pos);
                data.put("hand", hand);
                data.put("result", res);
                Data.addEvent("right_click_item", time, data);
            }
        }

        @SubscribeEvent
        public static void onPrintEventSummary(InputEvent.Key event) {
            int key = event.getKey();
            if (key == GLFW.GLFW_KEY_P) {
                Data.printSummary();
            }
        }


    }
}
