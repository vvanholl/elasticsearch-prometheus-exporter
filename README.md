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

## Compatibility matrix

### Version 7.X

| Elasticsearch  | Plugin         | Release date |
| -------------- | -------------- | ------------ |
| 7.17.7         | 7.17.7.0       | Oct 26, 2022 |
| 7.17.6         | 7.17.6.0       | Aug 31, 2022 |
| 7.17.5         | 7.17.5.0       | Jul 06, 2022 |
| 7.17.4         | 7.17.4.0       | May 24, 2022 |
| 7.17.3         | 7.17.3.0       | Apr 22, 2022 |
| 7.17.2         | 7.17.2.0       | Apr 08, 2022 |
| 7.17.1         | 7.17.1.0       | Mar 11, 2022 |
| 7.17.0         | 7.17.0.0       | Feb 01, 2022 |
| 7.16.3         | 7.16.3.0       | Jan 14, 2022 |
| 7.16.2         | 7.16.2.0       | Dec 20, 2021 |
| 7.16.1         | 7.16.1.0       | Dec 13, 2021 |
| 7.16.0         | 7.16.0.0       | Dec 09, 2021 |
| 7.15.2         | 7.15.2.0       | Nov 13, 2021 |
| 7.15.1         | 7.15.1.0       | Oct 16, 2021 |
| 7.15.0         | 7.15.0.0       | Oct 02, 2021 |
| 7.14.1         | 7.14.1.0       | Sep 04, 2021 |
| 7.14.0         | 7.14.0.0       | Aug 07, 2021 |
| 7.13.4         | 7.13.4.0       | Jul 21, 2021 |
| 7.13.3         | 7.13.3.0       | Jul 07, 2021 |
| 7.13.2         | 7.13.2.0       | Jun 15, 2021 |
| 7.13.1         | 7.13.1.0       | Jun 12, 2021 |
| 7.13.0         | 7.13.0.0       | May 27, 2021 |
| 7.12.1         | 7.12.1.0       | May 01, 2021 |
| 7.12.0         | 7.12.0.0       | Apr 04, 2021 |
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
| 7.6.2          | 7.6.2.0        | Apr 06, 2020 |
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

`./bin/elasticsearch-plugin install -b https://github.com/vvanholl/elasticsearch-prometheus-exporter/releases/download/7.17.7.0/prometheus-exporter-7.17.7.0.zip`

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

To run everything similar to the GitHub Actions pipeline you can do:
```
docker run -v $(pwd):/home/gradle gradle:7.0.2-jdk16 su gradle -c 'gradle check'
```
NOTE: Please keep version in sync with .github/workflows/ci.yml


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
