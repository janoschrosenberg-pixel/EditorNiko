# EditorNiko

Ein Java-basierter TSX/TypeScript Parser mit Tree-Sitter Integration.

## Features

- **Tree-Sitter TSX Parser**: Parst TSX/TypeScript Code mit der tree-sitter Library
- **Inkrementelles Parsing**: Unterstützt effiziente Updates des Syntax-Trees
- **Plattformübergreifend**: Funktioniert auf Windows, macOS und Linux
- **Vollständig Portabel**: Alle nativen Libraries sind in der JAR eingebettet
- **Keine Installation nötig**: Einfach die JAR ausführen - keine System-Dependencies!

## Voraussetzungen

### Java
- **JDK 25** oder höher

### Native Libraries

✅ **KEINE Installation nötig!** Alle nativen Libraries sind bereits in der JAR eingebettet und werden automatisch zur Laufzeit extrahiert.

Die JAR enthält bereits:

#### macOS ✅
- `libtree-sitter.dylib` - Tree-Sitter Core Library (210 KB)
- `tree-sitter-tsx.dylib` - TSX Language Parser (1.4 MB)

#### Windows ✅
- `tree-sitter.dll` - Tree-Sitter Core Library (304 KB)
- `tree-sitter-tsx.dll` - TSX Language Parser (1.5 MB)

#### Linux ⚠️ (noch nicht verfügbar)
- `libtree-sitter.so` - Tree-Sitter Core Library
- `tree-sitter-tsx.so` - TSX Language Parser

**Hinweis**: Linux-Unterstützung kann hinzugefügt werden, indem die entsprechenden `.so` Dateien in `src/main/resources/natives/linux/` kopiert und das Projekt neu gebaut wird.

## Build

### Kompilieren und JAR erstellen
```bash
mvn clean package
```

Dies erstellt:
- `target/EditorNiko-executable.jar` - Executable Fat JAR mit allen Dependencies und eingebetteten nativen Libraries (~970 KB)

### Nur kompilieren
```bash
mvn compile
```

### Schnellstart mit Run-Scripts
```bash
# Unix/macOS/Linux
./run.sh

# Windows
run.bat
```

## Ausführung

### Mit der executable JAR (empfohlen)
```bash
# Einfachste Methode
java --enable-native-access=ALL-UNNAMED -jar target/EditorNiko-executable.jar
```

Die JAR ist **vollständig portabel** und kann:
- ✅ Von jedem Verzeichnis ausgeführt werden
- ✅ Auf anderen Rechnern verwendet werden (nur Java 25+ erforderlich)
- ✅ Ohne Installation von System-Libraries laufen
- ✅ Auf **Windows und macOS** ohne zusätzliche Schritte funktionieren
- ✅ Native Libraries werden automatisch in ein temporäres Verzeichnis extrahiert

### Mit Maven
```bash
mvn exec:java -Dexec.mainClass="treesitter.TsxSyntax"
```

## Beispiel-Ausgabe

Das Programm parst TSX-Code und gibt den Syntax-Tree aus:

```
Lade native Libraries aus JAR...
✓ Loaded: libtree-sitter.dylib
Initialisiere TSX Parser...
✓ Extrahiert: tree-sitter-tsx.dylib
✓ TSX Parser bereit

=== Parse TSX Code ===
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

=== Parsing... ===
✓ Parsing erfolgreich!

Syntax Tree Info:
  Root Node Type: program
  Start Byte: 0
  End Byte: 518
  Child Count: 4
  Has Error: false

=== Root Children ===
  Child 0: import_statement [0-40]
  Child 1: interface_declaration [42-111]
  Child 2: lexical_declaration [113-491]
  Child 3: export_statement [493-517]

=== Test: Inkrementelles Parsing ===
✓ Inkrementelles Update erfolgreich!
  New Tree Has Error: false
```

## Projektstruktur

