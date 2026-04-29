package nikolai.lsp;

import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.launch.LSPLauncher;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageServer;

import java.io.*;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class TsLspClient {
    private final List<Error> collectedErrors = Collections.synchronizedList(new ArrayList<>());
    private final Map<String, CompletableFuture<Void>> diagnosticFutures = new ConcurrentHashMap<>();
    public static class GotoDefInfo {
        public File file;
        public int line;
        public int col;

        public GotoDefInfo(File file, int line, int col) {
            this.file = file;
            this.line = line;
            this.col = col;
        }

        @Override
        public String toString() {
            return file + ":" + line + ":" + col;
        }
    }

    private final LanguageServer server;
    private final Launcher<LanguageServer> launcher;
    private boolean initialized = false;

    public TsLspClient(InputStream in, OutputStream out) {
        // LSPLauncher.createClientLauncher ist die korrekte Factory-Methode
        // (nicht Launcher.createClientLauncher)
        LanguageClient client = new LanguageClient() {
            @Override
            public void telemetryEvent(Object object) {
                // ignoriert
            }

            @Override
            public void publishDiagnostics(PublishDiagnosticsParams diagnostics) {
                String fileUri = diagnostics.getUri();
                String filePath;
                try {
                    filePath = Paths.get(new URI(fileUri)).toAbsolutePath().toString();
                } catch (Exception e) {
                    filePath = fileUri;
                }

                String fp = filePath;
                collectedErrors.removeIf(err -> err.file().equals(fp));

                for (Diagnostic diag : diagnostics.getDiagnostics()) {
                    collectedErrors.add(new Error(
                            fp,
                            diag.getRange().getStart().getLine() + 1,
                            diag.getRange().getStart().getCharacter() + 1,
                            diag.getMessage(),
                            diag.getSeverity()
                    ));
                }

                // Signal: Diagnostics für diese URI sind da
                CompletableFuture<Void> future = diagnosticFutures.remove(fileUri);
                if (future != null) {
                    future.complete(null);
                }
            }

            @Override
            public void showMessage(MessageParams messageParams) {
                // ignoriert
            }

            @Override
            public void logMessage(MessageParams message) {
                // ignoriert
            }

            @Override
            public CompletableFuture<MessageActionItem> showMessageRequest(ShowMessageRequestParams requestParams) {
                return CompletableFuture.completedFuture(null);
            }
        };

        launcher = LSPLauncher.createClientLauncher(client, in, out);
        server = launcher.getRemoteProxy();
        launcher.startListening();
    }

    /**
     * Initialisiert den LSP-Server. Muss vor allen anderen Aufrufen erfolgen.
     */


    public void initialize(String workspacePath) throws Exception {
        InitializeParams initParams = new InitializeParams();
        initParams.setRootUri(Paths.get(workspacePath).toAbsolutePath().toUri().toString());

        // Dem Server mitteilen, dass wir publishDiagnostics verstehen
        ClientCapabilities capabilities = new ClientCapabilities();
        TextDocumentClientCapabilities textDocCaps = new TextDocumentClientCapabilities();
        textDocCaps.setPublishDiagnostics(new PublishDiagnosticsCapabilities());
        capabilities.setTextDocument(textDocCaps);
        initParams.setCapabilities(capabilities);

        server.initialize(initParams).get();
        server.initialized(new InitializedParams());
        initialized = true;
    }

    /**
     * Go to definition
     */
    public List<GotoDefInfo> gotoDef(String workspace, String fileName, int line, int col) throws Exception {
        if (!initialized) {
            initialize(workspace);
        }

        List<GotoDefInfo> result = new ArrayList<>();

        // 1. Datei öffnen (wichtig für LSP)
        String absPath = Paths.get(workspace, fileName).toAbsolutePath().toString();
        String uri = Paths.get(absPath).toUri().toString();
        String text = new String(Files.readAllBytes(Paths.get(absPath)));

        server.getTextDocumentService().didOpen(new DidOpenTextDocumentParams(
                new TextDocumentItem(uri, "typescript", 1, text)
        ));

        // 2. Definition-Request
        DefinitionParams params = new DefinitionParams(
                new TextDocumentIdentifier(uri),
                new Position(line - 1, col - 1) // LSP ist 0-basiert
        );

        CompletableFuture<Either<List<? extends Location>, List<? extends LocationLink>>> future =
                server.getTextDocumentService().definition(params);

        Either<List<? extends Location>, List<? extends LocationLink>> defResult = future.get();

        if (defResult == null) {
            return result;
        }

        if (defResult.isLeft()) {
            for (Location loc : defResult.getLeft()) {
                String defFile = Paths.get(new URI(loc.getUri())).toAbsolutePath().toString();
                int defLine = loc.getRange().getStart().getLine() + 1;
                int defCol = loc.getRange().getStart().getCharacter() + 1;
                result.add(new GotoDefInfo(new File(defFile), defLine, defCol));
            }
        } else if (defResult.isRight()) {
            for (LocationLink link : defResult.getRight()) {
                String defFile = Paths.get(new URI(link.getTargetUri())).toAbsolutePath().toString();
                int defLine = link.getTargetSelectionRange().getStart().getLine() + 1;
                int defCol = link.getTargetSelectionRange().getStart().getCharacter() + 1;
                result.add(new GotoDefInfo(new File(defFile), defLine, defCol));
            }
        }

        return result;
    }

    private  Set<String> openedFiles = new HashSet<>();
    private  Map<String, String> textCache = new HashMap<>();


    public List<String> getCompletions(String workspacePath, String fileName, String editorText, int line, int col) throws Exception {
        if (!initialized) {
            initialize(workspacePath); // Stelle sicher, dass workspacePath als Feld existiert oder übergeben wird
        }

        List<String> completions = new ArrayList<>();

        // Absolute Datei-URI (Identifier für LSP)
        Path absPath = Paths.get(fileName).toAbsolutePath();
        String uri = absPath.toUri().toString();

        // Track bereits geöffneter Dateien
        if (openedFiles == null) {
            openedFiles = new HashSet<>();
        }
        if (textCache == null) {
            textCache = new HashMap<>();
        }

        // --- Datei öffnen oder Änderungen senden ---
        if (!openedFiles.contains(uri)) {
            // Erstes Öffnen
            server.getTextDocumentService().didOpen(
                    new DidOpenTextDocumentParams(
                            new TextDocumentItem(uri, "typescript", 1, editorText)
                    )
            );
            openedFiles.add(uri);
            textCache.put(uri, editorText);
        } else if (!editorText.equals(textCache.get(uri))) {
            // Änderungen senden
            server.getTextDocumentService().didChange(
                    new DidChangeTextDocumentParams(
                            new VersionedTextDocumentIdentifier(uri, 1),
                            List.of(new TextDocumentContentChangeEvent(editorText))
                    )
            );
            textCache.put(uri, editorText);
        }

        // --- Position für Completion ---
        String[] lines = editorText.split("\n");
        int lspLine = Math.max(0, line - 1);
        lspLine = Math.min(lspLine, lines.length - 1);

        int lspCol = Math.max(0, col - 1);
        lspCol = Math.min(lspCol, lines[lspLine].length());

        CompletionParams params = new CompletionParams(
                new TextDocumentIdentifier(uri),
                new Position(lspLine, lspCol)
        );

        // --- Completion-Request ---
        CompletableFuture<Either<List<CompletionItem>, CompletionList>> future =
                server.getTextDocumentService().completion(params);

        Either<List<CompletionItem>, CompletionList> result = future.get();
        if (result != null) {
            if (result.isLeft()) {
                for (CompletionItem item : result.getLeft()) completions.add(item.getLabel());
            } else if (result.isRight()) {
                for (CompletionItem item : result.getRight().getItems()) completions.add(item.getLabel());
            }
        }

        return completions;
    }

    /**
     * Server herunterfahren.
     */
    public void shutdown() throws Exception {
        server.shutdown().get();
        server.exit();
    }

    public List<Error> findErrors() {
        return new ArrayList<>(collectedErrors);
    }

    public List<Error> findErrors(String workspacePath) throws Exception {
        if (!initialized) initialize(workspacePath);

        // Alle .ts / .tsx Dateien finden und öffnen
        try (var stream = Files.walk(Paths.get(workspacePath))) {
            List<Path> tsFiles = stream
                    .filter(p -> p.toString().endsWith(".ts") || p.toString().endsWith(".tsx"))
                    .filter(p -> !p.toString().contains("node_modules"))
                    .toList();

            for (Path file : tsFiles) {
                try {
                    openFileAndAwaitDiagnostics(file);
                } catch (java.util.concurrent.TimeoutException e) {
                    // Server hat für diese Datei keine Diagnostics geschickt
                    // (= keine Fehler) – weiter
                }
            }
        }

        return new ArrayList<>(collectedErrors);
    }
    private void openFileAndAwaitDiagnostics(Path file) throws Exception {
        String uri = file.toAbsolutePath().toUri().toString();

        if (openedFiles.contains(uri)) return;

        CompletableFuture<Void> future = new CompletableFuture<>();
        diagnosticFutures.put(uri, future);

        String text = Files.readString(file);
        server.getTextDocumentService().didOpen(
                new DidOpenTextDocumentParams(
                        new TextDocumentItem(uri, "typescript", 1, text)
                )
        );
        openedFiles.add(uri);
        textCache.put(uri, text);

        // Warten bis Server Diagnostics schickt (max 5 Sekunden)
        future.get(5, java.util.concurrent.TimeUnit.SECONDS);
    }
}