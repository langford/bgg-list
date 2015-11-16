(ns bgg-list.handler)

(use 'bgg-list.view)
(use 'bgg-list.core)

(defn all-games-response
  "response for the list of all games"
  []
   {:status 200
   :headers {"Content-Type" "text/html; charset-utf-8"}
   :body   (display-cleaned-game-list (collection-info-from-raw games-xml))})

(defn game-entry-response 
  "calculate a response about the entry" 
  [n]
  (println "fetching game entry " n)
  {:status 200
   :headers {"Content-Type" "text/html; charset-utf-8"}
   :body (render-game-entry (get-in game-db [n]))})

(defn good-games-with-response 
  "find a good with response" 
  [n]
  {:status 200
   :headers {"Content-Type" "text/html; charset-utf-8"}
   :body (render-good-with n game-db)})

(defn best-games-with-response 
  "create a best with response"
  [n]
  {:status 200
   :headers {"Content-Type" "text/html; charset-utf-8"}
   :body (render-best-with n game-db)})

(defn homepage-response 
  "reponse for the homepage"
  []
  {:status 200
   :headers {"Content-Type" "text/html; charset-utf-8"}
   :body (render-homepage)})



