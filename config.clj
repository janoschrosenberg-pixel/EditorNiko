(ns c
  (:import [view EditorFrame]))

(import '(java.awt Toolkit)
        '(java.awt.datatransfer DataFlavor)
        '(java.awt.datatransfer StringSelection)
        )

(defn read-clip []
  (let [clipboard (.getSystemClipboard (Toolkit/getDefaultToolkit))
        contents (.getContents clipboard nil)]
    (when (.isDataFlavorSupported contents DataFlavor/stringFlavor)
      (.getTransferData contents DataFlavor/stringFlavor))))

(defn write-clip [text]
  (let [clipboard (.getSystemClipboard (Toolkit/getDefaultToolkit))
        selection (StringSelection. text)]
    (.setContents clipboard selection nil)))

(def api (EditorFrame.))

(defn print-buffer [] (.print api))
(defn move-c [dir steps] (.moveCursor api dir steps))

(defn mode [name props]
  (.defineMode api name props))

(defn set-mode [name]
  (.setMode api name))

(defn enter []
  (.enter api))

(defn bind [key mode f]
  (.bindKey api key mode f))

(defn pop-mode [] (.popMode api))
(defn pop-menu-buffer [] (.popMenuBuffer api) (pop-mode))
 (defn select-file [] (.selectFile api) (pop-mode))

(defn move-left [] (move-c "LEFT" 1))
(defn move-right [] (move-c "RIGHT" 1))
(defn move-up [] (move-c "UP" 1))
(defn move-down [] (move-c "DOWN" 1))
(defn show-files [] (.showFiles api) (set-mode "files"))
(defn open-workspace [] (.openWorkspaceChooser api))
(defn back-space [] (.backspace api))
(defn insert-text [text] (.inserText api text))
(defn syntax-update [] (.updateToken api))
(defn set-status-text [text] (.setStatus api text))
(defn filter-files [] (.filterFiles api))
(defn command-text [] (.getCommandText api))
(defn clear-command [] (.clearCommand api))

(defn current-row [] (.getCurrentLine api))

(defn stack-size [] (.getBufferMenuStackLength api))
(defn del-command [] (.delCommand api))
(defn del-and-filter [] (del-command) (filter-files) )

(defn test-status [] (set-status-text (stack-size)))

(defn insert-tab [] (insert-text "  "))

(def repl-ns (create-ns 'c))

(defn eval-text [text]
  (binding [*ns* repl-ns]
    (let [form (read-string text)]
      (eval form))))

(defn eval-command []
 (eval-text (command-text)))

 (defn eval-row [] (eval-text (current-row) ))

(defn eval-command-and-clear [] (eval-command)(clear-command))
(defn enter-command-mode [] (set-mode "command"))

(mode "insert" {:insert-like true})
(mode "files" {:insert-like true :command true :trigger filter-files})
(mode "special" {})
(mode "command" {:insert-like true :command true})

(bind "ctrl-j" "insert" move-left)
(bind "ctrl-l" "insert" move-right)
(bind "ctrl-i" "insert" move-up)
(bind "ctrl-k" "insert" move-down)
(bind "enter" "insert" enter)
(bind "ctrl-p" "insert" show-files)
(bind "ctrl-E" "insert" (fn [] (set-mode "special")))
(bind "back" "insert" back-space)
(bind "tab" "insert" insert-tab)
(bind "ctrl-c" "insert" enter-command-mode)
(bind "ctrl-e" "insert" eval-row)

(bind "back" "command" del-command)
(bind "enter" "command" eval-command-and-clear)
(bind "esc" "command" pop-mode)

(bind "ctrl-j" "files" move-left)
(bind "ctrl-l" "files" move-right)
(bind "ctrl-i" "files" move-up)
(bind "ctrl-k" "files" move-down)
(bind "enter" "files" select-file)
(bind "esc" "files" pop-menu-buffer)
(bind "back" "files" del-and-filter)

(bind "esc" "special" pop-mode)
(bind "p" "special" open-workspace)
(bind "s" "special" syntax-update)
(bind "t" "special" test-status)

(set-mode "insert")
(print-buffer)
