package com.formlessDragon.ae2gtaddon;

import com.formlessDragon.ae2gtaddon.proxy.IProxy;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;

@Mod(modid = Tags.MOD_ID, name = Tags.MOD_NAME, version = Tags.VERSION)
public class AEGTAddon {

    @SidedProxy(modId = Tags.MOD_ID, clientSide = "com.formlessDragon.ae2gtaddon.proxy.ClientProxy", serverSide = "com.formlessDragon.ae2gtaddon.proxy.CommonProxy")
    public static IProxy proxy;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        proxy.init();
    }

}
