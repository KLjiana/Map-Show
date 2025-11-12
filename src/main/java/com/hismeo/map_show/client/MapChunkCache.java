package com.hismeo.map_show.client;

import it.unimi.dsi.fastutil.longs.Long2ByteMap;
import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReferenceArray;

/// @see net.minecraft.client.multiplayer.ClientChunkCache
/// @see net.minecraft.server.level.ChunkMap
public class MapChunkCache {
    private static final int SAVE_TICK = 200;
    private static final byte CHUNK_TYPE_REPLACEABLE = -1;
    private static final byte CHUNK_TYPE_UNKNOWN = 0;
    private static final byte CHUNK_TYPE_FULL = 1;
    volatile Storage storage;
    final MapLevel level;
    final ArrayDeque<MapChunk> pendingSaveChunks = new ArrayDeque<>();
    final Long2ObjectMap<MapChunk> visibleMapChunks = new Long2ObjectLinkedOpenHashMap<>();
    private final Long2ObjectMap<CompletableFuture<MapChunk>> scheduledLoadingChunks = new Long2ObjectLinkedOpenHashMap<>();
    private final Long2ByteMap chunkTypeCache = new Long2ByteOpenHashMap();
    private int saveTick = SAVE_TICK;

    public MapChunkCache(MapLevel level, int viewDistance) {
        this.level = level;
        this.storage = new Storage(calculateStorageRange(viewDistance));
    }

    public void tick() {
        if (--this.saveTick > 0) return;
        this.saveTick = SAVE_TICK;
        while (!pendingSaveChunks.isEmpty()) {
            MapChunk chunk = pendingSaveChunks.remove();
            ClientMap.getOrCreateSerializer(level.dimension).scheduleUnload(chunk);
        }
    }

    public void drop(ChunkPos pos) {
        if (storage.inRange(pos.x, pos.z)) {
            int index = storage.getIndex(pos.x, pos.z);
            MapChunk chunk = storage.getChunk(index);
            if (isValidChunk(chunk, pos.x, pos.z)) {
                storage.replace(index, chunk, null);
            }
        }
    }

    public void updateViewCenter(int x, int z) {
        this.storage.viewCenterX = x;
        this.storage.viewCenterZ = z;
    }

    public void updateViewRadius(int viewDistance) {
        int i = storage.chunkRadius;
        int j = calculateStorageRange(viewDistance);
        if (i != j) {
            Storage clientchunkcache$storage = new Storage(j);
            clientchunkcache$storage.viewCenterX = storage.viewCenterX;
            clientchunkcache$storage.viewCenterZ = storage.viewCenterZ;

            for (int k = 0; k < storage.chunks.length(); k++) {
                MapChunk chunk = storage.chunks.get(k);
                if (chunk != null) {
                    ChunkPos chunkpos = chunk.getPos();
                    if (clientchunkcache$storage.inRange(chunkpos.x, chunkpos.z)) {
                        clientchunkcache$storage.replace(clientchunkcache$storage.getIndex(chunkpos.x, chunkpos.z), chunk);
                    }
                }
            }

            this.storage = clientchunkcache$storage;
        }
    }

    /// 从原版获取区块
    ///
    /// @return 新获取的区块
    public @Nullable MapChunk setChunk(ChunkAccess chunkAccess, boolean copy) {
        ChunkPos pos = chunkAccess.getPos();
        if (storage.inRange(pos.x, pos.z)) {
            MapChunk chunk = MapChunk.fromVanilla(level, chunkAccess, copy);
            storage.replace(storage.getIndex(pos.x, pos.z), chunk);
            chunkTypeCache.put(pos.toLong(), CHUNK_TYPE_FULL);
            return chunk;
        }
        return null;
    }

