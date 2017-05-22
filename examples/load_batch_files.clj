(load-file "repl.clj")

(def importer (imp/create-importer "/datomic/mbrainz"))

;; entity types
imp/import-order

(def system (pro/connect {:access-key "mbrainz"
                          :secret "mbrainz"
                          :region "none"
                          :endpoint "localhost:8998"
                          :service "peer-server"}))

(def conn (<!! (client/connect system {:db-name "mbrainz"})))

(def import-schema
  [{:db/ident :mbrainz.initial-import/batch-id
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/unique :db.unique/value}])

(<!! (client/transact conn {:tx-data import-schema}))


(doseq [type (take 1 imp/import-order)]
  (println "Loading batch file for " type)
  (pp/pprint (<!! (imp/load-type 8 conn importer type))))





