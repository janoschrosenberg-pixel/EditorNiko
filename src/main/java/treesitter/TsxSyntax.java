package treesitter;

import io.github.treesitter.jtreesitter.Language;
import io.github.treesitter.jtreesitter.Parser;
import io.github.treesitter.jtreesitter.Tree;
import io.github.treesitter.jtreesitter.Node;

import java.awt.*;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.Arena;

import java.util.*;
import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;


public class TsxSyntax {
    private final static Parser parser;

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

   static {
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

    public static Optional<Tree> parse(String text) {
        return parser.parse(text);
    }

    public static Optional<Tree> parseIncremental(String text, Tree oldTree) {
        return parser.parse(text, oldTree);
    }
    public record SyntaxToken(Color color, int startCol, int startRow, int endCol, int endRow){ }

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

            TsxSyntax tsxParser = new TsxSyntax();


            Optional<Tree> treeOpt = tsxParser.parse(tsxCode);

            Set<String> blar = new HashSet<>();

            System.out.println( collectTokens(treeOpt.get()));

            System.out.println(blar);

        } catch (Exception e) {
            System.err.println("✗ Fehler:");
            e.printStackTrace();
        }
    }



    public static List<List<SyntaxToken>> collectTokens(Tree treeOpt) {
        List<List<SyntaxToken>> lines = new ArrayList<>();

        Node rootNode = treeOpt.getRootNode();

        // 1. Tokens nach Zeile sammeln
        consumeChild(rootNode, node -> {


            var start = node.getStartPoint();
            var end = node.getEndPoint();

            int row = start.row();

            // Stelle sicher, dass die Zeile existiert
            while (lines.size() <= row) {
                lines.add(new ArrayList<>());
            }


            lines.get(row).add(new SyntaxToken(
                   mapColor( node.getType()),
                    start.column(),
                    row,
                    end.column(),
                    end.row()
            ));
        });

        return lines;
    }

    static final Color[] CYBERPUNK_NEON = {
            new Color(0x00FFFF), // Neon Cyan
            new Color(0xFF00FF), // Neon Magenta
            new Color(0x00FFEA),
            new Color(0xFF2EFF),
            new Color(0x00E5FF),
            new Color(0x7CFF00), // Acid Green
            new Color(0xFF1744), // Neon Red
            new Color(0x1AFF1A),
            new Color(0x2979FF),
            new Color(0x18FFFF),
            new Color(0xFF9100),
            new Color(0xC6FF00),
            new Color(0x651FFF),
            new Color(0x00B0FF),
            new Color(0xF500FF),
            new Color(0x76FF03),

            new Color(0x40C4FF),
            new Color(0xE040FB),
            new Color(0x00FFC8),
            new Color(0xFF4081),
            new Color(0x64FFDA),
            new Color(0xFF6E40),
            new Color(0x00E676),
            new Color(0x536DFE),
            new Color(0x1DE9B6),
            new Color(0xFF3D00),
            new Color(0xC51162),
            new Color(0x00BFA5),
            new Color(0x6200EA),
            new Color(0x00E5FF),
            new Color(0xAEEA00),
            new Color(0xD500F9),

            new Color(0x18FFFF),
            new Color(0xFF80AB),
            new Color(0x69F0AE),
            new Color(0xFF5252),
            new Color(0x40C4FF),
            new Color(0xEEFF41),
            new Color(0xB388FF),
            new Color(0x00FFD5),
            new Color(0xFF1744),
            new Color(0x76FF03),
            new Color(0x7C4DFF),
            new Color(0x1DE9B6),
            new Color(0xFF9100),
            new Color(0x00B8D4),
            new Color(0xF50057),
            new Color(0xA7FFEB)
    };

    static Color mapColor(String type) {
        int index = Math.floorMod(type.hashCode(), CYBERPUNK_NEON.length);
        return CYBERPUNK_NEON[index];
    }


    private static void consumeChild( Node child, Consumer<Node> consumer) {
        if(child.getChildCount() == 0) {
            consumer.accept(child);
        }

        child.getChildren().forEach(e -> consumeChild(e, consumer));
    }
}