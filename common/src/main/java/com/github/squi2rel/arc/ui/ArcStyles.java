package com.github.squi2rel.arc.ui;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.Colors;
import arc.graphics.Pixmap;
import arc.graphics.Texture;
import arc.graphics.g2d.Font;
import arc.graphics.g2d.NinePatch;
import arc.graphics.g2d.TextureRegion;
import arc.graphics.gl.PixmapTextureData;
import arc.scene.Scene;
import arc.scene.style.Drawable;
import arc.scene.style.NinePatchDrawable;
import arc.scene.style.TextureRegionDrawable;
import arc.scene.ui.*;
import arc.scene.ui.layout.Scl;
import arc.struct.Seq;

public final class ArcStyles {
    public static final Color accent = Color.valueOf("80c080");
    public static final Color error = Color.valueOf("e06060");
    private static final TextureRegionDrawable whiteRegion = new TextureRegionDrawable();
    public static final Drawable white = whiteRegion;
    private static final Seq<NinePatchDrawable> patches = new Seq<>();
    public static Drawable panel;
    public static Drawable translucentPanel;
    public static Drawable overlay;
    public static Drawable selection;
    public static Label.LabelStyle label;
    public static Button.ButtonStyle button;
    public static TextButton.TextButtonStyle textButton, toolTextButton, toggleTextButton;
    public static ImageButton.ImageButtonStyle imageButton, toolImageButton;
    public static ScrollPane.ScrollPaneStyle scrollPane;
    public static Slider.SliderStyle slider;
    public static TextField.TextFieldStyle textField;
    public static CheckBox.CheckBoxStyle checkBox;
    public static Dialog.DialogStyle dialog;
    public static TreeElement.TreeStyle tree;
    private static boolean loaded;
    private static float styleScale = 1f;

    private ArcStyles() {
    }

