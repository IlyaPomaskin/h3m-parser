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
  (let [def-buffer (def->stream lod-def-info in)
        def-info (binary/decode def/root def-buffer)
        first-offset (get-in def-info [:groups 0 :offsets 0])]
    {:type (:type def-info)
     :group-count (:group-count def-info)
     :compression (get-in def-info [:groups 0 :frames first-offset :compression])
     :frames-count (get-in def-info [:groups 0 :frame-count])}
    def-info))


(defn read-frames [lod-in]
  (->> (binary/decode lod/root lod-in)
       :files
       (filter #(not= (:type %) 67))
       (filter #(= (:name %) "AB01_.def"))
      ;  (filter #(= (:name %) "AVA0001.def"))
      ;  (take 10)
       (map #(do
               (println (:name %))
               (assoc
                (read-lod-def % lod-in)
                :name (:name %))))
      ;  (filter #(not= (:compression %) 3))
       ))


(clojure.pprint/pprint
 (read-frames
  (new FileInputStream "./resources/H3sprite.lod"))
 (clojure.java.io/writer "AB01_.def.edn"))


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