```
EditorNiko/
├── src/main/java/
│   └── treesitter/
│       ├── TsxSyntax.java               # Hauptklasse mit TSX Parser
│       └── NativeLibraryLoader.java     # Utility zum Laden eingebetteter Libraries
├── src/main/resources/
│   └── natives/
│       ├── macos/
│       │   ├── libtree-sitter.dylib
│       │   └── tree-sitter-tsx.dylib
│       ├── linux/
│       │   └── README.txt               # Anleitung zum Hinzufügen von Linux Libraries
│       └── windows/
│           └── README.txt               # Anleitung zum Hinzufügen von Windows Libraries
├── pom.xml                               # Maven Build-Konfiguration
├── run.sh                                # Unix/macOS Run-Script
├── run.bat                               # Windows Run-Script
└── README.md
```

## Dependencies

- **jtreesitter** (0.25.5): Java Bindings für tree-sitter
- **piecetable** (0.6.2): Piece Table Datenstruktur
- **Maven Shade Plugin**: Für Fat JAR Erstellung mit eingebetteten Ressourcen

## Wie es funktioniert

### Native Library Loading

Die Klasse `NativeLibraryLoader` kümmert sich um das automatische Extrahieren und Laden:

1. **OS-Detection**: Erkennt automatisch das Betriebssystem (Windows/macOS/Linux)
2. **Extraktion**: Extrahiert die passenden Libraries aus der JAR in ein temporäres Verzeichnis
3. **Laden**: Lädt die Libraries mit `System.load()` und `SymbolLookup`
4. **Cleanup**: Temporäre Dateien werden beim Beenden automatisch gelöscht

```java
// Automatisches Laden der Core Library
NativeLibraryLoader.loadLibrary("libtree-sitter");

// Automatisches Extrahieren und Pfad zurückgeben
String libPath = NativeLibraryLoader.getLibraryPath("tree-sitter-tsx");
```

## Linux Support hinzufügen

Um Linux Support hinzuzufügen:

1. Kompiliere oder besorge die nativen Libraries für Linux:
   - **tree-sitter**: https://github.com/tree-sitter/tree-sitter
   - **tree-sitter-tsx**: https://github.com/tree-sitter/tree-sitter-typescript

2. Kopiere die Libraries in das Linux-Verzeichnis:
   ```bash
   cp libtree-sitter.so src/main/resources/natives/linux/
   cp tree-sitter-tsx.so src/main/resources/natives/linux/
   ```

3. Rebuild das Projekt:
   ```bash
   mvn clean package
   ```

Die JAR funktioniert dann automatisch auf Windows, macOS und Linux!

### Wie wurden die Windows DLLs gebaut?

Die Windows DLLs wurden mit MinGW-w64 Cross-Compiler auf macOS gebaut:

```bash
# Tree-Sitter Core Library
x86_64-w64-mingw32-gcc -shared -o tree-sitter.dll \
  -Ilib/include -Ilib/src lib/src/lib.c -O2

# Tree-Sitter TSX Parser
x86_64-w64-mingw32-gcc -shared -o tree-sitter-tsx.dll \
  -Itree-sitter/lib/include -Isrc src/parser.c src/scanner.c -O2
```

## Troubleshooting

### "Native library not found in JAR" Fehler
Die nativen Libraries für dein Betriebssystem sind nicht in der JAR enthalten.
- **Windows & macOS**: Sollte funktionieren! Libraries sind bereits enthalten.
- **Linux**: Siehe "Linux Support hinzufügen" oben.

### Native Access Warnung
Dies ist eine Warnung von Java 25+ beim Verwenden von Foreign Function Interface. Um sie zu unterdrücken:
```bash
java --enable-native-access=ALL-UNNAMED -jar target/EditorNiko-executable.jar
```

### Temporäre Dateien
Die extrahierten Libraries werden in einem temporären Verzeichnis gespeichert (z.B. `/tmp/treesitter-natives-xxxxx/`) und beim Beenden automatisch gelöscht.

## Lizenz

Siehe LICENSE Datei für Details.
