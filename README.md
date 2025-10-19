# Anthropic Clojure SDK

> [!NOTE]
> The Anthropic Clojure SDK is not **official** 
>


The Anthropic Clojure library provides convenient access to the Anthropic REST API from any Clojure application.

## Documentation

The REST API documentation can be found on [docs.anthropic.com](https://docs.anthropic.com/claude/reference/).

## Installation

Add the following to your `deps.edn`:

```clojure
{:deps {anthropic-sdk-clojure {:local/root "path/to/anthropic-sdk-clojure"}}}
```

Or add to your `project.clj` for Leiningen:

```clojure
[anthropic-sdk-clojure "0.1.0-SNAPSHOT"]
```

## Usage

### Basic Message Creation

```clojure
(require '[anthropic.client :as anthropic])

(def message
  (anthropic/create-message
    {:max-tokens 1024
     :messages [{:role "user" :content "Hello, Claude"}]
     :model "claude-sonnet-4-20250514"
     :options {:api-key (System/getenv "ANTHROPIC_API_KEY")}}))

(println (:content message))
```

### Using the Client

You can create a client with default options:

```clojure
(require '[anthropic.client :as anthropic])

(def client
  (anthropic/create-client
    {:api-key (System/getenv "ANTHROPIC_API_KEY")}))

;; Create a message
(def message
  ((:create (:messages client))
   {:max-tokens 1024
    :messages [{:role "user" :content "Hello, Claude"}]
    :model "claude-sonnet-4-20250514"}))
```

### Streaming

Stream responses using Server-Sent Events (SSE):

```clojure
(require '[anthropic.streaming :as streaming])

(let [stream (streaming/stream-messages
               {:max-tokens 1024
                :messages [{:role "user" :content "Hello, Claude"}]
                :model "claude-sonnet-4-20250514"
                :options {:api-key (System/getenv "ANTHROPIC_API_KEY")}})]
  (doseq [event (:events stream)]
    (println "Event:" (:event event))
    (println "Data:" (:data event)))
  ;; Close the stream when done
  ((:close stream)))
```

### Pagination

List methods in the Anthropic API are paginated. This library provides utilities for iterating through pages:

```clojure
(require '[anthropic.models :as models]
         '[anthropic.pagination :as pagination])

;; Get all models across all pages
(def all-models
  (pagination/fetch-all
    (fn [params]
      (models/list-models (assoc params :options {:api-key (System/getenv "ANTHROPIC_API_KEY")})))
    {}
    :data))

;; Or iterate lazily through items
(doseq [model (pagination/page-seq
                (fn [params]
                  (models/list-models (assoc params :options {:api-key (System/getenv "ANTHROPIC_API_KEY")}))))]
  (println (:id model)))
```

### Error Handling

API errors are thrown as `ex-info` exceptions with detailed information:

```clojure
(try
  (anthropic/create-message
    {:max-tokens 1024
     :messages [{:role "user" :content "Hello, Claude"}]
     :model "claude-sonnet-4-20250514"})
  (catch Exception e
    (let [data (ex-data e)]
      (case (:type data)
        :authentication (println "Authentication failed!")
        :rate-limit (println "Rate limit exceeded!")
        :not-found (println "Resource not found!")
        (println "Error:" (ex-message e))))))
```

Error types include:
- `:bad-request` - HTTP 400
- `:authentication` - HTTP 401
- `:permission-denied` - HTTP 403
- `:not-found` - HTTP 404
- `:conflict` - HTTP 409
- `:unprocessable-entity` - HTTP 422
- `:rate-limit` - HTTP 429
- `:internal-server` - HTTP >= 500
- `:api-error` - Other HTTP errors

### Retries

Certain errors are automatically retried 2 times by default with exponential backoff:
- Connection errors
- HTTP 408 (Request Timeout)
- HTTP 409 (Conflict)
- HTTP 429 (Rate Limit)
- HTTP >= 500 (Internal Server Error)

Configure retry behavior via options:

```clojure
;; Disable retries
(anthropic/create-message
  {:max-tokens 1024
   :messages [{:role "user" :content "Hello"}]
   :model "claude-sonnet-4-20250514"
   :options {:max-retries 0}})

;; Custom retry settings
(anthropic/create-message
  {:max-tokens 1024
   :messages [{:role "user" :content "Hello"}]
   :model "claude-sonnet-4-20250514"
   :options {:max-retries 5
             :initial-retry-delay 1000
             :max-retry-delay 10000}})
```

## Advanced Usage

### Beta Features

Access beta features through the beta namespace:

```clojure
(require '[anthropic.beta :as beta])

;; Create message batch
(def batch
  (beta/create-message-batch
    {:requests [{:custom_id "req-1"
                 :params {:max-tokens 1024
                          :messages [{:role "user" :content "Hello"}]
                          :model "claude-sonnet-4-20250514"}}]
     :options {:api-key (System/getenv "ANTHROPIC_API_KEY")}}))

;; Get batch status
(def status
  (beta/get-message-batch
    {:batch-id (:id batch)
     :options {:api-key (System/getenv "ANTHROPIC_API_KEY")}}))
```

### Undocumented Parameters

Send undocumented parameters using the `:extra-*` options:

```clojure
(anthropic/create-message
  {:max-tokens 1024
   :messages [{:role "user" :content "Hello"}]
   :model "claude-sonnet-4-20250514"
   :options {:extra-query-params {:my-query-param "value"}
             :extra-body-params {:my-body-param "value"}
             :extra-headers {"X-Custom-Header" "value"}}})
```

### Direct API Requests

Make requests to undocumented endpoints:

```clojure
(require '[anthropic.core :as core])

(core/request
  {:method :post
   :path "undocumented/endpoint"
   :query {:dog "woof"}
   :body {:hello "world"}
   :options {:extra-headers {"Useful-Header" "interesting-value"}}})
```

## API Reference

### Messages

- `anthropic.messages/create` - Create a message
- `anthropic.messages/count-tokens` - Count tokens in a message
- `anthropic.streaming/stream-messages` - Stream a message

### Models

- `anthropic.models/list-models` - List available models
- `anthropic.models/get-model` - Get model information

### Beta

- `anthropic.beta/create-message` - Create beta message
- `anthropic.beta/create-message-batch` - Create message batch
- `anthropic.beta/get-message-batch` - Get batch status
- `anthropic.beta/list-message-batches` - List batches
- `anthropic.beta/cancel-message-batch` - Cancel batch
- `anthropic.beta/get-batch-results` - Get batch results

### Pagination

- `anthropic.pagination/page-seq` - Lazy sequence of items across pages
- `anthropic.pagination/fetch-all` - Fetch all items from all pages
- `anthropic.pagination/page-iterator` - Iterator over page responses

## Requirements

- Clojure 1.11.1 or higher
- Java 8 or higher

## License

MIT License - see LICENSE file for details

## Contributing

This is a community implementation. Contributions are welcome! Please feel free to submit issues or pull requests.
