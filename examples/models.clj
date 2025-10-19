(ns examples.models
  "Example usage of the Anthropic Models API"
  (:require [anthropic.client :as anthropic]
            [anthropic.models :as models]
            [anthropic.pagination :as pagination]))

(defn list-models-example
  "Basic example of listing models"
  []
  (println "=== List Models Example ===")
  (let [response (anthropic/list-models
                   {:options {:api-key (or (System/getenv "ANTHROPIC_API_KEY")
                                          "your-api-key-here")}})]
    (println "\nModels:")
    (doseq [model (get response :data)]
      (println "- " (:id model) "-" (:display_name model)))))

(defn list-models-with-limit-example
  "Example of listing models with pagination limit"
  []
  (println "\n=== List Models with Limit Example ===")
  (let [response (models/list-models
                   {:limit 5
                    :options {:api-key (System/getenv "ANTHROPIC_API_KEY")}})]
    (println "\nFirst 5 models:")
    (println "Has more:" (:has_more response))
    (doseq [model (:data response)]
      (println "- " (:id model)))))

(defn get-model-example
  "Example of getting a specific model"
  []
  (println "\n=== Get Model Example ===")
  (let [model (anthropic/get-model
                {:model-id "claude-sonnet-4-20250514"
                 :options {:api-key (System/getenv "ANTHROPIC_API_KEY")}})]
    (println "\nModel details:")
    (println "ID:" (:id model))
    (println "Display Name:" (:display_name model))
    (println "Type:" (:type model))
    (println "Created At:" (:created_at model))))

(defn pagination-example
  "Example of iterating through all models with pagination"
  []
  (println "\n=== Pagination Example ===")
  (println "Fetching all models across all pages...")
  (let [all-models (pagination/fetch-all
                     (fn [params]
                       (models/list-models
                         (assoc params :options {:api-key (System/getenv "ANTHROPIC_API_KEY")})))
                     {:limit 10}
                     :data)]
    (println "\nTotal models:" (count all-models))
    (println "All model IDs:")
    (doseq [model all-models]
      (println "- " (:id model)))))

(defn lazy-pagination-example
  "Example of lazy iteration through models"
  []
  (println "\n=== Lazy Pagination Example ===")
  (println "Lazily iterating through models...")
  (let [model-seq (pagination/page-seq
                    (fn [params]
                      (models/list-models
                        (assoc params :options {:api-key (System/getenv "ANTHROPIC_API_KEY")})))
                    {:limit 5}
                    :data)]
    ;; Only take first 10 models (demonstrates laziness)
    (println "\nFirst 10 models:")
    (doseq [model (take 10 model-seq)]
      (println "- " (:id model)))))

(defn page-iterator-example
  "Example of iterating over pages (not individual items)"
  []
  (println "\n=== Page Iterator Example ===")
  (let [pages (pagination/page-iterator
                (fn [params]
                  (models/list-models
                    (assoc params :options {:api-key (System/getenv "ANTHROPIC_API_KEY")})))
                {:limit 5})]
    (println "Iterating through pages...")
    (doseq [[idx page] (map-indexed vector (take 3 pages))]
      (println "\n--- Page" (inc idx) "---")
      (println "Has more:" (:has_more page))
      (println "Models in this page:" (count (:data page)))
      (doseq [model (:data page)]
        (println "  -" (:id model))))))

(defn error-handling-example
  "Example of error handling with models API"
  []
  (println "\n=== Error Handling Example ===")
  (try
    (models/get-model
      {:model-id "non-existent-model"
       :options {:api-key (System/getenv "ANTHROPIC_API_KEY")}})
    (catch Exception e
      (println "Caught error!")
      (println "Message:" (ex-message e))
      (let [data (ex-data e)]
        (println "Type:" (:type data))
        (println "Status:" (:status data))))))

(defn -main
  "Run all model examples"
  [& args]
  (println "Anthropic Clojure SDK - Models Examples\n")

  ;; Uncomment the examples you want to run
  (list-models-example)
  ;; (list-models-with-limit-example)
  ;; (get-model-example)
  ;; (pagination-example)
  ;; (lazy-pagination-example)
  ;; (page-iterator-example)
  ;; (error-handling-example)

  (println "\n=== Examples Complete ==="))

;; To run: clj -M:examples -m examples.models
