# Prometheus Exporter Plugin for ElasticSearch

This is a builtin exporter from ElasticSearch to Prometheus.
It collects all relevant metrics and make them available to Prometheus via ElasticSearch REST API.

Current available metrics are :

- Cluster status
- Node status :
	- JVM
	- Indices (global)
	- Transport
	- HTTP
	- Scripts
	- Process
	- Operating System
	- File System
	- Circuit Breaker

## Compatibility matrix

| Elasticsearch  | Plugin         | Release date |
| -------------- | -------------- | ------------ |
| 2.3.4          | 2.3.4.0        | Jul 30, 2016 |

## Install

    ./bin/plugin install http://distfiles.compuscene.net/elasticsearch/elasticsearch-prometheus-exporter-2.3.4.0.zip

Do not forget to restart the node after installation !

Note that the plugin needs special permissions :

- java.lang.RuntimePermission accessClassInPackage.sun.misc
- java.lang.reflect.ReflectPermission suppressAccessChecks

## Uninstall
    ./bin/plugin remove prometheus-exporter

Do not forget to restart the node after installation !

## Usage

Metrics are directly available at address :

    http://<your_server_address>:9200/_prometheus/metrics

As a sample result, you get :

```
# HELP es_os_swap_used_bytes Swap used
# TYPE es_os_swap_used_bytes gauge
es_os_swap_used_bytes{cluster="develop",node="develop01",} 3.3759232E7
# HELP es_indices_querycache_cache_count No Help provided for the moment
# TYPE es_indices_querycache_cache_count gauge
es_indices_querycache_cache_count{cluster="develop",node="develop01",} 0.0
# HELP es_jvm_classes_total_loaded_count No Help provided for the moment
# TYPE es_jvm_classes_total_loaded_count gauge
es_jvm_classes_total_loaded_count{cluster="develop",node="develop01",} 7732.0
# HELP es_circuitbreaker_estimated_bytes Circuit breaker estimated size
# TYPE es_circuitbreaker_estimated_bytes gauge
es_circuitbreaker_estimated_bytes{cluster="develop",node="develop01",name="request",} 0.0
es_circuitbreaker_estimated_bytes{cluster="develop",node="develop01",name="fielddata",} 0.0
es_circuitbreaker_estimated_bytes{cluster="develop",node="develop01",name="parent",} 0.0
# HELP es_indices_percolate_count No Help provided for the moment
# TYPE es_indices_percolate_count gauge
es_indices_percolate_count{cluster="develop",node="develop01",} 0.0

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
