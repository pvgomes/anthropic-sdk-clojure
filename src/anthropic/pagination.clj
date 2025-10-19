(ns anthropic.pagination
  "Pagination utilities for Anthropic API"
  (:require [anthropic.core :as core]))

(defn- extract-pagination-info
  "Extract pagination information from API response"
  [response]
  {:has-more (get response :has_more false)
   :first-id (get response :first_id)
   :last-id (get response :last_id)})

(defn page-seq
  "Create a lazy sequence of pages from a paginated API endpoint

  Parameters:
  - :request-fn - Function that takes query params and returns a page
  - :initial-params - Initial query parameters map
  - :page-key - Key to extract items from response (default :data)

  Returns lazy sequence of items across all pages."
  ([request-fn]
   (page-seq request-fn {} :data))
  ([request-fn initial-params]
   (page-seq request-fn initial-params :data))
  ([request-fn initial-params page-key]
   (letfn [(fetch-pages [params]
             (lazy-seq
              (let [response (request-fn params)
                    items (get response page-key)
                    pagination (extract-pagination-info response)]
                (concat items
                        (when (:has-more pagination)
                          (fetch-pages (assoc params
                                              :after-id (:last-id pagination))))))))]
     (fetch-pages initial-params))))

(defn fetch-all
  "Fetch all items from a paginated endpoint

  Parameters:
  - :request-fn - Function that takes query params and returns a page
  - :initial-params - Initial query parameters map
  - :page-key - Key to extract items from response (default :data)

  Returns vector of all items across all pages."
  ([request-fn]
   (fetch-all request-fn {} :data))
  ([request-fn initial-params]
   (fetch-all request-fn initial-params :data))
  ([request-fn initial-params page-key]
   (vec (page-seq request-fn initial-params page-key))))

(defn page-iterator
  "Create an iterator over pages (not items)

  Parameters:
  - :request-fn - Function that takes query params and returns a page
  - :initial-params - Initial query parameters map

  Returns lazy sequence of page responses."
  ([request-fn]
   (page-iterator request-fn {}))
  ([request-fn initial-params]
   (letfn [(fetch-pages [params]
             (lazy-seq
              (let [response (request-fn params)
                    pagination (extract-pagination-info response)]
                (cons response
                      (when (:has-more pagination)
                        (fetch-pages (assoc params
                                            :after-id (:last-id pagination))))))))]
     (fetch-pages initial-params))))
