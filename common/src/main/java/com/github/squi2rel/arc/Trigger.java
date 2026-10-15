package com.github.squi2rel.arc;

/** Parameterless application notifications, subscribed to with {@code Events.run}. */
public enum Trigger{
    /** The Arc runtime is initialized. */
    init,
    /** Initial client loading or dedicated-server startup has completed. */
    ready,
    /** One client or dedicated-server tick, independent of rendering. */
    update,
    /** Once per client frame, immediately before the main scene is drawn. */
    draw,
    /** The effective UI scale has changed. Existing UI may need to rebuild its layout. */
    scaleChanged,
    /** Minecraft font resources changed; text and existing windows must be laid out again. */
    fontsChanged,
    /** A graceful application shutdown, before disposal. */
    exit,
    /** Application disposal, while backend resources are still available. */
    dispose
}
