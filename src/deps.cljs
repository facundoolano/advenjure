{:foreign-libs
  [{:file "jquery/jquery-3.1.1.js"
    :file-min "jquery/jquery-3.1.1.min.js"
    :provides ["jquery"]}
   {:file "jquery.terminal/jquery.terminal-0.11.10.js"
    :file-min "jquery.terminal/jquery.terminal-0.11.10.min.js"
    :requires ["jquery"]
    :provides ["jquery.terminal"]}
   {:file "jquery.terminal/jquery.mousewheel.js"
    :file-min "jquery.terminal/jquery.mousewheel.min.js"
    :requires ["jquery"]
    :provides ["jquery.mousewheel"]}
   {:file "xregexp/xregexp-all.js"
    :file-min "xregexp/xregexp-all.min.js"
    :provides ["xregexp"]}]
 :externs ["jquery/externs.js" "jquery.terminal/externs.js" "xregexp/externs.js"]}
