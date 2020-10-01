###Target Search Case Study

###### Tools used

* Gradle
* Java 11
* Elasticsearch for Indexed Search version 7.9.2

###### System configuration
* 1 TB SSD
* 6 Physical cores
* 32 GB RAM
* Mac OS

#### How to run
Files are in sample_output directory. You can enter any of the following options for search method
* 1 or String Match 
* 2 or Regular Expression 
* 3 or Indexed

##### Setting up Elasticsearch
https://www.elastic.co/guide/en/elasticsearch/reference/current/targz.html
```
tar -xzf elasticsearch-7.9.2-linux-x86_64.tar.gz
cd elasticsearch-7.9.2/ 
```
###### Setup JVM memory
* Copy tbe jvm_options attached in the config directory config/jvm.options 
* Copy the Elasticsearch yml file attached in the config/elasticsearch.yml

##### Running Elasticsearch
```
cd elasticsearch-7.9.2
./bin/elasticsearch
```
##### Check if the Elasticsearch node started
```
http://localhost:9200
```
#### Running the project using gradle
```
gradle clean build run
gradle clean build runMainJar 
```
##### Executing performance tests using gradle
```
gradle build runTestJar --args "'String Match'"
gradle build runTestJar --args 1
```
```
gradle build runTestJar --args "'Regular Expression'"
gradle build runTestJar --args 2
```
```
gradle build runTestJar --args "Indexed"
gradle build runTestJar --args 3
```
#### Running tests for 2M searches

##### Settings needed for Indexed Search
In Unix
```
vi /etc/security/limits.conf
* soft    nofile      65536
* hard    nofile      65536
* soft    nproc        4096
* hard    nproc        4096
*   -     fsize       unlimited
*   -        as       unlimited
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
7. Setup Prometheus to send metrics from CPU, RAM, Disk and network.  I would also setup health checks for the      elasticsearch cluster
8. Add clusters in multiple regions and setup cross cluster replication.
9. Setup circuit breakers available in elasticsearch to prevent OutOfMemory exceptions
10. Enable caching of queries where applicable. If we are expecting more searches than writes, we should allocate   enough RAM cache
    ###### Types of cache:
    1. Node query cache for filtered results of term query
    2. Shard cache for storing results for local searches
11. Configure keep-alive TCP settings transport.ping_schedule to less than timeout. Test and set it.
12. Setup thread pool configuration for search, write, merge and refresh and fetch shards operations. This is can be setup based on the configuration of hardware and testing with large data set.
13. OS will retransmit any lost messages a number of times before informing the sender of any problem.  Retransmissions back off exponentially. So reduce the retries
     ```
      sysctl -w net.ipv4.tcp_retries2=5
     ```
 14. Requesting the JVM to lock the heap in memory through mlockall to avoid swapping pages to the disk
 15. Increase the nproc since elasticsearch run many threads. At the minimum you need 4096 as limit for the number of processes. 
 16. Increase the number of file descriptors.
 17. Increase the file size to unlimited to allow large transaction logs.
 18. Elasticsearch also requires the ability to create many memory-mapped areas. The maximum map count check checks that the kernel allows a process to have at least 262,144 memory-mapped areas
     ```
      sysctl -w vm.max_map_count=262144
     ```
 19. Increase the addressable space to allow memory mapped files. Elasticsearch uses it address space quickly and improve performance.  This keeps the index data off the JVM heap but in memory for blazing fast access.
 20. How many half-open connections for which the client has not yet sent an ACK response can be kept in the queue (source)
   ```
    sysctl -w net.ipv4.tcp_max_syn_backlog=65536      
   ``` 
 21. The maximum number of connections that can be queued for acceptance
     ```
      sysctl -w net.core.somaxconn=32768
     ```
 22. The maximum number of packets in the receive queue that passed through the network interface and are waiting to be processed by the kernel.
     ```
     sysctl -w net.core.netdev_max_backlog=32768
     ```
 23. Set swappiness to avoid swapping too early.   
     ```
      vm.swappiness = 20
     ```
 24. vm.dirty_ratio is percentage of system memory which when dirty, the process doing writes would block and write out dirty pages to the disks.
      ```
       vm.dirty_ratio = 40
      ```
 25. vm.dirty_background_ratio is the percentage of system memory which when dirty then system can start writing data to the disks.
      ```
       vm.dirty_background_ratio = 10
      ```
 26. Enable snapshotting to elasticsearch S3 buckets to save the data that in indexed to replay it when there are major outages/failures and the cluster node cannot be brought back.