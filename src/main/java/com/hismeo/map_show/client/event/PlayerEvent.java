package com.hismeo.map_show.client.event;

import com.hismeo.map_show.MapShow;
import com.hismeo.map_show.client.screen.MapScreen;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientChatEvent;

@EventBusSubscriber(modid = MapShow.MODID, value = Dist.CLIENT)
public class PlayerEvent {
    @SubscribeEvent
    public static void chatEvent(ClientChatEvent event) {
        if (MapShow.isDev()) {
            if (event.getMessage().equals("MAPSHOW")) {
                Minecraft.getInstance().setScreen(new MapScreen());
            }
        }
    }
}
