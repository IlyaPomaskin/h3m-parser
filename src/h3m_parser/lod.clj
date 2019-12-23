(ns h3m-parser.lod
  (:require [org.clojars.smee.binary.core :as b]
            [h3m-parser.codec :as codec]))


(def file
  (codec/cond-codec
   :name (b/padding (b/c-string "ISO-8859-1") :length 16)
   :offset :int-le
   :size :int-le
   :type :int-le
   :compressed-size :int-le))


(def root
  (codec/cond-codec
   :unknown-1 (b/repeated :byte :length 8)
   :files-count :int-le
   :unknown-2 (b/repeated :byte :length 80)
   :files #(b/repeated file :length (:files-count %))))
