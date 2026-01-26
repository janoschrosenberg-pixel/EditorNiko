package view;

import nikolai.Buffer;
import nikolai.Direction;

import javax.swing.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class EditorFrame extends JFrame {

    public EditorFrame() {
        setTitle("Nikolai");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(900, 600);

        Buffer b1 = new Buffer();


        EditorPanel v1 = new EditorPanel(b1);


        v1.getEditorView().addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                if (!Character.isISOControl(e.getKeyChar())) {
                    b1.addChar(e.getKeyChar());
                    v1.repaintEditor();
                }
            }

            @Override
            public void keyPressed(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_LEFT -> b1.moveCursor(Direction.LEFT, 1);
                    case KeyEvent.VK_RIGHT -> b1.moveCursor(Direction.RIGHT, 1);
                    case KeyEvent.VK_UP -> b1.moveCursor(Direction.UP, 1);
                    case KeyEvent.VK_DOWN -> b1.moveCursor(Direction.DOWN, 1);
                    case KeyEvent.VK_ENTER -> b1.addChar('\n');
                    case KeyEvent.VK_BACK_SPACE -> b1.backspace();
                }
                v1.repaintEditor();
            }
        });

        setContentPane(v1);
        setVisible(true);
    }
}
