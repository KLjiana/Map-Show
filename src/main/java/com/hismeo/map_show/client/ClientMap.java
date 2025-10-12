package com.hismeo.map_show.client;

import com.hismeo.map_show.MapShow;
import com.hismeo.map_show.client.screen.MapScreen;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import org.jetbrains.annotations.Nullable;

public class ClientMap {
    private static final Reference2ObjectMap<ResourceKey<Level>, MapLevel> levels = new Reference2ObjectOpenHashMap<>();
    private static @Nullable MapLevel currentLevel;

    public static MapLevel getCurrentLevel(ClientLevel level) {
        if (currentLevel == null || currentLevel.dimension != level.dimension()) {
            levels.put(level.dimension(), currentLevel = new MapLevel(
                    level.dimension(),
                    level.effects(),
                    Minecraft.getInstance().options.serverRenderDistance,
                    level.getHeight(),
                    level.getMinBuildHeight()
            ));
        }
        return currentLevel;
    }

    public static @Nullable MapLevel getLevel(ResourceKey<Level> dimension) {
        return currentLevel != null && currentLevel.dimension == dimension ? currentLevel : levels.get(dimension);
    }

    public static @Nullable MapLevel setLevel(MapLevel level) {
        if (currentLevel != null && currentLevel.dimension == level.dimension) {
            currentLevel = level;
        }
        return levels.put(level.dimension, level);
    }

    @EventBusSubscriber(modid = MapShow.MODID, value = Dist.CLIENT)
    public static class Events {
        @SubscribeEvent
        public static void playerInteract$RightClickItem(PlayerInteractEvent.RightClickItem event) {
            if (FMLEnvironment.production || !(event.getLevel() instanceof ClientLevel clientLevel)) return;
            if (event.getItemStack().is(Items.FILLED_MAP)) {
                Minecraft.getInstance().setScreen(new MapScreen(getCurrentLevel(clientLevel)));
            }
        }

        @SubscribeEvent
        public static void chunk$Load(ChunkEvent.Load event) {
            if (event.getLevel() instanceof ClientLevel level) {
                getCurrentLevel(level).getChunkSource().setChunk(event.getChunk(), false); // 不复制，直接访问原版区块数据
            }
        }
    }
}
