package com.github.squi2rel.arc.event;

import arc.files.Fi;
import arc.struct.Seq;

import java.util.Objects;

/**
 * All files from one drop into the application window, in drop order.
 */
public final class FileDroppedEvent extends GameEvent {
    public final Seq<Fi> files;

    public FileDroppedEvent(Seq<Fi> files) {
        this.files = Objects.requireNonNull(files, "files");
    }
}
