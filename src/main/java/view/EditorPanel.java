package view;

import nikolai.Buffer;

import javax.swing.*;
import java.awt.*;

public class EditorPanel extends JPanel {

    private EditorView editorView;
    public EditorPanel(Buffer buffer) {
        setLayout(new BorderLayout());

        this.editorView = new EditorView(buffer);
        LineNumberView lineNumbers =
                new LineNumberView(buffer, editorView);

        add(lineNumbers, BorderLayout.WEST);
        add(editorView, BorderLayout.CENTER);
    }
    public void repaintEditor(){
        repaint();
        this.editorView.repaint();
    }

    public EditorView getEditorView() {
        return this.editorView;
    }
}
