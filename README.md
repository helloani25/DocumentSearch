zip file containing all necessary build steps and dependencies

Provide a README.md file with instructions for testing, running and interacting with your application and any details 
you feel are relevant to share

Recommendations to make your solution suitable for use in a production environment 

Functional implementation of the problem with associated tests

Run a performance test that does 2M searches with random search terms, and measures execution time. Which approach is fastest? Why?

Provide some thoughts on what you would do on the software or hardware side to make this program scale to handle massive content and/or 
very large request volume (5000 requests/second or more).

use Kafka or Logstash with dead letter queue to ingest the files into Elasticsearch 

log4j

logging drivers for docker containers - syslog or journald log to look for performance issues and trigger alerts.

Have a cluster of docker containers to support Elasticsearch with the heap size of 32GB. Set the queuesize for the pool of threads for search or index to 2000.
Have multiple shards for writing to the index and then merge and have shards of sizes between 20 - 40 GB and create multiple indices for time series data
primary shard count = number of docker containers
replicas shards = 2 for each primary shard



set filename index to false

searchAfter for large number of documents to paginate