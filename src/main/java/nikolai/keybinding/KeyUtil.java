package nikolai.keybinding;

import java.awt.event.InputEvent;
import java.util.List;

public class KeyUtil {

    public static int getSwingModifierMask(List<String> modifiers) {

        int mask = 0;

        for (String mod : modifiers) {

            if (mod == null) continue;

            switch (mod.trim().toLowerCase()) {

                case "ctrl":
                case "control":
                case "c":
                    mask |= InputEvent.CTRL_DOWN_MASK;
                    break;

                case "shift":
                case "s":
                    mask |= InputEvent.SHIFT_DOWN_MASK;
                    break;

                case "alt":
                case "a":
                case "meta":
                case "m":
                    mask |= InputEvent.ALT_DOWN_MASK;
                    break;

                case "super":
                case "cmd":
                case "command":
                    mask |= InputEvent.META_DOWN_MASK;
                    break;

                case "altgr":
                    mask |= InputEvent.ALT_GRAPH_DOWN_MASK;
                    break;

                default:
                    throw new IllegalArgumentException(
                        "Unbekannter Modifier: " + mod
                    );
            }
        }

        return mask;
    }
}