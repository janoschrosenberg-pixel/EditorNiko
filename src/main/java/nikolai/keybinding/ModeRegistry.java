package nikolai.keybinding;

import java.util.HashMap;
import java.util.Map;

public class ModeRegistry {

    private final Map<String, Mode> modes = new HashMap<>();

    public Mode getOrCreate(String name) {

        return modes.computeIfAbsent(
            name,
            Mode::new
        );
    }
}
