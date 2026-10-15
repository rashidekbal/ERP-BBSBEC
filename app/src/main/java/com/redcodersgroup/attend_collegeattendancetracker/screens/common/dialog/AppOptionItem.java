package com.redcodersgroup.attend_collegeattendancetracker.screens.common.dialog;

public class AppOptionItem {
    private final String id;
    private final String title;
    private final String subtitle;
    private final int iconRes;
    private final boolean isDestructive;

    public AppOptionItem(String id, String title, int iconRes) {
        this(id, title, null, iconRes, false);
    }

    public AppOptionItem(String id, String title, int iconRes, boolean isDestructive) {
        this(id, title, null, iconRes, isDestructive);
    }

    public AppOptionItem(String id, String title, String subtitle, int iconRes, boolean isDestructive) {
        this.id = id;
        this.title = title;
        this.subtitle = subtitle;
        this.iconRes = iconRes;
        this.isDestructive = isDestructive;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public int getIconRes() {
        return iconRes;
    }

    public boolean isDestructive() {
        return isDestructive;
    }
}
