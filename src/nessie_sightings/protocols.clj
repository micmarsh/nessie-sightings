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
    [entity]
    [entity query]
    "Query an entity (comment or submission) for comments.
     Query options the same as above unless I discover that's not possible or desirable."))

(defprotocol NextComments
  (next-comments [reddit ref]
    "Given a ref (a More in jReddit), load up some more comments"))

(defprotocol CommentText
  (comment-text [this]))
