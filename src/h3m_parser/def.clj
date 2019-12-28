(ns h3m-parser.def
  (:require [org.clojars.smee.binary.core :as b]
            [h3m-parser.codec :as codec])
  (:import java.io.RandomAccessFile))


(defn frame-without-compression [size]
  (b/repeated :ubyte :length size))


(defn frame-compressed-line-1 [offset]
  (codec/cond-codec
   ; TODO check offsets
   ; :assert (codec/offset-assert offset)
   :code :ubyte
   :length-byte :ubyte
   :length #(codec/constant (+ (:length-byte %) 1))
   :data #(b/repeated
           (if (= 0xFF (:code %))
             :ubyte
             (codec/constant (:code %)))
           :length (:length %))))


(defn frame-compressed-line-2-3 [offset]
  (codec/cond-codec
  ; TODO check offsets
  ; :assert (codec/offset-assert offset)
   :byte :ubyte
   :code #(codec/constant (bit-shift-right (:byte %) 5))
   :length #(codec/constant (+ (bit-and (:byte %) 0x1f) 1))
   :data #(b/repeated
           (if (= 0x7 (:code %))
             :ubyte
             (codec/constant (:code %)))
           :length (:length %))))


(defn frame-compressed [line-codec-fn offsets-length line-length]
  (codec/cond-codec
   :frame-content-start codec/reader-position
   :offsets (b/repeated :short-le :length offsets-length)
   :lines (fn [ctx]
            (apply
             codec/cond-codec
             (->> (:offsets ctx)
                  (distinct)
                  (sort)
                  (mapcat
                   #(let [relative-offset %
                          absolute-offset (+ relative-offset (:frame-content-start ctx))
                          codec (line-codec-fn absolute-offset)]
                      [relative-offset (codec/read-lines codec :length line-length)])))))))


; TODO legacy detection
(defn frame [offset]
  (codec/cond-codec
   :assert (codec/offset-assert offset "frame")
   :size :int-le
   :compression :int-le
   :full-width :int-le
   :full-height :int-le
   :width :int-le
   :height :int-le
   :x :int-le
   :y :int-le
   :data #(case (int (:compression %))
            0 (frame-without-compression (:size %))
            1 (frame-compressed
               frame-compressed-line-1
               (:height %)
               (:width %))
            2 (frame-compressed
               frame-compressed-line-2-3
               (:height %)
               (:width %))
            3 (frame-compressed
               frame-compressed-line-2-3
               (/ (* (:height %) (:width %)) 32)
               32)
            nil)))


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