package com.github.squi2rel.arc.forge;

import com.github.squi2rel.arc.ArcLib;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;

@Mod(ArcLib.MOD_ID)
public final class ArcLibForge{
    public ArcLibForge(){
        if(FMLEnvironment.dist == Dist.DEDICATED_SERVER){
            ArcLib.initHeadless(FMLPaths.GAMEDIR.get().toFile());
        }else{
            ArcLibForgeClient.register();
        }
    }
}
