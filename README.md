# Prometheus Exporter Plugin for ElasticSearch

This is a builtin exporter from ElasticSearch to Prometheus.
It collects all relevant metrics and make them available to Prometheus via ElasticSearch REST API.

**Current available metrics are :**

- Cluster status
- Nodes status :
    - JVM
    - Indices (global)
    - Transport
    - HTTP
    - Scripts
    - Process
    - Operating System
    - File System
    - Circuit Breaker
- Indices status

## Compatibility matrix

### Version 5.X

| Elasticsearch  | Plugin         | Release date |
| -------------- | -------------- | ------------ |
| 5.6.1          | 5.6.1.0        | Sep 19, 2017 |
| 5.6.0          | 5.6.0.0        | Sep 13, 2017 |
| 5.5.2          | 5.5.2.0        | Aug 18, 2017 |
| 5.5.1          | 5.5.1.0        | Jul 26, 2017 |
| 5.5.0          | 5.5.0.0        | Jul 07, 2017 |
| 5.4.3          | 5.4.3.0        | Jun 28, 2017 |
| 5.4.2          | 5.4.2.0        | Jun 22, 2017 |
| 5.4.1          | 5.4.1.0        | Jun 08, 2017 |
| 5.4.0          | 5.4.0.0        | May 04, 2017 |
| 5.3.2          | 5.3.2.0        | Apr 28, 2017 |
| 5.3.1          | 5.3.1.0        | Apr 24, 2017 |
| 5.3.0          | 5.3.0.0        | Mar 29, 2017 |
| 5.2.2          | 5.2.2.0        | Mar 03, 2017 |
| 5.2.1          | 5.2.1.0        | Feb 17, 2017 |
| 5.2.0          | 5.2.0.0        | Feb 03, 2017 |
| 5.1.2          | 5.1.2.0        | Jan 12, 2017 |
| 5.1.1          | 5.1.1.0        | Dev 29, 2016 |
| 5.0.1          | 5.0.1.0        | Nov 15, 2016 |
| 5.0.0          | 5.0.0.0        | Nov 14, 2016 |

### Version 2.X

| Elasticsearch  | Plugin         | Release date |
| -------------- | -------------- | ------------ |
| 2.4.1          | 2.4.1.0        | Sep 29, 2016 |
| 2.4.0          | 2.4.0.0        | Sep 01, 2016 |
| 2.3.5          | 2.3.5.5        | Aug 29, 2016 |
| 2.3.5          | 2.3.5.4        | Aug 24, 2016 |
| 2.3.5          | 2.3.5.3        | Aug 23, 2016 |
| 2.3.5          | 2.3.5.2        | Aug 23, 2016 |
| 2.3.5          | 2.3.5.1        | Aug 16, 2016 |
| 2.3.5          | 2.3.5.0        | Aug 04, 2016 |
| 2.3.4          | 2.3.4.2        | Aug 02, 2016 |
| 2.3.4          | 2.3.4.1        | Aug 01, 2016 |
| 2.3.4          | 2.3.4.0        | Jul 30, 2016 |

## Install

- Since ElasticSearch 5.0.0 :
    ./bin/elasticsearch-plugin install -b https://distfiles.compuscene.net/elasticsearch/elasticsearch-prometheus-exporter-5.6.1.0.zip

- On old 2.x.x versions :
    ./bin/plugin install https://github.com/vvanholl/elasticsearch-prometheus-exporter/releases/download/2.4.1.0/elasticsearch-prometheus-exporter-2.4.1.0.zip

**Do not forget to restart the node after installation !**

Note that the plugin needs special permissions :

- java.lang.RuntimePermission accessClassInPackage.sun.misc
- java.lang.RuntimePermission accessDeclaredMembers
- java.lang.reflect.ReflectPermission suppressAccessChecks

If you have a lot of indices and think this data is irelevant, you can disable in the main configuration file:

```
prometheus.indices: false
```

## Uninstall

- Since ElasticSearch 5.0.0 :
    ./bin/elasticsearch-plugin remove prometheus-exporter

- On old 2.x.x versions :
    ./bin/plugin remove prometheus-exporter

Do not forget to restart the node after installation !

## Usage

Metrics are directly available at address :

    http://<your_server_address>:9200/_prometheus/metrics

As a sample result, you get :

```
# HELP es_process_mem_total_virtual_bytes Memory used by ES process
# TYPE es_process_mem_total_virtual_bytes gauge
es_process_mem_total_virtual_bytes{cluster="develop",node="develop01",} 3.626733568E9
# HELP es_indices_indexing_is_throttled_bool Is indexing throttling ?
# TYPE es_indices_indexing_is_throttled_bool gauge
es_indices_indexing_is_throttled_bool{cluster="develop",node="develop01",} 0.0
# HELP es_jvm_gc_collection_time_seconds Time spent for GC collections
# TYPE es_jvm_gc_collection_time_seconds counter
es_jvm_gc_collection_time_seconds{cluster="develop",node="develop01",gc="old",} 0.0
es_jvm_gc_collection_time_seconds{cluster="develop",node="develop01",gc="young",} 0.0
# HELP es_indices_requestcache_memory_size_bytes Memory used for request cache
# TYPE es_indices_requestcache_memory_size_bytes gauge
es_indices_requestcache_memory_size_bytes{cluster="develop",node="develop01",} 0.0
# HELP es_indices_search_open_contexts_number Number of search open contexts
# TYPE es_indices_search_open_contexts_number gauge
es_indices_search_open_contexts_number{cluster="develop",node="develop01",} 0.0
# HELP es_jvm_mem_nonheap_used_bytes Memory used apart from heap
# TYPE es_jvm_mem_nonheap_used_bytes gauge
es_jvm_mem_nonheap_used_bytes{cluster="develop",node="develop01",} 5.5302736E7

...
```

### Configure Prometheus target

On your Prometheus servers, configure a new job as usual.

For example, if you have a cluster of 3 nodes :

```
-   job_name: elasticsearch
    scrape_interval: 10s
    metrics_path: "/_prometheus/metrics"
    static_configs:
    - targets:
      - node1:9200
      - node1:9200
      - node3:9200
```

Of course, you could use a service discovery service instead of a static config.

Just keep in mind that metrics_path must be `/_prometheus/metrics` otherwise Prometheus will find no metric.

## Project sources

The Maven project site is available at [Github](https://github.com/vvanholl/elasticsearch-prometheus-exporter)

## Credits

This plugin mainly use the [Prometheus JVM Client](https://github.com/prometheus/client_java).

## License

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
