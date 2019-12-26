(ns h3m-parser.core
  (:require [clojure.java.io :as io]
            [org.clojars.smee.binary.core :as binary]
            [h3m-parser.h3m :as h3m]
            [h3m-parser.lod :as lod]
            [h3m-parser.def :as def]
            [h3m-parser.frame :as frame])
  (:import java.io.RandomAccessFile))


(defn parse-h3m [file-path]
  (some-> file-path
          (io/input-stream)
          (java.util.zip.GZIPInputStream. 1)
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


(defn parse-def-frame
  ([^String file-path group-index file-index]
   (parse-def-frame file-path (parse-def file-path) group-index file-index))
  ([^String file-path def-info group-index file-index]
   (let [name (get-in def-info [:groups group-index :names file-index])
         offsets (get-in def-info [:groups group-index :offsets])
         offset (get offsets file-index)
         raf (new RandomAccessFile file-path "r")
         legacy-def? (def/legacy? raf offsets)]
     (when (and (some? name) (some? offset))
       (frame/parse raf offset legacy-def?)))))


(defn read-frames [lod-in ^RandomAccessFile lod-raf def-name]
  (let [lod-info (binary/decode lod/root lod-in)
        lod-def-info (->> lod-info
                          :files
                          (filter #(= (:name %) def-name))
                          (first))
        _ (println lod-def-info)
        def-memory (byte-array (:compressed-size lod-def-info))
        _ (doto lod-raf
            (.seek (:offset lod-def-info))
            (.read def-memory))
        inflater-in (new
                     java.util.zip.InflaterInputStream
                     (io/input-stream def-memory))
        def-info (binary/decode def/root inflater-in)
        offsets (get-in def-info [:groups 0 :offsets])
        def-legacy? false ; (def/legacy? lod-raf offsets)
        ]
    def-info
    ; (reduce
    ;  (fn [acc offset]
    ;    (conj
    ;     acc
    ;     [offset (frame/parse lod-raf offset def-legacy?)]))
    ;  {}
    ;  offsets)
  ;
    ))


(clojure.pprint/pprint
 (let [data (read-frames
             (new
              java.io.FileInputStream
              "./resources/H3sprite.lod")
             (new RandomAccessFile "./resources/H3sprite.lod" "r")
             "AvWAngl.def")]
   (dissoc data :palette :groups)
   data)
 (clojure.java.io/writer "AvWAngl.edn"))


(let [lod-file (io/input-stream "./resources/H3sprite.lod")
      ; lod-file (new java.io.FileInputStream "./resources/H3sprite.lod")
      _ (.mark lod-file (.available lod-file))
      raf (new RandomAccessFile "./resources/H3sprite.lod" "r")
      lod-info (binary/decode lod/root lod-file)
      lod-def-info (doall
                    (->> lod-info
                         :files
                         (filter #(= (:name %) "AvWAngl.def"))
                         (first)))
      _ (println lod-def-info)
      def-memory (byte-array (:compressed-size lod-def-info))
      def-unpacked (byte-array (:size lod-def-info))
      ; _ (doto lod-file
      ;     (.reset)
      ;     (.skip (:offset lod-def-info))
      ;     (.read def-memory))
      ; _ (println (.ready lod-file))
      ; _ (clojure.pprint/pprint
      ;    (clojure.reflect/reflect
      ;     raf))
      _ (.read
         raf
         def-memory
         (:offset lod-def-info)
         (:compressed-size lod-def-info))
      infl-in (new
               java.util.zip.InflaterInputStream
               (io/input-stream def-memory)
              ; (new java.util.zip.Inflater true)
               )
      ; _ (.skip in-inf (:offset lod-def-info))
      ; _ (println "MEMORY" (String. def-memory))
      ; _ (doto (new java.util.zip.Inflater)
      ;     (.setInput def-memory)
      ;     (.inflate def-unpacked)
      ;     (.end))
      ; _ (doto (new
      ;          java.util.zip.InflaterInputStream
      ;          (io/input-stream def-unpacked))
      ;     (.read def-unpacked))
      def-input (io/input-stream def-unpacked)
      def-info (binary/decode def/root infl-in)]
  (println def-info))


(clojure.pprint/pprint
 (clojure.reflect/reflect
  (io/input-stream "./resources/H3sprite.lod")))