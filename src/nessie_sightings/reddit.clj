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

(defn comments-response [comments]
  (let [more (last comments)]
    (if (instance? More more)
      {:comments (butlast comments)
       :more more}
      {:comments comments})))

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
           (.parse parser)
           (map (partial submission->full-submission (token this))))))
  p/NextComments
  (next-comments [this {:keys [submission more]}]
    (let [full-name (-> submission .getSubmission .getFullName)
          identifiers (.getChildren more)
          request (MoreCommentsRequest. full-name identifiers)]
      (-> (CommentsMoreParser.)
          (.parse (.get client (token this) request))
          (comments-response)))))

(def texts
  (comp (filter (partial satisfies? p/CommentText))
        (map p/comment-text)))

(extend-protocol p/GetComments
  FullSubmission
  (comments
    ([submission]
       (comments-response (.getCommentTree submission)))
    ([submission _]
       (p/comments submission))))

(extend-protocol p/CommentText
  Comment
  (comment-text [this] (.getBodyHTML this)))

(defn- recur-comments-query
  [reddit submission {:keys [comments more]}]
  (concat comments
          (when more
            (all-submission-comments reddit submission more))))

(defn all-submission-comments
  ([reddit submission]
     (let [comments-result (p/comments submission)]
       (recur-comments-query reddit submission comments-result)))
  ([reddit submission more]
     (let [query {:submission submission :more more}
           comments-result (p/next-comments reddit query)]
       (recur-comments-query reddit submission comments-result)))) 

(defn comments-seq
  "reddit client -> subreddit name -> [{:submission <submission>
                                        :comment <comment>}]"
  [reddit subreddit]
  (mapcat
   (partial all-submission-comments reddit)
   (p/submissions reddit subreddit)))
