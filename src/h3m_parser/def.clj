(ns h3m-parser.def
  (:require [org.clojars.smee.binary.core :as b]
            [h3m-parser.codec :as codec])
  (:import java.io.RandomAccessFile
           org.clojars.smee.binary.core.BinaryIO))


(defn read-while [codec initial-value]
  (reify BinaryIO
    (read-data [_ big-in little-in]
      (loop [value initial-value
             result []]
        (let [data (b/read-data codec big-in little-in)
              next-value (- value (:length data))
              next-result (conj result data)]
          (if (pos? next-value)
            (recur next-value next-result)
            next-result))))
    (write-data [_ big-out little-out value]
      nil)))


(defn frame-c-3-line [offset]
  (codec/cond-codec
  ; TODO check offsets
  ;  :assert (codec/offset-assert offset)
   :byte :ubyte
   :code #(codec/constant (bit-shift-right (:byte %) 5))
   :length #(codec/constant (+ (bit-and (:byte %) 0x1f) 1))
   :data #(b/repeated
           (if (= 0x7 (:code %))
             :ubyte
             (codec/constant (:code %)))
           :length (:length %))))


(defn frame-c-3 [height width]
  (codec/cond-codec
   :frame-content-start codec/reader-position
   :offsets (b/repeated :short-le :length (/ (* height width) 32))
   :lines (fn [ctx]
            (apply
             codec/cond-codec
             (->> (:offsets ctx)
                  (distinct)
                  (sort)
                  (mapcat
                   #(vector
                     %
                     (read-while
                      (frame-c-3-line (+ % (:frame-content-start ctx)))
                      32))))))))


(defn frame [offset]
  (codec/cond-codec
   :assert (codec/offset-assert offset)
   :size :int-le
   :compression :int-le
   :full-width :int-le
   :full-height :int-le
   :width :int-le
   :height :int-le
   :x :int-le
   :y :int-le
   :data #(frame-c-3 (:width %) (:height %))))


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
              :length (:frame-count %))
   :frame-start codec/reader-position
   :frames (fn [ctx]
             (apply
              codec/cond-codec
              (->> (:offsets ctx)
                   (distinct)
                   (sort)
                   (mapcat #(vector % (frame %))))))))


(def root
  (codec/cond-codec
   :type :int-le
   :full-width :int-le
   :full-height :int-le
   :group-count :int-le
   :palette (b/repeated [:ubyte :ubyte :ubyte] :length 256)
   :groups #(b/repeated group :length (:group-count %))
   :end codec/reader-position))


(defn legacy? [^RandomAccessFile raf offsets]
  (boolean
   (some
    (fn [offset]
      (let [_ (.seek raf offset)
            size (+ (Integer/reverseBytes (.readInt raf)) 32)]
        (> (+ size offset) (.length raf))))
    offsets)))