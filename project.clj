(defproject tormaroe.syntaxer "0.1.0-SNAPSHOT"
  :description "Source code to HTML formatter and syntax highlighter."
  :dependencies [[org.clojure/clojure "1.3.0"]
                 [hiccup "0.3.6"]]
  :dev-dependencies [[midje "1.3.2-SNAPSHOT"]
                   [com.stuartsierra/lazytest "1.2.3"]]
  :repositories {"stuart" "http://stuartsierra.com/maven2"}
  :main tormaroe.syntaxer)
