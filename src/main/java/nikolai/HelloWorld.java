package nikolai;

import clojure.java.api.Clojure;
import clojure.lang.IFn;
import com.mammb.code.piecetable.TextEdit;
import view.EditorFrame;

public class HelloWorld {
    public static void main(String[] args) {
        IFn loadFile = Clojure.var("clojure.core", "load-file");
        loadFile.invoke("config.clj");
    }
}