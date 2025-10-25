package com.hismeo.map_show.client.render.block;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.model.data.ModelData;

import javax.annotation.Nullable;
import java.util.BitSet;
import java.util.List;

public class NormalRenderer {
    static final Direction[] DIRECTIONS = Direction.values();
    private final BlockColors blockColors;

    public NormalRenderer(BlockColors blockColors) {
        this.blockColors = blockColors;
    }

    /**
     * @param checkSides if {@code true}, only renders each side if {@link
     *                   net.minecraft.world.level.block.Block#shouldRenderFace(
     *net.minecraft.world.level.block.state.BlockState,
     *                   net.minecraft.world.level.BlockGetter,
     *                   net.minecraft.core.BlockPos, net.minecraft.core.Direction,
     *                   net.minecraft.core.BlockPos)} returns {@code true}
     */
    public void tesselateBlock(
            BlockAndTintGetter level,
            BakedModel model,
            BlockState state,
            BlockPos pos,
            PoseStack poseStack,
            VertexConsumer consumer,
            boolean checkSides,
            RandomSource random,
            long seed,
            int packedOverlay,
            ModelData modelData,
            RenderType renderType
    ) {
        boolean flag = Minecraft.useAmbientOcclusion() && switch (model.useAmbientOcclusion(state, modelData, renderType)) {
            case TRUE -> true;
            case DEFAULT -> state.getLightEmission(level, pos) == 0;
            case FALSE -> false;
        };
        Vec3 vec3 = state.getOffset(level, pos);
        poseStack.translate(vec3.x, vec3.y, vec3.z);

        try {
            if (flag) {
                this.tesselateWithAO(level, model, state, pos, poseStack, consumer, checkSides, random, seed, packedOverlay, modelData, renderType);
            } else {
                this.tesselateWithoutAO(level, model, state, pos, poseStack, consumer, checkSides, random, seed, packedOverlay, modelData, renderType);
            }
        } catch (Throwable throwable) {
            CrashReport crashreport = CrashReport.forThrowable(throwable, "Tesselating block model");
            CrashReportCategory crashreportcategory = crashreport.addCategory("Block model being tesselated");
            CrashReportCategory.populateBlockDetails(crashreportcategory, level, pos, state);
            crashreportcategory.setDetail("Using AO", flag);
            throw new ReportedException(crashreport);
        }
    }

    /**
     * @param checkSides if {@code true}, only renders each side if {@link
     *                   net.minecraft.world.level.block.Block#shouldRenderFace(
     *net.minecraft.world.level.block.state.BlockState,
     *                   net.minecraft.world.level.BlockGetter,
     *                   net.minecraft.core.BlockPos, net.minecraft.core.Direction,
     *                   net.minecraft.core.BlockPos)} returns {@code true}
     */
    @Deprecated //Forge: Model data and render type parameter
    public void tesselateWithAO(
            BlockAndTintGetter level,
            BakedModel model,
            BlockState state,
            BlockPos pos,
            PoseStack poseStack,
            VertexConsumer consumer,
            boolean checkSides,
            RandomSource random,
            long seed,
            int packedOverlay
    ) {
        tesselateWithAO(level, model, state, pos, poseStack, consumer, checkSides, random, seed, packedOverlay, ModelData.EMPTY, null);
    }

    /**
     * @param checkSides if {@code true}, only renders each side if {@link
     *                   net.minecraft.world.level.block.Block#shouldRenderFace(
     *net.minecraft.world.level.block.state.BlockState,
     *                   net.minecraft.world.level.BlockGetter,
     *                   net.minecraft.core.BlockPos, net.minecraft.core.Direction,
     *                   net.minecraft.core.BlockPos)} returns {@code true}
     */
    public void tesselateWithAO(
            BlockAndTintGetter level,
            BakedModel model,
            BlockState state,
            BlockPos pos,
            PoseStack poseStack,
            VertexConsumer consumer,
            boolean checkSides,
            RandomSource random,
            long seed,
            int packedOverlay,
            ModelData modelData,
            RenderType renderType
    ) {
        float[] afloat = new float[DIRECTIONS.length * 2];
        BitSet bitset = new BitSet(3);
        ModelBlockRenderer.AmbientOcclusionFace modelblockrenderer$ambientocclusionface = new ModelBlockRenderer.AmbientOcclusionFace();
        BlockPos.MutableBlockPos blockpos$mutableblockpos = pos.mutable();

        for (Direction direction : DIRECTIONS) {
            random.setSeed(seed);
            List<BakedQuad> list = model.getQuads(state, direction, random, modelData, renderType);
            if (!list.isEmpty()) {
                blockpos$mutableblockpos.setWithOffset(pos, direction);
                if (!checkSides || Block.shouldRenderFace(state, level, pos, direction, blockpos$mutableblockpos)) {
                    this.renderModelFaceAO(
                            level, state, pos, poseStack, consumer, list, afloat, bitset, modelblockrenderer$ambientocclusionface, packedOverlay
                    );
                }
            }
        }

        random.setSeed(seed);
        List<BakedQuad> list1 = model.getQuads(state, null, random, modelData, renderType);
        if (!list1.isEmpty()) {
            this.renderModelFaceAO(
                    level, state, pos, poseStack, consumer, list1, afloat, bitset, modelblockrenderer$ambientocclusionface, packedOverlay
            );
        }
    }

