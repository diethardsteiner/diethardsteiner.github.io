#!/bin/bash

# delete index if already exists
  curl -XDELETE 'http://localhost:9200/tweets'
  curl -XDELETE 'http://localhost:9200/tweetsbylanguage'
# create index
  curl -XPUT 'http://localhost:9200/tweets'
  curl -XPUT 'http://localhost:9200/tweetsbylanguage'
# create mapping for lowest granularity data
curl -XPUT 'http://localhost:9200/tweets/_mapping/partition1' -d'
{
  "partition1" : {
    "properties" : {
    "id": {"type": "long"}
    , "creationTime": {"type": "date"}
    , "language": {"type": "string", "index": "not_analyzed"}
    , "user": {"type": "string"}
    , "favoriteCount": {"type": "integer"}
    , "retweetCount": {"type": "integer"}
    , "count": {"type": "integer"}
  }
  }
}'

curl -XPUT 'http://localhost:9200/tweets/_settings' -d '{
    "index" : {
        "refresh_interval" : "5s"
    }
}'
# create mapping for tweetsByLanguage
curl -XPUT 'http://localhost:9200/tweetsbylanguage/_mapping/partition1' -d'
{
  "partition1" : {
    "properties" : {
    "language": {"type": "string", "index": "not_analyzed"}
    , "windowStartTime": {"type": "date"}
    , "windowEndTime": {"type": "date"}
    , "countTweets": {"type": "integer"}
  }
  }
}'

curl -XPUT 'http://localhost:9200/tweetsbylanguage/_settings' -d '{
    "index" : {
        "refresh_interval" : "5s"
    }
}'

# Retrieve entries
# curl -XGET 'http://localhost:9200/tweets/partition1/_search?pretty'
# curl -XGET 'http://localhost:9200/tweetsbylanguage/partition1/_search?pretty'

# Retrieve storage details
# curl 'localhost:9200/_cat/indices?v'