package com.hismeo.map_show.client;

import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicReferenceArray;

/**
 * @see net.minecraft.client.multiplayer.ClientChunkCache
 */
public class MapChunkCache {
    volatile Storage storage;
    final MapLevel level;

    public MapChunkCache(MapLevel level, int viewDistance) {
        this.level = level;
        this.storage = new Storage(calculateStorageRange(viewDistance));
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

    public void setChunk(ChunkAccess chunkAccess, boolean copy) {
        ChunkPos pos = chunkAccess.getPos();
        if (storage.inRange(pos.x, pos.z)) {
            storage.replace(storage.getIndex(pos.x, pos.z), MapChunk.fromVanilla(level, chunkAccess, copy));
        }
    }

    public @Nullable MapChunk getChunk(int x, int z) {
        if (storage.inRange(x, z)) {
            MapChunk chunk = storage.getChunk(storage.getIndex(x, z));
            if (isValidChunk(chunk, x, z)) {
                return chunk;
            }
        }
        return null;
    }

    private static boolean isValidChunk(@Nullable MapChunk chunk, int x, int z) {
        if (chunk == null) {
            return false;
        } else {
            ChunkPos chunkpos = chunk.getPos();
            return chunkpos.x == x && chunkpos.z == z;
        }
    }

    private static int calculateStorageRange(int viewDistance) {
        return Math.max(2, viewDistance) + 3;
    }

    static final class Storage {
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
                //MapChunkCache.this.level.unload(mapChunk);
            }

            if (chunk != null) {
                this.chunkCount++;
            }
        }

        boolean inRange(int x, int z) {
            return Math.abs(x - viewCenterX) <= chunkRadius && Math.abs(z - viewCenterZ) <= chunkRadius;
        }

        @Nullable
        MapChunk getChunk(int chunkIndex) {
            return chunks.get(chunkIndex);
        }
    }
}
