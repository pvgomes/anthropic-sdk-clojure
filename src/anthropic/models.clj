(ns anthropic.models
  "Models API service for Anthropic"
  (:require [anthropic.core :as core]))

(defn list-models
  "List available models

  Optional parameters:
  - :before-id - ID of model to start pagination before
  - :after-id - ID of model to start pagination after
  - :limit - Number of models to return (default 20, max 1000)
  - :options - Request options

  Returns a map with :data (vector of models) and pagination info."
  ([]
   (list-models {}))
  ([{:keys [before-id after-id limit options]
     :as params}]
   (let [query (cond-> {}
                 before-id (assoc :before_id before-id)
                 after-id (assoc :after_id after-id)
                 limit (assoc :limit limit))]
     (core/request {:method :get
                    :path "v1/models"
                    :query query
                    :options options}))))

(defn get-model
  "Get information about a specific model

  Required parameters:
  - :model-id - The model identifier

  Optional parameters:
  - :options - Request options

  Returns model information."
  [{:keys [model-id options]}]
  (when-not model-id
    (throw (ex-info "model-id is required" {:type :validation-error})))
  (core/request {:method :get
                 :path (str "v1/models/" model-id)
                 :options options}))
