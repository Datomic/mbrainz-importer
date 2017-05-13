;; Copyright (c) Cognitect, Inc.
;; All rights reserved.

(ns cognitect.xform.batch
  (:require
   [clojure.core.async :as a :refer (<! <!! >! >!! close! go go-loop thread timeout)]
   [clojure.spec :as s]
   [clojure.string :as str]
   [cognitect.xform.async :refer (drain)]))

(set! *warn-on-reflection* true)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; core.async helpers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; for documentation only, no validation
(s/def ::spec any?)
(s/def ::readable any?)
(s/def ::writeable any?)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Datomic semantic helpers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(s/fdef reverse?
        :args (s/cat :attr keyword?)
        :ret boolean?)
(defn reverse?
  "Is the attribute name a reverse attribute lookup?"
  [attr]
  (str/starts-with? (name attr) "_"))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Batch helpers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn tx-data->batches
  "Returns a transducer that batches data, returning a ::batch
identified by its tx-attr value"
  [batch-size tx-attr prefix]
  (let [counter (atom 0)
        next-id (fn [] {:db/id "datomic.tx"
                        tx-attr (str prefix "-" (swap! counter inc))})]
    (comp
     (partition-all batch-size)
     (map (fn [data] {:batch-ident (next-id) :data data})))))

(defn batches->txes
  [batch-id-attr remove-batch-ids]
  (comp
   (remove #(contains? remove-batch-ids (get-in % [:batch-ident batch-id-attr])))
   (map (fn [{:keys [batch-ident data]}]
          (cons batch-ident data)))))

(defn already-transacted
  "Returns the set of values for batch-id-attr already in the database,
or an anomaly map"
  [conn batch-id-attr]
  (->> (client/q conn
                 {:query '[:find ?v
                           :in $ ?batch-id-attr
                           :where [_ ?batch-id-attr ?v]]
                  :limit -1
                  :args [(client/db conn) batch-id-attr]})
       (a/transduce
        (comp
         (halt-when client/error?)
         (map #(map first %)))
        into
        #{})))

(defn load-serial
  "Loads transaction data from ch serially onto conn. Returns
a channel that will get a map with :txes and :datoms counts
or an anomaly. Drains and closes ch if an error is encountered."
  [conn ch]
  (a/transduce
   (comp
    (map #(<!! (client/transact conn {:tx-data %})))
    (halt-when client/error? (fn [result bad-input]
                               (drain ch)
                               (assoc bad-input ::completed result))))
   (completing (fn [m {:keys [tx-data]}]
                 (-> m (update :txes inc) (update :datoms + (count tx-data)))))
   {:txes 0 :datoms 0}
   ch))

(defn load-parallel
  "Loads transaction data from ch onto conn with parallelism. Returns
a channel that will get a map with :txes and :datoms counts
or an anomaly. Drains and closes ch if an error is encountered."
  ([n conn ch]
     (load-parallel n conn (a/chan 1000) ch))
  ([n conn tx-result-ch ch]
     (a/pipeline-async
      n
      tx-result-ch
      (fn [tx ach]
        (let [txch (client/transact conn {:tx-data tx})]
          (go (>! ach (<! txch)) (close! ach))))
      ch)
     (a/transduce
      (halt-when client/error? (fn [result bad-input]
                                 (drain tx-result-ch)
                                 (drain ch)
                                 (assoc bad-input :completed result)))
      (completing (fn [m {:keys [tx-data]}]
                    (-> m (update :txes inc) (update :datoms + (count tx-data)))))
      {:txes 0 :datoms 0}
      tx-result-ch)))






