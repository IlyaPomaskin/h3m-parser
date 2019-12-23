(ns h3m-parser.def
  (:require [org.clojars.smee.binary.core :as b]
            [h3m-parser.codec :as codec]))


(def group
  (codec/cond-codec
   :type :int-le
   :frame-count :int-le
   :unknown-1 :int-le
   :unknown-2 :int-le
   :names #(b/repeated
            (b/padding (b/c-string "ISO-8859-1") :length 13)
            :length (:frame-count %))
   :offsets #(b/repeated
              :int-le
              :length (:frame-count %))))


(def root
  (codec/cond-codec
   :type :int-le
   :full-width :int-le
   :full-height :int-le
   :group-count :int-le
   :palette (b/repeated [:ubyte :ubyte :ubyte] :length 256)
   :groups #(b/repeated group :length (:group-count %))
   :end codec/reader-position))
