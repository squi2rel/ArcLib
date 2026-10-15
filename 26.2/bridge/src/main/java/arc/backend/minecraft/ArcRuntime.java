package arc.backend.minecraft;

import arc.Core;
import arc.Events;
import arc.Settings;
import arc.audio.Audio;
import arc.func.Cons;
import arc.graphics.Camera;
import arc.graphics.Color;
import arc.mock.MockAudio;
import arc.scene.Element;
import arc.scene.Group;
import arc.scene.Scene;
import arc.scene.event.Touchable;
import arc.scene.ui.TextField;
import arc.scene.ui.layout.Table;
import arc.scene.ui.layout.Scl;
import com.github.squi2rel.arc.runtime.ArcClientRuntime;
import com.github.squi2rel.arc.Trigger;
import com.github.squi2rel.arc.runtime.GameFiles;
import com.github.squi2rel.arc.ui.ArcStyles;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonInfo;
import org.joml.Matrix3x2fStack;

public final class ArcRuntime{
    private static Minecraft minecraft;
    private static MinecraftApplication app;
    private static MinecraftGraphics graphics;
    private static MinecraftInput input;
    private static MinecraftBatch batch;
    private static ArcSceneHost sceneHost;
    private static boolean initialized;
    private static boolean disposed;
    private static boolean scaleListenerInstalled;
    private static float layoutScale = 1f;

    private ArcRuntime(){
    }

    public static void init(Minecraft minecraft){
        if(initialized) return;
        if(!canInitialize(minecraft)) throw new IllegalStateException("ArcRuntime requires a created Minecraft window");
        ArcRuntime.minecraft = minecraft;
        app = new MinecraftApplication(minecraft);
        graphics = new MinecraftGraphics(minecraft);
        input = new MinecraftInput();
        batch = new MinecraftBatch(graphics.textureStore());

        Core.app = app;
        Core.graphics = graphics;
        Core.input = input;
        Core.files = new GameFiles(minecraft.gameDirectory);
        Core.settings = new Settings();
        Core.settings.setDataDirectory(Core.files.local("config/arclib"));
        Core.settings.load();
        Core.audio = new MockAudio();
        Core.camera = new Camera();
        Core.batch = batch;
        Core.atlas = new MinecraftAtlas();
        Core.gl = graphics.getGL20();
        Core.gl20 = graphics.getGL20();
        Core.gl30 = null;
        Scl.setProduct(Math.max(1f, (float)minecraft.getWindow().getGuiScale()) / 2f);

        Scene scene = new Scene();
        setActiveScene(scene);
        ArcStyles.install(scene, MinecraftFont.create());
        layoutScale = Scl.scl();

        sceneHost = new ArcSceneHost(scene);
        ArcClientRuntime.attach(scene);
        initialized = true;
        if(!scaleListenerInstalled){
            Events.run(Trigger.scaleChanged, () -> {
                MinecraftFontData.rescaleFonts();
                ArcStyles.rescale();
                fontsChanged();
            });
            scaleListenerInstalled = true;
        }
        app.initialize();
        resize();
    }

    public static boolean initialized(){
        return initialized;
    }

    public static boolean canInitialize(Minecraft minecraft){
        return minecraft != null && minecraft.getWindow() != null && minecraft.getWindow().handle() != 0L;
    }

    public static Minecraft minecraft(){
        return minecraft == null ? Minecraft.getInstance() : minecraft;
    }

    public static MinecraftBatch batch(){
        return batch;
    }

    public static MinecraftGraphics graphics(){
        return graphics;
    }

    public static MinecraftInput input(){
        return input;
    }

    public static ArcSceneHost sceneHost(){
        return sceneHost;
    }

    public static Scene scene(){
        return ArcClientRuntime.scene();
    }

    public static Scene createScene(){
        Scene scene = scene();
        setActiveScene(scene);
        return scene;
    }

    public static Table createContainer(){
        Scene scene = createScene();
        Table table = new Table();
        table.touchable = Touchable.childrenOnly;
        scene.root.addChild(table);
        return table;
    }

    public static Table createContainer(Scene source){
        Table table = createContainer();
        if(source != null && source != scene()){
            while(source.root.getChildren().size > 0){
                Element child = source.root.getChildren().first();
                child.remove();
                table.addChild(child);
            }
        }
        return table;
    }

    public static void buildInContainer(Table container, Cons<Scene> builder){
        Scene scene = createScene();
        int firstAdded = scene.root.getChildren().size;
        builder.get(scene);
        while(scene.root.getChildren().size > firstAdded){
            container.addChild(scene.root.getChildren().get(firstAdded));
        }
    }

    public static Scene activeScene(){
        Scene scene = ArcClientRuntime.activeScene();
        return scene == null && sceneHost != null ? sceneHost.scene() : scene;
    }

    public static void setActiveScene(Scene scene){
        ArcClientRuntime.setActiveScene(scene);
    }

