package com.hismeo.map_show.client.render.block;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap;
import net.minecraft.Util;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.renderer.RenderType;

import java.util.List;
import java.util.Map;

//TODO 分包 解耦 合并逻辑
public class BlockRenderAllocator {
    private static final List<RenderType> RENDER_TYPES = RenderType.chunkBufferLayers();
    private final Map<RenderType, ByteBufferBuilder> buffers = Util.make(new Reference2ObjectArrayMap<>(RENDER_TYPES.size()), map -> {
        for (RenderType rendertype : RENDER_TYPES) {
            map.put(rendertype, new ByteBufferBuilder(rendertype.bufferSize()));
        }
    });
    private final BlockColors blockColors;
    private final LiquidRenderer liquidRenderer;
    private final NormalRenderer normalRenderer;

    public BlockRenderAllocator(BlockColors blockColors) {
        this.blockColors = blockColors;
        this.liquidRenderer = new LiquidRenderer();
        this.normalRenderer = new NormalRenderer(blockColors);
    }

    public void renderBlock() {

    }
}
