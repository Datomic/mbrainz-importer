(set-env!
 :resource-paths #{"src" "resources"}
 :source-paths #{"test"}
 :dependencies
 '[[com.datomic/client-impl-cloud "0.8.2" :exclusions [com.fasterxml.jackson.core/jackson-core]]
   [org.clojure/core.async "0.2.395"]
   [org.clojure/test.check "0.9.0"]])

(deftask vanilla-repl
  "Vanilla Clojure REPL."
  []
  (clojure.main/repl)
  (fn [next-task]
    (fn [fileset]
      (next-task fileset))))

