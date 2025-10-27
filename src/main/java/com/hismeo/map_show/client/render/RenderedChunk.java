package com.hismeo.map_show.client.render;

import com.mojang.blaze3d.vertex.VertexBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.ChunkPos;

import java.util.Map;
import java.util.stream.Collectors;

public record RenderedChunk(ChunkPos chunkPos, Map<RenderType, VertexBuffer> renderLayer) {
    public RenderedChunk(ChunkPos chunkPos) {
        this(chunkPos, RenderType.chunkBufferLayers()
                .stream()
                .collect(Collectors.toMap(renderType -> renderType, renderType -> new VertexBuffer(VertexBuffer.Usage.STATIC))));
    }
}
