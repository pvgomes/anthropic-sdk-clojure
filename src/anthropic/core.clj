(ns anthropic.core
  "Core HTTP client functionality for Anthropic API"
  (:require [clj-http.client :as http]
            [clojure.data.json :as json]
            [clojure.string :as str]))

(def ^:const default-base-url "https://api.anthropic.com")
(def ^:const default-api-version "2023-06-01")
(def ^:const default-max-retries 2)
(def ^:const default-timeout 120000)

(defn- get-env
  "Get environment variable value"
  [var-name]
  (System/getenv var-name))

(defn- build-headers
  "Build request headers"
  [{:keys [api-key auth-token extra-headers]}]
  (merge
   {"anthropic-version" default-api-version
    "Content-Type" "application/json"
    "Accept" "application/json"}
   (when api-key
     {"X-Api-Key" api-key})
   (when auth-token
     {"Authorization" (str "Bearer " auth-token)})
   extra-headers))

(defn- build-url
  "Build full URL from base URL, path, and query params"
  [base-url path query-params]
  (let [url (str base-url "/" path)]
    (if (seq query-params)
      (str url "?" (http/generate-query-string query-params))
      url)))

(defn- should-retry?
  "Determine if request should be retried based on status code"
  [status]
  (or (= status 408)
      (= status 409)
      (= status 429)
      (>= status 500)))

(defn- calculate-retry-delay
  "Calculate exponential backoff delay in milliseconds"
  [retry-count initial-delay max-delay]
  (let [delay (* initial-delay (Math/pow 2 retry-count))]
    (min delay max-delay)))

(defn- make-request-with-retries
  "Make HTTP request with retry logic"
  [{:keys [method url headers body max-retries initial-retry-delay max-retry-delay timeout]
    :or {max-retries default-max-retries
         initial-retry-delay 500
         max-retry-delay 5000
         timeout default-timeout}}
   retry-count]
  (try
    (let [response (http/request
                    {:method method
                     :url url
                     :headers headers
                     :body (when body (json/write-str body))
                     :socket-timeout timeout
                     :connection-timeout timeout
                     :throw-exceptions false
                     :as :json})]
      (if (and (should-retry? (:status response))
               (< retry-count max-retries))
        (do
          (Thread/sleep (calculate-retry-delay retry-count initial-retry-delay max-retry-delay))
          (make-request-with-retries
           {:method method
            :url url
            :headers headers
            :body body
            :max-retries max-retries
            :initial-retry-delay initial-retry-delay
            :max-retry-delay max-retry-delay
            :timeout timeout}
           (inc retry-count)))
        response))
    (catch Exception e
      (if (< retry-count max-retries)
        (do
          (Thread/sleep (calculate-retry-delay retry-count initial-retry-delay max-retry-delay))
          (make-request-with-retries
           {:method method
            :url url
            :headers headers
            :body body
            :max-retries max-retries
            :initial-retry-delay initial-retry-delay
            :max-retry-delay max-retry-delay
            :timeout timeout}
           (inc retry-count)))
        (throw e)))))

(defn- handle-response
  "Handle HTTP response and throw appropriate errors"
  [response]
  (let [status (:status response)
        body (:body response)]
    (cond
      (< status 200)
      (throw (ex-info "Unexpected status code" {:status status :body body}))

      (< status 300)
      body

      (= status 400)
      (throw (ex-info "Bad Request" {:status status :body body :type :bad-request}))

      (= status 401)
      (throw (ex-info "Authentication Error" {:status status :body body :type :authentication}))

      (= status 403)
      (throw (ex-info "Permission Denied" {:status status :body body :type :permission-denied}))

      (= status 404)
      (throw (ex-info "Not Found" {:status status :body body :type :not-found}))

      (= status 409)
      (throw (ex-info "Conflict" {:status status :body body :type :conflict}))

      (= status 422)
      (throw (ex-info "Unprocessable Entity" {:status status :body body :type :unprocessable-entity}))

      (= status 429)
      (throw (ex-info "Rate Limit Exceeded" {:status status :body body :type :rate-limit}))

      (>= status 500)
      (throw (ex-info "Internal Server Error" {:status status :body body :type :internal-server}))

      :else
      (throw (ex-info "API Error" {:status status :body body :type :api-error})))))

(defn request
  "Make authenticated API request

  Options:
  - :api-key - Anthropic API key (defaults to ANTHROPIC_API_KEY env var)
  - :auth-token - Optional auth token (defaults to ANTHROPIC_AUTH_TOKEN env var)
  - :base-url - Base API URL (defaults to https://api.anthropic.com)
  - :extra-headers - Additional headers map
  - :extra-query-params - Additional query parameters map
  - :max-retries - Maximum number of retries (default 2)
  - :timeout - Request timeout in milliseconds (default 120000)"
  [{:keys [method path query body options]
    :or {method :post
         query {}
         options {}}}]
  (let [api-key (or (:api-key options) (get-env "ANTHROPIC_API_KEY"))
        auth-token (or (:auth-token options) (get-env "ANTHROPIC_AUTH_TOKEN"))
        base-url (or (:base-url options) (get-env "ANTHROPIC_BASE_URL") default-base-url)
        headers (build-headers (assoc options :api-key api-key :auth-token auth-token))
        query-params (merge query (:extra-query-params options))
        url (build-url base-url path query-params)
        merged-body (if (:extra-body-params options)
                      (merge body (:extra-body-params options))
                      body)
        response (make-request-with-retries
                  {:method method
                   :url url
                   :headers headers
                   :body merged-body
                   :max-retries (:max-retries options)
                   :initial-retry-delay (:initial-retry-delay options)
                   :max-retry-delay (:max-retry-delay options)
                   :timeout (:timeout options)}
                  0)]
    (handle-response response)))
