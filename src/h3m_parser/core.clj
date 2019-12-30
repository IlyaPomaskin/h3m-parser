(ns h3m-parser.core
  (:require
   [clojure.java.io :as io]
   [org.clojars.smee.binary.core :as binary]
   [h3m-parser.h3m :as h3m]
   [h3m-parser.lod :as lod]
   [h3m-parser.def :as def])
  (:import
   [java.io FileInputStream]
   [java.util.zip Inflater InflaterInputStream GZIPInputStream]))


(defn parse-h3m [in]
  (binary/decode h3m/root (new GZIPInputStream in)))


(defn parse-lod [in]
  (binary/decode lod/root in))


(defn parse-def [in]
  (binary/decode def/root in))


(defn get-def-stream-from-lod
  [lod-def-info ^FileInputStream in]
  (let [{compressed-size :compressed-size
         offset :offset} lod-def-info]
    (.position (.getChannel in) (long offset))
    (if (pos? compressed-size)
      (new InflaterInputStream in (new Inflater) compressed-size)
      in)))


(defn parse-def-from-lod
  [lod-def-info in]
  (let [{uncompressed-size :size
         name :name} lod-def-info
        def-stream (get-def-stream-from-lod lod-def-info in)
        def-info (parse-def def-stream)
        legacy? false ; (def/legacy? def-info def-stream uncompressed-size)
        ]
    (assoc
     def-info
     :legacy? legacy?
     :name name)))


(def map-objects
  #{(:terrain def/def-type)
    (:map def/def-type)})


(defn lines->bytes [frame]
  (let [{lines :lines
         offsets :offsets} frame]
    (mapcat
      (fn [offset] (mapcat :data (get lines offset)))
      offsets)))


(defn map-frame [frame]
  (-> frame
      (select-keys [:full-width
                    :full-height
                    :width
                    :height
                    :x
                    :y
                    :data])
      (update :data lines->bytes)))


(defn map-def [def-info]
  (-> def-info
      (select-keys [:name
                    :full-width
                    :full-height
                    :palette
                    :frames])
      (update :frames #(map map-frame %))
      (assoc :order (get-in def-info [:groups 0 :offsets]))))


(defn lod->texture-atlas [^FileInputStream lod-in]
  (let [lod-info (binary/decode lod/root lod-in)]
    (->> (:files lod-info)
         (filter #(map-objects (:type %)))
         (filter #(clojure.string/ends-with? (:name %) "AvWAngl.def"))
         (filter #(clojure.string/ends-with? (:name %) ".def"))
         (map #(parse-def-from-lod % lod-in))
         (filter #(false? (:legacy? %)))
         (map map-def)
         (take 1)
        ;  (map #(do
        ;          (println (:name %) (:group-count %) (:type %))
        ;          nil))
        ;  (count)
         )))


(lod->texture-atlas
  (new
   FileInputStream
   "./resources/H3sprite.lod"))
