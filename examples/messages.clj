(ns examples.messages
  "Example usage of the Anthropic Messages API"
  (:require [anthropic.client :as anthropic]))

(defn basic-message-example
  "Basic example of creating a message"
  []
  (println "=== Basic Message Example ===")
  (let [message (anthropic/create-message
                  {:max-tokens 1024
                   :messages [{:role "user"
                               :content "Hello, Claude! Tell me a short joke."}]
                   :model "claude-sonnet-4-20250514"
                   :options {:api-key (or (System/getenv "ANTHROPIC_API_KEY")
                                          "your-api-key-here")}})]
    (println "\nResponse:")
    (println message)))

(defn multi-turn-conversation-example
  "Example with multiple conversation turns"
  []
  (println "\n=== Multi-turn Conversation Example ===")
  (let [message (anthropic/create-message
                  {:max-tokens 1024
                   :messages [{:role "user"
                               :content "Hello there."}
                              {:role "assistant"
                               :content "Hi, I'm Claude. How can I help you?"}
                              {:role "user"
                               :content "Can you explain what an API is in simple terms?"}]
                   :model "claude-sonnet-4-20250514"
                   :options {:api-key (System/getenv "ANTHROPIC_API_KEY")}})]
    (println "\nResponse:")
    (println message)))

(defn with-system-prompt-example
  "Example using a system prompt"
  []
  (println "\n=== System Prompt Example ===")
  (let [message (anthropic/create-message
                  {:max-tokens 1024
                   :messages [{:role "user"
                               :content "Write a haiku about programming"}]
                   :model "claude-sonnet-4-20250514"
                   :system "You are a poetic assistant who speaks in haikus."
                   :options {:api-key (System/getenv "ANTHROPIC_API_KEY")}})]
    (println "\nResponse:")
    (println message)))

(defn with-temperature-example
  "Example using temperature parameter"
  []
  (println "\n=== Temperature Example ===")
  (println "High temperature (creative):")
  (let [creative (anthropic/create-message
                   {:max-tokens 1024
                    :messages [{:role "user"
                                :content "Describe a sunset in 3 words"}]
                    :model "claude-sonnet-4-20250514"
                    :temperature 0.9
                    :options {:api-key (System/getenv "ANTHROPIC_API_KEY")}})]
    (println creative))

  (println "\nLow temperature (analytical):")
  (let [analytical (anthropic/create-message
                     {:max-tokens 1024
                      :messages [{:role "user"
                                  :content "Describe a sunset in 3 words"}]
                      :model "claude-sonnet-4-20250514"
                      :temperature 0.1
                      :options {:api-key (System/getenv "ANTHROPIC_API_KEY")}})]
    (println analytical)))

(defn with-tools-example
  "Example using tool calling"
  []
  (println "\n=== Tool Calling Example ===")
  (let [message (anthropic/create-message
                  {:max-tokens 1024
                   :messages [{:role "user"
                               :content "What's the weather in San Francisco?"}]
                   :model "claude-sonnet-4-20250514"
                   :tools [{:name "get_weather"
                            :description "Get the current weather in a given location"
                            :input_schema {:type "object"
                                          :properties {:location {:type "string"
                                                                 :description "The city and state, e.g. San Francisco, CA"}
                                                      :unit {:type "string"
                                                            :enum ["celsius" "fahrenheit"]
                                                            :description "The unit of temperature"}}
                                          :required ["location"]}}]
                   :options {:api-key (System/getenv "ANTHROPIC_API_KEY")}})]
    (println "\nResponse:")
    (println message)))

(defn count-tokens-example
  "Example of counting tokens"
  []
  (println "\n=== Token Counting Example ===")
  (let [token-count (anthropic/count-tokens
                      {:messages [{:role "user"
                                   :content "Hello, Claude! How are you today?"}]
                       :model "claude-sonnet-4-20250514"
                       :options {:api-key (System/getenv "ANTHROPIC_API_KEY")}})]
    (println "\nToken count:")
    (println token-count)))

(defn error-handling-example
  "Example of error handling"
  []
  (println "\n=== Error Handling Example ===")
  (try
    (anthropic/create-message
      {:max-tokens 1024
       :messages [{:role "user" :content "Hello"}]
       :model "invalid-model"
       :options {:api-key (System/getenv "ANTHROPIC_API_KEY")}})
    (catch Exception e
      (println "Caught error!")
      (println "Message:" (ex-message e))
      (println "Type:" (:type (ex-data e)))
      (println "Status:" (:status (ex-data e))))))

(defn -main
  "Run all examples"
  [& args]
  (println "Anthropic Clojure SDK - Messages Examples\n")

  ;; Uncomment the examples you want to run
  (basic-message-example)
  ;; (multi-turn-conversation-example)
  ;; (with-system-prompt-example)
  ;; (with-temperature-example)
  ;; (with-tools-example)
  ;; (count-tokens-example)
  ;; (error-handling-example)

  (println "\n=== Examples Complete ==="))

;; To run: clj -M:examples -m examples.messages
