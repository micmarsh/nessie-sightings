(ns nessie-sightings.reddit.impl.request
  (:import org.apache.http.impl.client.HttpClientBuilder
           [com.github.jreddit.oauth
            app.RedditWebApp RedditOAuthAgent client.RedditHttpClient]
           [com.github.jreddit.parser
            single.FullSubmissionParser
            listing.SubmissionsListingParser
            listing.CommentsMoreParser]))

(def user-agent "P funk, uncut funk, the bomb")

(def redirect-url  "https://github.com/micmarsh/nessie-sightings")

(def client (RedditHttpClient. user-agent (.build (HttpClientBuilder/create))))

(defn get-token* [client-id client-secret]
  (let [reddit-app (RedditWebApp. client-id
                                  client-secret
                                  redirect-url)
        agent (RedditOAuthAgent. user-agent reddit-app)]
    (.tokenAppOnly agent false)))

(def get-token (memoize get-token*))
;; technically a 1-hour timer cache should be exactly what you need here

(defn parse [type response]
  (let [parser (case type
                 :full-submission (FullSubmissionParser.)
                 :submission-listing (SubmissionsListingParser.)
                 :comments-more (CommentsMoreParser.))]
    (.parse parser response)))

(defn get [{:keys [client-id client-secret]} request]
  (let [token (get-token client-id client-secret)]
    (.get client token request)))
