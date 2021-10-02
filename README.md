![CI](https://github.com/vvanholl/elasticsearch-prometheus-exporter/workflows/CI/badge.svg?branch=6.x)

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

## Compatibility matrix

### Version 6.X

| Elasticsearch  | Plugin         | Release date |
| -------------- | -------------- | ------------ |
| 6.8.19         | 6.8.19.0       | Oct 02, 2021 |
| 6.8.18         | 6.8.18.0       | Aug 07, 2021 |
| 6.8.17         | 6.8.17.0       | Jul 10, 2021 |
| 6.8.16         | 6.8.16.0       | May 29, 2021 |
| 6.8.15         | 6.8.15.0       | Apr 04, 2020 |
| 6.8.14         | 6.8.14.0       | Feb 22, 2020 |
| 6.8.13         | 6.8.13.0       | Nov 02, 2020 |
| 6.8.12         | 6.8.12.0       | Aug 19, 2020 |
| 6.8.11         | 6.8.11.0       | Aug 11, 2020 |
| 6.8.10         | 6.8.10.0       | Jun 14, 2020 |
| 6.8.9          | 6.8.9.0        | May 15, 2020 |
| 6.8.8          | 6.8.8.0        | Apr 06, 2020 |
| 6.8.7          | 6.8.7.0        | Mar 09, 2020 |
| 6.8.6          | 6.8.6.0        | Jan 01, 2020 |
| 6.8.5          | 6.8.5.0        | Nov 30, 2019 |
| 6.8.4          | 6.8.4.0        | Nov 03, 2019 |
| 6.8.3          | 6.8.3.0        | Sep 12, 2019 |
| 6.8.2          | 6.8.2.0        | Aug 12, 2019 |
| 6.8.1          | 6.8.1.0        | Jun 24, 2019 |
| 6.8.0          | 6.8.0.0        | May 23, 2019 |
| 6.7.2          | 6.7.2.0        | May 08, 2019 |
| 6.7.1          | 6.7.1.0        | Apr 04, 2019 |
| 6.7.0          | 6.7.0.0        | Apr 02, 2019 |
| 6.6.2          | 6.6.2.0        | Mar 14, 2019 |
| 6.6.1          | 6.6.1.0        | Feb 21, 2019 |
| 6.6.0          | 6.6.0.0        | Feb 11, 2019 |
| 6.5.4          | 6.5.4.0        | Jan 01, 2019 |
| 6.5.3          | 6.5.3.0        | Dec 13, 2018 |
| 6.5.2          | 6.5.2.0        | Dec 05, 2018 |
| 6.5.1          | 6.5.1.0        | Nov 21, 2018 |
| 6.5.0          | 6.5.0.0        | Nov 19, 2018 |
| 6.4.3          | 6.4.3.0        | Nov 07, 2018 |
| 6.4.2          | 6.4.2.0        | Oct 08, 2018 |
| 6.4.1          | 6.4.1.0        | Sep 24, 2018 |
| 6.4.0          | 6.4.0.0        | Aug 28, 2018 |
| 6.3.2          | 6.3.2.0        | Jul 28, 2018 |
| 6.3.1          | 6.3.1.0        | Jul 05, 2018 |
| 6.3.0          | 6.3.0.1        | Jun 20, 2018 |
| 6.3.0          | 6.3.0.0        | Jun 13, 2018 |
| 6.2.4          | 6.2.4.0        | Apr 17, 2018 |
| 6.2.3          | 6.2.3.0        | Mar 20, 2018 |
| 6.2.2          | 6.2.2.0        | Feb 20, 2018 |
| 6.2.1          | 6.2.1.0        | Feb 08, 2018 |
| 6.2.0          | 6.2.0.0        | Feb 07, 2018 |
| 6.1.3          | 6.1.3.0        | Jan 30, 2018 |
| 6.1.2          | 6.1.2.0        | Jan 16, 2018 |
| 6.1.1          | 6.1.1.0        | Dec 20, 2017 |
| 6.1.0          | 6.1.0.0        | Dec 14, 2017 |
| 6.0.1          | 6.0.1.0        | Dec 08, 2017 |
| 6.0.0          | 6.0.0.0        | Nov 27, 2017 |

## Install

`./bin/elasticsearch-plugin install -b https://github.com/vvanholl/elasticsearch-prometheus-exporter/releases/download/6.8.19.0/prometheus-exporter-6.8.19.0.zip`

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
    - node1:9200
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
