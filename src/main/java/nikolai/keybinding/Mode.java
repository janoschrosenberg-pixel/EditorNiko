package nikolai.keybinding;

import clojure.lang.IFn;
import java.util.HashMap;
import java.util.Map;

public class Mode {

    private final String name;

    private final Map<String, Object> props = new HashMap<>();
    private final Map<String, IFn> bindings = new HashMap<>();

    public Mode(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    /* Properties */

    public void setProp(String key, Object val) {
        props.put(key, val);
    }

    public boolean getBool(String key) {
        Object v = props.get(":"+key);
        return v instanceof Boolean && (Boolean) v;
    }

    /* Bindings */

    public void bind(String key, IFn fn) {
        bindings.put(key, fn);
    }

    public IFn getBinding(String key) {
        return bindings.get(key);
    }
}
