;; Copyright (c) Cognitect, Inc.
;; All rights reserved.

(ns cognitect.xform.async
  (:require
   [clojure.core.async :as a :refer (<! >! >!! close! go go-loop thread timeout)]
   [clojure.spec :as s]))

;; for documentation only
(s/def ::channel any?)

(s/fdef drain
        :args (s/cat :ch ::channel)
        :ret nil?)
(defn drain
  "Close ch and discard all items on it. Returns nil."
  [ch]
  (close! ch)
  (go-loop []
    (when (<! ch) (recur)))
  nil)

(defn threaded-onto
  "Like onto-chan but uses a real thread."
  ([ch coll] (threaded-onto ch coll true))
  ([ch coll close?]
     (let [vs (seq coll)]
       (thread
        (loop [vs vs]
          (if (and vs (>!! ch (first vs)))
            (recur (next vs))
            (when close?
              (close! ch))))))))

(s/fdef top
        :args (s/cat :ch ::channel :n nat-int?)
        :ret ::channel)
(defn top
  "Returns a channel with the first n items on ch, closing ch."
  [ch n]
  (go
   (<! (timeout 1000))
   (close! ch))
  (go
   (let [result (<! (a/into [] (a/take n ch)))]
     result)))
