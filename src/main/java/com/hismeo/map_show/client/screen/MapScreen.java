package com.hismeo.map_show.client.screen;

import com.hismeo.map_show.client.ClientMap;
import com.hismeo.map_show.client.MapLevel;
import com.hismeo.map_show.client.render.ChunkRenderAllocator;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.NotNull;

/**
 * @see ClientMap#getCurrentLevel(ClientLevel) 获取MapLevel
 */
public class MapScreen extends Screen {
    private final static float ROT_SPEED = 0.5f;
    private final static float TRAN_SPEED = 1.0f;
    private final static float MAX_SCALE = 60f;
    private final static float MIN_SCALE = 0.5f;
    private final static float MAX_Y_ROT = 90f;
    private final static float MIN_Y_ROT = -60;
    private final LerpStorage xTran = new LerpStorage(0.0f);
    private final LerpStorage yTran = new LerpStorage(0.0f);
    private final LerpStorage xRot = new LerpStorage(45.0f);
    private final LerpStorage yRot = new LerpStorage(25.0f);
    private final LerpStorage scale = new LerpStorage(20.0f);
    private double lastMouseX;
    private double lastMouseY;
    private final MapLevel mapLevel;
    private ChunkRenderAllocator chunkRenderAllocator;

    public MapScreen(@NotNull MapLevel mapLevel) {
        //TODO title补齐
        super(CommonComponents.EMPTY);
        this.mapLevel = mapLevel;
    }

    @Override
    protected void init() {
        super.init();
        this.chunkRenderAllocator = new ChunkRenderAllocator(mapLevel, minecraft.getBlockColors(), minecraft.getModelManager().getBlockModelShaper());
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        float deltaTicks = minecraft.getTimer() instanceof DeltaTracker.Timer timer ? timer.deltaTickResidual : partialTick;
        renderDebugMessage(guiGraphics, deltaTicks);

        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();

        poseStack.translate(width / 2f + xTran.lerp(deltaTicks), height / 2f + yTran.lerp(deltaTicks), 0);
        float s = scale.lerp(deltaTicks);
        poseStack.scale(s, -s, s);
        poseStack.mulPose(Axis.XP.rotationDegrees(yRot.lerp(deltaTicks)));
        poseStack.mulPose(Axis.YP.rotationDegrees(xRot.lerp(deltaTicks)));

        chunkRenderAllocator.renderCurrentChunk(poseStack);

        poseStack.popPose();
    }

    protected void renderDebugMessage(GuiGraphics guiGraphics, float partialTick) {
        if (FMLLoader.isProduction()) return;
        guiGraphics.drawString(font, "xTran: %s, %s".formatted(xTran.getNumber(), xTran.getoNumber()), 0, 0, 0xFFFFFF, false);
        guiGraphics.drawString(font, "yTran: %s, %s".formatted(yTran.getNumber(), yTran.getoNumber()), 0, 10, 0xFFFFFF, false);
        guiGraphics.drawString(font, "xRot: %s, %s".formatted(xRot.getNumber(), xRot.getoNumber()), 0, 20, 0xFFFFFF, false);
        guiGraphics.drawString(font, "yRot: %s, %s".formatted(yRot.getNumber(), yRot.getoNumber()), 0, 30, 0xFFFFFF, false);
        guiGraphics.drawString(font, "scale: %s, %s".formatted(scale.getNumber(), scale.getoNumber()), 0, 40, 0xFFFFFF, false);

        guiGraphics.drawString(font, "partialTick: %s".formatted(partialTick), 0, 55, 0xFFFFFF, false);
    }


    private void testQuad(MultiBufferSource.BufferSource bufferSource, PoseStack poseStack) {
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.DEBUG_QUADS);
        PoseStack.Pose last = poseStack.last();
        consumer.addVertex(last, 0.0f, 0.0f, 0.0f).setColor(1.0F, 1.0F, 1.0F, 1.0F);
        consumer.addVertex(last, -5f, 0.0f, 0.0f).setColor(1.0F, 1.0F, 1.0F, 1.0F);
        consumer.addVertex(last, -5f, -5f, 0.0f).setColor(1.0F, 1.0F, 1.0F, 1.0F);
        consumer.addVertex(last, 0.0f, -5f, 0.0f).setColor(1.0F, 1.0F, 1.0F, 1.0F);
        bufferSource.endBatch();
    }



    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        lastMouseX = mouseX;
        lastMouseY = mouseY;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
        double deltaX = mouseX - lastMouseX;
        double deltaY = mouseY - lastMouseY;

        if (button == 0) {
            xTran.updateNumber(xTran.getNumber() + (float) (deltaX * TRAN_SPEED));
            yTran.updateNumber(yTran.getNumber() + (float) (deltaY * TRAN_SPEED));
        }

        if (button == 1) {
            xRot.updateNumber(xRot.getNumber() + (float) (deltaX * ROT_SPEED));
            yRot.updateNumber(yRot.getNumber() + (float) (deltaY * ROT_SPEED));
            yRot.updateNumber(Math.clamp(yRot.getNumber(), MIN_Y_ROT, MAX_Y_ROT));
        }

        lastMouseX = mouseX;
        lastMouseY = mouseY;

        return super.mouseDragged(mouseX, mouseY, button, dx, dy);
    }

    //TODO 中心缩放
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        float newScale = scale.getNumber() * (1 + (float) scrollY * 0.1f);
        scale.updateNumber(Math.clamp(newScale, MIN_SCALE, MAX_SCALE));
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }
}
