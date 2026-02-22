package nikolai.keybinding;

import java.util.ArrayDeque;
import java.util.Deque;

public class ModeStack {

    private final Deque<Mode> stack = new ArrayDeque<>();

    public void push(Mode m) {
        stack.push(m);
    }

    public void pop() {

        if (!stack.isEmpty()) {
            stack.pop();
        }
    }

    public Mode current() {
        return stack.peek();
    }
}
