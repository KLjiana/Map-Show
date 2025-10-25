package com.hismeo.map_show.client.render.block;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.textures.FluidSpriteCache;

public class LiquidRenderer {
    public LiquidRenderer() {

    }

    public void renderLiquid(PoseStack poseStack, BlockAndTintGetter level, BlockPos pos, VertexConsumer buffer, BlockState blockState, FluidState fluidState) {
        boolean isLava = fluidState.is(FluidTags.LAVA);
        TextureAtlasSprite[] atextureatlassprite = FluidSpriteCache.getFluidSprites(level, pos, fluidState);
        int color = IClientFluidTypeExtensions.of(fluidState).getTintColor(fluidState, level, pos);
        float alpha = (float) (color >> 24 & 255) / 255.0F;
        float red = (float) (color >> 16 & 255) / 255.0F;
        float green = (float) (color >> 8 & 255) / 255.0F;
        float blue = (float) (color & 255) / 255.0F;

        BlockState downBlockState = level.getBlockState(pos.relative(Direction.DOWN));
        FluidState downFluidState = downBlockState.getFluidState();
        BlockState upBlockState = level.getBlockState(pos.relative(Direction.UP));
        FluidState upFluidState = upBlockState.getFluidState();
        BlockState northBlockState = level.getBlockState(pos.relative(Direction.NORTH));
        FluidState northFluidState = northBlockState.getFluidState();
        BlockState southBlockState = level.getBlockState(pos.relative(Direction.SOUTH));
        FluidState southFluidState = southBlockState.getFluidState();
        BlockState westBlockState = level.getBlockState(pos.relative(Direction.WEST));
        FluidState westFluidState = westBlockState.getFluidState();
        BlockState eastBlockState = level.getBlockState(pos.relative(Direction.EAST));
        FluidState eastFluidState = eastBlockState.getFluidState();

        boolean renderTop = !isNeighborStateHidingOverlay(fluidState, upBlockState, Direction.DOWN);
        boolean renderDown = shouldRenderFace(level, pos, fluidState, blockState, Direction.DOWN, downBlockState) && !isFaceOccludedByNeighbor(level, pos, Direction.DOWN, 0.8888889F, downBlockState);
        boolean renderNorth = shouldRenderFace(level, pos, fluidState, blockState, Direction.NORTH, northBlockState);
        boolean renderSouth = shouldRenderFace(level, pos, fluidState, blockState, Direction.SOUTH, southBlockState);
        boolean renderWest = shouldRenderFace(level, pos, fluidState, blockState, Direction.WEST, westBlockState);
        boolean renderEast = shouldRenderFace(level, pos, fluidState, blockState, Direction.EAST, eastBlockState);
        boolean shouldRender = renderTop || renderDown || renderEast || renderWest || renderNorth || renderSouth;

        if (shouldRender) {
            float downShade = level.getShade(Direction.DOWN, true);
            float upShade = level.getShade(Direction.UP, true);
            float northShade = level.getShade(Direction.NORTH, true);
            float westShade = level.getShade(Direction.WEST, true);
            Fluid fluid = fluidState.getType();
            float height = this.getHeight(level, fluid, pos, blockState, fluidState);
            float f7;
            float f8;
            float f9;
            float f10;
            if (height >= 1.0F) {
                f7 = 1.0F;
                f8 = 1.0F;
                f9 = 1.0F;
                f10 = 1.0F;
            } else {
                float northHeight = this.getHeight(level, fluid, pos.north(), northBlockState, northFluidState);
                float southHeight = this.getHeight(level, fluid, pos.south(), southBlockState, southFluidState);
                float eastHeight = this.getHeight(level, fluid, pos.east(), eastBlockState, eastFluidState);
                float westHeight = this.getHeight(level, fluid, pos.west(), westBlockState, westFluidState);
                f7 = this.calculateAverageHeight(level, fluid, height, northHeight, eastHeight, pos.relative(Direction.NORTH).relative(Direction.EAST));
                f8 = this.calculateAverageHeight(level, fluid, height, northHeight, westHeight, pos.relative(Direction.NORTH).relative(Direction.WEST));
                f9 = this.calculateAverageHeight(level, fluid, height, southHeight, eastHeight, pos.relative(Direction.SOUTH).relative(Direction.EAST));
                f10 = this.calculateAverageHeight(level, fluid, height, southHeight, westHeight, pos.relative(Direction.SOUTH).relative(Direction.WEST));
            }

            float x = (float) (pos.getX() & 15);
            float y = (float) (pos.getY());
            float z = (float) (pos.getZ() & 15);
            float f39 = 0.001F;
            float f16 = renderDown ? 0.001F : 0.0F;
            if (renderTop && !isFaceOccludedByNeighbor(level, pos, Direction.UP, Math.min(Math.min(f8, f10), Math.min(f9, f7)), upBlockState)) {
                f8 -= 0.001F;
                f10 -= 0.001F;
                f9 -= 0.001F;
                f7 -= 0.001F;
                Vec3 vec3 = fluidState.getFlow(level, pos);
                float f17;
                float f18;
                float f19;
                float f20;
                float f21;
                float f22;
                float f23;
                float f24;
                if (vec3.x == (double) 0.0F && vec3.z == (double) 0.0F) {
                    TextureAtlasSprite textureatlassprite1 = atextureatlassprite[0];
                    f17 = textureatlassprite1.getU(0.0F);
                    f21 = textureatlassprite1.getV(0.0F);
                    f18 = f17;
                    f22 = textureatlassprite1.getV(1.0F);
                    f19 = textureatlassprite1.getU(1.0F);
                    f23 = f22;
                    f20 = f19;
                    f24 = f21;
                } else {
                    TextureAtlasSprite textureatlassprite = atextureatlassprite[1];
                    float f25 = (float) Mth.atan2(vec3.z, vec3.x) - ((float) Math.PI / 2F);
                    float f26 = Mth.sin(f25) * 0.25F;
                    float f27 = Mth.cos(f25) * 0.25F;
                    float f28 = 0.5F;
                    f17 = textureatlassprite.getU(0.5F + (-f27 - f26));
                    f21 = textureatlassprite.getV(0.5F + -f27 + f26);
                    f18 = textureatlassprite.getU(0.5F + -f27 + f26);
                    f22 = textureatlassprite.getV(0.5F + f27 + f26);
                    f19 = textureatlassprite.getU(0.5F + f27 + f26);
                    f23 = textureatlassprite.getV(0.5F + (f27 - f26));
                    f20 = textureatlassprite.getU(0.5F + (f27 - f26));
                    f24 = textureatlassprite.getV(0.5F + (-f27 - f26));
                }

                float f53 = (f17 + f18 + f19 + f20) / 4.0F;
                float f54 = (f21 + f22 + f23 + f24) / 4.0F;
                float f55 = atextureatlassprite[0].uvShrinkRatio();
                f17 = Mth.lerp(f55, f17, f53);
                f18 = Mth.lerp(f55, f18, f53);
                f19 = Mth.lerp(f55, f19, f53);
                f20 = Mth.lerp(f55, f20, f53);
                f21 = Mth.lerp(f55, f21, f54);
                f22 = Mth.lerp(f55, f22, f54);
                f23 = Mth.lerp(f55, f23, f54);
                f24 = Mth.lerp(f55, f24, f54);
                int l = this.getLightColor(level, pos);
                float f57 = upShade * red;
                float f29 = upShade * green;
                float f30 = upShade * blue;
                this.vertex(buffer, poseStack, x + 0.0F, y + f8, z + 0.0F, f57, f29, f30, alpha, f17, f21, l);
                this.vertex(buffer, poseStack, x + 0.0F, y + f10, z + 1.0F, f57, f29, f30, alpha, f18, f22, l);
                this.vertex(buffer, poseStack, x + 1.0F, y + f9, z + 1.0F, f57, f29, f30, alpha, f19, f23, l);
                this.vertex(buffer, poseStack, x + 1.0F, y + f7, z + 0.0F, f57, f29, f30, alpha, f20, f24, l);
                if (fluidState.shouldRenderBackwardUpFace(level, pos.above())) {
                    this.vertex(buffer, poseStack, x + 0.0F, y + f8, z + 0.0F, f57, f29, f30, alpha, f17, f21, l);
                    this.vertex(buffer, poseStack, x + 1.0F, y + f7, z + 0.0F, f57, f29, f30, alpha, f20, f24, l);
                    this.vertex(buffer, poseStack, x + 1.0F, y + f9, z + 1.0F, f57, f29, f30, alpha, f19, f23, l);
                    this.vertex(buffer, poseStack, x + 0.0F, y + f10, z + 1.0F, f57, f29, f30, alpha, f18, f22, l);
                }
            }

            if (renderDown) {
                float f40 = atextureatlassprite[0].getU0();
                float f41 = atextureatlassprite[0].getU1();
                float f42 = atextureatlassprite[0].getV0();
                float f43 = atextureatlassprite[0].getV1();
                int k = this.getLightColor(level, pos.below());
                float f46 = downShade * red;
                float f48 = downShade * green;
                float f50 = downShade * blue;
                this.vertex(buffer, poseStack, x, y + f16, z + 1.0F, f46, f48, f50, alpha, f40, f43, k);
                this.vertex(buffer, poseStack, x, y + f16, z, f46, f48, f50, alpha, f40, f42, k);
                this.vertex(buffer, poseStack, x + 1.0F, y + f16, z, f46, f48, f50, alpha, f41, f42, k);
                this.vertex(buffer, poseStack, x + 1.0F, y + f16, z + 1.0F, f46, f48, f50, alpha, f41, f43, k);
            }

            int j = this.getLightColor(level, pos);

            for (Direction direction : Direction.Plane.HORIZONTAL) {
                float f44;
                float f45;
                float f47;
                float f49;
                float f51;
                float f52;
                boolean flag7;
                switch (direction) {
                    case NORTH:
                        f44 = f8;
                        f45 = f7;
                        f47 = x;
                        f51 = x + 1.0F;
                        f49 = z + 0.001F;
                        f52 = z + 0.001F;
                        flag7 = renderNorth;
                        break;
                    case SOUTH:
                        f44 = f9;
                        f45 = f10;
                        f47 = x + 1.0F;
                        f51 = x;
                        f49 = z + 1.0F - 0.001F;
                        f52 = z + 1.0F - 0.001F;
                        flag7 = renderSouth;
                        break;
                    case WEST:
                        f44 = f10;
                        f45 = f8;
                        f47 = x + 0.001F;
                        f51 = x + 0.001F;
                        f49 = z + 1.0F;
                        f52 = z;
                        flag7 = renderWest;
                        break;
                    default:
                        f44 = f7;
                        f45 = f9;
                        f47 = x + 1.0F - 0.001F;
                        f51 = x + 1.0F - 0.001F;
                        f49 = z;
                        f52 = z + 1.0F;
                        flag7 = renderEast;
                }

                if (flag7 && !isFaceOccludedByNeighbor(level, pos, direction, Math.max(f44, f45), level.getBlockState(pos.relative(direction)))) {
                    BlockPos blockpos = pos.relative(direction);
                    TextureAtlasSprite textureatlassprite2 = atextureatlassprite[1];
                    if (atextureatlassprite[2] != null && level.getBlockState(blockpos).shouldDisplayFluidOverlay(level, blockpos, fluidState)) {
                        textureatlassprite2 = atextureatlassprite[2];
                    }

                    float f56 = textureatlassprite2.getU(0.0F);
                    float f58 = textureatlassprite2.getU(0.5F);
                    float f59 = textureatlassprite2.getV((1.0F - f44) * 0.5F);
                    float f60 = textureatlassprite2.getV((1.0F - f45) * 0.5F);
                    float f31 = textureatlassprite2.getV(0.5F);
                    float f32 = direction.getAxis() == Direction.Axis.Z ? northShade : westShade;
                    float f33 = upShade * f32 * red;
                    float f34 = upShade * f32 * green;
                    float f35 = upShade * f32 * blue;
                    this.vertex(buffer, poseStack, f47, y + f44, f49, f33, f34, f35, alpha, f56, f59, j);
                    this.vertex(buffer, poseStack, f51, y + f45, f52, f33, f34, f35, alpha, f58, f60, j);
                    this.vertex(buffer, poseStack, f51, y + f16, f52, f33, f34, f35, alpha, f58, f31, j);
                    this.vertex(buffer, poseStack, f47, y + f16, f49, f33, f34, f35, alpha, f56, f31, j);
                    if (textureatlassprite2 != atextureatlassprite[2]) {
                        this.vertex(buffer, poseStack, f47, y + f16, f49, f33, f34, f35, alpha, f56, f31, j);
                        this.vertex(buffer, poseStack, f51, y + f16, f52, f33, f34, f35, alpha, f58, f31, j);
                        this.vertex(buffer, poseStack, f51, y + f45, f52, f33, f34, f35, alpha, f58, f60, j);
                        this.vertex(buffer, poseStack, f47, y + f44, f49, f33, f34, f35, alpha, f56, f59, j);
                    }
                }
            }
        }

    }

