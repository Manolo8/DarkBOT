package com.github.manolo8.darkbot.core.entities;

import com.google.gson.annotations.SerializedName;

public class Hangar {

    @SerializedName("hangarID")
    private String hangarId;

    @SerializedName("hangar_is_active")
    private boolean hangarIsActive;

    public Hangar(String hangarId, boolean hangarIsActive) {
        this.hangarId = hangarId;
        this.hangarIsActive = hangarIsActive;
    }

    public String getHangarID() {
        return hangarId;
    }

    public boolean hangarIsActive() {
        return hangarIsActive;
    }

}
