package com.hismeo.map_show.client;

import com.hismeo.map_show.MapShow;
import com.hismeo.map_show.client.screen.MapScreen;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.Objects;

import static com.hismeo.map_show.client.MapChunkCache.isValidChunk;

public class ClientMap {
    private static final Object2ObjectMap<ResourceKey<Level>, MapLevel> levels = new Object2ObjectOpenHashMap<>();
    private static final Object2ObjectMap<ResourceKey<Level>, MapData> mapData = new Object2ObjectOpenHashMap<>();
    private static @Nullable MapLevel currentLevel;
    private static String levelName;
    private static Path rootDir;
    private static Path saveDir;
    private static MapSerializer serializer;

    private static long biomeZoomSeed;

    public static MapLevel getCurrentLevel(ClientLevel level) {
        if (currentLevel == null || currentLevel.dimension != level.dimension()) {
            levels.put(level.dimension(), currentLevel = new MapLevel(level, getViewDistance()));
        }
        return currentLevel;
    }

    public static @Nullable MapLevel getLevel(ResourceKey<Level> dimension) {
        if (currentLevel != null && currentLevel.dimension == dimension) {
            return currentLevel;
        }

        MapLevel mapLevel = levels.get(dimension);
        if (mapLevel != null) return mapLevel;

        MapData data = mapData.get(dimension);
        if (data == null) return null;

        MapLevel level = new MapLevel(data, getViewDistance(), biomeZoomSeed);
        levels.put(dimension, level);
        return level;
    }

    /// 设置新MapLevel并返回旧MapLevel（如果有旧的的话）
    public static @Nullable MapLevel setLevel(MapLevel level) {
        if (currentLevel != null && currentLevel.dimension == level.dimension) {
            currentLevel = level;
        }
        return levels.put(level.dimension, level);
    }

    // todo 随设置变动
    public static int getViewDistance() {
        return Minecraft.getInstance().options.serverRenderDistance;
    }

    @EventBusSubscriber(modid = MapShow.MODID, value = Dist.CLIENT)
    public static class Events {
        @SubscribeEvent
        public static void playerInteract$RightClickItem(PlayerInteractEvent.RightClickItem event) {
            if (FMLEnvironment.production || !(event.getLevel() instanceof ClientLevel level)) return;
            if (event.getItemStack().is(Items.FILLED_MAP)) {
                Minecraft.getInstance().setScreen(new MapScreen(getCurrentLevel(level)));
            }
        }

        @SubscribeEvent
        public static void chunk$Load(ChunkEvent.Load event) {
            if (event.getLevel() instanceof ClientLevel level) {
                MapLevel mapLevel = getCurrentLevel(level);
                MapChunk chunk = mapLevel.getChunkSource().setChunk(event.getChunk(), false); // 不复制，直接访问原版区块数据
                if (chunk != null) {
                    mapLevel.onChunkLoaded(chunk);
                }
            }
        }

        @SubscribeEvent
        public static void chunk$Unload(ChunkEvent.Unload event) {
            if (event.getLevel() instanceof ClientLevel level) {
                ChunkPos pos = event.getChunk().getPos();
                MapChunkCache.Storage storage = getCurrentLevel(level).getChunkSource().storage;
                if (storage.inRange(pos.x, pos.z)) {
                    int index = storage.getIndex(pos.x, pos.z);
                    MapChunk chunk = storage.getChunk(index);
                    if (isValidChunk(chunk, pos.x, pos.z)) {
                        storage.replace(index, chunk, null);
                    }
                }
            }
        }

        @SubscribeEvent
        public static void clientPlayerNetwork$LoggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
            Minecraft minecraft = Minecraft.getInstance();
            IntegratedServer server = minecraft.getSingleplayerServer();
            if (server != null) {
                levelName = server.getWorldPath(LevelResource.ROOT).getParent().getFileName().toString();
            } else {
                ServerData serverData = Objects.requireNonNull(minecraft.getConnection()).getServerData();
                if (serverData == null) {
                    levelName = "__UNKNOWN__";
                } else {
                    levelName = serverData.ip.replace('.', '_');
                }
            }
            rootDir = FMLPaths.GAMEDIR.get().resolve(MapShow.MODID);
            saveDir = rootDir.resolve(levelName);
            ResourceKey<Level> dimension = event.getPlayer().level().dimension();
            serializer = new MapSerializer(levelName, dimension, DimensionType.getStorageFolder(dimension, saveDir).resolve("region"));

            /// @see ServerPlayer#createCommonSpawnInfo(ServerLevel)
            /// @see ClientPacketListener#handleLogin(ClientboundLoginPacket)
            /// 应该是每个维度都一致的，故仅在login事件初始化
            biomeZoomSeed = event.getPlayer().level().getBiomeManager().biomeZoomSeed;
        }

        /// 玩家重生、跨纬度时
        @SubscribeEvent
        public static void clientPlayerNetwork$Clone(ClientPlayerNetworkEvent.Clone event) {
            MapLevel neoLevel = new MapLevel(event.getNewPlayer().clientLevel, getViewDistance());
            MapLevel oldLevel = setLevel(neoLevel);
            if (oldLevel == null) return;
            MapData data = oldLevel.asData();
            mapData.put(data.dimension(), data);
            // todo 保存 data
        }
    }
}
