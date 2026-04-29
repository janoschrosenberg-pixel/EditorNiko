package treesitter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Utility class to extract and load native libraries from JAR resources.
 */
public class NativeLibraryLoader {

    private static final String TEMP_DIR_PREFIX = "treesitter-natives-";
    private static Path nativeLibDir = null;

    /**
     * Detects the operating system.
     */
    public static String getOS() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            return "windows";
        } else if (os.contains("mac")) {
            return "macos";
        } else {
            return "linux";
        }
    }

    /**
     * Gets the library extension for the current OS.
     */
    public static String getLibraryExtension() {
        String os = getOS();
        switch (os) {
            case "windows":
                return ".dll";
            case "macos":
                return ".dylib";
            default:
                return ".so";
        }
    }

    /**
     * Extracts a native library from JAR resources to a temporary directory.
     *
     * @param libraryName Name of the library (e.g., "libtree-sitter", "tree-sitter-tsx")
     * @return Path to the extracted library file
     * @throws IOException if extraction fails
     */
    public static Path extractLibrary(String libraryName) throws IOException {
        // Create temp directory if needed
        if (nativeLibDir == null) {
            nativeLibDir = Files.createTempDirectory(TEMP_DIR_PREFIX);
            nativeLibDir.toFile().deleteOnExit();
        }

        String os = getOS();
        String extension = getLibraryExtension();

        // Windows DLLs don't use the "lib" prefix
        if (os.equals("windows") && libraryName.startsWith("lib")) {
            libraryName = libraryName.substring(3); // Remove "lib" prefix
        }

        String fileName = libraryName + extension;
        String resourcePath = "/natives/" + os + "/" + fileName;

        // Check if library exists in resources
        InputStream input = NativeLibraryLoader.class.getResourceAsStream(resourcePath);
        if (input == null) {
            throw new IOException("Native library not found in JAR: " + resourcePath);
        }

        // Extract to temp directory
        Path outputPath = nativeLibDir.resolve(fileName);

        // Only extract if not already extracted
        if (!Files.exists(outputPath)) {
            Files.copy(input, outputPath, StandardCopyOption.REPLACE_EXISTING);
            outputPath.toFile().deleteOnExit();

            // Make executable on Unix systems
            if (!getOS().equals("windows")) {
                outputPath.toFile().setExecutable(true);
                outputPath.toFile().setReadable(true);
                outputPath.toFile().setWritable(true);
            }
        }

        input.close();
        return outputPath;
    }

    /**
     * Loads a native library by extracting it from JAR and calling System.load().
     *
     * @param libraryName Name of the library (e.g., "libtree-sitter", "tree-sitter-tsx")
     * @throws IOException if extraction fails
     * @throws UnsatisfiedLinkError if loading fails
     */
    public static void loadLibrary(String libraryName) throws IOException {
        Path libPath = extractLibrary(libraryName);
        System.load(libPath.toAbsolutePath().toString());
        System.out.println("✓ Loaded: " + libPath.getFileName());
    }

    /**
     * Gets the path to an extracted library without loading it.
     * Used for SymbolLookup.
     *
     * @param libraryName Name of the library
     * @return Absolute path to the extracted library
     * @throws IOException if extraction fails
     */
    public static String getLibraryPath(String libraryName) throws IOException {
        Path libPath = extractLibrary(libraryName);
        return libPath.toAbsolutePath().toString();
    }
}
