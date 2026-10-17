package com.ripti.cluerush.model;

import androidx.annotation.ColorRes;

public class Arena {

    private final String id;
    private final String name;
    private final String subtitle;
    private final int colorStartRes;
    private final int colorEndRes;

    public Arena(String id, String name, String subtitle,
                 @ColorRes int colorStartRes, @ColorRes int colorEndRes) {
        this.id = id;
        this.name = name;
        this.subtitle = subtitle;
        this.colorStartRes = colorStartRes;
        this.colorEndRes = colorEndRes;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getSubtitle() {
        return subtitle;
    }

    @ColorRes
    public int getColorStartRes() {
        return colorStartRes;
    }

    @ColorRes
    public int getColorEndRes() {
        return colorEndRes;
    }
}