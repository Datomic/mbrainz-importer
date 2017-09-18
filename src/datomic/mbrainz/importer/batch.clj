;; Copyright (c) Cognitect, Inc.
;; All rights reserved.

(ns datomic.mbrainz.importer.batch
  (:require
   [clojure.core.async :refer (<!!)]
   [clojure.edn :as edn]
   [datomic.mbrainz.importer :as imp]))

(defn -main
  "Create batches for an mbrainz import.

:basedir           directory with entity data
:batch-size        number of entities per batch, suggest 100"
  [basedir batch-size]
  (let [importer (imp/create-importer basedir)
        batch-size (edn/read-string batch-size)]
    (doseq [type imp/import-order]
      (println "batching up " type)
      (println (<!! (imp/create-batch-file importer batch-size type))))))
