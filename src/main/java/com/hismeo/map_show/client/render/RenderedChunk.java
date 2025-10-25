package com.hismeo.map_show.client.render;

import net.minecraft.world.level.ChunkPos;

public record RenderedChunk(ChunkPos chunkPos) implements AutoCloseable {
    @Override
    public void close() {

    }
}
