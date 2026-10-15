package arc;

import arc.files.Fi;

/**
 * <p>
 * An <code>ApplicationListener</code> is called when the {@link Application} is created, resumed, rendering, paused or destroyed.
 * Graphical backends call these methods on their rendering thread with a graphics context available.
 * Headless backends call them on their application thread without a graphics context.
 * </p>
 *
 * <p>
 * Lifecycle details depend on the backend. Mobile backends support pause/resume transitions;
 * ArcLib desktop and headless backends do not call pause/resume.
 * </p>
 * @author mzechner
 */
public interface ApplicationListener{
    /**
     * Called when the {@link Application} is first created.
     * ArcLib runtimes also initialize listeners registered after application startup, before their first callback.
     */
    default void init(){
    }

    /**
     * Called when the {@link Application} is resized. This can happen at any point during a non-paused state but will never happen
     * before a call to {@link #init()}.
     * @param width the new width in pixels
     * @param height the new height in pixels
     */
    default void resize(int width, int height){
    }

    /** Called once per rendering frame, or once per tick on the ArcLib headless backend. */
    default void update(){
    }

    /**
     * Called when the {@link Application} is paused, usually when it's not active or visible on screen. An Application is also
     * paused before it is destroyed on mobile backends. ArcLib desktop and headless backends do not synthesize this callback.
     */
    default void pause(){
    }

    /** Called when the {@link Application} is resumed from a paused state, usually when it regains focus. */
    default void resume(){
    }

    /** Called when the {@link Application} is destroyed. Mobile backends call {@link #pause()} first. */
    default void dispose(){
    }

    /**
     * Called when the applications exits gracefully, either through `Core.app.exit()` or through a window closing.
     * Never called after a crash, unlike dispose().
     * */
    default void exit(){

    }

    /**
     * Called when an external file is dropped into the window, e.g from the desktop.
     */
    default void fileDropped(Fi file){
    }
}