    public static void loadOnce(Font font) {
        if (loaded) return;
        if (font == null) throw new IllegalArgumentException("font cannot be null");
        whiteRegion.setRegion(Core.atlas.white());
        panel = box(Color.valueOf("242424"), Color.valueOf("666666"), 0f, 0f, 0f);
        translucentPanel = solid(Color.valueOf("242424cc"));
        overlay = solid(Color.valueOf("000000cc"));
        selection = solid(Color.valueOf("426642"));
        Drawable up = box(Color.valueOf("484848"), Color.valueOf("888888"), 0f, 0f, 8f);
        Drawable over = box(Color.valueOf("606060"), Color.white, 0f, 0f, 8f);
        Drawable down = box(Color.valueOf("303030"), accent, 0f, 0f, 8f);
        Drawable disabled = box(Color.valueOf("303030"), Color.valueOf("505050"), 0f, 0f, 8f);

        label = new Label.LabelStyle(font, Color.white);

        Tooltip.Tooltips.getInstance().textProvider = text -> new Tooltip(table -> {
            table.background(overlay);
            table.margin(6f);
            table.add(text);
        });

        button = new Button.ButtonStyle();
        button.up = up;
        button.over = over;
        button.down = down;
        button.disabled = disabled;

        textButton = new TextButton.TextButtonStyle();
        textButton.font = font;
        textButton.fontColor = Color.white;
        textButton.disabledFontColor = Color.gray;
        textButton.up = up;
        textButton.over = over;
        textButton.down = down;
        textButton.disabled = disabled;

        toolTextButton = new TextButton.TextButtonStyle(textButton);
        toolTextButton.up = solid(Color.clear);
        toolTextButton.over = solid(Color.valueOf("606060"));
        toolTextButton.down = selection;
        toolTextButton.disabled = toolTextButton.up;
        toggleTextButton = new TextButton.TextButtonStyle(toolTextButton);
        toggleTextButton.checked = selection;

        imageButton = new ImageButton.ImageButtonStyle();
        imageButton.up = up;
        imageButton.over = over;
        imageButton.down = down;
        imageButton.disabled = disabled;
        imageButton.imageUpColor = Color.white;
        imageButton.imageDisabledColor = Color.gray;
        toolImageButton = new ImageButton.ImageButtonStyle(imageButton);
        toolImageButton.up = toolTextButton.up;
        toolImageButton.over = toolTextButton.over;
        toolImageButton.down = selection;
        toolImageButton.disabled = toolTextButton.up;

        scrollPane = new ScrollPane.ScrollPaneStyle();
        scrollPane.vScroll = solid(Color.valueOf("303030"), 6f, 0f);
        scrollPane.hScroll = solid(Color.valueOf("303030"), 0f, 6f);
        scrollPane.vScrollKnob = solid(Color.gray, 6f, 18f);
        scrollPane.hScrollKnob = solid(Color.gray, 18f, 6f);

        slider = new Slider.SliderStyle();
        slider.background = box(Color.valueOf("505050"), Color.valueOf("505050"), 4f, 4f, 0f);
        slider.knob = box(Color.lightGray, Color.gray, 12f, 18f, 0f);
        slider.knobOver = box(Color.white, accent, 12f, 18f, 0f);
        slider.knobDown = box(accent, Color.white, 12f, 18f, 0f);

        textField = new TextField.TextFieldStyle();
        textField.font = font;
        textField.fontColor = Color.white;
        textField.disabledFontColor = Color.gray;
        textField.messageFont = font;
        textField.messageFontColor = Color.gray;
        textField.background = box(Color.valueOf("181818"), Color.gray, 0f, 0f, 6f);
        textField.disabledBackground = disabled;
        textField.invalidBackground = box(Color.valueOf("181818"), error, 0f, 0f, 6f);
        textField.selection = selection;
        textField.cursor = solid(Color.white, 1f, 0f);

        checkBox = new CheckBox.CheckBoxStyle();
        checkBox.font = font;
        checkBox.fontColor = Color.white;
        checkBox.disabledFontColor = Color.gray;
        checkBox.checkboxOff = checkBox(Color.valueOf("181818"), Color.gray, null);
        checkBox.checkboxOver = checkBox(Color.valueOf("303030"), Color.white, null);
        checkBox.checkboxOffDisabled = checkBox(Color.valueOf("181818"), Color.darkGray, null);
        checkBox.checkboxOn = checkBox(Color.valueOf("181818"), Color.gray, accent);
        checkBox.checkboxOnOver = checkBox(Color.valueOf("181818"), Color.white, accent);
        checkBox.checkboxOnDisabled = checkBox(Color.valueOf("181818"), Color.darkGray, Color.gray);

        dialog = new Dialog.DialogStyle();
        dialog.titleFont = font;
        dialog.titleFontColor = Color.white;
        dialog.background = box(Color.valueOf("242424"), Color.gray, 0f, 0f, 8f);
        dialog.background.setTopHeight(32f);
        dialog.stageBackground = overlay;

        tree = new TreeElement.TreeStyle();
        tree.plus = new TextDrawable("+", 18f);
        tree.minus = new TextDrawable("-", 18f);
        tree.background = translucentPanel;
        tree.over = selection;
        loaded = true;
        rescale();
    }

    public static void initializeThemeColor() {
        String themeColor = Core.settings.getString("themeColor", "80c080");
        try {
            accent.set(Color.valueOf(themeColor));
        } catch (Exception e) {
            Core.settings.remove("themeColor");
        }
        Colors.put("accent", accent);
    }

    /**
     * Keeps nine-patch borders, content padding and minimum sizes in UI units.
     */
    public static void rescale() {
        if (!loaded) return;
        float nextScale = Scl.scl();
        float ratio = nextScale / styleScale;
        if (ratio == 1f) return;
        styleScale = nextScale;
        for (NinePatchDrawable drawable : patches) {
            drawable.getPatch().scale(ratio, ratio);
            drawable.setMinWidth(drawable.getMinWidth() * ratio);
            drawable.setMinHeight(drawable.getMinHeight() * ratio);
            drawable.setLeftWidth(drawable.getLeftWidth() * ratio);
            drawable.setRightWidth(drawable.getRightWidth() * ratio);
            drawable.setTopHeight(drawable.getTopHeight() * ratio);
            drawable.setBottomHeight(drawable.getBottomHeight() * ratio);
        }
    }

