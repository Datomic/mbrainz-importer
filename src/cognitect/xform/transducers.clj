;; Copyright (c) Cognitect, Inc.
;; All rights reserved.

(ns cognitect.xform.transducers
  (:require
   [clojure.spec :as s]
   [cognitect.anomalies :as anom]))

(def ok (halt-when ::anom/category))

(defn validate
  "Returns a pasthru transducer that will validate spec against every
input, halting reduction with an anomaly map if input fails."
  [spec]
  (halt-when
   #(not (s/valid? spec %))
   (fn [result problem]
     (assoc (s/explain-data spec problem)
       ::partial-result result
       ::invalid-step problem))))

(defn counter
  "Reducing function that counts inputs."
  ([] 0)
  ([x] x)
  ([x y] (inc x)))

(defn dot
  "Returns a passthru transducer that prints a dot for progress
tracking."
  ([]
     (map
      (fn [x]
        (print ".")
        (flush)
        x)))
  ([msec]
     (let [a (atom (System/currentTimeMillis))]
       (map
        (fn [x]
          (let [now (System/currentTimeMillis)]
            (when (< (+ @a msec) now)
              (reset! a now)
              (print ".")
              (flush))
            x))))))


