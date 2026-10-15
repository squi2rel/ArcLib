package com.github.squi2rel.arc.fabric;

import com.github.squi2rel.arc.ArcLib;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.loader.api.FabricLoader;

public final class ArcLibFabric implements DedicatedServerModInitializer{
    @Override
    public void onInitializeServer(){
        ArcLib.initHeadless(FabricLoader.getInstance().getGameDir().toFile());
    }
}
