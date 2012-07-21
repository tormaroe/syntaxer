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
  { :ruby [{:type :whitespace :re #"^\s+"}
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
           {:type :word       :re #"^[a-z@$]+[A-Za-z0-9_]*"}]
   }) 

;; TOKENIZE

(defn get-first-token 
  "Given a sequence of tokenizers it finds the first match
  in s and returns the token as a two element list of
  the text that matched and the type of the tokenizer."
  [tokenizers s]
  (loop [[t & tokenizers] tokenizers]
    (if t
      (if-let [match (first (re-seq (:re t) s))]
        [ match (:type t) ]
        (recur tokenizers)))))

(defn tokenize
  "Given sourcecode as input and keyword lang specifying the
  language used, tokenize will produce a sequence of tokens."
  [lang input]
  (letfn [(input-eater [tokenizers input tokens]
            (if-let [t (get-first-token tokenizers input)]
              (recur tokenizers
                     (subs input (count (first t)))
                     (conj tokens t))
              tokens))]
    (input-eater (lang tokenizers) input [])))

;; HTML ENCODE

(defn char->html--force 
  "HTML encodes any character c to format &#n;
  where n is the numeric character code of c."
  [c]
  (str "&#" (int c) ";"))

(defn char->html 
  "HTML encodes <<dangerous>> character. If
  character is not considered dangerous it is
  returned as is."
  [c]
  (condp = c
    \< "&lt;"
    \> "&gt;"
    \\ (char->html--force \\)
    \" (char->html--force \")
    \' (char->html--force \')
    c))

(defn html-encode 
  "HTML encodes an entire string according to
  the rules of char->html"
  [s]
  (apply str (map char->html s)))

(defn token->html 
  "Given a language keyword and a token returns
  a hiccup vector representing it. Styles are retrieved
  from the configuration for the language."
  [lang [text type]]
  (if-let [color (type (lang colors))]
    (if (fn? color) 
      (if-let [dynamic-style (color text)]
        [:span {:style dynamic-style} text]
        text)
      [:span {:style color} text])
    text))

(defn swap-at 
  "Applyes f to the value at index idx of sequence lst,
  returning the list with the value swapped."
  [idx f lst]
  (assoc lst idx (f (nth lst idx))))

(defn source->html 
  "Given a language keyword and sourcecode string s,
  returns HTML formatted according to the configuration
  for the language."
  [lang s]
  (html [:pre 
         \newline
         (->> 
           (tokenize lang s)
           (map (partial swap-at 0 html-encode))
           (map (partial token->html lang))) 
         \newline]))

