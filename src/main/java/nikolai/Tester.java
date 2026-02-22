package nikolai;

public class Tester {
    static void main(String[] args) {
        FileScanner scanner = new FileScanner();
        scanner.scanDirectory();

        scanner.getFiles().forEach(System.out::println);
    }
}