    private float calculateAverageHeight(BlockAndTintGetter level, Fluid fluid, float currentHeight, float height1, float height2, BlockPos pos) {
        if (!(height2 >= 1.0F) && !(height1 >= 1.0F)) {
            float[] afloat = new float[2];
            if (height2 > 0.0F || height1 > 0.0F) {
                float f = this.getHeight(level, fluid, pos);
                if (f >= 1.0F) {
                    return 1.0F;
                }

                this.addWeightedHeight(afloat, f);
            }

            this.addWeightedHeight(afloat, currentHeight);
            this.addWeightedHeight(afloat, height2);
            this.addWeightedHeight(afloat, height1);
            return afloat[0] / afloat[1];
        } else {
            return 1.0F;
        }
    }

    private void addWeightedHeight(float[] output, float height) {
        if (height >= 0.8F) {
            output[0] += height * 10.0F;
            output[1] += 10.0F;
        } else if (height >= 0.0F) {
            output[0] += height;
            output[1]++;
        }
    }

    private float getHeight(BlockAndTintGetter level, Fluid fluid, BlockPos pos) {
        BlockState blockstate = level.getBlockState(pos);
        return this.getHeight(level, fluid, pos, blockstate, blockstate.getFluidState());
    }

    private void vertex(
            VertexConsumer vertexConsumer,
            PoseStack poseStack,
            float x, float y, float z,
            float red, float green, float blue, float alpha,
            float u, float v,
            int packedLight
    ) {
        vertexConsumer.addVertex(poseStack.last(), x, y, z)
                .setColor(red, green, blue, alpha)
                .setUv(u, v)
                .setLight(packedLight)
                .setNormal(0.0F, 1.0F, 0.0F);
    }

