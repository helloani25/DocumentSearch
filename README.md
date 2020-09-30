###Target Search Case Study

###### Tools used

1. Gradle
2. Java 11
3. Elasticsearch for Indexed Search version 7.9.2

###### System configuration
1. 1 TB SSD
2. 6 Physical cores
3. 32 GB RAM
4. Mac OS

#### How to run
Files are in sample_output directory
```
gradle clean build run
 ```
##### Running the jar
```
gradle build runMainJar
```
##### Executing performance tests
```
gradle build runTestJar 
```
##### Setting up Elasticsearch
tar -xzvf elasticsearch-7.9.2.tar.gz  Attached with the project

##### Running Elasticsearch
```
cd elasticsearch-7.9.2
./bin/elasticsearch
```


#### Running tests for 2M searches

##### Settings needed for Indexed Search
In Unix
```
vi /etc/security/limits.conf
* soft nofile 65536
* hard nofile 65536
```
In Mac OS
```
sudo launchctl limit maxfiles 65536 65536
```

##### TCP Connection states
During the lifetime of a request, each port goes through a series of states, from SYN_SENT when establishing a connection to ESTABLISHED when communication is actively happening, through a series of closing states eventually culminating in TIME_WAIT after the port has been closed.

During TIME_WAIT the port is held in limbo to ensure any remaining packets are not erroneously provided to a fresh connection. Duration of the TIME_WAIT state is the Maximum Segment Lifetime and is defined in net.inet.tcp.msl
```
sudo sysctl -w net.inet.tcp.msl=2000
```
##### Increase port ranges
```
sudo sysctl -w net.inet.ip.portrange.first=16384
```
##### Reduce the TCP idle time for connections, number of probes and intervals for checking if alive
```
sysctl -w net.inet.tcp.keepidle=180000 net.inet.tcp.keepcnt=3 net.inet.tcp.keepintvl=10000
```
#### Approaches to Search

##### Simple Search

This approach tokenizes all the words in the sentences and puts in an Array.  When a phrase is search, it's split into tokens as well and searched in the array. There is a match if the words in the phrase appear in consecutive locations in the list.  All the files are stored in a Map with the key as the filename and value as the contents of the file.

This search performs fairly well in comparison to the Regex search. The only downside is that the files has to be preprocessed and all the words of every file must reside in the memory. Since there is no pre-computed data available for faster searches, any search involving prefix, words in the phrase with distance between them or synonyms or stemming words (ran, run and running) would not match. This could make our search slower with the simple search

##### Regex Search

This approach does not tokenize or preprocess the text.  Rather regular expressions are used to search for the word or the phrase. The downside of this approach is that it does not scale well. So every search results in all the files being read and matched. It's computationally very expensive and intensive in memory as well.

##### Indexed Search Elasticsearch

Elasticsearch maintains invertex indexes and index terms. Since it's precomputed, finding a phrase or a word is as simple as and operation for all the terms 

![Alt text](inverted-index.svg?raw=true "Title")

Modifying our search to include synonyms, stemmers or words with distance between them is much easier since we have indexed our documents. Further we can cache results of our queries. If we use filters these are cached as well at shard level. So in a real world application, searches are not truly random. But rather there are products or words/phrases that are trending and get searched most often.
#### Recommendations for Production
Indexed Search
1. Have a cluster of multilple instances of Elasticsearch with the heap size of 32GB. Set the queues ize for the pool of threads for search or index to 2000.
2. Have multiple shards for writing to the index and then merge segments when there responses are slow. 
    1. have shards of sizes between 20 - 40 GB 
    2. create multiple indices based on category of search
    3. primary shard count = number of instances of elasticsearch
    4. replicas shards = 2 for each primary shard
    5. Have dedicated 3 <b>master nodes</b> of 8 GB RAM, 2 TB of SSD and 2 NICs and 8 CPU cores
    6. Have atleast 6 Data Nodes 32GB RAM allocated for elasticsearch with machines 20 TB SSDs with NVMe Disks and 12 core CPUs and 128GB RAM.  Have atleast 4-8 NICs
3. Run elasticsearch as a daemon process
    ```
    ./bin/elasticsearch -d -p pid
    ```
4. Setup Certificate-Based Mutual Authentication to between the services (Elasticsearch and the Application)
5. Load balance the cluster 
6. Setup loggers like syslog or journald and setup watcher to monitor logs.
7. Setup Prometheus to send metrics from CPU, RAM, Disk and network.  I would also setup health checks for the elasticsearch cluster
8. Add clusters in multiple regions and setup cross cluster replication.
9. Setup circuit breakers available in elasticsearch to prevent OutOfMemory exceptions
10. 

#####

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

logging drivers - syslog or journald log to look for performance issues and trigger alerts.





set filename index to false

searchAfter for large number of documents to paginate

production username and password with TLS



```