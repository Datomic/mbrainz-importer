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

(defn filter-batches
  [batch-id-attr remove-batch-ids]
  (remove #(contains? remove-batch-ids (get-in % [:batch-ident batch-id-attr]))))

(defn already-transacted
  "Returns the set of values for batch-id-attr already in the database,
or an anomaly map"
  [conn batch-id-attr]
  (->> (d/q {:query '[:find ?v
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

(defn create-backoff
  "Returns a backoff function that will return increasing backoffs from
start up to end, multiplying by factor"
  [start end factor]
  (let [a (atom (/ start factor))]
    #(swap! a (fn [x] (let [nxt (* x factor)]
                        (min nxt end))))))

(defn busy?
  [resp]
  (or (= ::anom/busy (::anom/category resp))
      (= ::anom/unavailable (::anom/category resp))
      (and (= ::anom/fault (::anom/category resp))
           (do (prn resp) (print "F") (flush) true))
      (#{429 503} (:datomic.client/http-error-status resp))))

(defn retrying
  "Retries f, a channel returning fn, using backoff to retry. Puts result on ch
and closes ch"
  [f backoff ch]
  (go-loop []
   (let [res (<! (f))]
     (if (busy? res)
       (let [msec (backoff)]
         (print "B") (flush)
         (if msec
           (do
             (<! (timeout msec))
             (recur))
           (doto ch (>! res) close!)))
       (doto ch (>! res) close!)))))

(defn transact-batch*
  [conn {:keys [batch-ident data]} timeout]
  (go
   (let [result (<! (d/transact conn {:tx-data (cons batch-ident data) :timeout timeout}))]
     (if (= ::anom/conflict (::anom/category result))
       (do
         (print "C") (flush)
         {:tx-data nil})
       result))))

(defn transact-batch
  "Creates a transducer from tx-data to tx-result"
  [conn timeout]
  (map
   (fn [batch]
     (let [ch (a/chan 1)]
       (retrying
        #(transact-batch* conn batch timeout)
        (create-backoff 100 30000 2)
        ch)
       (<!! ch)))))

(defn load-parallel
  "Loads batches from ch onto conn with parallelism n. Returns
a channel that will get a map with :txes and :datoms counts
or an anomaly. Drains and closes ch if an error is encountered."
  ([n conn tx-timeout ch]
     (load-parallel n conn tx-timeout (a/chan n) ch))
  ([n conn tx-timeout tx-result-ch ch]
     (a/pipeline-blocking
      n
      tx-result-ch
      (transact-batch conn tx-timeout)
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