    private float getHeight(BlockAndTintGetter level, Fluid fluid, BlockPos pos, BlockState blockState, FluidState fluidState) {
        if (fluid.isSame(fluidState.getType())) {
            BlockState blockstate = level.getBlockState(pos.above());
            return fluid.isSame(blockstate.getFluidState().getType()) ? 1.0F : fluidState.getOwnHeight();
        } else {
            return !blockState.isSolid() ? 0.0F : -1.0F;
        }
    }

    private int getLightColor(BlockAndTintGetter level, BlockPos pos) {
        int i = LevelRenderer.getLightColor(level, pos);
        int j = LevelRenderer.getLightColor(level, pos.above());
        int k = i & 0xFF;
        int l = j & 0xFF;
        int i1 = i >> 16 & 0xFF;
        int j1 = j >> 16 & 0xFF;
        return Math.max(k, l) | Math.max(i1, j1) << 16;
    }

    public static boolean shouldRenderFace(BlockAndTintGetter level, BlockPos pos, FluidState fluidState, BlockState selfState, Direction direction, BlockState otherState) {
        return !isFaceOccludedBySelf(level, pos, selfState, direction) && !isNeighborStateHidingOverlay(fluidState, otherState, direction.getOpposite());
    }

    private static boolean isFaceOccludedByNeighbor(BlockGetter level, BlockPos pos, Direction side, float height, BlockState blockState) {
        return isFaceOccludedByState(level, side, height, pos.relative(side), blockState);
    }

    private static boolean isFaceOccludedBySelf(BlockGetter level, BlockPos pos, BlockState state, Direction face) {
        return isFaceOccludedByState(level, face.getOpposite(), 1.0F, pos, state);
    }

    private static boolean isNeighborStateHidingOverlay(FluidState selfState, BlockState otherState, Direction neighborFace) {
        return otherState.shouldHideAdjacentFluidFace(neighborFace, selfState);
    }

    private static boolean isFaceOccludedByState(BlockGetter level, Direction face, float height, BlockPos pos, BlockState state) {
        if (state.canOcclude()) {
            VoxelShape voxelshape = Shapes.box(0.0F, 0.0F, 0.0F, 1.0F, height, 1.0F);
            VoxelShape voxelshape1 = state.getOcclusionShape(level, pos);
            return Shapes.blockOccudes(voxelshape, voxelshape1, face);
        } else {
            return false;
        }
    }
}
