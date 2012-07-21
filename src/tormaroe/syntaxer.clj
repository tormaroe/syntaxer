(ns tormaroe.syntaxer
  (:gen-class)
  (:use [tormaroe.syntaxer.core]))

(defn -main [& args]
  (if (and (= 2 (count args)))
    (let [[lang source-file] args]
      (println (source->html (keyword lang)
                             (slurp source-file))))
    (println "
             Arguments: language sourceFile
             Example: ruby ~/temp/foo.rb")))
