package view;

import nikolai.Buffer;

import javax.swing.*;
import java.awt.*;

public class EditorPanel extends JPanel {
    LineNumberView lineNumbers;
    private EditorView editorView;
    public EditorPanel(Buffer buffer) {
        setLayout(new BorderLayout());

        this.editorView = new EditorView(buffer);
         lineNumbers =
                new LineNumberView(buffer, editorView);

        add(lineNumbers, BorderLayout.WEST);
        add(editorView, BorderLayout.CENTER);
    }
    public void repaintEditor(){
        repaint();
        this.editorView.repaint();
    }

    public void changeBuffer(Buffer buffer) {

        this.editorView.changeBuffer(buffer);
        lineNumbers.changeBuffer(buffer);
    }

    public EditorView getEditorView() {
        return this.editorView;
    }
}
