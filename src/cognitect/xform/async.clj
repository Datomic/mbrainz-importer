;; Copyright (c) Cognitect, Inc.
;; All rights reserved.

(ns cognitect.xform.async
  (:require
   [clojure.core.async :as a :refer (<! >! >!! close! go go-loop thread timeout)]))

(defn drain
  "Close ch and discard all items on it. Returns nil."
  [ch]
  (close! ch)
  (go-loop []
    (when (<! ch) (recur)))
  nil)

(defn threaded-onto
  "Like c.c.a/onto-chan but uses a real thread and returns a map with :forms"
  ([ch coll]
     (threaded-onto ch coll true))
  ([ch coll close?]
     (let [vs (seq coll)]
       (thread
        (loop [vs vs n 0]
          (if (and vs (>!! ch (first vs)))
            (recur (next vs) (inc n))
            (do
              (when close?
                (close! ch))
              {:forms n})))))))


