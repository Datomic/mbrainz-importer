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
  "Like c.c.a/onto-chan but uses a real thread."
  ([ch coll]
     (threaded-onto ch coll true))
  ([ch coll close?]
     (let [vs (seq coll)]
       (thread
        (loop [vs vs]
          (if (and vs (>!! ch (first vs)))
            (recur (next vs))
            (when close?
              (close! ch))))))))

(defn top
  "Returns a channel with the first n items on ch, closing ch after msec."
  ([ch n] (top ch n 1000))
  ([ch n msec]
     (go
      (<! (timeout msec))
      (close! ch))
     (go
      (let [result (<! (a/into [] (a/take n ch)))]
        result))))
