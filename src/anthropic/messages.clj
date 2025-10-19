(ns anthropic.messages
  "Messages API service for Anthropic Claude"
  (:require [anthropic.core :as core]))

(defn create
  "Create a message with Claude

  Required parameters:
  - :max-tokens - Maximum number of tokens to generate
  - :messages - Vector of message maps with :role and :content
  - :model - Model identifier (e.g., \"claude-sonnet-4-20250514\")

  Optional parameters:
  - :metadata - Metadata about the request
  - :service-tier - Service tier (e.g., \"auto\", \"priority\")
  - :stop-sequences - Vector of stop sequences
  - :system - System prompt (string or vector of text blocks)
  - :temperature - Sampling temperature (0.0 to 1.0)
  - :thinking - Thinking configuration map
  - :tool-choice - Tool choice configuration
  - :tools - Vector of tool definitions
  - :top-k - Top-K sampling parameter
  - :top-p - Top-P (nucleus) sampling parameter
  - :options - Request options (see core/request)

  Example:
    (create {:max-tokens 1024
             :messages [{:role \"user\" :content \"Hello, Claude\"}]
             :model \"claude-sonnet-4-20250514\"})"
  [{:keys [max-tokens messages model metadata service-tier stop-sequences
           system temperature thinking tool-choice tools top-k top-p options]
    :as params}]
  (let [body (cond-> {:max_tokens max-tokens
                      :messages messages
                      :model model}
               metadata (assoc :metadata metadata)
               service-tier (assoc :service_tier service-tier)
               stop-sequences (assoc :stop_sequences stop-sequences)
               system (assoc :system system)
               temperature (assoc :temperature temperature)
               thinking (assoc :thinking thinking)
               tool-choice (assoc :tool_choice tool-choice)
               tools (assoc :tools tools)
               top-k (assoc :top_k top-k)
               top-p (assoc :top_p top-p))]
    (core/request {:method :post
                   :path "v1/messages"
                   :body body
                   :options options})))

(defn create-stream
  "Create a streaming message with Claude

  Same parameters as create, but returns a lazy sequence of server-sent events.
  Each event contains streaming message data.

  Example:
    (doseq [event (create-stream {:max-tokens 1024
                                   :messages [{:role \"user\" :content \"Hello\"}]
                                   :model \"claude-sonnet-4-20250514\"})]
      (println event))"
  [{:keys [max-tokens messages model metadata service-tier stop-sequences
           system temperature thinking tool-choice tools top-k top-p options]
    :as params}]
  (let [body (cond-> {:max_tokens max-tokens
                      :messages messages
                      :model model
                      :stream true}
               metadata (assoc :metadata metadata)
               service-tier (assoc :service_tier service-tier)
               stop-sequences (assoc :stop_sequences stop-sequences)
               system (assoc :system system)
               temperature (assoc :temperature temperature)
               thinking (assoc :thinking thinking)
               tool-choice (assoc :tool_choice tool-choice)
               tools (assoc :tools tools)
               top-k (assoc :top_k top-k)
               top-p (assoc :top_p top-p))]
    ;; Note: This is a placeholder. Full streaming implementation would use http-kit
    ;; or similar for proper SSE handling
    (core/request {:method :post
                   :path "v1/messages"
                   :body body
                   :options options})))

(defn count-tokens
  "Count tokens in a message without creating it

  Required parameters:
  - :messages - Vector of message maps
  - :model - Model identifier

  Optional parameters:
  - :system - System prompt
  - :thinking - Thinking configuration
  - :tool-choice - Tool choice configuration
  - :tools - Vector of tool definitions
  - :options - Request options

  Returns a map with token counts."
  [{:keys [messages model system thinking tool-choice tools options]
    :as params}]
  (let [body (cond-> {:messages messages
                      :model model}
               system (assoc :system system)
               thinking (assoc :thinking thinking)
               tool-choice (assoc :tool_choice tool-choice)
               tools (assoc :tools tools))]
    (core/request {:method :post
                   :path "v1/messages/count_tokens"
                   :body body
                   :options options})))
