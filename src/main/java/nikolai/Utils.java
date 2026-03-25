package nikolai;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class Utils {
    public static void writeWorkspace(String workspace) {
        Path filePath = Path.of("workspace.txt");

        try {
            Files.writeString(
                    filePath,
                    workspace,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,      // Erstellt Datei, falls sie nicht existiert
                    StandardOpenOption.TRUNCATE_EXISTING // Überschreibt Inhalt, falls sie existiert
            );
        } catch (IOException e) {
            // Optional: Logging oder Weiterwerfen
            e.printStackTrace();
        }
    }

    public static String[] readFile(String filename) {
        try {
            java.util.List<String> lines = Files.readAllLines(Paths.get(filename));
            return lines.toArray(new String[0]);
        } catch (IOException e) {
            e.printStackTrace();
            return new String[0]; // leeres Array bei Fehler
        }
    }


    public static String showFileChooser(Container parent) {
        JFileChooser fileChooser = new JFileChooser();

        // Nur Verzeichnisse erlauben
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fileChooser.setAcceptAllFileFilterUsed(false);

        // Dialog anzeigen
        int result = fileChooser.showOpenDialog(parent);

        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedDir = fileChooser.getSelectedFile();
            return selectedDir.getAbsolutePath();
        }

        // Abbruch oder Fehler
        return null;
    }
}
