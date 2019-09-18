# h3m-parser

Heroes of Might and Magic III: The Shadow of Death maps parser.
Parse only visual elements. A lot of gameplay elements are omit or unknown.

Reference: https://github.com/vcmi/vcmi/blob/develop/lib/mapping/MapFormatH3M.cpp

## Usage

```
(ns h3m-lwp-clj.core
  (:require [clojure.java.io :as io]
            [h3m-parser.main :as h3m]))

(def file-path "./resources/invasion.h3m")

(if (not (.exists (io/file file-path)))
    (throw (Exception. (str "File " file-path " doesn't exists")))
    (println (h3m/parse-file file-path)))
```

```
{:description "This map is taken from the catalogue www.heroesportal.net\n",
 :teams-count 2,
 :heroes [],
 :difficulty 3,
 :map-version 28,
 :has-players? true,
 ... }
 ```