(set-env!
 :resource-paths #{"src"}
 :source-paths #{"test"}
 :dependencies
 '[[com.cognitect/transcriptor "0.1.4"]
   #_[com.datomic/client-impl-pro "0.8.5" :exclusions [com.fasterxml.jackson.core/jackson-core]]
   #_[com.datomic/client-impl-shared "0.8.6"]
   [com.datomic/client "0.8.11"]
   [com.datomic/client-impl-cloud "0.8.22"]
   [org.clojure/core.async "0.3.442"]
   [org.clojure/test.check "0.9.0"]
   [org.clojure/tools.namespace "0.2.11"]])

(deftask vanilla-repl
  "Vanilla Clojure REPL."
  []
  (clojure.main/repl)
  (fn [next-task]
    (fn [fileset]
      (next-task fileset))))

(require '[clojure.spec.test.alpha :as stest]
         '[cognitect.transcriptor :as xr]
         '[clojure.tools.namespace.find :as find]
         '[clojure.java.io :as io])

(deftask run-examples
  "Runs transcriptor examples in dir, which need not be on classpath."
  [d dir DIR str "directory to test"
   i instrument RE regex "regex of syms to instrument"]
  (with-pass-thru
    [_]
    (assert (string? dir))
    (require 'cognitect.transcriptor)
    (let [test-namespaces (find/find-namespaces-in-dir (io/file '~dir))]
      (doseq [ns test-namespaces]
        (println "Requiring" ns)
        (require ns))
      (when '~instrument
        (println "Instrumenting"
                 (stest/instrument (->> (stest/instrumentable-syms)
                                        (filter #(re-matches '~instrument  (str %)))))))
      (doseq [f (cognitect.transcriptor/repl-files '~dir)]
        (cognitect.transcriptor/run f)))))


(comment

  )

