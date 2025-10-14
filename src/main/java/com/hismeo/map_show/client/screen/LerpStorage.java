package com.hismeo.map_show.client.screen;

import net.minecraft.util.Mth;

import java.util.Objects;

//泛型？
public class LerpStorage {
    private float lastDelta = 0;
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
        if (lastDelta > delta) return number;
        lastDelta = delta;
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

    @Override
    public String toString() {
        return "LerpStorage{" +
                "number=" + number +
                ", oNumber=" + oNumber +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        LerpStorage that = (LerpStorage) o;
        return Float.compare(number, that.number) == 0 && Float.compare(oNumber, that.oNumber) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(number, oNumber);
    }
}