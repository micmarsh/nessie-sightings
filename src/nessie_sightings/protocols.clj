(ns nessie-sightings.protocols)

(defprotocol GetSubmissions
  (submissions
    [this subreddit]
    [this subreddit query]
    "Query for submissions to the given subreddit. Query options:
      {:sort <:hot, :new>
       :limit <integer>
       :offset <integer>}"))

(defprotocol GetComments
  (comments
    [this]
    [this query]
    "Query an entity (comment or submission) for comments.
     Query options the same as above unless I discover that's not possible."))

(defprotocol CommentText
  (comment-text [this]))