    private static Drawable box(Color fill, Color border, float width, float height, float padding) {
        Pixmap pixels = new Pixmap(3, 3);
        pixels.fill(border);
        pixels.setRaw(1, 1, fill.rgba());
        NinePatchDrawable drawable = patch("ui-box-" + fill + "-" + border, pixels, 1, width, height);
        drawable.setLeftWidth(padding);
        drawable.setRightWidth(padding);
        drawable.setTopHeight(padding);
        drawable.setBottomHeight(padding);
        return drawable;
    }

    private static Drawable checkBox(Color fill, Color border, Color checked) {
        Pixmap pixels = new Pixmap(18, 18);
        pixels.fill(fill);
        pixels.drawRect(0, 0, 18, 18, border.rgba());
        if (checked != null) pixels.fillRect(4, 4, 10, 10, checked.rgba());
        return patch("ui-check-" + fill + "-" + border + "-" + checked, pixels, 4, 18f, 18f);
    }

    private static Drawable solid(Color color) {
        return solid(color, 0f, 0f);
    }

    private static Drawable solid(Color color, float width, float height) {
        // Bake color into the atlas so repeated draws do not accumulate tint in Draw's shared color.
        Pixmap pixels = new Pixmap(1, 1);
        pixels.fill(color);
        return image("ui-solid-" + color, pixels, width, height);
    }

    private static TextureRegionDrawable image(String name, Pixmap pixels, float width, float height) {
        TextureRegionDrawable drawable;
        if (Core.atlas instanceof GeneratedAtlas atlas) {
            try {
                drawable = new TextureRegionDrawable(atlas.pack(name, pixels));
            } finally {
                pixels.dispose();
            }
        } else {
            drawable = new TextureRegionDrawable(new TextureRegion(new Texture(new PixmapTextureData(pixels, false, true))));
        }
        drawable.setMinWidth(width);
        drawable.setMinHeight(height);
        return drawable;
    }

    private static NinePatchDrawable patch(String name, Pixmap pixels, int border, float width, float height) {
        TextureRegion region;
        if (Core.atlas instanceof GeneratedAtlas atlas) {
            try {
                var packed = atlas.pack(name, pixels);
                packed.splits = new int[]{border, border, border, border};
                region = packed;
            } finally {
                pixels.dispose();
            }
        } else {
            region = new TextureRegion(new Texture(new PixmapTextureData(pixels, false, true)));
        }
        NinePatchDrawable drawable = new NinePatchDrawable(new NinePatch(region, border, border, border, border));
        drawable.setMinWidth(width);
        drawable.setMinHeight(height);
        patches.add(drawable);
        return drawable;
    }

    public static void install(Scene scene, Font font) {
        loadOnce(font);
        scene.addStyle(Label.LabelStyle.class, label);
        scene.addStyle(Button.ButtonStyle.class, button);
        scene.addStyle(TextButton.TextButtonStyle.class, textButton);
        scene.addStyle(CheckBox.CheckBoxStyle.class, checkBox);
        scene.addStyle(TextField.TextFieldStyle.class, textField);
        scene.addStyle(ScrollPane.ScrollPaneStyle.class, scrollPane);
        scene.addStyle(ProgressBar.ProgressBarStyle.class, slider);
        scene.addStyle(Slider.SliderStyle.class, slider);
        scene.addStyle(ImageButton.ImageButtonStyle.class, imageButton);
        scene.addStyle(Dialog.DialogStyle.class, dialog);
        scene.addStyle(TreeElement.TreeStyle.class, tree);
    }
}
