package com.tabslab.tabsmod;

import com.mojang.logging.LogUtils;
import com.tabslab.tabsmod.data.Data;
import com.tabslab.tabsmod.exp.ExpHud;
import com.tabslab.tabsmod.exp.Timer;
import com.tabslab.tabsmod.init.BlockInit;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(TabsMod.MODID)
public class TabsMod {
    public static final String MODID = "tabsmod";
    public TabsMod() {
        final IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        BlockInit.BLOCKS.register(modEventBus);

        MinecraftForge.EVENT_BUS.register(this);
    }

    public static void endSession() {
        System.out.println("-----------------------------------------");
        System.out.println("Ending Session");
        System.out.println("-----------------------------------------");

        ExpHud.endSession();
        Timer.endSession();
        Data.endSession();
    }
}
