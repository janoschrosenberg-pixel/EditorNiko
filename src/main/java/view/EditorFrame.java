package view;
import clojure.lang.IFn;
import nikolai.Buffer;
import nikolai.Direction;
import nikolai.FileScanner;
import nikolai.Utils;
import nikolai.keybinding.*;


import javax.swing.*;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.nio.file.Path;
import java.util.*;
import java.util.List;

public class EditorFrame extends JFrame {

    private final ModeRegistry modeRegistry = new ModeRegistry();
    private final ModeStack modeStack = new ModeStack();
    private EditorPanel v1;
    private int unnamedCounter = 1;
    private Buffer currentBuffer;
    private Map<String, Buffer> bufferCache = new HashMap<>();
    private FileScanner scanner = new FileScanner();
    private final StatusPanel statusPanel = new StatusPanel();

    private Stack<Buffer> bufferMenuStack = new Stack<>();

    public int getKeyCodeFromChar(char c) {
        return KeyEvent.getExtendedKeyCodeForChar(c);
    }


    private static final List<String> otherControlKeys = new ArrayList<>();
    static {
        otherControlKeys.add("enter");
        otherControlKeys.add("tab");
        otherControlKeys.add("back");
        otherControlKeys.add("esc");
    }
    public void print() {
        System.out.println("Nikolai sagt Hallo");
    }

    public void bindKey(String key, String mode, IFn fn) {
        if(key == null) {
            return;
        }
        KeyStroke stroke = parseStroke(key);

        v1.getInputMap(JComponent.WHEN_FOCUSED)
                .put(stroke, key);

        v1.getActionMap().put(key, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Mode m = modeStack.current();
                if (m == null) return;

                /* InsertMode? */
                if (m.getBool("insert-like") && e.getModifiers() == 0 && !otherControlKeys.contains(key)) {
                    return;
                }

                /* CommandMode */
                IFn fn = m.getBinding(key);
                if (fn != null) fn.invoke();

                v1.changeBuffer(currentBuffer);
                v1.repaintEditor();
            }
        });

        Mode m = modeRegistry.getOrCreate(mode);
        m.bind(key, fn);
    }

    public void inserText(String text) {
        this.currentBuffer.addText(text);
    }

    public String getBufferMenuStackLength() {
        return bufferMenuStack.size()+"";
    }

    private KeyStroke parseStroke(String key) {
        if(key.length() == 1) {
            return parseOneDigitStroke(key.charAt(0));
        }
        if(key.equals("tab")) {
            return KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0);
        }
        if (key.equals("enter")) {
            return KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);
        }
        if (key.equals("esc")) {
            return KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
        }
        if(key.equals("back")) {
            return KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0);
        }

        if(key.contains("-")) {
            List<String> modifiers = new ArrayList<>();
            String[] parts = key.split("-");

            for(int i=0; i<parts.length-1;i++) {
                modifiers.add(parts[i]);
            }

            var lastKey = parseStroke(parts[parts.length-1]);

            int modifiersValue = KeyUtil.getSwingModifierMask(modifiers) | lastKey.getModifiers();

            return KeyStroke.getKeyStroke(lastKey.getKeyCode(), modifiersValue);
        }

        throw new IllegalStateException("parsing error "+key);
    }

    private KeyStroke parseOneDigitStroke(Character key) {
        int modifier = 0;
        if(Character.isUpperCase(key)) {
            modifier = KeyEvent.SHIFT_DOWN_MASK;
        }

        return KeyStroke.getKeyStroke(getKeyCodeFromChar(key), modifier);
    }

    public void backspace() {
        currentBuffer.backspace();
        currentBuffer.updateToken();
    }

    public void updateToken() {
        currentBuffer.updateToken();
    }

    public void defineMode(String name, Map props) {
        Mode m = modeRegistry.getOrCreate(name);
        for (Object k : props.keySet()) {
            m.setProp(k.toString(), props.get(k));
        }
    }
    public void popMode() {
        modeStack.pop();
        updateStatusProp();
    }
    public void setMode(String name) {
        Mode m = modeRegistry.getOrCreate(name);
        modeStack.push(m);
        updateStatusProp();
    }

    private void updateStatusProp() {
       Mode mode = modeStack.current();
       String name = mode.getName();
       var fileName = this.currentBuffer.getFileName();
        statusPanel.setProps(Arrays.asList(new StatusProp("Mode", name, Color.GREEN),
                new StatusProp("File", fileName, Color.LIGHT_GRAY)));
        statusPanel.repaint();
    }

    public void setStatus(String status) {
        statusPanel.setProps(Arrays.asList(new StatusProp("😀", status, Color.WHITE)));
        statusPanel.repaint();
    }

    public void newBuffer() {
        currentBuffer = new Buffer("unnamed"+unnamedCounter);

        bufferCache.put(currentBuffer.getFileName(), currentBuffer);
        unnamedCounter++;
    }

    public void loadFile(String file) {
        if(currentBuffer.getFileName().equals(file)){
            return;
        }

        if(bufferCache.equals(file)) {
            this.currentBuffer = bufferCache.get(file);
        }

        this.currentBuffer = new Buffer(Path.of(file));
        bufferCache.put(currentBuffer.getFileName(), currentBuffer);
    }


    public void selectFile() {
        bufferMenuStack.pop();
        loadCurrentRowAsFile();
        updateStatusProp();
    }

    public void loadCurrentRowAsFile() {
       String currentLine = currentBuffer.getCurrentLine();

       loadFile(currentLine);
    }

    public void popMenuBuffer(){
        if(bufferMenuStack.isEmpty()) {
            return;
        }
        currentBuffer = bufferMenuStack.pop();
    }

    public void moveCursor(String dir, int steps) {
        currentBuffer.moveCursor(Direction.valueOf(dir), steps);
    }

    public void showFiles() {
        String menuName = "File Menu";
        if(currentBuffer.getFileName().equals(menuName)) {
            return;
        }
        bufferMenuStack.push(currentBuffer);
        currentBuffer = new Buffer(menuName);
        currentBuffer.addText(scanner.filesToString());
    }


    public void openWorkspaceChooser() {
        String workspace = Utils.showFileChooser(this);
        if(workspace != null) {
            Utils.writeWorkspace(workspace);
        }
    }
    public EditorFrame() {
        scanner.scanDirectory();
        newBuffer();
        setTitle("Nikolai");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(900, 600);

        v1 = new EditorPanel(currentBuffer);
        v1.setFocusTraversalKeysEnabled(false);

        v1.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                if (!Character.isISOControl(e.getKeyChar())) {
                    Mode m = modeStack.current();

                    if (m == null || m.getBool("insert-like")) {
                        currentBuffer.addChar(e.getKeyChar());
                        v1.repaintEditor();
                    }

                }
            }
        });

        setLayout(new BorderLayout());
        add(v1, BorderLayout.CENTER);

        statusPanel.setPreferredSize(new Dimension(0, 30));
        add(statusPanel, BorderLayout.SOUTH);
        setVisible(true);
    }

    public void enter() {
        currentBuffer.addChar('\n');
        currentBuffer.updateToken();
    }
}
