package com.mycompany.oop.view;

public class SidebarState {

    public static final int EXPANDED_WIDTH = 252;
    public static final int COLLAPSED_WIDTH = 76;

    private boolean collapsed;

    public boolean isCollapsed() {
        return collapsed;
    }

    public void setCollapsed(boolean collapsed) {
        this.collapsed = collapsed;
    }

    public int getTargetWidth() {
        return collapsed ? COLLAPSED_WIDTH : EXPANDED_WIDTH;
    }
}
