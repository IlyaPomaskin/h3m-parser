(defproject h3m-parser "1.0.0"
  :description "Library for parsing *.h3m (Heroes of Might and Magic III maps)"
  :url "https://github.com/IlyaPomaskin/h3m-parser/"
  :license {:name "GPL-3.0"
            :url "https://www.gnu.org/licenses/gpl-3.0.en.html"}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [smee/binary "0.5.4"]]
  :main ^:skip-aot h3m-parser.main
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
