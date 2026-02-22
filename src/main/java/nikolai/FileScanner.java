package nikolai;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FileScanner {

    // Thread-safe Liste für parallele Zugriffe
    private final List<String> files = new CopyOnWriteArrayList<>();


    private final List<String> ignoredFolders = List.of(
            "out", "output", "target", ".vscode", "build", "node_modules"
    );

    public List<String> getFiles() {
        return files;
    }

    public void scanDirectory() {
        Path workspacePath;

        // workspace.txt einlesen
        try {
            workspacePath = Path.of(Files.readString(Path.of("workspace.txt")).trim());

        } catch (IOException e) {
            System.out.println("Fehler beim Lesen von workspace.txt: " + e.getMessage());
            return;
        }

        if (!Files.exists(workspacePath) || !Files.isDirectory(workspacePath)) {
            System.err.println("Workspace-Pfad ist ungültig: " + workspacePath);
            return;
        }

        try {
            scanRecursive(workspacePath);
        } catch (IOException e) {
            System.err.println("Fehler beim Scannen des Verzeichnisses: " + e.getMessage());
        }
    }

    public String filesToString() {
        return String.join("\n", getFiles());
    }
    private void scanRecursive(Path dir) throws IOException {
        try (Stream<Path> paths = Files.list(dir)) {
            paths.parallel().forEach(path -> {
                if (Files.isDirectory(path)) {
                    String folderName = path.getFileName().toString();
                    if (!ignoredFolders.contains(folderName)) {
                        try {
                            scanRecursive(path); // rekursiv scannen
                        } catch (IOException e) {
                            System.err.println("Fehler beim Zugriff auf Verzeichnis: " + path);
                        }
                    }
                } else if (Files.isRegularFile(path)) {
                    String name = path.getFileName().toString().toLowerCase();
                    if (name.endsWith(".tsx") || name.endsWith(".jsx") ||
                        name.endsWith(".ts") || name.endsWith(".js") ||
                        name.endsWith(".css") || name.endsWith(".html")) {
                        files.add(path.toAbsolutePath().toString());
                    }
                }
            });
        }
    }
}
