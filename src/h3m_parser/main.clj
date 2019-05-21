(ns h3m-parser.main
  (:require [clojure.spec.alpha :as s]
            [clojure.java.io :as io]
            [org.clojars.smee.binary.core :as binary]
            [cheshire.core :as cheshire]
            [h3m-parser.objects :as h3m-objects]
            [h3m-parser.h3m :as h3m]
            ))


(defn parse-stream [stream]
  (binary/decode h3m/root stream))


(defn parse-file [file-path]
  (if (not (.exists (io/file file-path)))
    (throw (Exception. (str "File " file-path " doesn't exists")))
    (some-> file-path
            (io/input-stream)
            (java.util.zip.GZIPInputStream. 1)
            (io/input-stream)
            (parse-stream))))


(defn -main [file-path]
    (-> file-path
        (parse-file)
        (cheshire/generate-string {:pretty true})
        (println)))