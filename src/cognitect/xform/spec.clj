;; Copyright (c) Cognitect, Inc.
;; All rights reserved.

(ns cognitect.xform.spec
  (:require [clojure.spec :as s]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; spec helpers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(s/fdef conform!
        :args (s/cat :spec ::spec :x ::any :msg (s/? string?))
        :ret any?)
(defn conform!
  "Like s/conform, but throws an error with s/explain-data on failure."
  ([spec x]
     (conform! spec x ""))
  ([spec x msg]
     (let [conformed (s/conform spec x)]
       (if (= ::s/invalid conformed)
         (throw (ex-info (str "Failed to conform " spec ", see ex-data")
                         {:data (s/explain-data spec x)
                          :value x}))
         conformed))))

