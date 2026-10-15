package com.github.squi2rel.arc.forge;

import com.github.squi2rel.arc.ArcLibClient;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

final class ArcLibForgeClient {
    static void register(FMLJavaModLoadingContext context) {
        context.getModEventBus().addListener((FMLClientSetupEvent event) -> event.enqueueWork(ArcLibClient::init));
    }
}