    public static void addBeforeDrawListener(Cons<Scene> listener){
        ArcClientRuntime.addBeforeDrawListener(listener);
    }

    public static void removeBeforeDrawListener(Cons<Scene> listener){
        ArcClientRuntime.removeBeforeDrawListener(listener);
    }

    /** The main application frame; component rendering does not dispatch application updates. */
    public static void renderFrame(Scene scene, GuiGraphicsExtractor gui, int width, int height, float deltaSeconds){
        renderScene(scene, gui, 0, 0, width, height, deltaSeconds, true);
    }

    public static void renderScene(Scene scene, GuiGraphicsExtractor gui, int x, int y, int width, int height, float deltaSeconds){
        renderScene(scene, gui, x, y, width, height, deltaSeconds, false);
    }

    private static void renderScene(Scene scene, GuiGraphicsExtractor gui, int x, int y, int width, int height, float deltaSeconds, boolean applicationFrame){
        if(scene == null || gui == null || width <= 0 || height <= 0) return;
        setActiveScene(scene);
        FrameScale scale = frameScale();
        int arcWidth = toArcSize(width, scale.x);
        int arcHeight = toArcSize(height, scale.y);
        graphics.beginFrame(arcWidth, arcHeight, deltaSeconds, gui, x, y, scale.x, scale.y);
        batch.begin(gui);
        scene.resize(arcWidth, arcHeight);
        if(applicationFrame){
            resize();
            app.defaultUpdate();
        }
        scene.act(Core.graphics.getDeltaTime());
        ArcClientRuntime.fireBeforeDraw(scene);

        Matrix3x2fStack pose = gui.pose();
        pose.pushMatrix();
        pose.translate(x, y + height);
        pose.scale(1f / scale.x, -1f / scale.y);
        if(applicationFrame){
            app.update();
            app.fireDraw();
        }
        scene.draw();
        pose.popMatrix();

        batch.end();
        graphics.endFrame();
        releaseMouseIfFocused(scene, minecraft().mouseHandler);
    }

    public static void renderElement(Element element, GuiGraphicsExtractor gui, int x, int y, int width, int height, float deltaSeconds){
        Scene scene = scene();
        if(scene == null || element == null || gui == null || width <= 0 || height <= 0) return;
        setActiveScene(scene);
        FrameScale scale = frameScale();
        int arcWidth = toArcSize(width, scale.x);
        int arcHeight = toArcSize(height, scale.y);
        graphics.beginFrame(arcWidth, arcHeight, deltaSeconds, gui, x, y, scale.x, scale.y);
        batch.begin(gui);
        scene.resize(arcWidth, arcHeight);
        element.setBounds(0f, 0f, arcWidth, arcHeight);
        element.act(Core.graphics.getDeltaTime());

        Matrix3x2fStack pose = gui.pose();
        pose.pushMatrix();
        pose.translate(x, y + height);
        pose.scale(1f / scale.x, -1f / scale.y);
        element.draw();
        pose.popMatrix();

        batch.end();
        graphics.endFrame();
        releaseMouseIfFocused(scene, minecraft().mouseHandler);
    }

    public static void layoutElement(Element element, int x, int y, int width, int height){
        if(element == null || width <= 0 || height <= 0) return;
        Minecraft mc = minecraft();
        if(mc == null) return;
        FrameScale scale = frameScale();
        int guiHeight = Math.max(1, mc.getWindow().getGuiScaledHeight());
        element.setBounds(x * scale.x, (guiHeight - y - height) * scale.y, width * scale.x, height * scale.y);
    }

    public static void renderScene(Scene scene, GuiGraphicsExtractor gui, int width, int height, float deltaSeconds){
        renderScene(scene, gui, 0, 0, width, height, deltaSeconds);
    }

    public static boolean mouseMoved(Scene scene, double x, double yTop, int width, int height){
        FrameScale scale = frameScale();
        int arcHeight = toArcSize(height, scale.y);
        return withScene(scene, toArcSize(width, scale.x), arcHeight, () -> input.mouseMoved(scene, x * scale.x, yTop * scale.y, arcHeight));
    }

    public static boolean mouseButton(Scene scene, double x, double yTop, int width, int height, MouseButtonInfo info, int action){
        FrameScale scale = frameScale();
        int arcHeight = toArcSize(height, scale.y);
        return withScene(scene, toArcSize(width, scale.x), arcHeight, () -> input.mouseButton(scene, x * scale.x, yTop * scale.y, arcHeight, info, action));
    }

    public static boolean mouseDragged(Scene scene, double x, double yTop, int width, int height){
        FrameScale scale = frameScale();
        int arcHeight = toArcSize(height, scale.y);
        return withScene(scene, toArcSize(width, scale.x), arcHeight, () -> input.mouseDragged(scene, x * scale.x, yTop * scale.y, arcHeight));
    }

