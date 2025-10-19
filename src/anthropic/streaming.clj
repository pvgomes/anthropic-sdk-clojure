(ns anthropic.streaming
  "Server-Sent Events (SSE) streaming support for Anthropic API"
  (:require [clojure.data.json :as json]
            [clojure.string :as str]
            [org.httpkit.client :as http-kit])
  (:import (java.io BufferedReader StringReader)))

(defn- parse-sse-line
  "Parse a single SSE line"
  [line]
  (when-not (str/blank? line)
    (let [[field value] (str/split line #":\s?" 2)]
      (when field
        {:field field :value (or value "")}))))

(defn- parse-sse-event
  "Parse accumulated SSE event lines into an event map"
  [lines]
  (let [parsed (keep parse-sse-line lines)
        event-type (some #(when (= (:field %) "event") (:value %)) parsed)
        data-lines (keep #(when (= (:field %) "data") (:value %)) parsed)
        data (str/join "\n" data-lines)]
    (when-not (str/blank? data)
      {:event (or event-type "message")
       :data (try
               (json/read-str data :key-fn keyword)
               (catch Exception _ data))})))

(defn- sse-seq
  "Create a lazy sequence of SSE events from a reader"
  [reader]
  (lazy-seq
   (let [lines (loop [acc []]
                 (if-let [line (.readLine reader)]
                   (if (str/blank? line)
                     acc
                     (recur (conj acc line)))
                   (when (seq acc) acc)))]
     (when lines
       (if-let [event (parse-sse-event lines)]
         (cons event (sse-seq reader))
         (sse-seq reader))))))

(defn create-stream
  "Create a streaming connection using Server-Sent Events

  Parameters:
  - :url - Full request URL
  - :headers - Request headers map
  - :body - Request body (will be JSON encoded)

  Returns a map with:
  - :events - Lazy sequence of parsed SSE events
  - :close - Function to close the stream"
  [{:keys [url headers body]}]
  (let [promise-chan (promise)
        request-opts {:url url
                      :method :post
                      :headers (assoc headers "Accept" "text/event-stream")
                      :body (json/write-str body)
                      :as :stream}
        response @(http-kit/request request-opts
                                     (fn [resp]
                                       (deliver promise-chan resp)))]
    (if-let [error (:error response)]
      (throw (ex-info "Streaming request failed" {:error error}))
      (let [status (:status response)]
        (if (and (>= status 200) (< status 300))
          (let [body (:body response)
                reader (BufferedReader. (clojure.java.io/reader body))
                events (sse-seq reader)]
            {:events events
             :close (fn []
                      (try
                        (.close reader)
                        (catch Exception _ nil)))})
          (throw (ex-info "Streaming request failed"
                          {:status status
                           :body (slurp (:body response))})))))))

(defn stream-messages
  "Helper function to stream messages with proper authentication

  Uses the same parameters as messages/create but returns a stream."
  [params]
  (let [api-key (or (get-in params [:options :api-key])
                    (System/getenv "ANTHROPIC_API_KEY"))
        auth-token (or (get-in params [:options :auth-token])
                       (System/getenv "ANTHROPIC_AUTH_TOKEN"))
        base-url (or (get-in params [:options :base-url])
                     (System/getenv "ANTHROPIC_BASE_URL")
                     "https://api.anthropic.com")
        headers (cond-> {"anthropic-version" "2023-06-01"
                         "Content-Type" "application/json"}
                  api-key (assoc "X-Api-Key" api-key)
                  auth-token (assoc "Authorization" (str "Bearer " auth-token))
                  (get-in params [:options :extra-headers])
                  (merge (get-in params [:options :extra-headers])))
        body (-> params
                 (dissoc :options)
                 (assoc :stream true)
                 (clojure.set/rename-keys {:max-tokens :max_tokens
                                           :stop-sequences :stop_sequences
                                           :tool-choice :tool_choice
                                           :top-k :top_k
                                           :top-p :top_p
                                           :service-tier :service_tier}))
        url (str base-url "/v1/messages")]
    (create-stream {:url url :headers headers :body body})))
