package com.github.squi2rel.arc.neoforge;

import com.github.squi2rel.arc.ArcLib;
import com.github.squi2rel.arc.ArcLibClient;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

@Mod(value = ArcLib.MOD_ID, dist = Dist.CLIENT)
public final class ArcLibNeoForgeClient {
    public ArcLibNeoForgeClient(IEventBus modBus) {
        modBus.addListener((FMLClientSetupEvent event) -> event.enqueueWork(ArcLibClient::init));
    }
}
