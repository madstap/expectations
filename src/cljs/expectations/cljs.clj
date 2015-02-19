(ns expectations.cljs
  (:require [cljs.analyzer.api :as aapi]
            [expectations :as e]))

(defn ->cljs-vars [namespace]
  (assert (symbol? namespace) (str namespace))
  (->> (aapi/ns-interns namespace)
    (map (fn [[k _]] `(var ~(symbol (name namespace) (name k)))))
    (into [])))

(defmacro run-tests [namespaces]
  (assert (and (vector? namespaces) (every? symbol? namespaces)) (str namespaces))
  `(let [all-vars# [~@(mapcat ->cljs-vars namespaces)]
         all-vars# (sort-by (fn [v#] [(-> v# meta :ns) (-> v# meta :line)]) all-vars#)
         vars-by-kind# (e/by-kind all-vars#)]
     (println "namespaces: " '~namespaces)
     (println "1st var: " (first all-vars#))
     ;(println "1st var value: " @(first all-vars#))
     (println "1st var meta: " (-> (first all-vars#) meta))
     (println "vars-by-kind: " vars-by-kind#)

     #_(if-let [focused (e/->focused-expectations expectations)]
       (doto (assoc (e/test-vars (assoc vars-by-kind :expectation focused) (- (count expectations) (count focused)))
               :type :summary)
         (e/report))
       (doto (assoc (e/test-vars vars-by-kind 0)
               :type :summary)
         (e/report)))))                                     ;TODO impl

(defmacro run-all-tests
  ([] `(run-all-tests nil))
  ([re] `(run-tests
           [~@(cond->> (filter
                         (comp not #(re-matches #"cljs\..+|clojure\..+|expectations|expectations\..+" %) name)
                         (aapi/all-ns))
                re (filter #(re-matches re (name %))))])))
