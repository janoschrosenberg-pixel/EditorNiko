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
      lineNumbers = new LineNumberView(buffer, editorView);

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

    public void setTextBox( java.util.List<String> items) {
      editorView.setTextBox(items);
    }

    public void nextText() {
      editorView.nextText();
    }
    public void clearTextBox() {
      editorView.clearTextBox();
    }

    public String getCurrentSelection() {
      return editorView.getCurrentSelection();
    }

    public void prevText() {
      editorView.prevText();
    }

    public EditorView getEditorView() {
      return this.editorView;
    }
}