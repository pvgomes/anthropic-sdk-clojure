(ns anthropic.client
  "Main client for Anthropic API

  This namespace provides a unified client interface that encompasses
  all API services (messages, models, beta) with convenient access patterns."
  (:require [anthropic.core :as core]
            [anthropic.messages :as messages]
            [anthropic.models :as models]
            [anthropic.beta :as beta]
            [anthropic.streaming :as streaming]
            [anthropic.pagination :as pagination]))

(defn create-client
  "Create an Anthropic API client

  Options:
  - :api-key - Anthropic API key (defaults to ANTHROPIC_API_KEY env var)
  - :auth-token - Optional auth token (defaults to ANTHROPIC_AUTH_TOKEN env var)
  - :base-url - Base API URL (defaults to https://api.anthropic.com)
  - :max-retries - Maximum number of retries (default 2)
  - :timeout - Request timeout in milliseconds (default 120000)

  Returns a client map with namespaced service functions."
  ([]
   (create-client {}))
  ([options]
   {:options options
    :messages {:create (fn [params]
                        (messages/create (assoc params :options options)))
               :create-stream (fn [params]
                               (streaming/stream-messages
                                (assoc params :options options)))
               :count-tokens (fn [params]
                              (messages/count-tokens
                               (assoc params :options options)))}
    :models {:list (fn
                    ([] (models/list-models {:options options}))
                    ([params] (models/list-models
                              (assoc params :options options))))
             :get (fn [params]
                   (models/get-model (assoc params :options options)))}
    :beta {:messages {:create (fn [params]
                               (beta/create-message
                                (assoc params :options options)))
                      :create-stream (fn [params]
                                      (beta/create-message-stream
                                       (assoc params :options options)))
                      :batches {:create (fn [params]
                                         (beta/create-message-batch
                                          (assoc params :options options)))
                                :get (fn [params]
                                      (beta/get-message-batch
                                       (assoc params :options options)))
                                :list (fn
                                       ([] (beta/list-message-batches
                                           {:options options}))
                                       ([params] (beta/list-message-batches
                                                 (assoc params :options options))))
                                :cancel (fn [params]
                                         (beta/cancel-message-batch
                                          (assoc params :options options)))
                                :results (fn [params]
                                          (beta/get-batch-results
                                           (assoc params :options options)))}}
           :models {:list (fn
                           ([] (beta/list-models {:options options}))
                           ([params] (beta/list-models
                                     (assoc params :options options))))
                    :get (fn [params]
                          (beta/get-model (assoc params :options options)))}}}))

;; Convenience functions for direct API access without creating a client

(defn create-message
  "Create a message with Claude (convenience function)

  See anthropic.messages/create for parameters."
  [params]
  (messages/create params))

(defn stream-message
  "Stream a message with Claude (convenience function)

  See anthropic.streaming/stream-messages for parameters."
  [params]
  (streaming/stream-messages params))

(defn count-tokens
  "Count tokens in a message (convenience function)

  See anthropic.messages/count-tokens for parameters."
  [params]
  (messages/count-tokens params))

(defn list-models
  "List available models (convenience function)

  See anthropic.models/list-models for parameters."
  ([]
   (models/list-models))
  ([params]
   (models/list-models params)))

(defn get-model
  "Get model information (convenience function)

  See anthropic.models/get-model for parameters."
  [params]
  (models/get-model params))

;; Re-export pagination utilities

(def page-seq pagination/page-seq)
(def fetch-all pagination/fetch-all)
(def page-iterator pagination/page-iterator)
