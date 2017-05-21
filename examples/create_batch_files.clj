(load-file "repl.clj")

(def importer (imp/create-importer "/datomic/mbrainz"))

;; entity types
imp/import-order

(doseq [type imp/import-order]
  (println "Creating batch file for " type)
  (prn (<!! (imp/create-batch-file importer 1000 type))))