    /**
     * @param checkSides if {@code true}, only renders each side if {@link
     *                   net.minecraft.world.level.block.Block#shouldRenderFace(
     *net.minecraft.world.level.block.state.BlockState,
     *                   net.minecraft.world.level.BlockGetter,
     *                   net.minecraft.core.BlockPos, net.minecraft.core.Direction,
     *                   net.minecraft.core.BlockPos)} returns {@code true}
     */
    @Deprecated //Forge: Model data and render type parameter
    public void tesselateWithoutAO(
            BlockAndTintGetter level,
            BakedModel model,
            BlockState state,
            BlockPos pos,
            PoseStack poseStack,
            VertexConsumer consumer,
            boolean checkSides,
            RandomSource random,
            long seed,
            int packedOverlay
    ) {
        tesselateWithoutAO(level, model, state, pos, poseStack, consumer, checkSides, random, seed, packedOverlay, ModelData.EMPTY, null);
    }

    /**
     * @param checkSides if {@code true}, only renders each side if {@link
     *                   net.minecraft.world.level.block.Block#shouldRenderFace(
     *net.minecraft.world.level.block.state.BlockState,
     *                   net.minecraft.world.level.BlockGetter,
     *                   net.minecraft.core.BlockPos, net.minecraft.core.Direction,
     *                   net.minecraft.core.BlockPos)} returns {@code true}
     */
    public void tesselateWithoutAO(
            BlockAndTintGetter level,
            BakedModel model,
            BlockState state,
            BlockPos pos,
            PoseStack poseStack,
            VertexConsumer consumer,
            boolean checkSides,
            RandomSource random,
            long seed,
            int packedOverlay,
            ModelData modelData,
            RenderType renderType
    ) {
        BitSet bitset = new BitSet(3);
        BlockPos.MutableBlockPos blockpos$mutableblockpos = pos.mutable();

        for (Direction direction : DIRECTIONS) {
            random.setSeed(seed);
            List<BakedQuad> list = model.getQuads(state, direction, random, modelData, renderType);
            if (!list.isEmpty()) {
                blockpos$mutableblockpos.setWithOffset(pos, direction);
                if (!checkSides || Block.shouldRenderFace(state, level, pos, direction, blockpos$mutableblockpos)) {
                    int i = LevelRenderer.getLightColor(level, state, blockpos$mutableblockpos);
                    this.renderModelFaceFlat(level, state, pos, i, packedOverlay, false, poseStack, consumer, list, bitset);
                }
            }
        }

        random.setSeed(seed);
        List<BakedQuad> list1 = model.getQuads(state, null, random, modelData, renderType);
        if (!list1.isEmpty()) {
            this.renderModelFaceFlat(level, state, pos, -1, packedOverlay, true, poseStack, consumer, list1, bitset);
        }
    }

    /**
     * @param shape      the array, of length 12, to store the shape bounds in
     * @param shapeFlags the bit set to store the shape flags in. The first bit will
     *                   be {@code true} if the face should be offset, and the second
     *                   if the face is less than a block in width and height.
     */
    private void renderModelFaceAO(
            BlockAndTintGetter level,
            BlockState state,
            BlockPos pos,
            PoseStack poseStack,
            VertexConsumer consumer,
            List<BakedQuad> quads,
            float[] shape,
            BitSet shapeFlags,
            ModelBlockRenderer.AmbientOcclusionFace aoFace,
            int packedOverlay
    ) {
        for (BakedQuad bakedquad : quads) {
            this.calculateShape(level, state, pos, bakedquad.getVertices(), bakedquad.getDirection(), shape, shapeFlags);
            if (!net.neoforged.neoforge.client.ClientHooks.calculateFaceWithoutAO(level, state, pos, bakedquad, shapeFlags.get(0), aoFace.brightness, aoFace.lightmap))
                aoFace.calculate(level, state, pos, bakedquad.getDirection(), shape, shapeFlags, bakedquad.isShade());
            this.putQuadData(
                    level,
                    state,
                    pos,
                    consumer,
                    poseStack.last(),
                    bakedquad,
                    aoFace.brightness[0],
                    aoFace.brightness[1],
                    aoFace.brightness[2],
                    aoFace.brightness[3],
                    aoFace.lightmap[0],
                    aoFace.lightmap[1],
                    aoFace.lightmap[2],
                    aoFace.lightmap[3],
                    packedOverlay
            );
        }
    }

