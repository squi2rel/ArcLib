package com.github.squi2rel.arc.fabric;

import com.github.squi2rel.arc.ArcLibClient;
import net.fabricmc.api.ClientModInitializer;

public final class ArcLibFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ArcLibClient.init();
    }
}
