(ns nessie-sightings.reddit
  (:refer-clojure :exclude [mapcat])
  (:require [nessie-sightings.protocols :as p]
            [mutils.seq.lazy :refer [mapcat]])
  (:import org.apache.http.impl.client.HttpClientBuilder
           [com.github.jreddit.oauth
            app.RedditWebApp RedditOAuthAgent client.RedditHttpClient]
           [com.github.jreddit.request.retrieval
            param.SubmissionSort
            submissions.SubmissionsOfSubredditRequest
            mixed.FullSubmissionRequest
            comments.MoreCommentsRequest]
           [com.github.jreddit.parser
            single.FullSubmissionParser
            listing.SubmissionsListingParser
            listing.CommentsMoreParser
            entity.imaginary.FullSubmission
            entity.Comment
            entity.More]))

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

(def sort-enum
  {:hot SubmissionSort/HOT
   :new SubmissionSort/NEW})

(defn create-submission-request [subreddit {:keys [sort limit]}]
  {:pre [(not (nil? sort))]}
  (cond-> (SubmissionsOfSubredditRequest. subreddit (sort-enum sort))
          limit (.setLimit limit)))

(defn submission->full-submission [token submission]
  (let [request (FullSubmissionRequest. submission)
        parse (FullSubmissionParser.)]
    (.parse parse (.get client token request))))

(def more? (partial instance? More))

(defn comments-response [comments]
  (let [more (first (filter more? comments))]
    {:comments (remove more? comments)
     :more more}))

(defprotocol ^:private Token (token [client]))

(defrecord RedditClient [client-id client-secret]
  Token
  (token [this]
    (get-token client-id client-secret))
  p/GetSubmissions
  (submissions [this subreddit]
    (p/submissions this subreddit {:sort :hot}))
  (submissions [this subreddit query]
    (let [parser (SubmissionsListingParser.)
          request (create-submission-request subreddit query)]
      (->> request
           (.get client (token this))
           (.parse parser))))
  p/GetComments
  (comments [this submisssion]
    (->> submisssion
         (submission->full-submission (token this))
         (.getCommentTree)))
  p/NextComments
  (next-comments [this submission more]
    (let [full-name (.getFullName submission)
          identifiers (.getChildren more)
          request (MoreCommentsRequest. full-name identifiers)]
      (-> (CommentsMoreParser.)
          (.parse (.get client (token this) request))))))

(extend-protocol p/CommentText
  Comment
  (comment-text [this] (.getBody this)))

(defn all-submission-comments
  ([reddit submission]
     (lazy-seq
      (all-submission-comments reddit submission (p/comments reddit submission))))
  ([reddit submission comments]
     (lazy-seq
      (let [{:keys [comments more]} (comments-response comments)]
        (concat comments
                (when more
                  (let [comments (p/next-comments reddit submission more) ]
                   (all-submission-comments reddit submission comments))))))))


