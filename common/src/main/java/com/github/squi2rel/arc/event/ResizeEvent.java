package com.github.squi2rel.arc.event;

/** A change to the full application framebuffer size, in pixels. */
public final class ResizeEvent extends GameEvent{
    public final int width, height;

    public ResizeEvent(int width, int height){
        this.width = width;
        this.height = height;
    }
}
