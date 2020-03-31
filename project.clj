(defproject h3m-parser "1.2.6"
  :description "Library for parsing *.h3m (Heroes of Might and Magic III maps)"
  :url "https://github.com/IlyaPomaskin/h3m-parser/"
  :license {:name "GPL-3.0"
            :url "https://www.gnu.org/licenses/gpl-3.0.en.html"}
  :deploy-repositories [["clojars" {:sign-releases false}]]
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [smee/binary "0.5.5"]])
