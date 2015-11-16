(ns bgg-list.core
  (:require  [clojure.java.io :as io]
            [clojure.data.zip.xml :as zx]
            [clojure.xml :as xml]
            [clojure.string :as string]
            [clojure.zip :as zip]
            [clojure.pprint :as pp]
            [clojure.edn :as edn]
            ))

(defn tag-fetch 
  "fetch the tag, related to the item" 
  [tag entry]
  (cond 
    (= :poll tag) [(str (get-in entry [:attrs :name])) entry]
    :default [tag entry]))

(defn entry-type 
  "determine if it's an expansion or board game" 
  [entry]
  (let [t  (get-in entry [:attrs :type])]
    (cond (= t "boardgameexpansion") :expac
          (= t "boardgame") :game
          :default :unknown)))


(defn deep-game-attributes 
  "dig out the items that are really important about the game" 
  [entry]
  (let [ic (get-in entry [:content])
        size (count ic)
        lutlist (for [i (range 0 size)
                      :let [entry (nth ic i)]]
                  (tag-fetch (:tag entry) entry))
        lut (into {} lutlist)]
    lut))

(defn game-player-number-poll 
  "find the poll item"
  [entry]
  (get (deep-game-attributes entry) :poll))

(defn xml-from-raw 
  "get out the xml" 
  [raw]
  (xml/parse (java.io.ByteArrayInputStream. (.getBytes raw))))


