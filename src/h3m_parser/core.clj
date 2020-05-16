(ns h3m-parser.core
  (:require
   [org.clojars.smee.binary.core :as binary]
   [h3m-parser.h3m :as h3m]
   [h3m-parser.lod :as lod]
   [h3m-parser.def :as def-file]
   [cheshire.core :as cheshire])
  (:import
   [java.io FileInputStream BufferedInputStream]
   [java.util.zip Inflater InflaterInputStream GZIPInputStream]))


(defn parse-h3m [in]
  (binary/decode h3m/root (new GZIPInputStream in)))


(defn parse-lod [in]
  (binary/decode lod/root in))


(defn parse-def [in]
  (binary/decode def-file/root in))


(defn get-def-stream-from-lod
  [lod-def-info ^FileInputStream in]
  (let [{size            :size
         compressed-size :compressed-size
         offset          :offset} lod-def-info]
    (.position (.getChannel in) (long offset))
    (if (pos? compressed-size)
      (new InflaterInputStream in (new Inflater) compressed-size)
      (new BufferedInputStream in size))))

(def opts
  {:key-fn
   (fn [key]
     (let [[head & rest] (clojure.string/split (name key) #"-")]
       (clojure.string/join
        (concat [head] (mapv clojure.string/capitalize rest)))))
   :pretty true})


(defn get-order
  [object]
  (get-in object [:def :placement-order]))


(defn def-visitable?
  [object]
  (= [0 0] (get-in object [:def :active-cells])))


(defn compare-objects
  [a b]
  (cond
    (not= (get-order a) (get-order b)) (> (get-order a) (get-order b))
    (not= (:y a) (:y b)) (< (:y a) (:y b))
    (and (not= (:object a) :hero) (= (:object b) :hero)) true
    (and (not= (:object b) :hero) (= (:object a) :hero)) false
    (and (not (def-visitable? a)) (def-visitable? b)) false
    (and (not (def-visitable? b)) (def-visitable? a)) true
    (< (:x a) (:x b)) false
    :else false))

(comment
  (let [m (parse-h3m (new FileInputStream "resources/invasion.h3m"))
        m1 (update
            m :objects
            #(->> %1
                  ;;  (filterv (fn [obj] (zero? (:z obj))))
                  (map (fn [obj] (assoc obj :def (nth (:defs m) (:def-index obj)))))
                  ;(sort compare-objects)
                  ;(reverse)
                  ))]
    (.write
     (new java.io.FileOutputStream "invasion.sml")
     (cheshire/generate-smile m1 opts))

    (cheshire/generate-stream
     m1
     (clojure.java.io/writer "invasion-unordered.json")
     opts)))