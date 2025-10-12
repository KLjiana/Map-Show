package com.hismeo.map_show.client.screen;

import net.minecraft.util.Mth;

//泛型？
public class LerpStorage {
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