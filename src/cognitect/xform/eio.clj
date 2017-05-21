;; Copyright (c) Cognitect, Inc.
;; All rights reserved.

(ns cognitect.xform.eio
  (:import [java.io IOException PushbackReader])
  (:refer-clojure :exclude (read))
  (:require
   [clojure.java.io :as io]
   [clojure.edn :as edn]))

(defn top
  [readable limit]
  (try
   (with-open [rdr (io/reader readable)]
     (let [pbr (PushbackReader. rdr)
           end (Object.)]
       (loop [result [] n 0]
         (let [form (edn/read {:eof end} pbr)]
           (if (= form end)
             result
             (if (< n limit)
               (recur (conj result form) (inc n))
               result))))))))



