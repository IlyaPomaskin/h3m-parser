(ns h3m-parser.frame
  (:import java.io.RandomAccessFile))


(defn frame-compression-0 [^RandomAccessFile raf size]
  (let [buffer (byte-array size)]
    (.read raf buffer)
    buffer))


(defn frame-compression-1 [^RandomAccessFile raf start-offset width height]
  (map
   (fn [offset]
     (.seek raf (+ start-offset offset))
     (loop [left width
            result []]
       (let [code (.readUnsignedByte raf)
             length (+ (.readUnsignedByte raf) 1)
             data (if (= 0xFF code)
                    (doall
                     (for [_ (range 0 length)]
                       (.readUnsignedByte raf)))
                    (repeat length code))
             segment {:code code
                      :length length
                      :data data}
             next-left (- left length)
             next-result (conj result segment)]
         (if (pos? next-left)
           (recur next-left next-result)
           next-result))))
   (doall
    (for [_ (range 0 height)]
      (Integer/reverseBytes (.readInt raf))))))


(defn frame-compression-2 [^RandomAccessFile raf start-offset width height]
  (let [offsets (doall
                 (for [_ (range 0 height)]
                   (bit-and 16rFFFF (Short/reverseBytes (.readShort raf)))))]
    (.skipBytes raf 2)
    (map
     (fn [offset]
       (.seek raf (+ start-offset offset))
       (loop [left width
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
     offsets)))


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


(defn parse [^RandomAccessFile raf offset legacy?]
  (let [_ (.seek raf offset)
        size (Integer/reverseBytes (.readInt raf))
        compression (Integer/reverseBytes (.readInt raf))
        full-width (Integer/reverseBytes (.readInt raf))
        full-height (Integer/reverseBytes (.readInt raf))
        width (if legacy? full-width (Integer/reverseBytes (.readInt raf)))
        height (if legacy? full-height (Integer/reverseBytes (.readInt raf)))
        x (if legacy? 0 (Integer/reverseBytes (.readInt raf)))
        y (if legacy? 0 (Integer/reverseBytes (.readInt raf)))
        data-offset (.getFilePointer raf)
        segments (case compression
               0 (frame-compression-0 raf size)
               1 (frame-compression-1 raf data-offset width height)
               2 (frame-compression-2 raf data-offset width height)
               3 (frame-compression-3 raf data-offset width height)
               [])]
    (hash-map
     :size size
     :compression compression
     :full-width full-width
     :full-height full-height
     :width width
     :height height
     :x x
     :y y
     :segments (vec segments))))
