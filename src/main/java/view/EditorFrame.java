package view;
import clojure.lang.IFn;

import nikolai.*;
import nikolai.keybinding.*;
import nikolai.lsp.LSP;
import nikolai.piecetable.Pos;


import javax.swing.*;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.nio.file.Path;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class EditorFrame extends JFrame {

    private final ModeRegistry modeRegistry = new ModeRegistry();
    private final ModeStack modeStack = new ModeStack();
    private EditorPanel v1;
    private int unnamedCounter = 1;
    private Buffer currentBuffer;
    private Map<String, Buffer> bufferCache = new HashMap<>();
    private FileScanner scanner = new FileScanner();
    private final StatusPanel statusPanel = new StatusPanel();
    private CommandPanel commandPanel = new CommandPanel();
    private final LSP lsp = LSP.INSTANCE;
    Deque<Buffer> bufferBackStack = new ArrayDeque<>();
    Deque<Buffer> bufferForwardStack = new ArrayDeque<>();

    //TODO Muss ein String sein weil sich Positionen verändern werden
    private List<String> internalClipboard = new ArrayList<>();

    private String completionPrefix = "";

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

    public void copyCurrentToken() {
      var text = this.currentBuffer.getTextFromCurrentToken();
      if(text != null) {
          if(!this.internalClipboard.contains(text)) {
              this.internalClipboard.add(text);
          }
      }
    }


    public void insertLastInternalClipboard() {
        if(!this.internalClipboard.isEmpty()) {
           String text = this.internalClipboard.getLast();

           this.currentBuffer.addText(text);
        }
        this.currentBuffer.updateToken();
    }

    public void showInternalClipboard() {
        v1.setTextBox(this.internalClipboard);
    }

    public void inserText(String text) {
        this.currentBuffer.addText(text);
    }

    public String getBufferMenuStackLength() {
        return bufferMenuStack.size()+"";
    }

    public void jumpToDefinition() {
       Pos cursor = this.currentBuffer.getCursor();
        try {
         var gotoDef = lsp.getDefinition(cursor.line()+1, cursor.col()+1, currentBuffer.getFileName());
         if (gotoDef != null) {
            var newFile =  gotoDef.file.getAbsolutePath();
            if(!newFile.equals(currentBuffer.getFileName())) {
                loadFile(newFile);
            }
             currentBuffer.setCursor(new Pos(gotoDef.line-1, gotoDef.col-1));
         }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void prevToken() {
        this.currentBuffer.moveToToken(Direction.LEFT);
    }
    public void nextToken() {
        this.currentBuffer.moveToToken(Direction.RIGHT);
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

    public void clearCommand() {
        this.commandPanel.clear();
    }

    public String getCommandText() {
        return this.commandPanel.getText();
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
        var current = modeStack.current();
        commandPanel.setVisible(current.getBool("command"));
        updateStatusProp();
    }
    public void setMode(String name) {
        Mode m = modeRegistry.getOrCreate(name);
        modeStack.push(m);
        if(m.getBool("command")) {
            commandPanel.setVisible(true);
        }
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

        String menuName = "File Menu";
        if(!this.currentBuffer.getFileName().equals(menuName)){
            bufferBackStack.push(this.currentBuffer );
        }else{
            bufferBackStack.push(bufferMenuStack.peek());
        }

        bufferForwardStack.clear();

        if(bufferCache.containsKey(file)) {
            this.currentBuffer = bufferCache.get(file);
        }else{
            this.currentBuffer = new Buffer(Path.of(file));
            bufferCache.put(currentBuffer.getFileName(), currentBuffer);
        }

        updateStatusProp();
    }

    public void goBufferBack() {
        if (bufferBackStack.isEmpty()) return;

        bufferForwardStack.push(this.currentBuffer);
        this.currentBuffer = bufferBackStack.pop();
        updateStatusProp();
    }

    public void goBufferForward() {

        if (bufferForwardStack.isEmpty()) return;

        bufferBackStack.push(currentBuffer);
        currentBuffer = bufferForwardStack.pop();
        updateStatusProp();
    }


    public void insertCompletion() {
      String completion =  this.getCurrentSelection();
      this.currentBuffer.addText(completion.substring(completionPrefix.length()));
      exitTextMode();
    }
    public void selectFile() {
        loadCurrentRowAsFile();
        bufferMenuStack.pop();
        updateStatusProp();
    }

    public void loadCurrentRowAsFile() {
       String currentLine = currentBuffer.getCurrentLine();

       loadFile(currentLine);
    }

    public String getCurrentLine() {
        return currentBuffer.getCurrentLine();
    }

    public void saveFile(String fileName) {
        currentBuffer.saveFile(fileName);
    }

    public boolean isNewFile(){
        return currentBuffer.isNewFile();
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

    public void delCommand() {
        this.commandPanel.del();
    }

    public void filterFiles() {
      String filter = this.commandPanel.getText();
      String filtered =  scanner.getFiles().stream().filter(f->f.contains(filter)).collect(Collectors.joining("\n"));
      currentBuffer.clear();
      currentBuffer.addText(filtered);
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

                    if (m != null && m.getBool("insert-like")) {
                        if(m.getBool("command")) {
                            commandPanel.addChar(e.getKeyChar());
                        }else{
                            currentBuffer.addChar(e.getKeyChar());
                        }

                        var trigger = m.getFunc("trigger");

                        if(trigger != null) {
                            trigger.invoke();
                        }

                        v1.repaintEditor();
                    }



                }
            }
        });

        setLayout(new BorderLayout());

        commandPanel.setPreferredSize(new Dimension(0, 30));
        commandPanel.setVisible(false);
        add(commandPanel, BorderLayout.NORTH);
        add(v1, BorderLayout.CENTER);

        statusPanel.setPreferredSize(new Dimension(0, 30));
        add(statusPanel, BorderLayout.SOUTH);
        setVisible(true);
    }

    public void enter() {
        currentBuffer.addReturn();
        currentBuffer.updateToken();
    }

    public void jumpToBegin() {
       this.currentBuffer.jumpToBegin();
    }

    public void jumpToEnd() {
       this.currentBuffer.jumoToEnd();
    }

    public void showCompletions() {
        Pos cursor = this.currentBuffer.getCursor();
        var line = this.currentBuffer.getTextFromStartlineTo(cursor);

        String lastTyped = "";
        if(line != null && line.length()>0) {
            var split = line.split("[\\s\\p{Punct}]+");
            if(split.length>0)
            lastTyped = split[split.length-1];
        } else {
            lastTyped = "";
        }

        if(line.endsWith(".") || line.endsWith(" ")) {
            lastTyped = "";
        }

        try {
        var completions = this.lsp.getCompletions(cursor.line()+1, cursor.col()+1,currentBuffer.getText(), currentBuffer.getFileName());
            completionPrefix = lastTyped;
            var currentCompletions = completions.stream().filter(s->s.startsWith(completionPrefix)).toList();
            setTextBox(currentCompletions);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String getCurrentSelection() {
        return  v1.getCurrentSelection();
    }

    public void setTextBox( java.util.List<String> items) {
        this.v1.setTextBox(items);
    }

    public void nextText() {
        this.v1.nextText();
    }

    public void exitTextMode() {
        this.v1.clearTextBox();
    }

    public void prevText() {
        this.v1.prevText();
    }

}
