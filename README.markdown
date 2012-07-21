# tormaroe.syntaxer

Source code to HTML formatter and syntax highlighter.

Status: Alpha! Seems to be working for ruby source code, but I know there are some holes.

There exists many libraries and tools like this, but I wanted to make my own. 

## Usage

If you have leiningen, you may run:

  lein run ruby some_ruby_file.rb

HTML will be printed to standard out.

## Roadmap

I intend to support all the languages I use regularly, which includes Ruby, C#, Clojure, JavaScript, and Erlang.

I intent to set up a simple Noir frontend on Heroku to showcase the library.

I intend to split the various language definitions out into separate files. I'll also experiment with some kind of DSL for specifying the formating rules.

## License

Copyright (C) 2012 Torbjørn Marø

Distributed under the Eclipse Public License, the same as Clojure.
