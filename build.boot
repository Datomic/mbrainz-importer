(System/setProperty "org.eclipse.jetty.util.log.class"
                    "org.eclipse.jetty.util.log.Slf4jLog")
(System/setProperty "org.eclipse.jetty.LEVEL" "DEBUG")

(set-env!
 :resource-paths #{"src" "resources"}
 :source-paths #{"test"}
 :dependencies
 '[[ch.qos.logback/logback-classic "1.0.13"]
   [com.cognitect/transcriptor "0.1.4"]
   [com.datomic/client-impl-pro "0.8.8" :exclusions [com.fasterxml.jackson.core/jackson-core]]
   [com.datomic/client "0.8.16"]
   [com.datomic/client-impl-cloud "0.8.25"]
   [com.datomic/client-impl-shared "0.8.19"]
   [org.clojure/core.async "0.3.442"]
   [org.clojure/test.check "0.9.0"]
   [org.clojure/tools.namespace "0.2.11"]
   [seesaw "1.4.5"]])

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

