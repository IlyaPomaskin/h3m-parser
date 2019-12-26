(ns h3m-parser.def
  (:require [org.clojars.smee.binary.core :as b]
            [h3m-parser.codec :as codec])
  (:import java.io.RandomAccessFile
           org.clojars.smee.binary.core.BinaryIO))


(defn frame-compression-3 [^RandomAccessFile raf start-offset width height]
  (map
    (fn [offset]
      (.seek raf (+ start-offset offset))
      (loop [left 32
             result []]
        (let [control-byte (.readUnsignedByte raf)
              code (bit-shift-right control-byte 5)
              length (+ (bit-and control-byte 0x1f) 1)
              data (if (= 0x7 code)
                     (doall
                      (for [_ (range 0 length)]
                        (.readUnsignedByte raf)))
                     (repeat length code))
              segment {:byte control-byte
                       :code code
                       :length length
                       :data data}
              next-left (- left length)
              next-result (conj result segment)]
          (if (pos? next-left)
            (recur next-left next-result)
            next-result))))
    (doall
     (for [_ (range 0 (/ (* height width) 32))]
       (bit-and 16rFFFF (Short/reverseBytes (.readShort raf)))))))


(defn read-while [codec initial-value offset]
  (reify BinaryIO
    (read-data [_ big-in little-in]
      (let [current-position (b/read-data codec/reader-position big-in little-in)
            skip-length (- offset current-position)]
        (println "cur-pos" current-position "offset" offset "skip" skip-length)
        (when (pos? skip-length)
          (println "need to skip cur-pos:" current-position "should be:" offset)
          (.skipBytes little-in)))
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


(def frame-c-3-line
  (codec/cond-codec
   :line-start codec/reader-position
  ;  :logger (codec/logger "line-start" #(:line-start %))
   :byte :ubyte
   :code #(codec/constant (bit-shift-right (:byte %) 5))
   :length #(codec/constant (+ (bit-and (:byte %) 0x1f) 1))
   :data #(b/repeated
           (if (= 0x7 (:code %))
             :ubyte
             (codec/constant (:code %)))
           :length (:length %))
   :line-end codec/reader-position
  ;  :logger (codec/logger "line-end" #(:line-end %))
  ;  :logger (codec/logger "line")
   ))


(defn frame-c-3 [height width]
  (codec/cond-codec
   :frame-content-start codec/reader-position
  ;  :logger (codec/logger "frame-content-start" #(:frame-content-start %))
   :offsets (b/repeated :short-le :length (/ (* height width) 32))
   :logger (codec/logger "frame lines offsets" #(:offsets %))
   :lines #(->> (:offsets %)
                (sort)
                (mapv
                 (fn [offset]
                   (read-while
                    frame-c-3-line
                    32
                    (+ offset (:frame-content-start %))))))
   :pos codec/reader-position))


(def frame
  (codec/cond-codec
   :frame-start codec/reader-position
   :logger (codec/logger "frame start" #(:frame-start %))
   :logger (codec/logger "frame start" #(:frame-start %))
   :logger (codec/logger "frame start" #(:frame-start %))
   :logger (codec/logger "frame start" #(:frame-start %))
   :logger (codec/logger "frame start" #(:frame-start %))
   :logger (codec/logger "frame start" #(:frame-start %))
   :logger (codec/logger "frame start" #(:frame-start %))
   :logger (codec/logger "frame start" #(:frame-start %))
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
   :logger (codec/logger
            "frames start"
            #(vector (:frame-start %) (:offsets %)))
   :frames #(apply
             codec/cond-codec
             (interleave
              (sort (:offsets %))
              (repeat frame)))))


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