package nikolai;

import org.jspecify.annotations.NonNull;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;

public class FileScanner {

    // Thread-safe Listen für parallele Zugriffe
    private final List<String> files = new CopyOnWriteArrayList<>();
    private final List<String> folders = new CopyOnWriteArrayList<>();

    private final List<String> ignoredFolders = List.of(
            "out", "output", "target", ".vscode", "build", "node_modules"
    );

    public List<String> getFiles() {
        return files;
    }

    public List<String> getFolders() {
        return folders;
    }

    public void scanDirectory() {
        Path workspacePath;

        // workspace.txt einlesen
        try {
            workspacePath = getPath();
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

    public static @NonNull Path getPath() throws IOException {
        Path workspacePath;
        workspacePath = Path.of(Files.readString(Path.of("workspace.txt")).trim());
        return workspacePath;
    }

    public String filesToString() {
        return String.join("\n", getFiles());
    }

    public String foldersToString() {
        return String.join("\n", getFolders());
    }

    private void scanRecursive(Path dir) throws IOException {
        folders.add(dir.toAbsolutePath().toString()); // Verzeichnis hinzufügen

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