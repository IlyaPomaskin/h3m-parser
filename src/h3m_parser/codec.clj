(ns h3m-parser.codec
  (:require [org.clojars.smee.binary.core :as b]
            [h3m-parser.objects :as h3m-objects])
  (:import org.clojars.smee.binary.core.BinaryIO))


(defn reduce-kvs
  "Like `reduce-kv` but takes a flat sequence of kv pairs."
  [rf init kvs]
  (transduce (partition-all 2)
             (completing (fn [acc [k v]] (rf acc k v))) init kvs))


(defn cond-codec
  "Codec for selecting nested codec based on map value"
  [& kvs]
  (reify BinaryIO
    (read-data [_ big-in little-in]
      (reduce-kvs
       (fn [decoded-map param-key param-config]
         (if-some [codec (if (fn? param-config)
                           (param-config decoded-map)
                           param-config)]
           (try
             (some-> codec
                     (b/read-data big-in little-in)
                     (as-> v (assoc decoded-map param-key v)))
             (catch java.io.EOFException e
               (println "param-key" param-key)
               (println "decoded-map" decoded-map)
               (throw e)))
           decoded-map))
       {}
       kvs))

    (write-data [_ big-out little-out value-map]
      big-out)))


(def int-sized-string (b/compile-codec (b/string "ISO-8859-1" :prefix :int-le)))


(def byte->bool (b/compile-codec :byte #(if (true? %1) 1 0) pos?))


(def int->object (b/compile-codec :int-le (constantly 99) #(get h3m-objects/object-by-id %1 :no-obj)))
