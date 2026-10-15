package com.github.squi2rel.arc.neoforge;

import com.github.squi2rel.arc.ArcLib;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;

@Mod(value = ArcLib.MOD_ID, dist = Dist.DEDICATED_SERVER)
public final class ArcLibNeoForge{
    public ArcLibNeoForge(){
        ArcLib.initHeadless(FMLPaths.GAMEDIR.get().toFile());
    }
}
