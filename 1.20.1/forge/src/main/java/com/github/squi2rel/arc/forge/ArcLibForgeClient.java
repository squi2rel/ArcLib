package com.github.squi2rel.arc.forge;

import com.github.squi2rel.arc.ArcLibClient;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

final class ArcLibForgeClient{
    static void register(){
        FMLJavaModLoadingContext.get().getModEventBus().addListener(
            (FMLClientSetupEvent event) -> event.enqueueWork(ArcLibClient::init));
    }
}