(defn parse-int 
  "parse out an integer from a string...it's a little lose" 
  [s]
  (Integer. (re-find #"[0-9]*" s)))

(defn rec-counts 
  "get the counts for the different type of votes" 
  [poll-results]
  (into {} (for [i (range 0 (count poll-results))
                 :let [item (nth poll-results i)
                       rating (get-in item [:attrs :value])
                       votes  (get-in item [:attrs :numvotes])
                       num-votes (parse-int votes)]]
             [rating num-votes])))

(defn simplify-rec-poll 
  "gets a map of counts mapped to Best, Recommended and Not Recommended" 
  [entry]
  (let [deep (deep-game-attributes entry)
        poll (get deep "suggested_numplayers")
        poll-content (get-in poll [:content])
        count-pairs (for [i (range 0 (count poll-content))
                          :let [result (nth poll-content i)
                                k (get-in result [:attrs :numplayers])
                                v (get-in result [:content])]]
                      (do
                        [k (rec-counts v)]))
        count-map (into {} count-pairs)
        ]
    count-map))

(def best-t "Best")
(def rec-t  "Recommended")
(def n-rec-t "Not Recommended")


;todo, make the all 0 case come out alright in the recommendations....
(defn is-best? 
  "returns true if for n, best is as least as high as
  recommended and not-recommended and not 0"
  [sp n]
  (let [item (get-in sp [(str n)])
        best (get-in item [best-t])
        rec  (get-in item [rec-t])
        n-rec (get-in item [n-rec-t])]
    (if (nil? best)
      false
      (and (>= best rec) (>= best n-rec) (not= 0 best)))))

(defn is-rec? 
  "returns true if rec is greater than best, and greater than not rec, and not 0"
  [sp n]
  (let [item (get-in sp [(str n)])
        best (get-in item [best-t])
        rec  (get-in item [rec-t])
        n-rec (get-in item [n-rec-t])]
    (if (nil? best)
      false
      (and (>= rec best) (>= rec n-rec) (not= 0 rec)))))

(defn is-n-rec? 
  "returns true if nrec is nonzero and the highest alternative"
  [sp n]
  (let [item (get-in sp [(str n)])
        best (get-in item [best-t])
        rec  (get-in item [rec-t])
        n-rec (get-in item [n-rec-t])]
    (if (nil? best)
      false
      (and (>= n-rec rec) (>= n-rec best) (not= 0 n-rec)))))

(defn is-at-least-rec? 
  "returns true if nrec is nonzero and the highest alternative"
  [sp n]
  (or (is-rec? sp n)
      (is-best? sp n)))

(defn over-10 
  "takes a simplfilied poll, returns a vector of player counts the game is rated best with"
  [pred? sp]
  (let [master (into [] (for [x (range 1 10)] x))
        bw     (into #{} (filter #(pred? sp %) master))]
    bw))


(defn simplify-game-entry 
  "fish out the intersting information" 
  [game-names entry]
  (let [ t        (entry-type entry)
        objectid  (Integer/parseInt (get-in entry [:attrs :id]))
        deep      (deep-game-attributes entry)
        named     (get game-names objectid)
        thumbnail-src (str "http:" (first (get-in deep [:thumbnail :content])))
        image-src (str "http:" (first (get-in deep [:image :content])))
        thumb-tag (str "<img src='" thumbnail-src "'></img>")
        img-tag   (str "<img src='" image-src "'></img>")
        minplayers (get-in deep [:minplayers :attrs :value])
        maxplayers (get-in deep [:maxplayers :attrs :value])
        minplaytime (Integer/parseInt (get-in deep [:minplaytime :attrs :value]))
        maxplaytime (Integer/parseInt (get-in deep [:maxplaytime :attrs :value]))
        desc        (first (get-in deep [:description :content]))
        playtime-string (if (= minplaytime maxplaytime)
                          (str minplaytime " minutes")
                          (str minplaytime "-" maxplaytime " minutes"))
        pretty  (with-out-str (pp/pprint entry))
        sp      (simplify-rec-poll entry)
        poll    (with-out-str (pp/pprint sp ))
        prov-bw        (over-10 is-best? sp)
        at-least-r (over-10 is-at-least-rec? sp)
        bw      (if (> (count prov-bw) 0) prov-bw at-least-r) ;;guarentee at least 1 best with
        ]
    {:type t
     :name named
     :img-uri image-src
     :thumb-uri thumbnail-src
     :min-players minplayers
     :max-players maxplayers
     :min-playtime minplaytime
     :max-playtime maxplaytime
     :pretty-playtime playtime-string
     :description desc
     :np-poll poll
     :objectid objectid
     :players-best-with bw
     :players-at-least-recommended-with at-least-r
     :deep deep
     :raw entry
     }))





(defn map-from-taglist 
  "clean up the taglist" 
  [taglist]
  (into {} (for [i (range 0 (count taglist))
                 :let [entry (nth taglist i)
                       tag (get-in entry [:tag])
                       ]]
             (tag-fetch tag entry ))))

(defn cleaned-item-from-dirty 
  "get some pieces out are used in display" 
  [dirty]
  (let [ objectid (Integer/parseInt (get-in dirty [:attrs :objectid]))
        inner    (map-from-taglist (get dirty :content))
        named (first (get-in inner [:name :content]))
        thumb (first (get-in inner [:thumbnail :content]))
        ]

    {:objectid objectid
     :name named
     :thumb thumb}))

(defn collection-info-from-raw 
  "unwrap the data some" 
  [raw]
  (let [xml (xml-from-raw raw)
        items (:content xml)
        cleaned (map cleaned-item-from-dirty items)
        ]
    cleaned))

(defn map-from-game-db-inner 
  "clean out the inner db"
  [game-names db]
  (into {} (for [i (range 0 (count db))
                 :let [entry (nth db i)
                       itemid (get-in entry [:attrs :id])
                       n (Integer/parseInt itemid) ]]
             [n (simplify-game-entry game-names entry)])))

(defn good-with 
  "make the map for good with information" 
  [n db]
  (map second (filter #(-> (:players-at-least-recommended-with (second %)) (contains? n)) db)))

(defn best-with 
  "make the map for the best with information" 
  [n db]
  (map second (filter #(-> (:players-best-with (second %)) (contains? n)) db)))


(defn fetch-database 
  "get the database from bgg" 
  [username]
  (println "Beginning data load...")
  (def games-with-expacs-uri "https://www.boardgamegeek.com/xmlapi2/collection?username=gte910h&own=1")
  (def games-xml (slurp games-with-expacs-uri))
  (println "Game info downloaded, beginning processing...")
  (def collection  (collection-info-from-raw games-xml))
  (def game-id-list (into [] (map #(:objectid %) collection)))
  (def game-detail-query-string (clojure.string/join "," game-id-list))
  (def game-db-prefix-uri "https://www.boardgamegeek.com/xmlapi2/thing?id=")
  (def game-db-list (xml-from-raw (slurp (str game-db-prefix-uri game-detail-query-string))))
  (def game-db-inner (->> game-db-list :content))
  (def game-name-map (into {} (map #(vector (:objectid %) (:name %)) collection)))
  (def game-db-proto (map-from-game-db-inner game-name-map game-db-inner))
  (println "finished data load.")
  {:game-db     game-db-proto
   :raw-xml     games-xml})


;; How to get a list of the forum for the game https://www.boardgamegeek.com/xmlapi2/forumlist?type=thing&id=13
;; How to get the rules forum https://www.boardgamegeek.com/xmlapi2/forum?id=1508&page=0 pages start indexing from 1, and you can figure out the number of pages from the first call to this url (page size 50)

(def db-file-name "cached.db.edn")
(defn cache-db-reply! 
  "save the db" 
  [db]
  (spit db-file-name (prn-str db)))

(defn cached-db-reply 
  "get the cached db" 
  []
  (try
    (edn/read-string (slurp db-file-name))
    (catch Exception e
      nil)))

(def db-reply
  (let [c (cached-db-reply)]
    (if (not (nil? c))
      c
      (let [fdb (fetch-database (System/getenv "BGGLIST_USERNAME"))]
        (cache-db-reply! fdb)
        fdb))))

(def game-db    (:game-db db-reply))
(def games-xml  (:raw-xml db-reply))





