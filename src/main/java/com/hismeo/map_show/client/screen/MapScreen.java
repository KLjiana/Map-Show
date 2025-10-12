package com.hismeo.map_show.client.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

/**
 * @see com.hismeo.map_show.client.ClientMap#getCurrentLevel(ClientLevel) 获取MapLevel
 */
public class MapScreen extends Screen {
    private final LerpStorage xTran = new LerpStorage(0.0f);
    private final LerpStorage yTran = new LerpStorage(0.0f);
    private final LerpStorage zTran = new LerpStorage(0.0f);
    private final LerpStorage xRot = new LerpStorage(0.0f);
    private final LerpStorage yRot = new LerpStorage(0.0f);
    private final LerpStorage zRot = new LerpStorage(0.0f);
    private final LerpStorage scale = new LerpStorage(0.0f);

    public MapScreen() {
        //TODO title补齐
        super(CommonComponents.EMPTY);
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        PoseStack poseStack = guiGraphics.pose();
        poseStack.popPose();
        poseStack.pushPose();
    }

    //泛型？
    static class LerpStorage {
        private float number;
        private float oNumber;

        public LerpStorage(float number) {
            this.number = number;
            this.oNumber = number;
        }

        public LerpStorage(float number, float oNumber) {
            this.number = number;
            this.oNumber = oNumber;
        }

        public float lerp(float delta) {
            return Mth.lerp(delta, oNumber, number);
        }

        public float updateNumber(float number) {
            this.oNumber = this.number;
            this.number = number;
            return this.oNumber;
        }

        public float getNumber() {
            return number;
        }

        public float getoNumber() {
            return oNumber;
        }
    }
}
