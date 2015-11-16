(ns bgg_list.test-runner
  (:require
   [cljs.test :refer-macros [run-tests]]
   [bgg_list.core-test]))

(enable-console-print!)

(defn runner []
  (if (cljs.test/successful?
       (run-tests
        'bgg_list.core-test))
    0
    1))
