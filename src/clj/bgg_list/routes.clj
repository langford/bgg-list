(ns bgg-list.routes
  (:require [compojure.core :refer [GET defroutes]]
            [compojure.route :refer [resources]]))

(use 'bgg-list.handler)
(use 'bgg-list.core)


(defroutes routes
  (resources "/")
  (GET "/goodwith/:str-n" [str-n] (good-games-with-response (Integer/parseInt str-n)))
  (GET "/bestwith/:str-n" [str-n] (best-games-with-response (Integer/parseInt str-n)))
  (GET "/all-list" req all-games-response)
  (GET "/_remove_cache" req remove-cache-response)
  (GET "/entry/:entry" [entry] (game-entry-response (Integer/parseInt entry)))
  (GET "/" [entry] (homepage-response))
  (resources "/react" {:root "react"}))



