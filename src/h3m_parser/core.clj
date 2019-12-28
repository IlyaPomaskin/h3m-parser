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


(defn parse-h3m [file-path]
  (some-> file-path
          (io/input-stream)
          (GZIPInputStream.)
          (io/input-stream)
          (as-> stream (binary/decode h3m/root stream))))


(defn parse-lod [file-path]
  (some-> file-path
          (io/input-stream)
          (as-> stream (binary/decode lod/root stream))))


(defn parse-def [file-path]
  (some-> file-path
          (io/input-stream)
          (as-> stream (binary/decode def/root stream))))


(defn def->stream
  [lod-def-info ^FileInputStream in]
  (let [{compressed-size :compressed-size
         offset :offset} lod-def-info]
    (.position (.getChannel in) (long offset))
    (if (pos? compressed-size)
      (new InflaterInputStream in (new Inflater) compressed-size)
      in)))


(defn read-lod-def
  [lod-def-info in]
  (let [def-stream (def->stream lod-def-info in)
        def-info (binary/decode def/root def-stream)
        uncompressed-size (:size lod-def-info)]
    (-> def-info
        (assoc
         :name (:name lod-def-info)
         :legacy? (def/legacy? def-info def-stream uncompressed-size)
         :frames-count (get-in def-info [:groups 0 :frame-count]))
        (dissoc :frames :palette))))


(def map-objects
  #{(:sprite def/def-type)
    (:terrain def/def-type)
    (:map def/def-type)
    (:map-hero def/def-type)})


(defn read-frames [in]
  (->> (binary/decode lod/root in)
       :files
       (filter #(map-objects (:type %)))
       (filter #(clojure.string/ends-with? (:name %) ".def"))
       (map #(read-lod-def % in))
       (map #(do
               (println (:name %) (:type %))
               nil))
       (count)))


(clojure.pprint/pprint
 (read-frames
  (new FileInputStream "./resources/H3sprite.lod")))


(defn test-reading-def []
  (clojure.pprint/pprint
   (let [def-info (binary/decode def/root (io/input-stream "./resources/AvWAngl.def"))
         palette (:palette def-info)
         frame-0 (get-in def-info [:groups 0 :frames 1310])
         frame-0-pixels (mapcat
                         (fn [offset]
                           (mapcat
                            :data
                            (get-in frame-0 [:data :lines offset])))
                         (get-in frame-0 [:data :offsets]))]
     {:palette palette
      :full-width (:full-width frame-0)
      :full-height (:full-height frame-0)
      :width (:width frame-0)
      :height (:height frame-0)
      :x (:x frame-0)
      :y (:y frame-0)
      :pixels frame-0-pixels})
  ;  (clojure.java.io/writer "AvWAngl-frame-0.edn")
   ))


(comment
  (test-reading-def))
