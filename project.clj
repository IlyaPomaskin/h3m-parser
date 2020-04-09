(defproject h3m-parser "1.3.0"
  :description "Library for parsing *.h3m (Heroes of Might and Magic III maps)"
  :url "https://github.com/IlyaPomaskin/h3m-parser/"
  :license {:name "GPL-3.0"
            :url "https://www.gnu.org/licenses/gpl-3.0.en.html"}
  :deploy-repositories [["clojars" {:sign-releases false}]]
  :exclusions [org.clojure/clojure]
  :dependencies [[smee/binary "0.5.5"]]
  :profiles {:dev {:dependencies [[org.clojure/clojure "1.10.0"]]}})
