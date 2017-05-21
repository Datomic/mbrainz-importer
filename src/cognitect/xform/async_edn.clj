;; Copyright (c) Cognitect, Inc.
;; All rights reserved.

(ns cognitect.xform.async-edn
  (:import [java.io IOException PushbackReader])
  (:refer-clojure :exclude (read))
  (:require
   [clojure.core.async :as a :refer (<!! >! >!! close! go thread)]
   [clojure.java.io :as io]
   [clojure.spec.alpha :as s]
   [clojure.edn :as edn]
   [cognitect.anomalies :as anom]))

(s/def ::channel any?)

(defmacro with-ex-anom
  [& forms]
  `(try
    ~@forms
    (catch Throwable t#
      {::anom/category ::anom/fault
       ::anom/message (.getMessage t#)})))

(s/fdef reader
        :args (s/cat :readable ::readable :ch ::channel :close? any?)
        :ret ::channel)
(defn reader
  "Reads forms from readable, placing them on ch.  Closes the
reader when readable complete or when ch found closed.
Closes ch when input side closes iff close? is truthy.
Returns a channel that will get the number of forms read and
possible anomaly map."
  ([readable ch]
     (reader readable ch true))
  ([readable ch close?]
     (thread
      (with-ex-anom
        (try
         (with-open [rdr (io/reader readable)]
           (let [pbr (PushbackReader. rdr)]
             (loop [n 0]
               (if-let [form (edn/read {:eof nil} pbr)]
                 (if (>!! ch form)
                   (recur (inc n))
                   {::anom/category ::anom/interrupted
                    ::anom/message "Destination channel closed"
                    :forms n})
                 {:forms n}))))
         (finally
          (when close?
            (close! ch))))))))

(defn read
  [readable]
  (let [ch (a/chan 10)]
    (go (let [result (<!! (reader readable ch false))]
          (when (::anom/category result)
            (>! ch result))
          (close! ch)))
    ch))

(s/fdef writer
        :args (s/cat :ch ::channel :writeable ::writeable)
        :ret ::channel)

(defn writer
  "Takes forms from ch, appending them to writeable with
prn. Closes writeable when ch closes. Returns a map with
number of :forms written."
  [ch writeable]
  (thread
   (with-ex-anom
     (with-open [writer (io/writer writeable)]
       (binding [*print-length* nil
                 *print-level* nil
                 *out* writer]
         (loop [n 0]
           (if-let [form (<!! ch)]
             (do
               (prn form)
               (recur (inc n)))
             {:forms n})))))))


