(ns examples.streaming
  "Example usage of streaming with Anthropic API"
  (:require [anthropic.streaming :as streaming]))

(defn basic-streaming-example
  "Basic example of streaming a message"
  []
  (println "=== Basic Streaming Example ===")
  (let [stream (streaming/stream-messages
                 {:max-tokens 1024
                  :messages [{:role "user"
                              :content "Tell me a story about a robot, one sentence at a time."}]
                  :model "claude-sonnet-4-20250514"
                  :options {:api-key (or (System/getenv "ANTHROPIC_API_KEY")
                                         "your-api-key-here")}})]
    (try
      (println "\nStreaming response:")
      (doseq [event (:events stream)]
        (println "\n--- Event ---")
        (println "Type:" (:event event))
        (println "Data:" (:data event)))
      (finally
        ((:close stream))))))

(defn text-accumulation-example
  "Example showing how to accumulate text from streaming"
  []
  (println "\n=== Text Accumulation Example ===")
  (let [stream (streaming/stream-messages
                 {:max-tokens 1024
                  :messages [{:role "user"
                              :content "Write a haiku about Clojure"}]
                  :model "claude-sonnet-4-20250514"
                  :options {:api-key (System/getenv "ANTHROPIC_API_KEY")}})]
    (try
      (println "\nAccumulated text:")
      (let [text (atom "")]
        (doseq [event (:events stream)]
          (when (= (:event event) "content_block_delta")
            (when-let [delta (get-in event [:data :delta :text])]
              (swap! text str delta)
              (print delta)
              (flush))))
        (println "\n\nFinal text:" @text))
      (finally
        ((:close stream))))))

(defn streaming-with-tools-example
  "Example of streaming with tool calls"
  []
  (println "\n=== Streaming with Tools Example ===")
  (let [stream (streaming/stream-messages
                 {:max-tokens 1024
                  :messages [{:role "user"
                              :content "What's 25 * 4?"}]
                  :model "claude-sonnet-4-20250514"
                  :tools [{:name "calculator"
                           :description "Perform basic arithmetic operations"
                           :input_schema {:type "object"
                                         :properties {:operation {:type "string"
                                                                 :enum ["add" "subtract" "multiply" "divide"]}
                                                     :a {:type "number"}
                                                     :b {:type "number"}}
                                         :required ["operation" "a" "b"]}}]
                  :options {:api-key (System/getenv "ANTHROPIC_API_KEY")}})]
    (try
      (println "\nStreaming with tool calls:")
      (doseq [event (:events stream)]
        (case (:event event)
          "message_start" (println "Message started")
          "content_block_start" (println "Content block started:"
                                        (get-in event [:data :content_block :type]))
          "content_block_delta" (let [delta (get-in event [:data :delta])]
                                 (when (:text delta)
                                   (print (:text delta))
                                   (flush)))
          "content_block_stop" (println "\nContent block stopped")
          "message_stop" (println "Message stopped")
          (println "Event:" (:event event))))
      (finally
        ((:close stream))))))

(defn error-handling-streaming-example
  "Example of error handling with streaming"
  []
  (println "\n=== Error Handling Streaming Example ===")
  (try
    (let [stream (streaming/stream-messages
                   {:max-tokens 1024
                    :messages [{:role "user" :content "Hello"}]
                    :model "invalid-model"
                    :options {:api-key (System/getenv "ANTHROPIC_API_KEY")}})]
      (doseq [event (:events stream)]
        (println event))
      ((:close stream)))
    (catch Exception e
      (println "Caught streaming error!")
      (println "Message:" (ex-message e))
      (when-let [data (ex-data e)]
        (println "Status:" (:status data))
        (println "Body:" (:body data))))))

(defn -main
  "Run streaming examples"
  [& args]
  (println "Anthropic Clojure SDK - Streaming Examples\n")

  ;; Uncomment the examples you want to run
  (basic-streaming-example)
  ;; (text-accumulation-example)
  ;; (streaming-with-tools-example)
  ;; (error-handling-streaming-example)

  (println "\n=== Streaming Examples Complete ==="))

;; To run: clj -M:examples -m examples.streaming
