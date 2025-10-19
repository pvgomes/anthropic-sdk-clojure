(ns anthropic.beta
  "Beta API services for Anthropic"
  (:require [anthropic.core :as core]))

;; Beta Messages API

(defn create-message
  "Create a beta message with Claude

  Same as messages/create but with beta features enabled.
  Uses the anthropic-beta header for beta features.

  Optional :beta-features parameter to specify beta features:
  - Vector of feature strings like [\"max-tokens-3-5-sonnet-2024-07-15\"]"
  [{:keys [beta-features options] :as params}]
  (let [beta-header (when (seq beta-features)
                      {"anthropic-beta" (clojure.string/join "," beta-features)})
        updated-options (update options :extra-headers merge beta-header)]
    ((requiring-resolve 'anthropic.messages/create)
     (assoc params :options updated-options))))

(defn create-message-stream
  "Create a streaming beta message with Claude"
  [{:keys [beta-features options] :as params}]
  (let [beta-header (when (seq beta-features)
                      {"anthropic-beta" (clojure.string/join "," beta-features)})
        updated-options (update options :extra-headers merge beta-header)]
    ((requiring-resolve 'anthropic.messages/create-stream)
     (assoc params :options updated-options))))

;; Beta Message Batches API

(defn create-message-batch
  "Create a batch of messages for async processing

  Required parameters:
  - :requests - Vector of request maps, each with :custom_id and message params

  Returns a batch object with status and processing info."
  [{:keys [requests options]}]
  (when-not (seq requests)
    (throw (ex-info "requests is required and must not be empty" {:type :validation-error})))
  (core/request {:method :post
                 :path "v1/messages/batches"
                 :body {:requests requests}
                 :options (update options :extra-headers merge
                                  {"anthropic-beta" "message-batches-2024-09-24"})}))

(defn get-message-batch
  "Get status and details of a message batch

  Required parameters:
  - :batch-id - The batch identifier"
  [{:keys [batch-id options]}]
  (when-not batch-id
    (throw (ex-info "batch-id is required" {:type :validation-error})))
  (core/request {:method :get
                 :path (str "v1/messages/batches/" batch-id)
                 :options (update options :extra-headers merge
                                  {"anthropic-beta" "message-batches-2024-09-24"})}))

(defn list-message-batches
  "List message batches

  Optional parameters:
  - :before-id - ID to start pagination before
  - :after-id - ID to start pagination after
  - :limit - Number of batches to return"
  ([]
   (list-message-batches {}))
  ([{:keys [before-id after-id limit options]}]
   (let [query (cond-> {}
                 before-id (assoc :before_id before-id)
                 after-id (assoc :after_id after-id)
                 limit (assoc :limit limit))]
     (core/request {:method :get
                    :path "v1/messages/batches"
                    :query query
                    :options (update options :extra-headers merge
                                     {"anthropic-beta" "message-batches-2024-09-24"})}))))

(defn cancel-message-batch
  "Cancel a message batch

  Required parameters:
  - :batch-id - The batch identifier"
  [{:keys [batch-id options]}]
  (when-not batch-id
    (throw (ex-info "batch-id is required" {:type :validation-error})))
  (core/request {:method :post
                 :path (str "v1/messages/batches/" batch-id "/cancel")
                 :options (update options :extra-headers merge
                                  {"anthropic-beta" "message-batches-2024-09-24"})}))

(defn get-batch-results
  "Get results of a message batch

  Required parameters:
  - :batch-id - The batch identifier

  Returns a stream of results."
  [{:keys [batch-id options]}]
  (when-not batch-id
    (throw (ex-info "batch-id is required" {:type :validation-error})))
  (core/request {:method :get
                 :path (str "v1/messages/batches/" batch-id "/results")
                 :options (update options :extra-headers merge
                                  {"anthropic-beta" "message-batches-2024-09-24"})}))

;; Beta Models API

(defn list-models
  "List available models (beta)"
  ([]
   (list-models {}))
  ([{:keys [before-id after-id limit options]}]
   (let [query (cond-> {}
                 before-id (assoc :before_id before-id)
                 after-id (assoc :after_id after-id)
                 limit (assoc :limit limit))]
     (core/request {:method :get
                    :path "v1/models"
                    :query query
                    :options (update options :extra-headers merge
                                     {"anthropic-beta" "models-2024-11-21"})}))))

(defn get-model
  "Get information about a specific model (beta)

  Required parameters:
  - :model-id - The model identifier"
  [{:keys [model-id options]}]
  (when-not model-id
    (throw (ex-info "model-id is required" {:type :validation-error})))
  (core/request {:method :get
                 :path (str "v1/models/" model-id)
                 :options (update options :extra-headers merge
                                  {"anthropic-beta" "models-2024-11-21"})}))
