package arc.scene.ui.layout;

import arc.*;
import com.github.squi2rel.arc.Trigger;

/** Utilities for UI scale.*/
public class Scl{
    private static float scl = -1;
    private static float addition = 0f;
    private static float product = 1f;
    private static final float debugScale = 1f;

    public static void setProduct(float product){
        if(!Float.isFinite(product) || product <= 0f) throw new IllegalArgumentException("scale product must be positive and finite");
        if(Float.compare(Scl.product, product) == 0) return;
        Scl.product = product;
        scl = -1;
        if(Core.app != null) Events.fire(Trigger.scaleChanged);
    }

    public static void setAddition(float addition){
        if(Float.compare(Scl.addition, addition) == 0) return;
        Scl.addition = addition;
        scl = -1;
        if(Core.app != null && !Core.app.isDesktop() && !Core.app.isWeb()) Events.fire(Trigger.scaleChanged);
    }

    public static float scl(){
        return scl(1f);
    }

    public static float scl(float amount){
        if(scl < 0f){
            //calculate scaling value if it hasn't been set yet
            if(Core.app.isDesktop() || Core.app.isWeb()){
                scl = product;
            }else{
                //mobile scaling
                scl = Math.max(Math.round((Core.graphics.getDensity() / 1.5f + addition) / 0.5) * 0.5f, 1f) * product;
            }
        }
        return amount * scl * debugScale;
    }
}
