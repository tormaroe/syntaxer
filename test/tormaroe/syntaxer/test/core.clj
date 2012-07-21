(ns tormaroe.syntaxer.test.core
  (:use [tormaroe.syntaxer.core])
  (:use [hiccup.core :only [html]])
  (:use [midje.sweet]))

(fact 'canary-test        true          => true)
(fact 'canary-test-hiccup (html [:div]) => "<div></div>")

(fact "Get tokens from ruby source" 
  (let [get-first-ruby-token (partial get-first-token 
                                      (:ruby tokenizers))]
  
    (get-first-ruby-token "")                => nil

    (get-first-ruby-token " ")               => [" " :whitespace]
    (get-first-ruby-token "  ")              => ["  " :whitespace]
    (get-first-ruby-token " \t")             => [" \t" :whitespace]
    (get-first-ruby-token "\n")              => ["\n" :whitespace]

    (get-first-ruby-token "||=")             => ["||=" :syntax]

    (get-first-ruby-token "123")             => ["123" :number]
    (get-first-ruby-token "1.05")            => ["1.05" :number]
    (get-first-ruby-token "foo")             => ["foo" :word]
    (get-first-ruby-token "'foo'")           => ["'foo'" :string]
    (get-first-ruby-token "\"foo\"")         => ["\"foo\"" :string]
    (get-first-ruby-token "\"foo\\\"bar\"")  => ["\"foo\\\"bar\"" :string]
    (get-first-ruby-token "'foo\\'bar'")     => ["'foo\\'bar'" :string]
    (get-first-ruby-token "\"foo\",\"bar\"") => ["\"foo\"" :string]
    
    (get-first-ruby-token "foo, bar")        => ["foo" :word]
    (get-first-ruby-token "foo-bar")         => ["foo" :word]
    (get-first-ruby-token "foo(bar)")        => ["foo" :word]
    
    ))

(fact
  (tokenize :ruby "foo")        => [["foo" :word]]
  (tokenize :ruby "foo_1")      => [["foo_1" :word]]
  
  (tokenize :ruby "foo bar")    => [["foo" :word]
                                    [" " :whitespace]
                                    ["bar" :word]]

  (tokenize :ruby "Foo::Bar")   => [["Foo" :constant]
                                    ["::" :syntax]
                                    ["Bar" :constant]]
  
  (tokenize :ruby "foo(1,2,3)") =>  [["foo", :word] 
                                     ["(", :syntax] 
                                     ["1", :number] 
                                     [",", :syntax] 
                                     ["2", :number] 
                                     [",", :syntax] 
                                     ["3", :number] 
                                     [")", :syntax]] 

  (tokenize :ruby "1+2==")      => [["1" :number]
                                    ["+" :operator]
                                    ["2" :number]
                                    ["==" :operator]]

  (tokenize :ruby ":foo =>")    => [[":foo" :keyword]
                                    [" " :whitespace]
                                    ["=>" :syntax]]

  (tokenize :ruby "[1,2]")      => [["[" :syntax]
                                    ["1" :number]
                                    ["," :syntax]
                                    ["2" :number]
                                    ["]" :syntax]]

  (tokenize :ruby "{1=>2}")     => [["{" :syntax]
                                    ["1" :number]
                                    ["=>" :syntax]
                                    ["2" :number]
                                    ["}" :syntax]]

  (tokenize :ruby "$foo=1 # this is a comment
                   exit")
            =>
            [["$foo"                   :word]
             ["="                      :syntax]
             ["1"                      :number]
             [" "                      :whitespace]
             ["# this is a comment"    :comment]
             ["\n                   "  :whitespace]
             ["exit"                   :word]]

  (tokenize :ruby "class Foo
                    def initialize
                      @bar = 1
                    end
                  end") 
            =>  
          [["class", :word] 
           [" ", :whitespace] 
           ["Foo", :constant] 
           ["\n                    ", :whitespace] 
           ["def", :word] 
           [" ", :whitespace] 
           ["initialize", :word] 
           ["\n                      ", :whitespace] 
           ["@bar", :word] 
           [" ", :whitespace] 
           ["=", :syntax] 
           [" ", :whitespace] 
           ["1", :number] 
           ["\n                    ", :whitespace] 
           ["end", :word] 
           ["\n                  ", :whitespace] 
           ["end", :word]])

(fact 
  (tokenize :ruby "foo=\"bar\"
zot=\"google\"")
            =>
            [["foo"      :word]
             ["="        :syntax]
             ["\"bar\""  :string]
             ["\n"       :whitespace]
             ["zot"      :word]
             ["="        :syntax]
             ["\"google\"" :string]]
  (tokenize :ruby "foo='bar'
zot='google'")
            =>
            [["foo"      :word]
             ["="        :syntax]
             ["'bar'"    :string]
             ["\n"       :whitespace]
             ["zot"      :word]
             ["="        :syntax]
             ["'google'" :string]])

(fact 
  (html-encode "1 <> 3") => "1 &lt;&gt; 3"
  (html-encode "\"")     => "&#34;"
  (html-encode "'")      => "&#39;"
  (html-encode "\\")     => "&#92;")

(fact 
  (let [ruby->html (partial token->html :ruby)]

    (ruby->html ["exit" :word]) => "exit"
    
    (ruby->html ["# test" :comment]) 
        => [:span {:style "color:grey;"} "# test"]

    (ruby->html ["and" :word]) 
        => [:span {:style "color:blue;"} "and"]
    (ruby->html ["if" :word]) 
        => [:span {:style "color:blue;"} "if"]
  ))

(fact
  (source->html :ruby "exit # test") =>
"<pre>
exit <span style=\"color:grey;\"># test</span>
</pre>")

(fact
  (source->html :ruby "require 'foo'
given = 'James'
surname = 'Bond'

puts \"My name is #{surname}, #{given} #{surname}.\"

# => My name is Bond, James Bond.")
    =>
  "<pre>
<span style=\"color:purple;\">require</span> <span style=\"color:green;\">&#39;foo&#39;</span>
given = <span style=\"color:green;\">&#39;James&#39;</span>
surname = <span style=\"color:green;\">&#39;Bond&#39;</span>

puts <span style=\"color:green;\">&#34;My name is #{surname}, #{given} #{surname}.&#34;</span>

<span style=\"color:grey;\"># =&gt; My name is Bond, James Bond.</span>
</pre>")
