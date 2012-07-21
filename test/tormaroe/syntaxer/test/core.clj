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

    (get-first-ruby-token " ")               => {:token " " :type :whitespace}
    (get-first-ruby-token "  ")              => {:token "  " :type :whitespace}
    (get-first-ruby-token " \t")             => {:token " \t" :type :whitespace}
    (get-first-ruby-token "\n")              => {:token "\n" :type :whitespace}

    (get-first-ruby-token "||=")              => {:token "||=" :type :syntax}

    (get-first-ruby-token "123")             => {:token "123" :type :number}
    (get-first-ruby-token "1.05")            => {:token "1.05" :type :number}
    (get-first-ruby-token "foo")             => {:token "foo" :type :word}
    (get-first-ruby-token "'foo'")           => {:token "'foo'" :type :string}
    (get-first-ruby-token "\"foo\"")         => {:token "\"foo\"" :type :string}
    (get-first-ruby-token "\"foo\\\"bar\"")  => {:token "\"foo\\\"bar\"" :type :string}
    (get-first-ruby-token "'foo\\'bar'")     => {:token "'foo\\'bar'" :type :string}
    (get-first-ruby-token "\"foo\",\"bar\"") => {:token "\"foo\"" :type :string}
    
    (get-first-ruby-token "foo, bar")        => {:token "foo" :type :word}
    (get-first-ruby-token "foo-bar")         => {:token "foo" :type :word}
    (get-first-ruby-token "foo(bar)")        => {:token "foo" :type :word}
    
    ))

(fact
  (tokenize :ruby "foo") => [{:token "foo" :type :word}]
  (tokenize :ruby "foo_1") => [{:token "foo_1" :type :word}]
  
  (tokenize :ruby "foo bar") => [{:token "foo" :type :word}
                                 {:token " " :type :whitespace}
                                 {:token "bar" :type :word}]

  (tokenize :ruby "Foo::Bar") => [{:token "Foo" :type :constant}
                                  {:token "::" :type :syntax}
                                  {:token "Bar" :type :constant}]
  
  (tokenize :ruby "foo(1,2,3)") =>  [{:token "foo", :type :word} 
                                     {:token "(", :type :syntax} 
                                     {:token "1", :type :number} 
                                     {:token ",", :type :syntax} 
                                     {:token "2", :type :number} 
                                     {:token ",", :type :syntax} 
                                     {:token "3", :type :number} 
                                     {:token ")", :type :syntax}] 

  (tokenize :ruby "1+2==") => [{:token "1" :type :number}
                               {:token "+" :type :operator}
                               {:token "2" :type :number}
                               {:token "==" :type :operator}]

  (tokenize :ruby ":foo =>") => [{:token ":foo" :type :keyword}
                                 {:token " " :type :whitespace}
                                 {:token "=>" :type :syntax}]

  (tokenize :ruby "[1,2]") => [{:token "[" :type :syntax}
                               {:token "1" :type :number}
                               {:token "," :type :syntax}
                               {:token "2" :type :number}
                               {:token "]" :type :syntax}]

  (tokenize :ruby "{1=>2}") => [{:token "{" :type :syntax}
                               {:token "1" :type :number}
                               {:token "=>" :type :syntax}
                               {:token "2" :type :number}
                               {:token "}" :type :syntax}]

  (tokenize :ruby "$foo=1 # this is a comment
                   exit")
            =>
            [{:token "$foo"                   :type :word}
             {:token "="                      :type :syntax}
             {:token "1"                     :type :number}
             {:token " "                      :type :whitespace}
             {:token "# this is a comment"    :type :comment}
             {:token "\n                   "  :type :whitespace}
             {:token "exit"                   :type :word}]

  (tokenize :ruby "class Foo
                    def initialize
                      @bar = 1
                    end
                  end") 
            =>  
          [{:token "class", :type :word} 
           {:token " ", :type :whitespace} 
           {:token "Foo", :type :constant} 
           {:token "\n                    ", :type :whitespace} 
           {:token "def", :type :word} 
           {:token " ", :type :whitespace} 
           {:token "initialize", :type :word} 
           {:token "\n                      ", :type :whitespace} 
           {:token "@bar", :type :word} 
           {:token " ", :type :whitespace} 
           {:token "=", :type :syntax} 
           {:token " ", :type :whitespace} 
           {:token "1", :type :number} 
           {:token "\n                    ", :type :whitespace} 
           {:token "end", :type :word} 
           {:token "\n                  ", :type :whitespace} 
           {:token "end", :type :word}])

(fact 
  (tokenize :ruby "foo=\"bar\"
zot=\"google\"")
            =>
            [{:token "foo"      :type :word}
             {:token "="        :type :syntax}
             {:token "\"bar\""  :type :string}
             {:token "\n"       :type :whitespace}
             {:token "zot"      :type :word}
             {:token "="        :type :syntax}
             {:token "\"google\"" :type :string}]
  (tokenize :ruby "foo='bar'
zot='google'")
            =>
            [{:token "foo"      :type :word}
             {:token "="        :type :syntax}
             {:token "'bar'"    :type :string}
             {:token "\n"       :type :whitespace}
             {:token "zot"      :type :word}
             {:token "="        :type :syntax}
             {:token "'google'" :type :string}])

(fact 
  (html-encode "1 <> 3") => "1 &lt;&gt; 3"
  (html-encode "\"")     => "&#34;"
  (html-encode "'")     => "&#39;"
  (html-encode "\\")     => "&#92;")

(fact 
  (let [ruby->html (partial token->html :ruby)]

    (ruby->html {:token "exit" :type :word}) => "exit"
    
    (ruby->html {:token "# test" :type :comment}) 
        => [:span {:style "color:grey;"} "# test"]

    (ruby->html {:token "and" :type :word}) 
        => [:span {:style "color:blue;"} "and"]
    (ruby->html {:token "if" :type :word}) 
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
