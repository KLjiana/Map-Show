package com.hismeo.map_show.client;

import com.hismeo.map_show.MapShow;
import net.minecraft.world.level.ChunkPos;

import javax.annotation.Nullable;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
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

    public MapChunk getChunk(int x, int z) {
        if (storage.inRange(x, z)) {
            MapChunk chunk = storage.getChunk(storage.getIndex(x, z));
            if (isValidChunk(chunk, x, z)) {
                return chunk;
            }
        }
        throw new NullPointerException();
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
            MapChunk levelchunk = chunks.getAndSet(chunkIndex, chunk);
            if (levelchunk != null) {
                this.chunkCount--;
                MapChunkCache.this.level.unload(levelchunk);
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

        @Nullable
        MapChunk getChunk(int chunkIndex) {
            return chunks.get(chunkIndex);
        }

        private void dumpChunks(String filePath) {
            try (FileOutputStream fileoutputstream = new FileOutputStream(filePath)) {
                int i = MapChunkCache.this.storage.chunkRadius;

                for (int j = viewCenterZ - i; j <= viewCenterZ + i; j++) {
                    for (int k = viewCenterX - i; k <= viewCenterX + i; k++) {
                        MapChunk mapChunk = MapChunkCache.this.storage.chunks.get(MapChunkCache.this.storage.getIndex(k, j));
                        if (mapChunk != null) {
                            ChunkPos chunkpos = mapChunk.getPos();
                            fileoutputstream.write((chunkpos.x + "\t" + chunkpos.z + "\n").getBytes(StandardCharsets.UTF_8));
                        }
                    }
                }
            } catch (IOException ioexception) {
                MapShow.LOGGER.error("Failed to dump chunks to file {}", filePath, ioexception);
            }
        }
    }
}
