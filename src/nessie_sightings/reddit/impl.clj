(ns nessie-sightings.reddit.impl
  (:refer-clojure :exclude [mapcat])
  (:require [nessie-sightings.protocols :as p]
            [mutils.seq.lazy :refer [mapcat]]
            [nessie-sightings.reddit.impl.request :as req])
  (:import [com.github.jreddit.request.retrieval
            param.SubmissionSort
            submissions.SubmissionsOfSubredditRequest
            mixed.FullSubmissionRequest
            comments.MoreCommentsRequest]
           [com.github.jreddit.parser
            entity.imaginary.FullSubmission
            entity.Comment
            entity.More]))

(def sort-enum
  {:hot SubmissionSort/HOT
   :new SubmissionSort/NEW})

(defn create-submission-request [subreddit {:keys [sort limit]}]
  {:pre [(not (nil? sort))]}
  (cond-> (SubmissionsOfSubredditRequest. subreddit (sort-enum sort))
          limit (.setLimit limit)))

(def more? (partial instance? More))

(defn comments-response [comments]
  (let [more (first (filter more? comments))]
    {:comments (remove more? comments)
     :more more}))

(defrecord RedditClient [client-id client-secret]
  p/GetSubmissions
  (submissions [this subreddit]
    (p/submissions this subreddit {:sort :hot}))
  (submissions [this subreddit query]    
    (->> (create-submission-request subreddit query)
         (req/get this)
         (req/parse :submission-listing)))
  p/GetComments
  (comments [this submisssion]
    (->> submisssion
         (FullSubmissionRequest.)
         (req/get this)
         (req/parse :full-submission)
         ;; TODO deal with the reflection here
         (.getCommentTree)))
  p/NextComments
  (next-comments [this submission more]
    (let [full-name (.getFullName submission)
          identifiers (.getChildren more)]
      (->> (MoreCommentsRequest. full-name identifiers)
           (req/get this)
           (req/parse :comments-more)))))

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
