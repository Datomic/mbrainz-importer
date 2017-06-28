;; Copyright (c) Cognitect, Inc.
;; All rights reserved.

(ns cognitect.xform.batch
  (:require
   [clojure.core.async :as a :refer (<! <!! >! >!! close! go go-loop thread timeout)]
   [clojure.spec.alpha :as s]
   [clojure.string :as str]
   [cognitect.anomalies :as anom]
   [cognitect.xform.async :refer (drain)]
   [datomic.client.api.async.alpha :as d]))

(set! *warn-on-reflection* true)

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
  (->> (d/q conn
                 {:query '[:find ?v
                           :in $ ?batch-id-attr
                           :where [_ ?batch-id-attr ?v]]
                  :limit -1
                  :args [(d/db conn) batch-id-attr]})
       (a/transduce
        (comp
         (halt-when ::anom/category)
         (map #(map first %)))
        into
        #{})))

(defn load-serial
  "Loads transaction data from ch serially onto conn. Returns
a channel that will get a map with :txes and :datoms counts
or an anomaly. Drains and closes ch if an error is encountered."
  [conn tx-timeout ch]
  (a/transduce
   (comp
    (map #(<!! (d/transact conn {:tx-data % :timeout tx-timeout})))
    (halt-when ::anom/category (fn [result bad-input]
                               (drain ch)
                               (assoc bad-input ::completed result))))
   (completing (fn [m {:keys [tx-data]}]
                 (-> m (update :txes inc) (update :datoms + (count tx-data)))))
   {:txes 0 :datoms 0}
   ch))


(defn create-backoff
  "Returns a backoff function that will return increasing backoffs from
start up to max by multiplicative factor, then nil."
  [start max factor]
  (let [a (atom (/ start factor))]
    #(let [backoff (long (swap! a * factor))]
       (when (<= backoff max)
         backoff))))

(defn busy?
  [resp]
  (or (::anom/busy (::anom/category resp))
      (#{429 503} (:datomic.client/http-error-status resp))))

(defn retrying
  "Retries f, a channel returning fn, using backoff to retry. Puts result on ch
and closes ch"
  [f backoff ch]
  (go-loop []
   (let [res (<! (f))]
     (if (busy? res)
       (let [msec (backoff)]
         (println {:backoff msec})
         (if msec
           (do
             (<! (timeout msec))
             (recur))
           (doto ch (>! res) close!)))
       (doto ch (>! res) close!)))))

(defn transact-xform
  "Creates a transducer from tx-data to tx-result"
  [conn timeout]
  (map
   (fn [tx-data]
     (let [ch (a/chan 1)]
       (retrying
        #(d/transact conn {:tx-data tx-data :timeout timeout})
        (create-backoff 100 timeout 2)
        ch)
       (<!! ch)))))

(defn load-parallel
  "Loads transaction data from ch onto conn with parallelism n. Returns
a channel that will get a map with :txes and :datoms counts
or an anomaly. Drains and closes ch if an error is encountered."
  ([n conn tx-timeout ch]
     (load-parallel n conn tx-timeout (a/chan n) ch))
  ([n conn tx-timeout tx-result-ch ch]
     (a/pipeline-blocking
      n
      tx-result-ch
      (transact-xform conn tx-timeout)
      ch)
     (a/transduce
      (halt-when ::anom/category (fn [result bad-input]
                                 (drain tx-result-ch)
                                 (drain ch)
                                 (assoc bad-input :completed result)))
      (completing (fn [m {:keys [tx-data]}]
                    (-> m (update :txes inc) (update :datoms + (count tx-data)))))
      {:txes 0 :datoms 0}
      tx-result-ch)))






