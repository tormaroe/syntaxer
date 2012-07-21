(ns tormaroe.syntaxer.core
  (:use [hiccup.core :only [html]]))

; To complete Ruby lexing, see 
; http://web.njit.edu/all_topics/Prog_Lang_Docs/html/ruby/syntax.html#lexical

(def colors
  { :ruby { :comment "color:grey;"
            :string "color:green;"
            :number "color:magenta;"
            :word #(condp some #{%} 
                      #{"alias" "and" "begin" "break" "case" 
                        "class" "def" "define" "do" "else" "elsif" 
                        "end" "ensure" "false" "for" "if" "in"
                        "module" "next" "nil" "not" "or" "redu"
                        "rescue" "retry" "return" "self" "super"
                        "then" "true" "undef" "unless" "until"
                        "when" "while" "yield"} 
                            "color:blue;"
                      #{"require" "attr_accessor" "attr_reader" 
                        "attr_writer"}
                            "color:purple;"
                     nil) 
           } ; end ruby styles
   })

(def tokenizers
  { :ruby [{:type :whitespace  :re #"^\s+"}
           {:type :string     :re #"^\"(?:[^\"\\]*(?:\\.[^\"\\]*)*)\""}
           {:type :string     :re #"^'(?:[^'\\]*(?:\\.[^'\\]*)*)'"}
           {:type :comment    :re #"^#[^\n]*"} 
           {:type :operator   :re #"^(?:\+|\-|\*|/|==|>|<|>=|<=)"}
           {:type :syntax     :re #"^\:{2}"}
           {:type :syntax     :re #"^=>|^\|\|="}
           {:type :syntax     :re #"^[\(\)\,=\[\]\{\}\|]"}
           {:type :number     :re #"^\d+(?:\.\d+)?"}
           {:type :keyword    :re #"^\:[A-Za-z]+[A-Za-z0-9_]*"} 
           {:type :constant   :re #"^[A-Z]+[A-Za-z0-9_]*"}
           {:type :word       :re #"^[a-z@$]+[A-Za-z0-9_]*"}
           ]}) 

;; TOKENIZE

(defn get-first-token [tokenizers s]
  (loop [[t & tokenizers] tokenizers]
    (if t
      (if-let [match (first (re-seq (:re t) s))]
        { :token match 
          :type (:type t) }
        (recur tokenizers)))))

(defn tokenize
  ([lang-key input] 
    (tokenize (lang-key tokenizers) 
              input
              []))
  ([lang-tokenizers input tokens]
    (if-let [t (get-first-token lang-tokenizers input)]
      (recur lang-tokenizers 
             (subs input (count (:token t)))
             (conj tokens t))
      tokens)))

;; HTML ENCODE

(defn char->html--force [c]
  (str "&#" (int c) ";"))

(defn char->html [c]
  (condp = c
    \< "&lt;"
    \> "&gt;"
    \\ (char->html--force \\)
    \" (char->html--force \")
    \' (char->html--force \')
    c))

(defn html-encode [s]
  (->> 
    (map char->html s)
    (apply str)))

(defn token->html [lang {text :token type :type}]
  (if-let [color (type (lang colors))]
    (if (fn? color) 
      (if-let [dynamic-style (color text)]
        [:span {:style dynamic-style} text]
        text)
      [:span {:style color} text])
    text))

(defn source->html [lang s]
  (html
    [:pre 
      \newline
      (->> 
        (tokenize lang s)
        (map #(assoc % :token (html-encode (:token %))))
        (map (partial token->html lang))) 
      \newline]))
