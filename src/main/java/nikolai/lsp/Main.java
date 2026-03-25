package nikolai.lsp;

import nikolai.FileScanner;
import nikolai.Utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.List;

public class Main {
    public static void main(String[] args) throws Exception {
        Process tsProcess = new ProcessBuilder(Utils.readFile("lsp_args.txt"))
                .directory(FileScanner.getPath().toFile())
                .redirectErrorStream(false)
                .start();

        // stderr in eigenem Thread konsumieren, sonst blockiert der Prozess!
        Thread stderrThread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(tsProcess.getErrorStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.err.println("[tsserver stderr] " + line);
                }
            } catch (Exception e) {
                // ignorieren
            }
        });
        stderrThread.setDaemon(true);
        stderrThread.start();

        TsLspClient client = new TsLspClient(tsProcess.getInputStream(), tsProcess.getOutputStream());

        System.out.println("Requesting gotoDef...");
        List<TsLspClient.GotoDefInfo> defs = client.gotoDef(
                "/Users/nikolai/react-redux-realworld-example-app",
                "src/components/App.js",
                2, 25
        );

        if (defs.isEmpty()) {
            System.out.println("Keine Definition gefunden.");
        } else {
            System.out.println("Es gibt was");
            System.out.println(defs.getFirst().file);
            defs.forEach(System.out::println);
        }

        client.shutdown();
        tsProcess.destroy();
    }
}