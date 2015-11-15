(ns nessie-sightings.reddit
  (:require [nessie-sightings.protocols :as p])
  (:import org.apache.http.impl.client.HttpClientBuilder
           [com.github.jreddit.oauth
            app.RedditWebApp RedditOAuthAgent client.RedditHttpClient]
           [com.github.jreddit.request.retrieval
            param.SubmissionSort
            submissions.SubmissionsOfSubredditRequest
            mixed.FullSubmissionRequest]
           [com.github.jreddit.parser
            single.FullSubmissionParser
            listing.SubmissionsListingParser
            entity.imaginary.FullSubmission]))

(def user-agent "P funk, uncut funk, the bomb")

(def redirect-url  "https://github.com/micmarsh/nessie-sightings")

(def client (RedditHttpClient. user-agent (.build (HttpClientBuilder/create))))

(defn get-token [client-id client-secret]
  (let [reddit-app (RedditWebApp. client-id
                                  client-secret
                                  redirect-url)
        agent (RedditOAuthAgent. user-agent reddit-app)]
    (.tokenAppOnly agent false)))

(def sort-enum
  {:hot SubmissionSort/HOT
   :new SubmissionSort/NEW})

(defn create-subreddit-request [subreddit {:keys [sort limit]}]
  {:pre [(not (nil? sort))]}
  (cond-> (SubmissionsOfSubredditRequest. subreddit (sort-enum sort))
          limit (.setLimit limit)))


(defn submission->full-submission [token submission]
  (let [request (FullSubmissionRequest. submission)
        parse (FullSubmissionParser.)]
    (.parse parse (.get client token request))))

(defrecord SubmissionsQueries [client-id client-secret]
  p/GetSubmissions

  (submissions [this subreddit]
    (p/submissions this subreddit {:sort :hot}))
  
  (submissions [this subreddit query]
    (let [parser (SubmissionsListingParser.)
          request (create-subreddit-request subreddit query)
          token (get-token client-id client-secret)]
      (->> request
           (.get client token)
           (.parse parser)
           (map (partial submission->full-submission token))))))

(extend-type FullSubmission
  p/GetComments
  (comments
    ([submission]
       (.getCommentTree submission))
    ([submission _]
       (p/comments submission))))
