(load-file "repl.clj")

(def system (pro/connect {:access-key "mbrainz"
                          :secret "mbrainz"
                          :region "none"
                          :endpoint "localhost:8998"
                          :service "peer-server"}))

(def conn (<!! (client/connect system {:db-name "mbrainz"})))

(def db (client/db conn))

(def unique-attrs (->> (client/q conn {:query '[:find ?attr
                                                :where
                                                [?e :db/unique]
                                                [?e :db/ident ?attr]]
                                       :args [db]})
                       (a/transduce (comp (halt-when ::anom/category)
                                          (map #(map first %)))
                                    into [])
                       <!!))

;; how many of each unique entity type?
(->> unique-attrs
     (mapv #(client/q conn {:query '[:find ?attr (count ?e)
                                     :in $ ?attr
                                     :where [?e ?attr]]
                            :args [db %]}))
     (transduce
      (comp (map <!!) (halt-when ::anom/category))
      into {}))

(defn batch-entity
  [batch-id]
  (str/replace batch-id #"-[^-]*$" ""))

(->> (client/q conn {:query '[:find ?id
                              :where [_ :mbrainz.initial-import/batch-id ?id]]
                     :args [db]})
     (a/transduce
      (comp (halt-when ::anom/category)
            (map #(map (comp batch-entity first) %)))
      into [])
     <!!
     frequencies)
