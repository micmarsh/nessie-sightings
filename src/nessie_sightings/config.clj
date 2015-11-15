(ns nessie-sightings.config
  (:require [clojure.string :as str]))

(defn kw->string [kw]
  (-> (name kw)
      (str/replace "-" "_")
      (str/upper-case)))

(def val-at
  (memoize
   (fn [kw]
     (cond (keyword? kw) (System/getenv (kw->string kw))
           (string? kw) (System/getenv kw)))))

(def config
  (reify clojure.lang.ILookup
    (valAt [this kw] (val-at kw))))
