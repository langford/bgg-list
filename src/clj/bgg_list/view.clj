(ns bgg-list.view
  (:require  [clojure.java.io :as io]
             [bgg-list.dev :refer [is-dev? inject-devmode-html browser-repl start-figwheel]]
             [compojure.core :refer [GET defroutes]]
             [compojure.route :refer [resources]]
             [net.cgrand.enlive-html :refer [deftemplate]]
             [net.cgrand.reload :refer [auto-reload]]
             [ring.middleware.reload :as reload]
             [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
             [environ.core :refer [env]]
             [ring.adapter.jetty :refer [run-jetty]]
             [clojure.data.zip.xml :as zx]
             [clojure.xml :as xml]
             [clojure.string :as string]
             [clojure.zip :as zip]
             [clojure.pprint :as pp]
             [schema.core :as s]
             [clojure.edn :as edn]))

(use 'bgg-list.core)

(defn display-cleaned-item 
  "render a clean item into a string"
  [item]
  (str "<a href=\"../entry/" (:objectid item) ".html\">" (:name item) "</a>"))

(defn display-cleaned-game-list 
  "show the clean list" 
  [cleaned]
  (str "<h1>Games</h1><br><ol>" (string/join (map #(str "<li>" (display-cleaned-item %)) cleaned))))

(defn render-game-entry 
  "make the game look pretty for display" 
  [e]
  (let [img-tag (str "<img src='" (:thumb-uri e) "'></img>")
        pretty-deep (with-out-str (pp/pprint (:deep e)))
        pretty-all (with-out-str (pp/pprint e))
        pretty-poll (with-out-str (pp/pprint (:np-poll e)))
        pretty-bw   (clojure.string/join "," (:players-best-with e))
        pretty-al   (clojure.string/join "," (:players-at-least-recommended-with e))
        ;pretty-counts (with-out-str (pp/pprint (simplify-rec-poll entry)))
]
    (str img-tag "<br>"
         "Name: " (:name e) "<br>"
         "Players: " (:min-players e) "-" (:max-players e) "<br>"
         "Playtime: " (:pretty-playtime e) "<br>"
         (:type e)  "<br>"
         "Description: " (:description e) "<br>"
         "<pre>Best with: " pretty-bw "</pre><br>"
         "<pre>Good with: " pretty-al "</pre><br>"
         "<br>")))

(defn display-simplified-game 
  "render the simple game information" 
  [item]
  (str "<a href=\"../entry/" (:objectid item) ".html\">" (:name item) " (" (:pretty-playtime item) ")</a>"))

(defn display-desc-game 
  "display a game description" 
  [item]
  (str "<tr ><td style='padding-bottom: 3em'><img src=\"" (:thumb-uri item) "\" align=\"left\"/><a href=\"../entry/" (:objectid item) ".html\">" (:name item) " (" (:pretty-playtime item) ")</a><br>" (:description item) "<br>\n\n\n\n"))

(defn render-good-with 
  "show a good with item" 
  [n db]
  (let [gw (good-with n db)
        sorted-gw (sort-by :max-playtime > gw)
        gamematch (map #(str (display-desc-game %)) sorted-gw)
        ls (string/join gamematch)]
    (str "<h1>Good with " n "</h1><table>" ls)))

(defn render-best-with 
  "show a best with item" 
  [n db]
  (let [gw (best-with n db)
        sorted-gw (sort-by :max-playtime > gw)
        gamematch (map #(str (display-desc-game %)) sorted-gw)
        ls (string/join gamematch)]
    (str "<h1>Best with " n "</h1><table>" ls)))

(defn button 
  "makes a button that you'd insert into html"
  [uri text w h]
  (str "<a href=\"" uri "\"><button style='width:" w "px; height:" h "px;'>" text "</button></a>"))

(defn big-button 
  "make a button big enough to click"
  [uri text]
  (button uri text 300 44))

(defn best-with-button 
  "properly format the button for best with" 
  [n]
  (big-button (str "bestwith/" n ".html") (if (= n 1) "1 Player" 
                                       (str n " Players"))))

(defn good-with-button 
  "properly format the button for good with" 
  [n]
  (big-button (str "goodwith/" n ".html") (if (= n 1) "1 Player" 
                                       (str n " Players"))))

(defn br 
  "show a break" 
  [] 
  "<br>")

(defn render-homepage 
  "take render homepage for the buttons to go to the specifc page" 
  []
  (str " <!DOCTYPE html>
       <html lang='en'><head><meta name='viewport' content='width=device-width, initial-scale=1'></head>
       <body><h1>Game Library</h1>"
       "<h2>Good With:</h2>"
       (good-with-button 1) (br)
       (good-with-button 2) (br)
       (good-with-button 3) (br)
       (good-with-button 4) (br)
       (good-with-button 5) (br)
       (good-with-button 6) (br)
       (good-with-button 7) (br)
       (good-with-button 8) (br)
       (good-with-button 9) (br)
       "<H2>Best With:</h2>"
       (best-with-button 1) (br)
       (best-with-button 2) (br)
       (best-with-button 3) (br)
       (best-with-button 4) (br)
       (best-with-button 5) (br)
       (best-with-button 6) (br)
       (best-with-button 7) (br)
       (best-with-button 8) (br)
       (best-with-button 9) (br)

       "<a href='/all-list'>All Games</a>"
       "</body></html>"))

(use 'clojure.java.io)
(defn write-file
  "export the file by string"
  [f s dir]
  (with-open [wrtr (writer (str dir f))]
      (.write wrtr s)))

(defn export-static
  "exports a standard html site to the given directory"
  [directory]
  (let [dir (str directory "/")]
    (write-file "index.html" (render-homepage) dir)
    (doseq [bw-index (range 1 10)]
      (write-file (str "/bestwith/" bw-index ".html") 
                     (render-best-with bw-index game-db) 
                     dir))
    (doseq [gw-index (range 1 10)]
      (write-file (str "/goodwith/" gw-index ".html") 
                     (render-good-with gw-index game-db) 
                     dir))
    (doseq [[k v] game-db]
      (write-file (str "/entry/" k ".html") 
                     (render-game-entry (get-in game-db [k]))
                     dir))))


(export-static "/Users/mlangford/Desktop")
