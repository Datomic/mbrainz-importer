;; Copyright (c) Cognitect, Inc.
;; All rights reserved.

(ns datomic.mbrainz.importer.entities
  (:require [clojure.spec.alpha :as s]))

(s/def ::non-empty-string (and string? #(not-empty %)))

(s/def ::gid uuid?)
(s/def ::name ::non-empty-string)
(s/def ::sortname ::non-empty-string)
(s/def ::country ::non-empty-string)
(s/def ::type ::non-empty-string)
(s/def ::artist_credit ::non-empty-string)
(s/def ::year int?)
(s/def ::month (s/int-in 1 13))
(s/def ::day (s/int-in 1 32))
(s/def ::begin_date_year ::year)
(s/def ::begin_date_month ::month)
(s/def ::begin_date_day ::day)
(s/def ::end_date_year ::year)
(s/def ::end_date_month ::month)
(s/def ::end_date_day ::day)
(s/def ::date_year ::year)
(s/def ::date_month ::month)
(s/def ::date_day ::day)
(s/def ::position pos-int?)
(s/def ::track-count pos-int?)
(s/def ::format ::non-empty-string)
(s/def ::tracknum pos-int?)
(s/def ::length pos-int?)
(s/def ::release uuid?)
(s/def ::artist uuid?)
(s/def ::release_group uuid?)


(s/def ::artist-ent (s/keys :req-un [::gid ::sortname ::name]
                            :opt-un [::type ::gender ::country
                                     ::begin_date_year ::begin_date_month ::begin_date_day
                                     ::end_date_year ::end_date_month ::end_date_day]))
(s/def ::release-ent (s/keys :req-un [::gid ::name ::release_group]
                         :opt-un [::artist_credit ::label ::packaging ::status ::country
                                  ::script ::barcode ::date_year ::date_month ::date_day]))
(s/def ::arelease-ent (s/keys :req-un [::gid ::name ::artist_credit]
                              :opt-un [::type]))
(s/def ::label-ent (s/keys :req-un [::gid ::name ::sort_name]
                           :opt-un [::type ::country ::begin_date_year ::begin_date_month ::begin_date_day
                                    ::end_date_year ::end_date_month ::end_date_day]))
(s/def ::medium-ent (s/keys :req-un [::position ::track_count]
                            :opt-un [::format]))
(s/def ::release-artist-ent (s/keys :req-un [::release ::artist]))