    /// 获取正在动态更新的区块
    public @Nullable MapChunk getChunk(int x, int z) {
        if (storage.inRange(x, z)) {
            MapChunk chunk = storage.getChunk(storage.getIndex(x, z));
            if (isValidChunk(chunk, x, z)) {
                return chunk;
            }
        }
        return null;
    }

    /// 获取所有可见的区块
    ///
    /// @param load 设置为true时，如果缓存中没有则立刻开始读取，且不阻塞
    public @Nullable MapChunk getVisibleChunk(int x, int z, boolean load) {
        long l = ChunkPos.asLong(x, z);
        MapChunk chunk = visibleMapChunks.get(l);
        if (chunk == null && load) {
            byte type = chunkTypeCache.get(l);
            if (type == CHUNK_TYPE_UNKNOWN || type == CHUNK_TYPE_REPLACEABLE) return null;

            CompletableFuture<MapChunk> task = scheduledLoadingChunks.computeIfAbsent(l, j -> {
                MapSerializer serializer = ClientMap.getOrCreateSerializer(level.dimension);
                return serializer.scheduleChunkLoad(level, new ChunkPos(x, z));
            });
            if (task == MapSerializer.FAILED_TO_LOAD_MAP_CHUNK) {
                chunkTypeCache.put(l, CHUNK_TYPE_REPLACEABLE);
                return null;
            }
            chunk = task.getNow(null);
            if (chunk == MapSerializer.UNKNOWN_MAP_CHUNK) {
                chunkTypeCache.put(l, CHUNK_TYPE_UNKNOWN);
            } else {
                visibleMapChunks.put(l, chunk);
                scheduledLoadingChunks.remove(l);
                chunkTypeCache.put(l, CHUNK_TYPE_FULL);
            }
        }
        return chunk;
    }

    static boolean isValidChunk(@Nullable MapChunk chunk, int x, int z) {
        if (chunk == null) {
            return false;
        }
        ChunkPos chunkpos = chunk.getPos();
        return chunkpos.x == x && chunkpos.z == z;
    }

    private static int calculateStorageRange(int viewDistance) {
        return Math.max(2, viewDistance) + 3;
    }

    public int viewCenterX() {
        return storage.viewCenterX;
    }

    public int viewCenterZ() {
        return storage.viewCenterZ;
    }

    public int chunkRadius() {
        return storage.chunkRadius;
    }

    public int getChunkCount() {
        return storage.chunkCount;
    }

    final class Storage {
        final AtomicReferenceArray<MapChunk> chunks;
        final int chunkRadius;
        private final int viewRange;
        volatile int viewCenterX;
        volatile int viewCenterZ;
        int chunkCount;

        Storage(int chunkRadius) {
            this.chunkRadius = chunkRadius;
            this.viewRange = chunkRadius * 2 + 1;
            this.chunks = new AtomicReferenceArray<>(viewRange * viewRange);
        }

        int getIndex(int x, int z) {
            return Math.floorMod(z, viewRange) * viewRange + Math.floorMod(x, viewRange);
        }

        void replace(int chunkIndex, @Nullable MapChunk chunk) {
            MapChunk mapChunk = chunks.getAndSet(chunkIndex, chunk);
            if (mapChunk != null) {
                this.chunkCount--;
                MapChunkCache.this.level.unload(mapChunk);
            }

            if (chunk != null) {
                this.chunkCount++;
            }
        }

        MapChunk replace(int chunkIndex, MapChunk chunk, @Nullable MapChunk replaceWith) {
            if (chunks.compareAndSet(chunkIndex, chunk, replaceWith) && replaceWith == null) {
                this.chunkCount--;
            }

            MapChunkCache.this.level.unload(chunk);
            return chunk;
        }

        boolean inRange(int x, int z) {
            return Math.abs(x - viewCenterX) <= chunkRadius && Math.abs(z - viewCenterZ) <= chunkRadius;
        }

        @Nullable MapChunk getChunk(int chunkIndex) {
            return chunks.get(chunkIndex);
        }
    }
}
