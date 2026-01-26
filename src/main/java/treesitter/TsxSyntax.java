package treesitter;

import io.github.treesitter.jtreesitter.Language;
import io.github.treesitter.jtreesitter.Parser;
import io.github.treesitter.jtreesitter.Tree;
import io.github.treesitter.jtreesitter.Node;

import java.lang.foreign.SymbolLookup;
import java.lang.foreign.Arena;
import java.util.Optional;
import java.io.IOException;

public class TsxSyntax {
    private final Parser parser;

    static {
        try {
            // Lade die tree-sitter Core Library aus den eingebetteten Ressourcen
            System.out.println("Lade native Libraries aus JAR...");
            NativeLibraryLoader.loadLibrary("libtree-sitter");
        } catch (IOException e) {
            System.err.println("Fehler beim Extrahieren der tree-sitter Core Library: " + e.getMessage());
            throw new RuntimeException(e);
        } catch (UnsatisfiedLinkError e) {
            System.err.println("Fehler beim Laden der tree-sitter Core Library: " + e.getMessage());
            throw e;
        }
    }

    public TsxSyntax() {
        parser = new Parser();

        try {
            // Lade die TSX language library aus den eingebetteten Ressourcen
            String library = NativeLibraryLoader.getLibraryPath("tree-sitter-tsx");
            System.out.println("✓ Extrahiert: tree-sitter-tsx" + NativeLibraryLoader.getLibraryExtension());

            SymbolLookup lookup = SymbolLookup.libraryLookup(library, Arena.global());
            Language tsxLang = Language.load(lookup, "tree_sitter_tsx");
            parser.setLanguage(tsxLang);
            System.out.println("✓ TSX Parser bereit");
        } catch (IOException e) {
            throw new RuntimeException("Fehler beim Extrahieren der TSX Library: " + e.getMessage(), e);
        }
    }

    public Optional<Tree> parse(String text) {
        return parser.parse(text);
    }

    public Optional<Tree> parseIncremental(String text, Tree oldTree) {
        return parser.parse(text, oldTree);
    }

    public static void main(String[] args) {
        String tsxCode = """
            import React, { useState } from 'react';
            
            interface ButtonProps {
                label: string;
                onClick: () => void;
            }
            
            const MyButton: React.FC<ButtonProps> = ({ label, onClick }) => {
                const [count, setCount] = useState(0);
                
                const handleClick = () => {
                    setCount(count + 1);
                    onClick();
                };
                
                return (
                    <button 
                        className="btn-primary"
                        onClick={handleClick}
                    >
                        {label} - Clicked {count} times
                    </button>
                );
            };
            
            export default MyButton;
            """;

        try {
            System.out.println("Initialisiere TSX Parser...");
            TsxSyntax tsxParser = new TsxSyntax();

            System.out.println("\n=== Parse TSX Code ===");
            System.out.println(tsxCode);
            System.out.println("\n=== Parsing... ===");

            Optional<Tree> treeOpt = tsxParser.parse(tsxCode);

            if (treeOpt.isPresent()) {
                Tree tree = treeOpt.get();
                Node rootNode = tree.getRootNode();

                System.out.println("✓ Parsing erfolgreich!");
                System.out.println("\nSyntax Tree Info:");
                System.out.println("  Root Node Type: " + rootNode.getType());
                System.out.println("  Start Byte: " + rootNode.getStartByte());
                System.out.println("  End Byte: " + rootNode.getEndByte());
                System.out.println("  Child Count: " + rootNode.getChildCount());
                System.out.println("  Has Error: " + rootNode.hasError());

                System.out.println("\n=== Tree Structure ===");
                System.out.println(tree.toString());

                System.out.println("\n=== Root Children ===");
                for (int i = 0; i < rootNode.getChildCount(); i++) {
                    Optional<Node> childOpt = rootNode.getChild(i);
                    if (childOpt.isPresent()) {
                        Node child = childOpt.get();
                        System.out.println("  Child " + i + ": " + child.getType() +
                                " [" + child.getStartByte() + "-" + child.getEndByte() + "]");
                    }
                }

                System.out.println("\n=== Test: Inkrementelles Parsing ===");
                String modifiedCode = tsxCode.replace("count + 1", "count + 2");
                Optional<Tree> newTreeOpt = tsxParser.parseIncremental(modifiedCode, tree);

                if (newTreeOpt.isPresent()) {
                    System.out.println("✓ Inkrementelles Update erfolgreich!");
                    Tree newTree = newTreeOpt.get();
                    System.out.println("  New Tree Has Error: " + newTree.getRootNode().hasError());
                }

            } else {
                System.err.println("✗ Parsing fehlgeschlagen!");
            }

        } catch (Exception e) {
            System.err.println("✗ Fehler:");
            e.printStackTrace();
        }
    }
}