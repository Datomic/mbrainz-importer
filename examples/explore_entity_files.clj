(load-file "repl.clj")

(def artists "/datomic/mbrainz/entities/artists.edn")

;; what are artists supposed to look like
(-> ::ent/artist-ent s/spec s/form pp/pprint)

;; show me a few artists
(-> artists (top 2))

;; how many artists?
(<!! (a/transduce (halt-when ::anom/category) xform/counter 0 (aedn/read artists)))



