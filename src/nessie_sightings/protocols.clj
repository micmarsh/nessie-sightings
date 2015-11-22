(ns nessie-sightings.protocols)

(defprotocol GetSubmissions
  (submissions
    [reddit subreddit]
    [reddit subreddit query]
    "Query for submissions to the given subreddit. Query options:
      {:sort <:hot, :new>
       :limit <integer>
       :offset <integer>}"))

(defprotocol GetComments
  (comments
    [reddit entity]
    "Query an entity (comment or submission) for comments."))

(defprotocol NextComments
  (next-comments [reddit parent-sub more]
    "Given a ref (a More in jReddit), load up some more comments"))

(defprotocol CommentText
  (comment-text [this]))
