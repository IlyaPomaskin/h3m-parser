(ns h3m-parser.core
  (:require [clojure.spec.alpha :as s]
            [org.clojars.smee.binary.core :as b]
            [clojure.pprint :as pp]
            [h3m-parser.objects :as h3m-objects]
            [h3m-parser.h3m :as h3m]
            [clojure.java.io :as io]))


(defn -main [file-path]
  (if (not (.exists (io/file file-path)))
    (println "File" file-path "not exists")
    (some-> file-path
            (io/input-stream)
            (java.util.zip.GZIPInputStream. 1)
            (io/input-stream)
            (as-> stream (b/decode h3m/root stream))
            (dissoc :players)
            (dissoc :predefined-heroes)
            (dissoc :placeholder-1)
            (dissoc :placeholder-2)
            (dissoc :placeholder-3)
            (dissoc :terrain)
            (dissoc :defs)
            (pp/pprint))))

