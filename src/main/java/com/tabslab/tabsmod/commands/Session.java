package com.tabslab.tabsmod.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.tabslab.tabsmod.TabsMod;
import com.tabslab.tabsmod.exp.Timer;
import net.minecraft.commands.CommandSourceStack;

import net.minecraft.commands.Commands;

public class Session {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("session")
                .then(Commands.literal("end")
                        .executes(Session::end))
                .then(Commands.literal("phase")
                        .then(Commands.argument("num", IntegerArgumentType.integer(1, 3))
                                .executes((command) -> {
                                    return setSession(command, IntegerArgumentType.getInteger(command, "num"));
                                }))));
    }

    private static int end(CommandContext<CommandSourceStack> command) {
        TabsMod.endSession();
        return Command.SINGLE_SUCCESS;
    }

    private static int setSession(CommandContext<CommandSourceStack> command, int phase) {
        Timer.setPhase(phase);
        return Command.SINGLE_SUCCESS;
    }
}
