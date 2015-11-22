(ns nessie-sightings.process
  (:require [clojure.core.async :as a]))

(defn start-process [{:keys [input output errors]} transform]
  (future
    (let [continue (atom true)]
     (while @continue
       (if-let [in (a/<!! input)]
         (try
           (let [result (transform in)]
             (a/put! output result))
           (catch Exception e
             (a/put! errors {:input in :exception e})))
         (reset! continue false))))))
