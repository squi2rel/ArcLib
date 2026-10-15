package arc.backend.minecraft;

import arc.input.KeyCode;
import com.mojang.blaze3d.platform.InputConstants;

public final class KeyMapper{
    private KeyMapper(){
    }

    public static KeyCode key(int key){
        return switch(key){
            case InputConstants.KEY_A -> KeyCode.a;
            case InputConstants.KEY_B -> KeyCode.b;
            case InputConstants.KEY_C -> KeyCode.c;
            case InputConstants.KEY_D -> KeyCode.d;
            case InputConstants.KEY_E -> KeyCode.e;
            case InputConstants.KEY_F -> KeyCode.f;
            case InputConstants.KEY_G -> KeyCode.g;
            case InputConstants.KEY_H -> KeyCode.h;
            case InputConstants.KEY_I -> KeyCode.i;
            case InputConstants.KEY_J -> KeyCode.j;
            case InputConstants.KEY_K -> KeyCode.k;
            case InputConstants.KEY_L -> KeyCode.l;
            case InputConstants.KEY_M -> KeyCode.m;
            case InputConstants.KEY_N -> KeyCode.n;
            case InputConstants.KEY_O -> KeyCode.o;
            case InputConstants.KEY_P -> KeyCode.p;
            case InputConstants.KEY_Q -> KeyCode.q;
            case InputConstants.KEY_R -> KeyCode.r;
            case InputConstants.KEY_S -> KeyCode.s;
            case InputConstants.KEY_T -> KeyCode.t;
            case InputConstants.KEY_U -> KeyCode.u;
            case InputConstants.KEY_V -> KeyCode.v;
            case InputConstants.KEY_W -> KeyCode.w;
            case InputConstants.KEY_X -> KeyCode.x;
            case InputConstants.KEY_Y -> KeyCode.y;
            case InputConstants.KEY_Z -> KeyCode.z;
            case InputConstants.KEY_0 -> KeyCode.num0;
            case InputConstants.KEY_1 -> KeyCode.num1;
            case InputConstants.KEY_2 -> KeyCode.num2;
            case InputConstants.KEY_3 -> KeyCode.num3;
            case InputConstants.KEY_4 -> KeyCode.num4;
            case InputConstants.KEY_5 -> KeyCode.num5;
            case InputConstants.KEY_6 -> KeyCode.num6;
            case InputConstants.KEY_7 -> KeyCode.num7;
            case InputConstants.KEY_8 -> KeyCode.num8;
            case InputConstants.KEY_9 -> KeyCode.num9;
            case InputConstants.KEY_F1 -> KeyCode.f1;
            case InputConstants.KEY_F2 -> KeyCode.f2;
            case InputConstants.KEY_F3 -> KeyCode.f3;
            case InputConstants.KEY_F4 -> KeyCode.f4;
            case InputConstants.KEY_F5 -> KeyCode.f5;
            case InputConstants.KEY_F6 -> KeyCode.f6;
            case InputConstants.KEY_F7 -> KeyCode.f7;
            case InputConstants.KEY_F8 -> KeyCode.f8;
            case InputConstants.KEY_F9 -> KeyCode.f9;
            case InputConstants.KEY_F10 -> KeyCode.f10;
            case InputConstants.KEY_F11 -> KeyCode.f11;
            case InputConstants.KEY_F12 -> KeyCode.f12;
            case InputConstants.KEY_ESCAPE -> KeyCode.escape;
            case InputConstants.KEY_RETURN, InputConstants.KEY_NUMPADENTER -> KeyCode.enter;
            case InputConstants.KEY_TAB -> KeyCode.tab;
            case InputConstants.KEY_SPACE -> KeyCode.space;
            case InputConstants.KEY_BACKSPACE -> KeyCode.backspace;
            case InputConstants.KEY_DELETE -> KeyCode.forwardDel;
            case InputConstants.KEY_LEFT -> KeyCode.left;
            case InputConstants.KEY_RIGHT -> KeyCode.right;
            case InputConstants.KEY_UP -> KeyCode.up;
            case InputConstants.KEY_DOWN -> KeyCode.down;
            case InputConstants.KEY_HOME -> KeyCode.home;
            case InputConstants.KEY_END -> KeyCode.end;
            case InputConstants.KEY_PAGEUP -> KeyCode.pageUp;
            case InputConstants.KEY_PAGEDOWN -> KeyCode.pageDown;
            case InputConstants.KEY_INSERT -> KeyCode.insert;
            case InputConstants.KEY_LSHIFT -> KeyCode.shiftLeft;
            case InputConstants.KEY_RSHIFT -> KeyCode.shiftRight;
            case InputConstants.KEY_LCONTROL -> KeyCode.controlLeft;
            case InputConstants.KEY_RCONTROL -> KeyCode.controlRight;
            case InputConstants.KEY_LALT -> KeyCode.altLeft;
            case InputConstants.KEY_RALT -> KeyCode.altRight;
            case InputConstants.KEY_MINUS -> KeyCode.minus;
            case InputConstants.KEY_EQUALS -> KeyCode.equals;
            case InputConstants.KEY_LBRACKET -> KeyCode.leftBracket;
            case InputConstants.KEY_RBRACKET -> KeyCode.rightBracket;
            case InputConstants.KEY_BACKSLASH -> KeyCode.backslash;
            case InputConstants.KEY_SEMICOLON -> KeyCode.semicolon;
            case InputConstants.KEY_APOSTROPHE -> KeyCode.apostrophe;
            case InputConstants.KEY_GRAVE -> KeyCode.backtick;
            case InputConstants.KEY_COMMA -> KeyCode.comma;
            case InputConstants.KEY_PERIOD -> KeyCode.period;
            case InputConstants.KEY_SLASH -> KeyCode.slash;
            default -> KeyCode.unknown;
        };
    }

    public static KeyCode mouse(int button){
        return switch(button){
            case InputConstants.MOUSE_BUTTON_LEFT -> KeyCode.mouseLeft;
            case InputConstants.MOUSE_BUTTON_RIGHT -> KeyCode.mouseRight;
            case InputConstants.MOUSE_BUTTON_MIDDLE -> KeyCode.mouseMiddle;
            case 3 -> KeyCode.mouseBack;
            case 4 -> KeyCode.mouseForward;
            default -> KeyCode.unknown;
        };
    }

    public static int minecraftMouse(KeyCode code){
        return switch(code){
            case mouseLeft -> InputConstants.MOUSE_BUTTON_LEFT;
            case mouseRight -> InputConstants.MOUSE_BUTTON_RIGHT;
            case mouseMiddle -> InputConstants.MOUSE_BUTTON_MIDDLE;
            case mouseBack -> 3;
            case mouseForward -> 4;
            default -> -1;
        };
    }
}