    private void putQuadData(
            BlockAndTintGetter level,
            BlockState state,
            BlockPos pos,
            VertexConsumer consumer,
            PoseStack.Pose pose,
            BakedQuad quad,
            float brightness0,
            float brightness1,
            float brightness2,
            float brightness3,
            int lightmap0,
            int lightmap1,
            int lightmap2,
            int lightmap3,
            int packedOverlay
    ) {
        float f;
        float f1;
        float f2;
        if (quad.isTinted()) {
            int i = this.blockColors.getColor(state, level, pos, quad.getTintIndex());
            f = (float) (i >> 16 & 0xFF) / 255.0F;
            f1 = (float) (i >> 8 & 0xFF) / 255.0F;
            f2 = (float) (i & 0xFF) / 255.0F;
        } else {
            f = 1.0F;
            f1 = 1.0F;
            f2 = 1.0F;
        }

        consumer.putBulkData(
                pose,
                quad,
                new float[]{brightness0, brightness1, brightness2, brightness3},
                f,
                f1,
                f2,
                1.0F,
                new int[]{lightmap0, lightmap1, lightmap2, lightmap3},
                packedOverlay,
                true
        );
    }

    /**
     * Calculates the shape and corresponding flags for the specified {@code direction} and {@code vertices}, storing the resulting shape in the specified {@code shape} array and the shape flags in {@code shapeFlags}.
     *
     * @param shape      the array, of length 12, to store the shape bounds in, or {@code null} to only calculate shape flags
     * @param shapeFlags the bit set to store the shape flags in. The first bit will
     *                   be {@code true} if the face should be offset, and the second
     *                   if the face is less than a block in width and height.
     */
    private void calculateShape(
            BlockAndTintGetter level,
            BlockState state,
            BlockPos pos,
            int[] vertices,
            Direction direction,
            @Nullable float[] shape,
            BitSet shapeFlags
    ) {
        float f = 32.0F;
        float f1 = 32.0F;
        float f2 = 32.0F;
        float f3 = -32.0F;
        float f4 = -32.0F;
        float f5 = -32.0F;

        for (int i = 0; i < 4; i++) {
            float f6 = Float.intBitsToFloat(vertices[i * 8]);
            float f7 = Float.intBitsToFloat(vertices[i * 8 + 1]);
            float f8 = Float.intBitsToFloat(vertices[i * 8 + 2]);
            f = Math.min(f, f6);
            f1 = Math.min(f1, f7);
            f2 = Math.min(f2, f8);
            f3 = Math.max(f3, f6);
            f4 = Math.max(f4, f7);
            f5 = Math.max(f5, f8);
        }

        if (shape != null) {
            shape[Direction.WEST.get3DDataValue()] = f;
            shape[Direction.EAST.get3DDataValue()] = f3;
            shape[Direction.DOWN.get3DDataValue()] = f1;
            shape[Direction.UP.get3DDataValue()] = f4;
            shape[Direction.NORTH.get3DDataValue()] = f2;
            shape[Direction.SOUTH.get3DDataValue()] = f5;
            int j = DIRECTIONS.length;
            shape[Direction.WEST.get3DDataValue() + j] = 1.0F - f;
            shape[Direction.EAST.get3DDataValue() + j] = 1.0F - f3;
            shape[Direction.DOWN.get3DDataValue() + j] = 1.0F - f1;
            shape[Direction.UP.get3DDataValue() + j] = 1.0F - f4;
            shape[Direction.NORTH.get3DDataValue() + j] = 1.0F - f2;
            shape[Direction.SOUTH.get3DDataValue() + j] = 1.0F - f5;
        }

        float f9 = 1.0E-4F;
        float f10 = 0.9999F;
        switch (direction) {
            case DOWN:
                shapeFlags.set(1, f >= 1.0E-4F || f2 >= 1.0E-4F || f3 <= 0.9999F || f5 <= 0.9999F);
                shapeFlags.set(0, f1 == f4 && (f1 < 1.0E-4F || state.isCollisionShapeFullBlock(level, pos)));
                break;
            case UP:
                shapeFlags.set(1, f >= 1.0E-4F || f2 >= 1.0E-4F || f3 <= 0.9999F || f5 <= 0.9999F);
                shapeFlags.set(0, f1 == f4 && (f4 > 0.9999F || state.isCollisionShapeFullBlock(level, pos)));
                break;
            case NORTH:
                shapeFlags.set(1, f >= 1.0E-4F || f1 >= 1.0E-4F || f3 <= 0.9999F || f4 <= 0.9999F);
                shapeFlags.set(0, f2 == f5 && (f2 < 1.0E-4F || state.isCollisionShapeFullBlock(level, pos)));
                break;
            case SOUTH:
                shapeFlags.set(1, f >= 1.0E-4F || f1 >= 1.0E-4F || f3 <= 0.9999F || f4 <= 0.9999F);
                shapeFlags.set(0, f2 == f5 && (f5 > 0.9999F || state.isCollisionShapeFullBlock(level, pos)));
                break;
            case WEST:
                shapeFlags.set(1, f1 >= 1.0E-4F || f2 >= 1.0E-4F || f4 <= 0.9999F || f5 <= 0.9999F);
                shapeFlags.set(0, f == f3 && (f < 1.0E-4F || state.isCollisionShapeFullBlock(level, pos)));
                break;
            case EAST:
                shapeFlags.set(1, f1 >= 1.0E-4F || f2 >= 1.0E-4F || f4 <= 0.9999F || f5 <= 0.9999F);
                shapeFlags.set(0, f == f3 && (f3 > 0.9999F || state.isCollisionShapeFullBlock(level, pos)));
        }
    }

