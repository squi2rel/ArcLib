package com.github.squi2rel.arc.event;

import arc.files.Fi;
import java.util.Objects;

/** One file dropped into the application window. */
public final class FileDroppedEvent extends GameEvent{
    public final Fi file;

    public FileDroppedEvent(Fi file){
        this.file = Objects.requireNonNull(file, "file");
    }
}
