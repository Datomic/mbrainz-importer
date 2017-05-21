(load-file "repl.clj")

(def file "/datomic/mbrainz/entities/media.edn")

;; this should fail -- wrong entity type
(<!! (a/transduce
      (comp (halt-when ::anom/category)
            (xform/dot 1000)
            (xform/validate ::ent/label-ent))
      xform/counter
      0
      (aedn/read file)))
(pp/pprint *1)

;; this will take a minute, should succeed
(<!! (a/transduce
      (comp (halt-when ::anom/category)
            (xform/dot 1000)
            (xform/validate ::ent/medium-ent))
      xform/counter
      0
      (aedn/read file)))

;; could parallelize this with e.g. https://github.com/aphyr/tesser
