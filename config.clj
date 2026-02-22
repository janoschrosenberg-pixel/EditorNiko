(ns user.config
  (:import [view EditorFrame]))

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

(defn stack-size [] (.getBufferMenuStackLength api))

(defn test-status [] (set-status-text (stack-size)))

(defn insert-tab [] (insert-text "  "))


(mode "insert" {:insert-like true})
(mode "files" {})
(mode "special" {})

(bind "ctrl-j" "insert" move-left)
(bind "ctrl-l" "insert" move-right)
(bind "ctrl-i" "insert" move-up)
(bind "ctrl-k" "insert" move-down)
(bind "enter" "insert" enter)
(bind "ctrl-p" "insert" show-files)
(bind "ctrl-E" "insert" (fn [] (set-mode "special")))
(bind "back" "insert" back-space)
(bind "tab" "insert" insert-tab)

(bind "ctrl-j" "files" move-left)
(bind "ctrl-l" "files" move-right)
(bind "ctrl-i" "files" move-up)
(bind "ctrl-k" "files" move-down)
(bind "enter" "files" select-file)
(bind "esc" "files" pop-menu-buffer)

(bind "esc" "special" pop-mode)
(bind "p" "special" open-workspace)
(bind "s" "special" syntax-update)
(bind "t" "special" test-status)

(set-mode "insert")
(print-buffer)