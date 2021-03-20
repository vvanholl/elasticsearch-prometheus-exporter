![CI](https://github.com/vvanholl/elasticsearch-prometheus-exporter/workflows/CI/badge.svg?branch=master)

# Prometheus Exporter Plugin for Elasticsearch

This is a builtin exporter from Elasticsearch to Prometheus.
It collects all relevant metrics and makes them available to Prometheus via the Elasticsearch REST API.

**Currently, the available metrics are:**

- Cluster status
- Nodes status:
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
- Cluster settings (selected [disk allocation settings](https://www.elastic.co/guide/en/elasticsearch/reference/master/disk-allocator.html) only)

## Metrics
|Name|Type|Description|
|--|--|--|
|es_circuitbreaker_estimated_bytes|gauge|Circuit breaker estimated size|
|es_circuitbreaker_limit_bytes|gauge|Circuit breaker size limit|
|es_circuitbreaker_overhead_ratio|gauge|Circuit breaker overhead ratio|
|es_circuitbreaker_tripped_count|gauge|Circuit breaker tripped count|
|es_cluster_datanodes_number|gauge|Number of data nodes in the cluster|
|es_cluster_inflight_fetch_number|gauge|Number of in flight fetches|
|es_cluster_is_timedout_bool|gauge|Is the cluster timed out ?|
|es_cluster_nodes_number|gauge|Number of nodes in the cluster|
|es_cluster_pending_tasks_number|gauge|Number of pending tasks|
|es_cluster_routing_allocation_disk_threshold_enabled|gauge|Disk allocation decider is enabled|
|es_cluster_routing_allocation_disk_watermark_flood_stage_bytes|gauge|Flood stage for disk usage in bytes|
|es_cluster_routing_allocation_disk_watermark_flood_stage_pct|gauge|Flood stage watermark for disk usage in pct|
|es_cluster_routing_allocation_disk_watermark_high_bytes|gauge|High watermark for disk usage in bytes|
|es_cluster_routing_allocation_disk_watermark_high_pct|gauge|High watermark for disk usage in pct|
|es_cluster_routing_allocation_disk_watermark_low_bytes|gauge|Low watermark for disk usage in bytes|
|es_cluster_routing_allocation_disk_watermark_low_pct|gauge|Low watermark for disk usage in pct|
|es_cluster_shards_active_percent|gauge|Percent of active shards|
|es_cluster_shards_number|gauge|Number of shards|
|es_cluster_status|gauge|Cluster status|
|es_cluster_task_max_waiting_time_seconds|gauge|Max waiting time for tasks|
|es_fs_io_total_operations|gauge|Total IO operations|
|es_fs_io_total_read_bytes|gauge|Total IO read bytes|
|es_fs_io_total_read_operations|gauge|Total IO read operations|
|es_fs_io_total_write_bytes|gauge|Total IO write bytes|
|es_fs_io_total_write_operations|gauge|Total IO write operations|
|es_fs_least_usage_free_bytes|gauge|Free disk space for least used mountpoint|
|es_fs_least_usage_total_bytes|gauge|Total disk space for least used mountpoint|
|es_fs_most_usage_free_bytes|gauge|Free disk space for most used mountpoint|
|es_fs_most_usage_total_bytes|gauge|Total disk space for most used mountpoint|
|es_fs_path_available_bytes|gauge|Available disk space|
|es_fs_path_free_bytes|gauge|Free disk space|
|es_fs_path_total_bytes|gauge|Total disk space|
|es_fs_total_available_bytes|gauge|Available disk space for all mount points|
|es_fs_total_free_bytes|gauge|Free disk space for all mountpoints|
|es_fs_total_total_bytes|gauge|Total disk space for all mount points|
|es_http_open_server_number|gauge|Number of open server connections|
|es_http_open_total_count|gauge|Count of opened connections|
|es_index_completion_size_bytes|gauge|Size of completion suggest statistics|
|es_index_doc_deleted_number|gauge|Number of deleted documents|
|es_index_doc_number|gauge|Total number of documents|
|es_index_fielddata_evictions_count|gauge|Count of evictions in field data cache|
|es_index_fielddata_memory_size_bytes|gauge|Memory usage of field date cache|
|es_index_flush_total_count|gauge|Count of flushes|
|es_index_flush_total_time_seconds|gauge|Total time spent while flushes|
|es_index_get_count|gauge|Count of get commands|
|es_index_get_current_number|gauge|Current rate of get commands|
|es_index_get_exists_count|gauge|Count of existing documents when get command|
|es_index_get_exists_time_seconds|gauge|Time spent while existing documents get command|
|es_index_get_missing_count|gauge|Count of missing documents when get command|
|es_index_get_missing_time_seconds|gauge|Time spent while missing documents get command|
|es_index_get_time_seconds|gauge|Time spent while get commands|
|es_index_indexing_delete_count|gauge|Count of documents deleted|
|es_index_indexing_delete_current_number|gauge|Current rate of documents deleted|
|es_index_indexing_delete_time_seconds|gauge|Time spent while deleting documents|
|es_index_indexing_index_count|gauge|Count of documents indexed|
|es_index_indexing_index_current_number|gauge|Current rate of documents indexed|
|es_index_indexing_index_failed_count|gauge|Count of failed to index documents|
|es_index_indexing_index_time_seconds|gauge|Time spent while indexing documents|
|es_index_indexing_is_throttled_bool|gauge|Is indexing throttling ?|
|es_index_indexing_noop_update_count|gauge|Count of noop document updates|
|es_index_indexing_throttle_time_seconds|gauge|Time spent while throttling|
|es_index_merges_current_docs_number|gauge|Current rate of documents merged|
|es_index_merges_current_number|gauge|Current rate of merges|
|es_index_merges_current_size_bytes|gauge|Current rate of bytes merged|
|es_index_merges_total_auto_throttle_bytes|gauge|Bytes merged while throttling|
|es_index_merges_total_docs_count|gauge|Count of documents merged|
|es_index_merges_total_number|gauge|Count of merges|
|es_index_merges_total_size_bytes|gauge|Count of bytes of merged documents|
|es_index_merges_total_stopped_time_seconds|gauge|Time spent while merge process stopped|
|es_index_merges_total_throttled_time_seconds|gauge|Time spent while merging when throttling|
|es_index_merges_total_time_seconds|gauge|Time spent while merging|
|es_index_querycache_cache_count|gauge|Count of queries in cache|
|es_index_querycache_cache_size_bytes|gauge|Query cache size|
|es_index_querycache_evictions_count|gauge|Count of evictions in query cache|
|es_index_querycache_hit_count|gauge|Count of hits in query cache|
|es_index_querycache_memory_size_bytes|gauge|Memory usage of query cache|
|es_index_querycache_miss_number|gauge|Count of misses in query cache|
|es_index_querycache_total_number|gauge|Count of usages of query cache|
|es_index_recovery_current_number|gauge|Current number of recoveries|
|es_index_recovery_throttle_time_seconds|gauge|Time spent while throttling recoveries|
|es_index_refresh_listeners_number|gauge|Number of refresh listeners|
|es_index_refresh_total_count|gauge|Count of refreshes|
|es_index_refresh_total_time_seconds|gauge|Time spent while refreshes|
|es_index_replicas_number|gauge|Number of replicas|
|es_index_requestcache_evictions_count|gauge|Number of evictions in request cache|
|es_index_requestcache_hit_count|gauge|Number of hits in request cache|
|es_index_requestcache_memory_size_bytes|gauge|Memory used for request cache|
|es_index_requestcache_miss_count|gauge|Number of misses in request cache|
|es_index_search_fetch_count|gauge|Count of search fetches|
|es_index_search_fetch_current_number|gauge|Current rate of search fetches|
|es_index_search_fetch_time_seconds|gauge|Time spent while search fetches|
|es_index_search_open_contexts_number|gauge|Number of search open contexts|
|es_index_search_query_count|gauge|Count of search queries|
|es_index_search_query_current_number|gauge|Current rate of search queries|
|es_index_search_query_time_seconds|gauge|Time spent while search queries|
|es_index_search_scroll_count|gauge|Count of search scrolls|
|es_index_search_scroll_current_number|gauge|Current rate of search scrolls|
|es_index_search_scroll_time_seconds|gauge|Time spent while search scrolls|
|es_index_segments_memory_bytes|gauge|Memory used by segments|
|es_index_segments_number|gauge|Current number of segments|
|es_index_shards_number|gauge|Number of shards|
|es_index_status|gauge|Index status|
|es_index_store_size_bytes|gauge|Store size of the indices in bytes|
|es_index_suggest_count|gauge|Count of suggests|
|es_index_suggest_current_number|gauge|Current rate of suggests|
|es_index_suggest_time_seconds|gauge|Time spent while making suggests|
|es_index_translog_operations_number|gauge|Current number of translog operations|
|es_index_translog_size_bytes|gauge|Translog size|
|es_index_translog_uncommitted_operations_number|gauge|Current number of uncommitted translog operations|
|es_index_translog_uncommitted_size_bytes|gauge|Translog uncommitted size|
|es_index_warmer_count|gauge|Counter of warmers|
|es_index_warmer_current_number|gauge|Current number of warmer|
|es_index_warmer_time_seconds|gauge|Time spent during warmers|
|es_indices_completion_size_bytes|gauge|Size of completion suggest statistics|
|es_indices_doc_deleted_number|gauge|Number of deleted documents|
|es_indices_doc_number|gauge|Total number of documents|
|es_indices_fielddata_evictions_count|gauge|Count of evictions in field data cache|
|es_indices_fielddata_memory_size_bytes|gauge|Memory usage of field date cache|
|es_indices_flush_total_count|gauge|Count of flushes|
|es_indices_flush_total_time_seconds|gauge|Total time spent while flushes|
|es_indices_get_count|gauge|Count of get commands|
|es_indices_get_current_number|gauge|Current rate of get commands|
|es_indices_get_exists_count|gauge|Count of existing documents when get command|
|es_indices_get_exists_time_seconds|gauge|Time spent while existing documents get command|
|es_indices_get_missing_count|gauge|Count of missing documents when get command|
|es_indices_get_missing_time_seconds|gauge|Time spent while missing documents get command|
|es_indices_get_time_seconds|gauge|Time spent while get commands|
|es_indices_indexing_delete_count|gauge|Count of documents deleted|
|es_indices_indexing_delete_current_number|gauge|Current rate of documents deleted|
|es_indices_indexing_delete_time_seconds|gauge|Time spent while deleting documents|
|es_indices_indexing_index_count|gauge|Count of documents indexed|
|es_indices_indexing_index_current_number|gauge|Current rate of documents indexed|
|es_indices_indexing_index_failed_count|gauge|Count of failed to index documents|
|es_indices_indexing_index_time_seconds|gauge|Time spent while indexing documents|
|es_indices_indexing_is_throttled_bool|gauge|Is indexing throttling ?|
|es_indices_indexing_noop_update_count|gauge|Count of noop document updates|
|es_indices_indexing_throttle_time_seconds|gauge|Time spent while throttling|
|es_indices_merges_current_docs_number|gauge|Current rate of documents merged|
|es_indices_merges_current_number|gauge|Current rate of merges|
|es_indices_merges_current_size_bytes|gauge|Current rate of bytes merged|
|es_indices_merges_total_auto_throttle_bytes|gauge|Bytes merged while throttling|
|es_indices_merges_total_docs_count|gauge|Count of documents merged|
|es_indices_merges_total_number|gauge|Count of merges|
|es_indices_merges_total_size_bytes|gauge|Count of bytes of merged documents|
|es_indices_merges_total_stopped_time_seconds|gauge|Time spent while merge process stopped|
|es_indices_merges_total_throttled_time_seconds|gauge|Time spent while merging when throttling|
|es_indices_merges_total_time_seconds|gauge|Time spent while merging|
|es_indices_percolate_count|gauge|Count of percolates|
|es_indices_percolate_current_number|gauge|Rate of percolates|
|es_indices_percolate_memory_size_bytes|gauge|Percolate memory size|
|es_indices_percolate_queries_count|gauge|Count of queries percolated|
|es_indices_percolate_time_seconds|gauge|Time spent while percolating|
|es_indices_querycache_cache_count|gauge|Count of queries in cache|
|es_indices_querycache_cache_size_bytes|gauge|Query cache size|
|es_indices_querycache_evictions_count|gauge|Count of evictions in query cache|
|es_indices_querycache_hit_count|gauge|Count of hits in query cache|
|es_indices_querycache_memory_size_bytes|gauge|Memory usage of query cache|
|es_indices_querycache_miss_number|gauge|Count of misses in query cache|
|es_indices_querycache_total_number|gauge|Count of usages of query cache|
|es_indices_recovery_current_number|gauge|Current number of recoveries|
|es_indices_recovery_throttle_time_seconds|gauge|Time spent while throttling recoveries|
|es_indices_refresh_listeners_number|gauge|Number of refresh listeners|
|es_indices_refresh_total_count|gauge|Count of refreshes|
|es_indices_refresh_total_time_seconds|gauge|Time spent while refreshes|
|es_indices_requestcache_evictions_count|gauge|Number of evictions in request cache|
|es_indices_requestcache_hit_count|gauge|Number of hits in request cache|
|es_indices_requestcache_memory_size_bytes|gauge|Memory used for request cache|
|es_indices_requestcache_miss_count|gauge|Number of misses in request cache|
|es_indices_search_fetch_count|gauge|Count of search fetches|
|es_indices_search_fetch_current_number|gauge|Current rate of search fetches|
|es_indices_search_fetch_time_seconds|gauge|Time spent while search fetches|
|es_indices_search_open_contexts_number|gauge|Number of search open contexts|
|es_indices_search_query_count|gauge|Count of search queries|
|es_indices_search_query_current_number|gauge|Current rate of search queries|
|es_indices_search_query_time_seconds|gauge|Time spent while search queries|
|es_indices_search_scroll_count|gauge|Count of search scrolls|
|es_indices_search_scroll_current_number|gauge|Current rate of search scrolls|
|es_indices_search_scroll_time_seconds|gauge|Time spent while search scrolls|
|es_indices_segments_memory_bytes|gauge|Memory used by segments|
|es_indices_segments_number|gauge|Current number of segments|
|es_indices_store_size_bytes|gauge|Store size of the indices in bytes|
|es_indices_suggest_count|gauge|Count of suggests|
|es_indices_suggest_current_number|gauge|Current rate of suggests|
|es_indices_suggest_time_seconds|gauge|Time spent while making suggests|
|es_ingest_pipeline_processor_total_count|gauge|Ingestion total number|
|es_ingest_pipeline_processor_total_current|gauge|Ingestion total current|
|es_ingest_pipeline_processor_total_failed_count|gauge|Ingestion total failed|
|es_ingest_pipeline_processor_total_time_seconds|gauge|Ingestion total time in seconds|
|es_ingest_pipeline_total_count|gauge|Ingestion total number|
|es_ingest_pipeline_total_current|gauge|Ingestion total current|
|es_ingest_pipeline_total_failed_count|gauge|Ingestion total failed|
|es_ingest_pipeline_total_time_seconds|gauge|Ingestion total time in seconds|
|es_ingest_total_count|gauge|Ingestion total number|
|es_ingest_total_current|gauge|Ingestion total current|
|es_ingest_total_failed_count|gauge|Ingestion total failed|
|es_ingest_total_time_seconds|gauge|Ingestion total time in seconds|
|es_jvm_bufferpool_number|gauge|Number of buffer pools|
|es_jvm_bufferpool_total_capacity_bytes|gauge|Total capacity provided by buffer pools|
|es_jvm_bufferpool_used_bytes|gauge|Used memory in buffer pools|
|es_jvm_classes_loaded_number|gauge|Count of loaded classes|
|es_jvm_classes_total_loaded_number|gauge|Total count of loaded classes|
|es_jvm_classes_unloaded_number|gauge|Count of unloaded classes|
|es_jvm_gc_collection_count|gauge|Count of GC collections|
|es_jvm_gc_collection_time_seconds|gauge|Time spent for GC collections|
|es_jvm_mem_heap_committed_bytes|gauge|Committed bytes in heap|
|es_jvm_mem_heap_max_bytes|gauge|Maximum used memory in heap|
|es_jvm_mem_heap_used_bytes|gauge|Memory used in heap|
|es_jvm_mem_heap_used_percent|gauge|Percentage of memory used in heap|
|es_jvm_mem_nonheap_committed_bytes|gauge|Committed bytes apart from heap|
|es_jvm_mem_nonheap_used_bytes|gauge|Memory used apart from heap|
|es_jvm_mem_pool_max_bytes|gauge|Maximum usage of memory pool|
|es_jvm_mem_pool_peak_max_bytes|gauge|Maximum usage peak of memory pool|
|es_jvm_mem_pool_peak_used_bytes|gauge|Used memory peak in memory pool|
|es_jvm_mem_pool_used_bytes|gauge|Used memory in memory pool|
|es_jvm_threads_number|gauge|Number of threads|
|es_jvm_threads_peak_number|gauge|Peak number of threads|
|es_jvm_uptime_seconds|gauge|JVM uptime|
|es_metrics_generate_time_seconds|summary|Time spent while generating metrics|
|es_node_role_bool|gauge|Node role|
|es_os_cpu_percent|gauge|CPU usage in percent|
|es_os_load_average_fifteen_minutes|gauge|CPU load|
|es_os_load_average_five_minutes|gauge|CPU load|
|es_os_load_average_one_minute|gauge|CPU load|
|es_os_mem_free_bytes|gauge|Memory free|
|es_os_mem_free_percent|gauge|Memory free in percent|
|es_os_mem_total_bytes|gauge|Total memory size|
|es_os_mem_used_bytes|gauge|Memory used|
|es_os_mem_used_percent|gauge|Memory used in percent|
|es_os_swap_free_bytes|gauge|Swap free|
|es_os_swap_total_bytes|gauge|Total swap size|
|es_os_swap_used_bytes|gauge|Swap used|
|es_process_cpu_percent|gauge|CPU percentage used by ES process|
|es_process_cpu_time_seconds|gauge|CPU time used by ES process|
|es_process_file_descriptors_max_number|gauge|Max file descriptors|
|es_process_file_descriptors_open_number|gauge|Open file descriptors|
|es_process_mem_total_virtual_bytes|gauge|Memory used by ES process|
|es_script_cache_evictions_count|gauge|Number of evictions in scripts cache|
|es_script_compilations_count|gauge|Number of scripts compilations|
|es_threadpool_tasks_number|gauge|Number of tasks in thread pool|
|es_threadpool_threads_count|gauge|Count of threads in thread pool|
|es_threadpool_threads_number|gauge|Number of threads in thread pool|
|es_transport_rx_bytes_count|gauge|Bytes received|
|es_transport_rx_packets_count|gauge|Received packets|
|es_transport_server_open_number|gauge|Opened server connections|
|es_transport_tx_bytes_count|gauge|Bytes sent|
|es_transport_tx_packets_count|gauge|Sent packets|

## Compatibility matrix

### Version 7.X

| Elasticsearch  | Plugin         | Release date |
| -------------- | -------------- | ------------ |
| 7.11.2         | 7.11.2.0       | Mar 20, 2021 |
| 7.11.1         | 7.11.1.0       | Feb 22, 2021 |
| 7.10.2         | 7.10.2.0       | Jan 24, 2021 |
| 7.10.1         | 7.10.1.0       | Dec 13, 2020 |
| 7.10.0         | 7.10.0.0       | Nov 15, 2020 |
| 7.9.3          | 7.9.3.0        | Oct 22, 2020 |
| 7.9.2          | 7.9.2.0        | Oct 04, 2020 |
| 7.9.1          | 7.9.1.0        | Sep 06, 2020 |
| 7.9.0          | 7.9.0.0        | Aug 18, 2020 |
| 7.8.1          | 7.8.1.0        | Aug 10, 2020 |
| 7.8.0          | 7.8.0.0        | Jun 22, 2020 |
| 7.7.1          | 7.7.1.0        | Jun 04, 2020 |
| 7.7.0          | 7.7.0.0        | May 14, 2020 |
| 7.6.2          | 7.6.2.0        | Apr  6, 2020 |
| 7.6.1          | 7.6.1.0        | Mar 30, 2020 |
| 7.6.0          | 7.6.0.0        | Feb 12, 2020 |
| 7.5.2          | 7.5.2.0        | Jan 25, 2020 |
| 7.5.1          | 7.5.1.0        | Jan 21, 2020 |
| 7.5.0          | 7.5.0.0        | Jan 16, 2020 |
| 7.4.2          | 7.4.2.0        | Jan 13, 2020 |
| 7.4.1          | 7.4.1.0        | Jan 13, 2020 |
| 7.4.0          | 7.4.0.0        | Jan 07, 2020 |
| 7.3.2          | 7.3.2.0        | Oct 05, 2019 |
| 7.3.1          | 7.3.1.0        | Sep 18, 2019 |
| 7.3.0          | 7.3.0.0        | Sep 17, 2019 |
| 7.2.1          | 7.2.1.0        | Jul 31, 2019 |
| 7.2.0          | 7.2.0.0        | Jul 12, 2019 |
| 7.1.1          | 7.1.1.0        | May 31, 2019 |
| 7.1.0          | 7.1.0.0        | May 23, 2019 |
| 7.0.1          | 7.0.1.0        | May 08, 2019 |
| 7.0.0          | 7.0.0.0        | Apr 11, 2019 |

## Install

`./bin/elasticsearch-plugin install -b https://github.com/vvanholl/elasticsearch-prometheus-exporter/releases/download/7.11.2.0/prometheus-exporter-7.11.2.0.zip`

**Do not forget to restart the node after the installation!**

Note that the plugin needs the following special permissions:

- java.lang.RuntimePermission accessClassInPackage.sun.misc
- java.lang.RuntimePermission accessDeclaredMembers
- java.lang.reflect.ReflectPermission suppressAccessChecks

If you have a lot of indices and think this data is irrelevant, you can disable in the main configuration file:

```
prometheus.indices: false
```

To disable exporting cluster settings use:
```
prometheus.cluster.settings: false
```

These settings can be also [updated dynamically](https://www.elastic.co/guide/en/elasticsearch/reference/master/cluster-update-settings.html).

## Uninstall

`./bin/elasticsearch-plugin remove prometheus-exporter`

Do not forget to restart the node after installation!

## Usage

Metrics are directly available at:

    http://<your-elasticsearch-host>:9200/_prometheus/metrics

As a sample result, you get:

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

### Configure the Prometheus target

On your Prometheus servers, configure a new job as usual.

For example, if you have a cluster of 3 nodes:

```YAML
- job_name: elasticsearch
  scrape_interval: 10s
  metrics_path: "/_prometheus/metrics"
  static_configs:
  - targets:
    - node1:9200
    - node2:9200
    - node3:9200
```

Of course, you could use the service discovery service instead of a static config.

Just keep in mind that `metrics_path` must be `/_prometheus/metrics`, otherwise Prometheus will find no metric.

## Project sources

The Maven project site is available at [GitHub](https://github.com/vvanholl/elasticsearch-prometheus-exporter).

## Testing

Project contains [integration tests](src/test/resources/rest-api-spec) implemented using
[rest layer](https://github.com/elastic/elasticsearch/blob/master/TESTING.asciidoc#testing-the-rest-layer)
framework.

To run everything similar to the gitlab pipeline you can do:
```
docker run -v $(pwd):/home/gradle gradle:6.6.0-jdk14 su gradle -c 'gradle check'
```
NOTE: Please keep version in sync with .gitlab-ci.yml


Complete test suite is run using:
```
gradle clean check
```

To run individual test file use:
```
gradle :integTest \
  -Dtests.class=org.elasticsearch.rest.PrometheusRestHandlerClientYamlTestSuiteIT \
  -Dtests.method="test {yaml=resthandler/20_metrics/Prometheus metrics can be pulled}"
```

## Credits

This plugin mainly uses the [Prometheus JVM Client](https://github.com/prometheus/client_java).

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