    /**
     * @param repackLight {@code true} if packed light should be re-calculated
     * @param shapeFlags  the bit set to store the shape flags in. The first bit will
     *                    be {@code true} if the face should be offset, and the second
     *                    if the face is less than a block in width and height.
     */
    private void renderModelFaceFlat(
            BlockAndTintGetter level,
            BlockState state,
            BlockPos pos,
            int packedLight,
            int packedOverlay,
            boolean repackLight,
            PoseStack poseStack,
            VertexConsumer consumer,
            List<BakedQuad> quads,
            BitSet shapeFlags
    ) {
        for (BakedQuad bakedquad : quads) {
            if (repackLight) {
                this.calculateShape(level, state, pos, bakedquad.getVertices(), bakedquad.getDirection(), null, shapeFlags);
                BlockPos blockpos = shapeFlags.get(0) ? pos.relative(bakedquad.getDirection()) : pos;
                packedLight = LevelRenderer.getLightColor(level, state, blockpos);
            }

            float f = level.getShade(bakedquad.getDirection(), bakedquad.isShade());
            this.putQuadData(
                    level, state, pos, consumer, poseStack.last(), bakedquad, f, f, f, f, packedLight, packedLight, packedLight, packedLight, packedOverlay
            );
        }
    }

    @Deprecated //Forge: Model data and render type parameter
    public void renderModel(
            PoseStack.Pose pose,
            VertexConsumer consumer,
            @Nullable BlockState state,
            BakedModel model,
            float red,
            float green,
            float blue,
            int packedLight,
            int packedOverlay
    ) {
        renderModel(pose, consumer, state, model, red, green, blue, packedLight, packedOverlay, ModelData.EMPTY, null);
    }

    public void renderModel(
            PoseStack.Pose pose,
            VertexConsumer consumer,
            @Nullable BlockState state,
            BakedModel model,
            float red,
            float green,
            float blue,
            int packedLight,
            int packedOverlay,
            ModelData modelData,
            RenderType renderType
    ) {
        RandomSource randomsource = RandomSource.create();
        long i = 42L;

        for (Direction direction : DIRECTIONS) {
            randomsource.setSeed(42L);
            renderQuadList(pose, consumer, red, green, blue, model.getQuads(state, direction, randomsource, modelData, renderType), packedLight, packedOverlay);
        }

        randomsource.setSeed(42L);
        renderQuadList(pose, consumer, red, green, blue, model.getQuads(state, null, randomsource, modelData, renderType), packedLight, packedOverlay);
    }

    private static void renderQuadList(
            PoseStack.Pose pose,
            VertexConsumer consumer,
            float red,
            float green,
            float blue,
            List<BakedQuad> quads,
            int packedLight,
            int packedOverlay
    ) {
        for (BakedQuad bakedquad : quads) {
            float f;
            float f1;
            float f2;
            if (bakedquad.isTinted()) {
                f = Mth.clamp(red, 0.0F, 1.0F);
                f1 = Mth.clamp(green, 0.0F, 1.0F);
                f2 = Mth.clamp(blue, 0.0F, 1.0F);
            } else {
                f = 1.0F;
                f1 = 1.0F;
                f2 = 1.0F;
            }

            consumer.putBulkData(pose, bakedquad, f, f1, f2, 1.0F, packedLight, packedOverlay);
        }
    }
}