    public static boolean scrolled(Scene scene, double x, double yTop, int width, int height, double horizontal, double vertical){
        FrameScale scale = frameScale();
        int arcHeight = toArcSize(height, scale.y);
        return withScene(scene, toArcSize(width, scale.x), arcHeight, () -> input.scrolled(scene, x * scale.x, yTop * scale.y, arcHeight, horizontal, vertical));
    }

    public static boolean keyPressed(Scene scene, KeyEvent event, int width, int height){
        FrameScale scale = frameScale();
        return withScene(scene, toArcSize(width, scale.x), toArcSize(height, scale.y), () -> input.keyPressed(scene, event));
    }

    public static boolean keyReleased(Scene scene, KeyEvent event, int width, int height){
        FrameScale scale = frameScale();
        return withScene(scene, toArcSize(width, scale.x), toArcSize(height, scale.y), () -> input.keyReleased(scene, event));
    }

    public static boolean charTyped(Scene scene, CharacterEvent event, int width, int height){
        FrameScale scale = frameScale();
        return withScene(scene, toArcSize(width, scale.x), toArcSize(height, scale.y), () -> input.charTyped(scene, event));
    }

    public static void endTick(){
        if(!initialized || disposed) return;
        if(minecraft.isGameLoadFinished()) app.markReady();
        try{
            app.fireUpdate();
        }finally{
            if(input != null) input.postUpdate();
        }
    }

    public static void resize(){
        if(initialized && !disposed && minecraft.getWindow() != null){
            // Read the full framebuffer, outside component-local graphics size overrides.
            app.resize(minecraft.getWindow().getWidth(), minecraft.getWindow().getHeight());
        }
    }

    /** Called after Minecraft has recalculated its GUI scale. */
    public static void guiScaleChanged(){
        if(initialized && !disposed && minecraft.getWindow() != null){
            Scl.setProduct(Math.max(1f, (float)minecraft.getWindow().getGuiScale()) / 2f);
            resize();
        }
    }

    public static void requestExit(){
        if(app != null) app.requestExit();
    }

    public static void fileDropped(java.nio.file.Path path){
        if(initialized && !disposed) app.fileDropped(new arc.files.Fi(path.toFile()));
    }

    public static void dispose(){
        if(disposed) return;
        disposed = true;
        try{
            if(app != null) app.dispose();
        }finally{
            try{
                try{
                    if(Core.atlas != null) Core.atlas.dispose();
                }finally{
                    if(graphics != null) graphics.disposeResources();
                }
            }finally{
                initialized = false;
                ArcClientRuntime.detach();
            }
        }
    }

    public static int textWidth(String text){
        return (int)Math.ceil(textAdvance(text));
    }

    public static float textAdvance(String text){
        Minecraft mc = minecraft();
        return mc == null || mc.font == null ? text.length() * 6f : mc.font.getSplitter().stringWidth(text);
    }

    public static void fontsChanged(){
        MinecraftFontData.invalidateFonts();
        if(!initialized || disposed) return;
        Scene scene = scene();
        float nextScale = Scl.scl();
        float ratio = nextScale / layoutScale;
        layoutScale = nextScale;
        if(scene != null) refreshFontLayouts(scene.root, ratio);
        Events.fire(Trigger.fontsChanged);
    }

    private static void refreshFontLayouts(Element element, float ratio){
        if(element instanceof Table table) table.rescale(ratio);
        element.invalidate();
        if(element instanceof TextField field) field.refreshFontMetrics();
        if(element instanceof Group group){
            for(Element child : group.getChildren()) refreshFontLayouts(child, ratio);
        }
    }

    public static void drawText(String text, float x, float y, Color color, float scaleX, float scaleY){
        batch.drawText(text, x, y, color, scaleX, scaleY);
    }

    static void releaseMouseIfFocused(Scene scene, MouseHandler mouse){
        if(scene.getKeyboardFocus() != null) releaseMouse(mouse);
    }

    static void releaseMouse(MouseHandler mouse){
        if(mouse.isMouseGrabbed()){
            KeyMapping.releaseAll();
            mouse.releaseMouse();
        }
    }

    private static boolean withScene(Scene scene, int width, int height, Bool bool){
        if(scene == null) return false;
        setActiveScene(scene);
        final boolean[] result = {false};
        graphics.withSize(width, height, () -> result[0] = bool.get());
        releaseMouseIfFocused(scene, minecraft().mouseHandler);
        return result[0];
    }

    private static FrameScale frameScale(){
        Minecraft mc = minecraft();
        if(mc == null || mc.getWindow() == null) return new FrameScale(1f, 1f);
        float scale = Math.max(1f, (float)mc.getWindow().getGuiScale());
        return new FrameScale(scale, scale);
    }

    private static int toArcSize(int guiSize, float scale){
        return Math.max(1, Math.round(guiSize * scale));
    }

    private record FrameScale(float x, float y){
    }

    private interface Bool{
        boolean get();
    }
}
