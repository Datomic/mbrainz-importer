;; Copyright (c) Cognitect, Inc.
;; All rights reserved.

(ns cognitect.xform.async.specs
  (:require
   [cognitect.xform.async :as a]
   [clojure.spec.alpha :as s]))

;; for documentation only
(s/def ::channel any?)

(s/fdef a/drain
        :args (s/cat :ch ::channel)
        :ret nil?)

(s/fdef a/threaded-onto
        :args (s/cat :ch ::channel :coll coll? :close any?)
        :ret nil?)

(s/fdef a/top
        :args (s/cat :ch ::channel :n nat-int?)
        :ret ::channel)

