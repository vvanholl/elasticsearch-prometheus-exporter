/*
 * Copyright [2016] [Vincent VAN HOLLEBEKE]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.compuscene.metrics.prometheus;

import org.elasticsearch.action.ClusterStatsData;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.admin.cluster.node.stats.NodeStats;
import org.elasticsearch.action.admin.indices.stats.CommonStats;
import org.elasticsearch.action.admin.indices.stats.IndexStats;
import org.elasticsearch.action.admin.indices.stats.IndicesStatsResponse;
import org.elasticsearch.cluster.health.ClusterIndexHealth;
import org.elasticsearch.cluster.node.DiscoveryNode.Role;
import org.elasticsearch.http.HttpStats;
import org.elasticsearch.indices.NodeIndicesStats;
import org.elasticsearch.indices.breaker.AllCircuitBreakerStats;
import org.elasticsearch.indices.breaker.CircuitBreakerStats;
import org.elasticsearch.ingest.IngestStats;
import org.elasticsearch.monitor.fs.FsInfo;
import org.elasticsearch.monitor.jvm.JvmStats;
import org.elasticsearch.monitor.os.OsStats;
import org.elasticsearch.monitor.process.ProcessStats;
import org.elasticsearch.script.ScriptStats;
import org.elasticsearch.threadpool.ThreadPoolStats;
import org.elasticsearch.transport.TransportStats;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.prometheus.client.Summary;

/**
 * A class that describes a Prometheus metrics collector.
 */
public class PrometheusMetricsCollector {

    private boolean isPrometheusClusterSettings;
    private boolean isPrometheusIndices;
    private PrometheusMetricsCatalog catalog;

    public PrometheusMetricsCollector(PrometheusMetricsCatalog catalog,
                                      boolean isPrometheusIndices,
                                      boolean isPrometheusClusterSettings) {
        this.isPrometheusClusterSettings = isPrometheusClusterSettings;
        this.isPrometheusIndices = isPrometheusIndices;
        this.catalog = catalog;
    }

    public void registerMetrics() {
        catalog.registerSummaryTimer("metrics_generate_time_seconds", "Time spent while generating metrics");

        registerClusterMetrics();
        registerNodeMetrics();
        registerIndicesMetrics();
        registerPerIndexMetrics();
        registerTransportMetrics();
        registerHTTPMetrics();
        registerThreadPoolMetrics();
        registerIngestMetrics();
        registerCircuitBreakerMetrics();
        registerScriptMetrics();
        registerProcessMetrics();
        registerJVMMetrics();
        registerOsMetrics();
        registerFsMetrics();
        registerESSettings();
    }

    private void registerClusterMetrics() {
        catalog.registerClusterGauge("cluster_status", "Cluster status");

        catalog.registerClusterGauge("cluster_nodes_number", "Number of nodes in the cluster");
        catalog.registerClusterGauge("cluster_datanodes_number", "Number of data nodes in the cluster");

        catalog.registerClusterGauge("cluster_shards_active_percent", "Percent of active shards");
        catalog.registerClusterGauge("cluster_shards_number", "Number of shards", "type");

        catalog.registerClusterGauge("cluster_pending_tasks_number", "Number of pending tasks");
        catalog.registerClusterGauge("cluster_task_max_waiting_time_seconds", "Max waiting time for tasks");

        catalog.registerClusterGauge("cluster_is_timedout_bool", "Is the cluster timed out ?");

        catalog.registerClusterGauge("cluster_inflight_fetch_number", "Number of in flight fetches");
    }

    private void updateClusterMetrics(ClusterHealthResponse chr) {
        if (chr != null) {
            catalog.setClusterGauge("cluster_status", chr.getStatus().value());

            catalog.setClusterGauge("cluster_nodes_number", chr.getNumberOfNodes());
            catalog.setClusterGauge("cluster_datanodes_number", chr.getNumberOfDataNodes());

            catalog.setClusterGauge("cluster_shards_active_percent", chr.getActiveShardsPercent());

            catalog.setClusterGauge("cluster_shards_number", chr.getActiveShards(), "active");
            catalog.setClusterGauge("cluster_shards_number", chr.getActivePrimaryShards(), "active_primary");
            catalog.setClusterGauge("cluster_shards_number", chr.getDelayedUnassignedShards(), "unassigned");
            catalog.setClusterGauge("cluster_shards_number", chr.getInitializingShards(), "initializing");
            catalog.setClusterGauge("cluster_shards_number", chr.getRelocatingShards(), "relocating");
            catalog.setClusterGauge("cluster_shards_number", chr.getUnassignedShards(), "unassigned");

            catalog.setClusterGauge("cluster_pending_tasks_number", chr.getNumberOfPendingTasks());
            catalog.setClusterGauge("cluster_task_max_waiting_time_seconds", chr.getTaskMaxWaitingTime().getSeconds());

            catalog.setClusterGauge("cluster_is_timedout_bool", chr.isTimedOut() ? 1 : 0);

            catalog.setClusterGauge("cluster_inflight_fetch_number", chr.getNumberOfInFlightFetch());
        }
    }

    private void registerNodeMetrics() {
        catalog.registerNodeGauge("node_role_bool", "Node role", "role");
    }

    private void updateNodeMetrics(NodeStats ns) {
        if (ns != null) {
            Map<String, Integer> roles = new HashMap<>();

            roles.put("master", 0);
            roles.put("data", 0);
            roles.put("ingest", 0);

            for (Role r : ns.getNode().getRoles()) {
                roles.put(r.getRoleName(), 1);
            }

            for (String k : roles.keySet()) {
                catalog.setNodeGauge("node_role_bool", roles.get(k), k);
            }
        }
    }

    private void registerIndicesMetrics() {
        catalog.registerNodeGauge("indices_doc_number", "Total number of documents");
        catalog.registerNodeGauge("indices_doc_deleted_number", "Number of deleted documents");

        catalog.registerNodeGauge("indices_store_size_bytes", "Store size of the indices in bytes");

        catalog.registerNodeGauge("indices_indexing_delete_count", "Count of documents deleted");
        catalog.registerNodeGauge("indices_indexing_delete_current_number", "Current rate of documents deleted");
        catalog.registerNodeGauge("indices_indexing_delete_time_seconds", "Time spent while deleting documents");
        catalog.registerNodeGauge("indices_indexing_index_count", "Count of documents indexed");
        catalog.registerNodeGauge("indices_indexing_index_current_number", "Current rate of documents indexed");
        catalog.registerNodeGauge("indices_indexing_index_failed_count", "Count of failed to index documents");
        catalog.registerNodeGauge("indices_indexing_index_time_seconds", "Time spent while indexing documents");
        catalog.registerNodeGauge("indices_indexing_noop_update_count", "Count of noop document updates");
        catalog.registerNodeGauge("indices_indexing_is_throttled_bool", "Is indexing throttling ?");
        catalog.registerNodeGauge("indices_indexing_throttle_time_seconds", "Time spent while throttling");

        catalog.registerNodeGauge("indices_get_count", "Count of get commands");
        catalog.registerNodeGauge("indices_get_time_seconds", "Time spent while get commands");
        catalog.registerNodeGauge("indices_get_exists_count", "Count of existing documents when get command");
        catalog.registerNodeGauge("indices_get_exists_time_seconds", "Time spent while existing documents get command");
        catalog.registerNodeGauge("indices_get_missing_count", "Count of missing documents when get command");
        catalog.registerNodeGauge("indices_get_missing_time_seconds", "Time spent while missing documents get command");
        catalog.registerNodeGauge("indices_get_current_number", "Current rate of get commands");

        catalog.registerNodeGauge("indices_search_open_contexts_number", "Number of search open contexts");
        catalog.registerNodeGauge("indices_search_fetch_count", "Count of search fetches");
        catalog.registerNodeGauge("indices_search_fetch_current_number", "Current rate of search fetches");
        catalog.registerNodeGauge("indices_search_fetch_time_seconds", "Time spent while search fetches");
        catalog.registerNodeGauge("indices_search_query_count", "Count of search queries");
        catalog.registerNodeGauge("indices_search_query_current_number", "Current rate of search queries");
        catalog.registerNodeGauge("indices_search_query_time_seconds", "Time spent while search queries");
        catalog.registerNodeGauge("indices_search_scroll_count", "Count of search scrolls");
        catalog.registerNodeGauge("indices_search_scroll_current_number", "Current rate of search scrolls");
        catalog.registerNodeGauge("indices_search_scroll_time_seconds", "Time spent while search scrolls");

        catalog.registerNodeGauge("indices_merges_current_number", "Current rate of merges");
        catalog.registerNodeGauge("indices_merges_current_docs_number", "Current rate of documents merged");
        catalog.registerNodeGauge("indices_merges_current_size_bytes", "Current rate of bytes merged");
        catalog.registerNodeGauge("indices_merges_total_number", "Count of merges");
        catalog.registerNodeGauge("indices_merges_total_time_seconds", "Time spent while merging");
        catalog.registerNodeGauge("indices_merges_total_docs_count", "Count of documents merged");
        catalog.registerNodeGauge("indices_merges_total_size_bytes", "Count of bytes of merged documents");
        catalog.registerNodeGauge("indices_merges_total_stopped_time_seconds", "Time spent while merge process stopped");
        catalog.registerNodeGauge("indices_merges_total_throttled_time_seconds", "Time spent while merging when throttling");
        catalog.registerNodeGauge("indices_merges_total_auto_throttle_bytes", "Bytes merged while throttling");

        catalog.registerNodeGauge("indices_refresh_total_count", "Count of refreshes");
        catalog.registerNodeGauge("indices_refresh_total_time_seconds", "Time spent while refreshes");
        catalog.registerNodeGauge("indices_refresh_listeners_number", "Number of refresh listeners");

        catalog.registerNodeGauge("indices_flush_total_count", "Count of flushes");
        catalog.registerNodeGauge("indices_flush_total_time_seconds", "Total time spent while flushes");

        catalog.registerNodeGauge("indices_querycache_cache_count", "Count of queries in cache");
        catalog.registerNodeGauge("indices_querycache_cache_size_bytes", "Query cache size");
        catalog.registerNodeGauge("indices_querycache_evictions_count", "Count of evictions in query cache");
        catalog.registerNodeGauge("indices_querycache_hit_count", "Count of hits in query cache");
        catalog.registerNodeGauge("indices_querycache_memory_size_bytes", "Memory usage of query cache");
        catalog.registerNodeGauge("indices_querycache_miss_number", "Count of misses in query cache");
        catalog.registerNodeGauge("indices_querycache_total_number", "Count of usages of query cache");

        catalog.registerNodeGauge("indices_fielddata_memory_size_bytes", "Memory usage of field date cache");
        catalog.registerNodeGauge("indices_fielddata_evictions_count", "Count of evictions in field data cache");

        catalog.registerNodeGauge("indices_percolate_count", "Count of percolates");
        catalog.registerNodeGauge("indices_percolate_current_number", "Rate of percolates");
        catalog.registerNodeGauge("indices_percolate_memory_size_bytes", "Percolate memory size");
        catalog.registerNodeGauge("indices_percolate_queries_count", "Count of queries percolated");
        catalog.registerNodeGauge("indices_percolate_time_seconds", "Time spent while percolating");

        catalog.registerNodeGauge("indices_completion_size_bytes", "Size of completion suggest statistics");

        catalog.registerNodeGauge("indices_segments_number", "Current number of segments");
        catalog.registerNodeGauge("indices_segments_memory_bytes", "Memory used by segments", "type");

        catalog.registerNodeGauge("indices_suggest_current_number", "Current rate of suggests");
        catalog.registerNodeGauge("indices_suggest_count", "Count of suggests");
        catalog.registerNodeGauge("indices_suggest_time_seconds", "Time spent while making suggests");

        catalog.registerNodeGauge("indices_requestcache_memory_size_bytes", "Memory used for request cache");
        catalog.registerNodeGauge("indices_requestcache_hit_count", "Number of hits in request cache");
        catalog.registerNodeGauge("indices_requestcache_miss_count", "Number of misses in request cache");
        catalog.registerNodeGauge("indices_requestcache_evictions_count", "Number of evictions in request cache");

        catalog.registerNodeGauge("indices_recovery_current_number", "Current number of recoveries", "type");
        catalog.registerNodeGauge("indices_recovery_throttle_time_seconds", "Time spent while throttling recoveries");
    }

    private void updateIndicesMetrics(NodeIndicesStats idx) {
        if (idx != null) {
            catalog.setNodeGauge("indices_doc_number", idx.getDocs().getCount());
            catalog.setNodeGauge("indices_doc_deleted_number", idx.getDocs().getDeleted());

            catalog.setNodeGauge("indices_store_size_bytes", idx.getStore().getSizeInBytes());

            catalog.setNodeGauge("indices_indexing_delete_count", idx.getIndexing().getTotal().getDeleteCount());
            catalog.setNodeGauge("indices_indexing_delete_current_number", idx.getIndexing().getTotal().getDeleteCurrent());
            catalog.setNodeGauge("indices_indexing_delete_time_seconds",
                    idx.getIndexing().getTotal().getDeleteTime().seconds());
            catalog.setNodeGauge("indices_indexing_index_count", idx.getIndexing().getTotal().getIndexCount());
            catalog.setNodeGauge("indices_indexing_index_current_number", idx.getIndexing().getTotal().getIndexCurrent());
            catalog.setNodeGauge("indices_indexing_index_failed_count", idx.getIndexing().getTotal().getIndexFailedCount());
            catalog.setNodeGauge("indices_indexing_index_time_seconds", idx.getIndexing().getTotal().getIndexTime().seconds());
            catalog.setNodeGauge("indices_indexing_noop_update_count", idx.getIndexing().getTotal().getNoopUpdateCount());
            catalog.setNodeGauge("indices_indexing_is_throttled_bool", idx.getIndexing().getTotal().isThrottled() ? 1 : 0);
            catalog.setNodeGauge("indices_indexing_throttle_time_seconds",
                    idx.getIndexing().getTotal().getThrottleTime().seconds());

            catalog.setNodeGauge("indices_get_count", idx.getGet().getCount());
            catalog.setNodeGauge("indices_get_time_seconds", idx.getGet().getTimeInMillis() / 1000.0);
            catalog.setNodeGauge("indices_get_exists_count", idx.getGet().getExistsCount());
            catalog.setNodeGauge("indices_get_exists_time_seconds", idx.getGet().getExistsTimeInMillis() / 1000.0);
            catalog.setNodeGauge("indices_get_missing_count", idx.getGet().getMissingCount());
            catalog.setNodeGauge("indices_get_missing_time_seconds", idx.getGet().getMissingTimeInMillis() / 1000.0);
            catalog.setNodeGauge("indices_get_current_number", idx.getGet().current());

            catalog.setNodeGauge("indices_search_open_contexts_number", idx.getSearch().getOpenContexts());
            catalog.setNodeGauge("indices_search_fetch_count", idx.getSearch().getTotal().getFetchCount());
            catalog.setNodeGauge("indices_search_fetch_current_number", idx.getSearch().getTotal().getFetchCurrent());
            catalog.setNodeGauge("indices_search_fetch_time_seconds",
                    idx.getSearch().getTotal().getFetchTimeInMillis() / 1000.0);
            catalog.setNodeGauge("indices_search_query_count", idx.getSearch().getTotal().getQueryCount());
            catalog.setNodeGauge("indices_search_query_current_number", idx.getSearch().getTotal().getQueryCurrent());
            catalog.setNodeGauge("indices_search_query_time_seconds",
                    idx.getSearch().getTotal().getQueryTimeInMillis() / 1000.0);
            catalog.setNodeGauge("indices_search_scroll_count", idx.getSearch().getTotal().getScrollCount());
            catalog.setNodeGauge("indices_search_scroll_current_number", idx.getSearch().getTotal().getScrollCurrent());
            catalog.setNodeGauge("indices_search_scroll_time_seconds",
                    idx.getSearch().getTotal().getScrollTimeInMillis() / 1000.0);

            catalog.setNodeGauge("indices_merges_current_number", idx.getMerge().getCurrent());
            catalog.setNodeGauge("indices_merges_current_docs_number", idx.getMerge().getCurrentNumDocs());
            catalog.setNodeGauge("indices_merges_current_size_bytes", idx.getMerge().getCurrentSizeInBytes());
            catalog.setNodeGauge("indices_merges_total_number", idx.getMerge().getTotal());
            catalog.setNodeGauge("indices_merges_total_time_seconds", idx.getMerge().getTotalTimeInMillis() / 1000.0);
            catalog.setNodeGauge("indices_merges_total_docs_count", idx.getMerge().getTotalNumDocs());
            catalog.setNodeGauge("indices_merges_total_size_bytes", idx.getMerge().getTotalSizeInBytes());
            catalog.setNodeGauge("indices_merges_total_stopped_time_seconds",
                    idx.getMerge().getTotalStoppedTimeInMillis() / 1000.0);
            catalog.setNodeGauge("indices_merges_total_throttled_time_seconds",
                    idx.getMerge().getTotalThrottledTimeInMillis() / 1000.0);
            catalog.setNodeGauge("indices_merges_total_auto_throttle_bytes", idx.getMerge().getTotalBytesPerSecAutoThrottle());

            catalog.setNodeGauge("indices_refresh_total_count", idx.getRefresh().getTotal());
            catalog.setNodeGauge("indices_refresh_total_time_seconds", idx.getRefresh().getTotalTimeInMillis() / 1000.0);
            catalog.setNodeGauge("indices_refresh_listeners_number", idx.getRefresh().getListeners());

            catalog.setNodeGauge("indices_flush_total_count", idx.getFlush().getTotal());
            catalog.setNodeGauge("indices_flush_total_time_seconds", idx.getFlush().getTotalTimeInMillis() / 1000.0);

            catalog.setNodeGauge("indices_querycache_cache_count", idx.getQueryCache().getCacheCount());
            catalog.setNodeGauge("indices_querycache_cache_size_bytes", idx.getQueryCache().getCacheSize());
            catalog.setNodeGauge("indices_querycache_evictions_count", idx.getQueryCache().getEvictions());
            catalog.setNodeGauge("indices_querycache_hit_count", idx.getQueryCache().getHitCount());
            catalog.setNodeGauge("indices_querycache_memory_size_bytes", idx.getQueryCache().getMemorySizeInBytes());
            catalog.setNodeGauge("indices_querycache_miss_number", idx.getQueryCache().getMissCount());
            catalog.setNodeGauge("indices_querycache_total_number", idx.getQueryCache().getTotalCount());

            catalog.setNodeGauge("indices_fielddata_memory_size_bytes", idx.getFieldData().getMemorySizeInBytes());
            catalog.setNodeGauge("indices_fielddata_evictions_count", idx.getFieldData().getEvictions());

            catalog.setNodeGauge("indices_completion_size_bytes", idx.getCompletion().getSizeInBytes());

            catalog.setNodeGauge("indices_segments_number", idx.getSegments().getCount());
            catalog.setNodeGauge("indices_segments_memory_bytes", idx.getSegments().getMemoryInBytes(), "all");
            catalog.setNodeGauge("indices_segments_memory_bytes", idx.getSegments().getBitsetMemoryInBytes(), "bitset");
            catalog.setNodeGauge("indices_segments_memory_bytes", idx.getSegments().getDocValuesMemoryInBytes(), "docvalues");
            catalog.setNodeGauge("indices_segments_memory_bytes", idx.getSegments().getIndexWriterMemoryInBytes(),
                    "indexwriter");
            catalog.setNodeGauge("indices_segments_memory_bytes", idx.getSegments().getNormsMemoryInBytes(), "norms");
            catalog.setNodeGauge("indices_segments_memory_bytes", idx.getSegments().getStoredFieldsMemoryInBytes(),
                    "storefields");
            catalog.setNodeGauge("indices_segments_memory_bytes", idx.getSegments().getTermsMemoryInBytes(), "terms");
            catalog.setNodeGauge("indices_segments_memory_bytes", idx.getSegments().getTermVectorsMemoryInBytes(),
                    "termvectors");
            catalog.setNodeGauge("indices_segments_memory_bytes", idx.getSegments().getVersionMapMemoryInBytes(), "versionmap");
            catalog.setNodeGauge("indices_segments_memory_bytes", idx.getSegments().getPointsMemoryInBytes(), "points");

            catalog.setNodeGauge("indices_suggest_current_number", idx.getSearch().getTotal().getSuggestCurrent());
            catalog.setNodeGauge("indices_suggest_count", idx.getSearch().getTotal().getSuggestCount());
            catalog.setNodeGauge("indices_suggest_time_seconds", idx.getSearch().getTotal().getSuggestTimeInMillis() / 1000.0);

            catalog.setNodeGauge("indices_requestcache_memory_size_bytes", idx.getRequestCache().getMemorySizeInBytes());
            catalog.setNodeGauge("indices_requestcache_hit_count", idx.getRequestCache().getHitCount());
            catalog.setNodeGauge("indices_requestcache_miss_count", idx.getRequestCache().getMissCount());
            catalog.setNodeGauge("indices_requestcache_evictions_count", idx.getRequestCache().getEvictions());

            catalog.setNodeGauge("indices_recovery_current_number", idx.getRecoveryStats().currentAsSource(), "source");
            catalog.setNodeGauge("indices_recovery_current_number", idx.getRecoveryStats().currentAsTarget(), "target");
            catalog.setNodeGauge("indices_recovery_throttle_time_seconds", idx.getRecoveryStats().throttleTime().getSeconds());
        }
    }

    @SuppressWarnings("checkstyle:LineLength")
    private void registerPerIndexMetrics() {
        catalog.registerClusterGauge("index_status", "Index status", "index");
        catalog.registerClusterGauge("index_replicas_number", "Number of replicas", "index");
        catalog.registerClusterGauge("index_shards_number", "Number of shards", "type", "index");

        catalog.registerClusterGauge("index_doc_number", "Total number of documents", "index", "context");
        catalog.registerClusterGauge("index_doc_deleted_number", "Number of deleted documents", "index", "context");

        catalog.registerClusterGauge("index_store_size_bytes", "Store size of the indices in bytes", "index", "context");

        catalog.registerClusterGauge("index_indexing_delete_count", "Count of documents deleted", "index", "context");
        catalog.registerClusterGauge("index_indexing_delete_current_number", "Current rate of documents deleted", "index", "context");
        catalog.registerClusterGauge("index_indexing_delete_time_seconds", "Time spent while deleting documents", "index", "context");
        catalog.registerClusterGauge("index_indexing_index_count", "Count of documents indexed", "index", "context");
        catalog.registerClusterGauge("index_indexing_index_current_number", "Current rate of documents indexed", "index", "context");
        catalog.registerClusterGauge("index_indexing_index_failed_count", "Count of failed to index documents", "index", "context");
        catalog.registerClusterGauge("index_indexing_index_time_seconds", "Time spent while indexing documents", "index", "context");
        catalog.registerClusterGauge("index_indexing_noop_update_count", "Count of noop document updates", "index", "context");
        catalog.registerClusterGauge("index_indexing_is_throttled_bool", "Is indexing throttling ?", "index", "context");
        catalog.registerClusterGauge("index_indexing_throttle_time_seconds", "Time spent while throttling", "index", "context");

        catalog.registerClusterGauge("index_get_count", "Count of get commands", "index", "context");
        catalog.registerClusterGauge("index_get_time_seconds", "Time spent while get commands", "index", "context");
        catalog.registerClusterGauge("index_get_exists_count", "Count of existing documents when get command", "index", "context");
        catalog.registerClusterGauge("index_get_exists_time_seconds", "Time spent while existing documents get command", "index", "context");
        catalog.registerClusterGauge("index_get_missing_count", "Count of missing documents when get command", "index", "context");
        catalog.registerClusterGauge("index_get_missing_time_seconds", "Time spent while missing documents get command", "index", "context");
        catalog.registerClusterGauge("index_get_current_number", "Current rate of get commands", "index", "context");

        catalog.registerClusterGauge("index_search_open_contexts_number", "Number of search open contexts", "index", "context");
        catalog.registerClusterGauge("index_search_fetch_count", "Count of search fetches", "index", "context");
        catalog.registerClusterGauge("index_search_fetch_current_number", "Current rate of search fetches", "index", "context");
        catalog.registerClusterGauge("index_search_fetch_time_seconds", "Time spent while search fetches", "index", "context");
        catalog.registerClusterGauge("index_search_query_count", "Count of search queries", "index", "context");
        catalog.registerClusterGauge("index_search_query_current_number", "Current rate of search queries", "index", "context");
        catalog.registerClusterGauge("index_search_query_time_seconds", "Time spent while search queries", "index", "context");
        catalog.registerClusterGauge("index_search_scroll_count", "Count of search scrolls", "index", "context");
        catalog.registerClusterGauge("index_search_scroll_current_number", "Current rate of search scrolls", "index", "context");
        catalog.registerClusterGauge("index_search_scroll_time_seconds", "Time spent while search scrolls", "index", "context");

        catalog.registerClusterGauge("index_merges_current_number", "Current rate of merges", "index", "context");
        catalog.registerClusterGauge("index_merges_current_docs_number", "Current rate of documents merged", "index", "context");
        catalog.registerClusterGauge("index_merges_current_size_bytes", "Current rate of bytes merged", "index", "context");
        catalog.registerClusterGauge("index_merges_total_number", "Count of merges", "index", "context");
        catalog.registerClusterGauge("index_merges_total_time_seconds", "Time spent while merging", "index", "context");
        catalog.registerClusterGauge("index_merges_total_docs_count", "Count of documents merged", "index", "context");
        catalog.registerClusterGauge("index_merges_total_size_bytes", "Count of bytes of merged documents", "index", "context");
        catalog.registerClusterGauge("index_merges_total_stopped_time_seconds", "Time spent while merge process stopped", "index", "context");
        catalog.registerClusterGauge("index_merges_total_throttled_time_seconds", "Time spent while merging when throttling", "index", "context");
        catalog.registerClusterGauge("index_merges_total_auto_throttle_bytes", "Bytes merged while throttling", "index", "context");

        catalog.registerClusterGauge("index_refresh_total_count", "Count of refreshes", "index", "context");
        catalog.registerClusterGauge("index_refresh_total_time_seconds", "Time spent while refreshes", "index", "context");
        catalog.registerClusterGauge("index_refresh_listeners_number", "Number of refresh listeners", "index", "context");

        catalog.registerClusterGauge("index_flush_total_count", "Count of flushes", "index", "context");
        catalog.registerClusterGauge("index_flush_total_time_seconds", "Total time spent while flushes", "index", "context");

        catalog.registerClusterGauge("index_querycache_cache_count", "Count of queries in cache", "index", "context");
        catalog.registerClusterGauge("index_querycache_cache_size_bytes", "Query cache size", "index", "context");
        catalog.registerClusterGauge("index_querycache_evictions_count", "Count of evictions in query cache", "index", "context");
        catalog.registerClusterGauge("index_querycache_hit_count", "Count of hits in query cache", "index", "context");
        catalog.registerClusterGauge("index_querycache_memory_size_bytes", "Memory usage of query cache", "index", "context");
        catalog.registerClusterGauge("index_querycache_miss_number", "Count of misses in query cache", "index", "context");
        catalog.registerClusterGauge("index_querycache_total_number", "Count of usages of query cache", "index", "context");

        catalog.registerClusterGauge("index_fielddata_memory_size_bytes", "Memory usage of field date cache", "index", "context");
        catalog.registerClusterGauge("index_fielddata_evictions_count", "Count of evictions in field data cache", "index", "context");

        // Percolator cache was removed in ES 5.x
        // See https://github.com/elastic/elasticsearch/commit/80fee8666ff5dd61ba29b175857cf42ce3b9eab9

        catalog.registerClusterGauge("index_completion_size_bytes", "Size of completion suggest statistics", "index", "context");

        catalog.registerClusterGauge("index_segments_number", "Current number of segments", "index", "context");
        catalog.registerClusterGauge("index_segments_memory_bytes", "Memory used by segments", "type", "index", "context");

        catalog.registerClusterGauge("index_suggest_current_number", "Current rate of suggests", "index", "context");
        catalog.registerClusterGauge("index_suggest_count", "Count of suggests", "index", "context");
        catalog.registerClusterGauge("index_suggest_time_seconds", "Time spent while making suggests", "index", "context");

        catalog.registerClusterGauge("index_requestcache_memory_size_bytes", "Memory used for request cache", "index", "context");
        catalog.registerClusterGauge("index_requestcache_hit_count", "Number of hits in request cache", "index", "context");
        catalog.registerClusterGauge("index_requestcache_miss_count", "Number of misses in request cache", "index", "context");
        catalog.registerClusterGauge("index_requestcache_evictions_count", "Number of evictions in request cache", "index", "context");

        catalog.registerClusterGauge("index_recovery_current_number", "Current number of recoveries", "type", "index", "context");
        catalog.registerClusterGauge("index_recovery_throttle_time_seconds", "Time spent while throttling recoveries", "index", "context");

        catalog.registerClusterGauge("index_translog_operations_number", "Current number of translog operations", "index", "context");
        catalog.registerClusterGauge("index_translog_size_bytes", "Translog size", "index", "context");
        catalog.registerClusterGauge("index_translog_uncommitted_operations_number", "Current number of uncommitted translog operations", "index", "context");
        catalog.registerClusterGauge("index_translog_uncommitted_size_bytes", "Translog uncommitted size", "index", "context");

        catalog.registerClusterGauge("index_warmer_current_number", "Current number of warmer", "index", "context");
        catalog.registerClusterGauge("index_warmer_time_seconds", "Time spent during warmers", "index", "context");
        catalog.registerClusterGauge("index_warmer_count", "Counter of warmers", "index", "context");
    }

    private void updatePerIndexMetrics(ClusterHealthResponse chr, IndicesStatsResponse isr) {

        if (chr != null && isr != null) {
            for (Map.Entry<String, IndexStats> entry : isr.getIndices().entrySet()) {
                String indexName = entry.getKey();
                ClusterIndexHealth cih = chr.getIndices().get(indexName);
                catalog.setClusterGauge("index_status", cih.getStatus().value(), indexName);
                catalog.setClusterGauge("index_replicas_number", cih.getNumberOfReplicas(), indexName);
                catalog.setClusterGauge("index_shards_number", cih.getActiveShards(), "active", indexName);
                catalog.setClusterGauge("index_shards_number", cih.getNumberOfShards(), "shards", indexName);
                catalog.setClusterGauge("index_shards_number", cih.getActivePrimaryShards(), "active_primary", indexName);
                catalog.setClusterGauge("index_shards_number", cih.getInitializingShards(), "initializing", indexName);
                catalog.setClusterGauge("index_shards_number", cih.getRelocatingShards(), "relocating", indexName);
                catalog.setClusterGauge("index_shards_number", cih.getUnassignedShards(), "unassigned", indexName);
                IndexStats indexStats = entry.getValue();
                updatePerIndexContextMetrics(indexName, "total", indexStats.getTotal());
                updatePerIndexContextMetrics(indexName, "primaries", indexStats.getPrimaries());
            }
        }
    }

    @SuppressWarnings("checkstyle:LineLength")
    private void updatePerIndexContextMetrics(String indexName, String context, CommonStats idx) {
        catalog.setClusterGauge("index_doc_number", idx.getDocs().getCount(), indexName, context);
        catalog.setClusterGauge("index_doc_deleted_number", idx.getDocs().getDeleted(), indexName, context);

        catalog.setClusterGauge("index_store_size_bytes", idx.getStore().getSizeInBytes(), indexName, context);

        catalog.setClusterGauge("index_indexing_delete_count", idx.getIndexing().getTotal().getDeleteCount(), indexName, context);
        catalog.setClusterGauge("index_indexing_delete_current_number", idx.getIndexing().getTotal().getDeleteCurrent(), indexName, context);
        catalog.setClusterGauge("index_indexing_delete_time_seconds", idx.getIndexing().getTotal().getDeleteTime().seconds(), indexName, context);
        catalog.setClusterGauge("index_indexing_index_count", idx.getIndexing().getTotal().getIndexCount(), indexName, context);
        catalog.setClusterGauge("index_indexing_index_current_number", idx.getIndexing().getTotal().getIndexCurrent(), indexName, context);
        catalog.setClusterGauge("index_indexing_index_failed_count", idx.getIndexing().getTotal().getIndexFailedCount(), indexName, context);
        catalog.setClusterGauge("index_indexing_index_time_seconds", idx.getIndexing().getTotal().getIndexTime().seconds(), indexName, context);
        catalog.setClusterGauge("index_indexing_noop_update_count", idx.getIndexing().getTotal().getNoopUpdateCount(), indexName, context);
        catalog.setClusterGauge("index_indexing_is_throttled_bool", idx.getIndexing().getTotal().isThrottled() ? 1 : 0, indexName, context);
        catalog.setClusterGauge("index_indexing_throttle_time_seconds", idx.getIndexing().getTotal().getThrottleTime().seconds(), indexName, context);

        catalog.setClusterGauge("index_get_count", idx.getGet().getCount(), indexName, context);
        catalog.setClusterGauge("index_get_time_seconds", idx.getGet().getTimeInMillis() / 1000.0, indexName, context);
        catalog.setClusterGauge("index_get_exists_count", idx.getGet().getExistsCount(), indexName, context);
        catalog.setClusterGauge("index_get_exists_time_seconds", idx.getGet().getExistsTimeInMillis() / 1000.0, indexName, context);
        catalog.setClusterGauge("index_get_missing_count", idx.getGet().getMissingCount(), indexName, context);
        catalog.setClusterGauge("index_get_missing_time_seconds", idx.getGet().getMissingTimeInMillis() / 1000.0, indexName, context);
        catalog.setClusterGauge("index_get_current_number", idx.getGet().current(), indexName, context);

        catalog.setClusterGauge("index_search_open_contexts_number", idx.getSearch().getOpenContexts(), indexName, context);
        catalog.setClusterGauge("index_search_fetch_count", idx.getSearch().getTotal().getFetchCount(), indexName, context);
        catalog.setClusterGauge("index_search_fetch_current_number", idx.getSearch().getTotal().getFetchCurrent(), indexName, context);
        catalog.setClusterGauge("index_search_fetch_time_seconds", idx.getSearch().getTotal().getFetchTimeInMillis() / 1000.0, indexName, context);
        catalog.setClusterGauge("index_search_query_count", idx.getSearch().getTotal().getQueryCount(), indexName, context);
        catalog.setClusterGauge("index_search_query_current_number", idx.getSearch().getTotal().getQueryCurrent(), indexName, context);
        catalog.setClusterGauge("index_search_query_time_seconds", idx.getSearch().getTotal().getQueryTimeInMillis() / 1000.0, indexName, context);
        catalog.setClusterGauge("index_search_scroll_count", idx.getSearch().getTotal().getScrollCount(), indexName, context);
        catalog.setClusterGauge("index_search_scroll_current_number", idx.getSearch().getTotal().getScrollCurrent(), indexName, context);
        catalog.setClusterGauge("index_search_scroll_time_seconds", idx.getSearch().getTotal().getScrollTimeInMillis() / 1000.0, indexName, context);

        catalog.setClusterGauge("index_merges_current_number", idx.getMerge().getCurrent(), indexName, context);
        catalog.setClusterGauge("index_merges_current_docs_number", idx.getMerge().getCurrentNumDocs(), indexName, context);
        catalog.setClusterGauge("index_merges_current_size_bytes", idx.getMerge().getCurrentSizeInBytes(), indexName, context);
        catalog.setClusterGauge("index_merges_total_number", idx.getMerge().getTotal(), indexName, context);
        catalog.setClusterGauge("index_merges_total_time_seconds", idx.getMerge().getTotalTimeInMillis() / 1000.0, indexName, context);
        catalog.setClusterGauge("index_merges_total_docs_count", idx.getMerge().getTotalNumDocs(), indexName, context);
        catalog.setClusterGauge("index_merges_total_size_bytes", idx.getMerge().getTotalSizeInBytes(), indexName, context);
        catalog.setClusterGauge("index_merges_total_stopped_time_seconds", idx.getMerge().getTotalStoppedTimeInMillis() / 1000.0, indexName, context);
        catalog.setClusterGauge("index_merges_total_throttled_time_seconds", idx.getMerge().getTotalThrottledTimeInMillis() / 1000.0, indexName, context);
        catalog.setClusterGauge("index_merges_total_auto_throttle_bytes", idx.getMerge().getTotalBytesPerSecAutoThrottle(), indexName, context);

        catalog.setClusterGauge("index_refresh_total_count", idx.getRefresh().getTotal(), indexName, context);
        catalog.setClusterGauge("index_refresh_total_time_seconds", idx.getRefresh().getTotalTimeInMillis() / 1000.0, indexName, context);
        catalog.setClusterGauge("index_refresh_listeners_number", idx.getRefresh().getListeners(), indexName, context);

        catalog.setClusterGauge("index_flush_total_count", idx.getFlush().getTotal(), indexName, context);
        catalog.setClusterGauge("index_flush_total_time_seconds", idx.getFlush().getTotalTimeInMillis() / 1000.0, indexName, context);

        catalog.setClusterGauge("index_querycache_cache_count", idx.getQueryCache().getCacheCount(), indexName, context);
        catalog.setClusterGauge("index_querycache_cache_size_bytes", idx.getQueryCache().getCacheSize(), indexName, context);
        catalog.setClusterGauge("index_querycache_evictions_count", idx.getQueryCache().getEvictions(), indexName, context);
        catalog.setClusterGauge("index_querycache_hit_count", idx.getQueryCache().getHitCount(), indexName, context);
        catalog.setClusterGauge("index_querycache_memory_size_bytes", idx.getQueryCache().getMemorySizeInBytes(), indexName, context);
        catalog.setClusterGauge("index_querycache_miss_number", idx.getQueryCache().getMissCount(), indexName, context);
        catalog.setClusterGauge("index_querycache_total_number", idx.getQueryCache().getTotalCount(), indexName, context);

        catalog.setClusterGauge("index_fielddata_memory_size_bytes", idx.getFieldData().getMemorySizeInBytes(), indexName, context);
        catalog.setClusterGauge("index_fielddata_evictions_count", idx.getFieldData().getEvictions(), indexName, context);

        // Percolator cache was removed in ES 5.x
        // See https://github.com/elastic/elasticsearch/commit/80fee8666ff5dd61ba29b175857cf42ce3b9eab9

        catalog.setClusterGauge("index_completion_size_bytes", idx.getCompletion().getSizeInBytes(), indexName, context);

        catalog.setClusterGauge("index_segments_number", idx.getSegments().getCount(), indexName, context);
        catalog.setClusterGauge("index_segments_memory_bytes", idx.getSegments().getMemoryInBytes(), "all", indexName, context);
        catalog.setClusterGauge("index_segments_memory_bytes", idx.getSegments().getBitsetMemoryInBytes(), "bitset", indexName, context);
        catalog.setClusterGauge("index_segments_memory_bytes", idx.getSegments().getDocValuesMemoryInBytes(), "docvalues", indexName, context);
        catalog.setClusterGauge("index_segments_memory_bytes", idx.getSegments().getIndexWriterMemoryInBytes(), "indexwriter", indexName, context);
        catalog.setClusterGauge("index_segments_memory_bytes", idx.getSegments().getNormsMemoryInBytes(), "norms", indexName, context);
        catalog.setClusterGauge("index_segments_memory_bytes", idx.getSegments().getStoredFieldsMemoryInBytes(), "storefields", indexName, context);
        catalog.setClusterGauge("index_segments_memory_bytes", idx.getSegments().getTermsMemoryInBytes(), "terms", indexName, context);
        catalog.setClusterGauge("index_segments_memory_bytes", idx.getSegments().getTermVectorsMemoryInBytes(), "termvectors", indexName, context);
        catalog.setClusterGauge("index_segments_memory_bytes", idx.getSegments().getVersionMapMemoryInBytes(), "versionmap", indexName, context);
        catalog.setClusterGauge("index_segments_memory_bytes", idx.getSegments().getPointsMemoryInBytes(), "points", indexName, context);

        catalog.setClusterGauge("index_suggest_current_number", idx.getSearch().getTotal().getSuggestCurrent(), indexName, context);
        catalog.setClusterGauge("index_suggest_count", idx.getSearch().getTotal().getSuggestCount(), indexName, context);
        catalog.setClusterGauge("index_suggest_time_seconds", idx.getSearch().getTotal().getSuggestTimeInMillis() / 1000.0, indexName, context);

        catalog.setClusterGauge("index_requestcache_memory_size_bytes", idx.getRequestCache().getMemorySizeInBytes(), indexName, context);
        catalog.setClusterGauge("index_requestcache_hit_count", idx.getRequestCache().getHitCount(), indexName, context);
        catalog.setClusterGauge("index_requestcache_miss_count", idx.getRequestCache().getMissCount(), indexName, context);
        catalog.setClusterGauge("index_requestcache_evictions_count", idx.getRequestCache().getEvictions(), indexName, context);

        catalog.setClusterGauge("index_recovery_current_number", idx.getRecoveryStats().currentAsSource(), "source", indexName, context);
        catalog.setClusterGauge("index_recovery_current_number", idx.getRecoveryStats().currentAsTarget(), "target", indexName, context);
        catalog.setClusterGauge("index_recovery_throttle_time_seconds", idx.getRecoveryStats().throttleTime().getSeconds(), indexName, context);

        catalog.setClusterGauge("index_translog_operations_number", idx.getTranslog().estimatedNumberOfOperations(), indexName, context);
        catalog.setClusterGauge("index_translog_size_bytes", idx.getTranslog().getTranslogSizeInBytes(), indexName, context);
        catalog.setClusterGauge("index_translog_uncommitted_operations_number", idx.getTranslog().getUncommittedOperations(), indexName, context);
        catalog.setClusterGauge("index_translog_uncommitted_size_bytes", idx.getTranslog().getUncommittedSizeInBytes(), indexName, context);

        catalog.setClusterGauge("index_warmer_current_number", idx.getWarmer().current(), indexName, context);
        catalog.setClusterGauge("index_warmer_time_seconds", idx.getWarmer().totalTimeInMillis(), indexName, context);
        catalog.setClusterGauge("index_warmer_count", idx.getWarmer().total(), indexName, context);
    }

    private void registerTransportMetrics() {
        catalog.registerNodeGauge("transport_server_open_number", "Opened server connections");

        catalog.registerNodeGauge("transport_rx_packets_count", "Received packets");
        catalog.registerNodeGauge("transport_tx_packets_count", "Sent packets");

        catalog.registerNodeGauge("transport_rx_bytes_count", "Bytes received");
        catalog.registerNodeGauge("transport_tx_bytes_count", "Bytes sent");
    }

    private void updateTransportMetrics(TransportStats ts) {
        if (ts != null) {
            catalog.setNodeGauge("transport_server_open_number", ts.getServerOpen());

            catalog.setNodeGauge("transport_rx_packets_count", ts.getRxCount());
            catalog.setNodeGauge("transport_tx_packets_count", ts.getTxCount());

            catalog.setNodeGauge("transport_rx_bytes_count", ts.getRxSize().getBytes());
            catalog.setNodeGauge("transport_tx_bytes_count", ts.getTxSize().getBytes());
        }
    }

    private void registerHTTPMetrics() {
        catalog.registerNodeGauge("http_open_server_number", "Number of open server connections");
        catalog.registerNodeGauge("http_open_total_count", "Count of opened connections");
    }

    private void updateHTTPMetrics(HttpStats http) {
        if (http != null) {
            catalog.setNodeGauge("http_open_server_number", http.getServerOpen());
            catalog.setNodeGauge("http_open_total_count", http.getTotalOpen());
        }
    }

    private void registerThreadPoolMetrics() {
        catalog.registerNodeGauge("threadpool_threads_number", "Number of threads in thread pool", "name", "type");
        catalog.registerNodeGauge("threadpool_threads_count", "Count of threads in thread pool", "name", "type");
        catalog.registerNodeGauge("threadpool_tasks_number", "Number of tasks in thread pool", "name", "type");
    }

    private void updateThreadPoolMetrics(ThreadPoolStats tps) {
        if (tps != null) {
            for (ThreadPoolStats.Stats st : tps) {
                String name = st.getName();
                catalog.setNodeGauge("threadpool_threads_number", st.getThreads(), name, "threads");
                catalog.setNodeGauge("threadpool_threads_number", st.getActive(), name, "active");
                catalog.setNodeGauge("threadpool_threads_number", st.getLargest(), name, "largest");
                catalog.setNodeGauge("threadpool_threads_count", st.getCompleted(), name, "completed");
                catalog.setNodeGauge("threadpool_threads_count", st.getRejected(), name, "rejected");
                catalog.setNodeGauge("threadpool_tasks_number", st.getQueue(), name, "queue");
            }
        }
    }

    @SuppressWarnings("checkstyle:LineLength")
    private void registerIngestMetrics() {
        catalog.registerNodeGauge("ingest_total_count", "Ingestion total number");
        catalog.registerNodeGauge("ingest_total_time_seconds", "Ingestion total time in seconds");
        catalog.registerNodeGauge("ingest_total_current", "Ingestion total current");
        catalog.registerNodeGauge("ingest_total_failed_count", "Ingestion total failed");

        catalog.registerNodeGauge("ingest_pipeline_total_count", "Ingestion total number", "pipeline");
        catalog.registerNodeGauge("ingest_pipeline_total_time_seconds", "Ingestion total time in seconds", "pipeline");
        catalog.registerNodeGauge("ingest_pipeline_total_current", "Ingestion total current", "pipeline");
        catalog.registerNodeGauge("ingest_pipeline_total_failed_count", "Ingestion total failed", "pipeline");

        catalog.registerNodeGauge("ingest_pipeline_processor_total_count", "Ingestion total number", "pipeline", "processor");
        catalog.registerNodeGauge("ingest_pipeline_processor_total_time_seconds", "Ingestion total time in seconds", "pipeline", "processor");
        catalog.registerNodeGauge("ingest_pipeline_processor_total_current", "Ingestion total current", "pipeline", "processor");
        catalog.registerNodeGauge("ingest_pipeline_processor_total_failed_count", "Ingestion total failed", "pipeline", "processor");
    }

    @SuppressWarnings("checkstyle:LineLength")
    private void updateIngestMetrics(IngestStats is) {
        if (is != null) {
            catalog.setNodeGauge("ingest_total_count", is.getTotalStats().getIngestCount());
            catalog.setNodeGauge("ingest_total_time_seconds", is.getTotalStats().getIngestTimeInMillis() / 1000.0);
            catalog.setNodeGauge("ingest_total_current", is.getTotalStats().getIngestCurrent());
            catalog.setNodeGauge("ingest_total_failed_count", is.getTotalStats().getIngestFailedCount());

            for (IngestStats.PipelineStat st : is.getPipelineStats()) {
                String pipeline = st.getPipelineId();
                catalog.setNodeGauge("ingest_pipeline_total_count", st.getStats().getIngestCount(), pipeline);
                catalog.setNodeGauge("ingest_pipeline_total_time_seconds", st.getStats().getIngestTimeInMillis() / 1000.0,
                        pipeline);
                catalog.setNodeGauge("ingest_pipeline_total_current", st.getStats().getIngestCurrent(), pipeline);
                catalog.setNodeGauge("ingest_pipeline_total_failed_count", st.getStats().getIngestFailedCount(), pipeline);

                List<IngestStats.ProcessorStat> pss = is.getProcessorStats().get(pipeline);
                if (pss != null) {
                    for (IngestStats.ProcessorStat ps : pss) {
                        String processor = ps.getName();
                        catalog.setNodeGauge("ingest_pipeline_processor_total_count", ps.getStats().getIngestCount(), pipeline, processor);
                        catalog.setNodeGauge("ingest_pipeline_processor_total_time_seconds", ps.getStats().getIngestTimeInMillis() / 1000.0,
                                pipeline, processor);
                        catalog.setNodeGauge("ingest_pipeline_processor_total_current", ps.getStats().getIngestCurrent(), pipeline, processor);
                        catalog.setNodeGauge("ingest_pipeline_processor_total_failed_count", ps.getStats().getIngestFailedCount(), pipeline, processor);
                    }
                }
            }
        }
    }

    private void registerCircuitBreakerMetrics() {
        catalog.registerNodeGauge("circuitbreaker_estimated_bytes", "Circuit breaker estimated size", "name");
        catalog.registerNodeGauge("circuitbreaker_limit_bytes", "Circuit breaker size limit", "name");
        catalog.registerNodeGauge("circuitbreaker_overhead_ratio", "Circuit breaker overhead ratio", "name");
        catalog.registerNodeGauge("circuitbreaker_tripped_count", "Circuit breaker tripped count", "name");
    }

    private void updateCircuitBreakersMetrics(AllCircuitBreakerStats acbs) {
        if (acbs != null) {
            for (CircuitBreakerStats cbs : acbs.getAllStats()) {
                String name = cbs.getName();
                catalog.setNodeGauge("circuitbreaker_estimated_bytes", cbs.getEstimated(), name);
                catalog.setNodeGauge("circuitbreaker_limit_bytes", cbs.getLimit(), name);
                catalog.setNodeGauge("circuitbreaker_overhead_ratio", cbs.getOverhead(), name);
                catalog.setNodeGauge("circuitbreaker_tripped_count", cbs.getTrippedCount(), name);
            }
        }
    }

    private void registerScriptMetrics() {
        catalog.registerNodeGauge("script_cache_evictions_count", "Number of evictions in scripts cache");
        catalog.registerNodeGauge("script_compilations_count", "Number of scripts compilations");
    }

    private void updateScriptMetrics(ScriptStats sc) {
        if (sc != null) {
            catalog.setNodeGauge("script_cache_evictions_count", sc.getCacheEvictions());
            catalog.setNodeGauge("script_compilations_count", sc.getCompilations());
        }
    }

    private void registerProcessMetrics() {
        catalog.registerNodeGauge("process_cpu_percent", "CPU percentage used by ES process");
        catalog.registerNodeGauge("process_cpu_time_seconds", "CPU time used by ES process");

        catalog.registerNodeGauge("process_mem_total_virtual_bytes", "Memory used by ES process");

        catalog.registerNodeGauge("process_file_descriptors_open_number", "Open file descriptors");
        catalog.registerNodeGauge("process_file_descriptors_max_number", "Max file descriptors");
    }

    private void updateProcessMetrics(ProcessStats ps) {
        if (ps != null) {
            catalog.setNodeGauge("process_cpu_percent", ps.getCpu().getPercent());
            catalog.setNodeGauge("process_cpu_time_seconds", ps.getCpu().getTotal().getSeconds());

            catalog.setNodeGauge("process_mem_total_virtual_bytes", ps.getMem().getTotalVirtual().getBytes());

            catalog.setNodeGauge("process_file_descriptors_open_number", ps.getOpenFileDescriptors());
            catalog.setNodeGauge("process_file_descriptors_max_number", ps.getMaxFileDescriptors());
        }
    }

    private void registerJVMMetrics() {
        catalog.registerNodeGauge("jvm_uptime_seconds", "JVM uptime");
        catalog.registerNodeGauge("jvm_mem_heap_max_bytes", "Maximum used memory in heap");
        catalog.registerNodeGauge("jvm_mem_heap_used_bytes", "Memory used in heap");
        catalog.registerNodeGauge("jvm_mem_heap_used_percent", "Percentage of memory used in heap");
        catalog.registerNodeGauge("jvm_mem_nonheap_used_bytes", "Memory used apart from heap");
        catalog.registerNodeGauge("jvm_mem_heap_committed_bytes", "Committed bytes in heap");
        catalog.registerNodeGauge("jvm_mem_nonheap_committed_bytes", "Committed bytes apart from heap");

        catalog.registerNodeGauge("jvm_mem_pool_max_bytes", "Maximum usage of memory pool", "pool");
        catalog.registerNodeGauge("jvm_mem_pool_peak_max_bytes", "Maximum usage peak of memory pool", "pool");
        catalog.registerNodeGauge("jvm_mem_pool_used_bytes", "Used memory in memory pool", "pool");
        catalog.registerNodeGauge("jvm_mem_pool_peak_used_bytes", "Used memory peak in memory pool", "pool");

        catalog.registerNodeGauge("jvm_threads_number", "Number of threads");
        catalog.registerNodeGauge("jvm_threads_peak_number", "Peak number of threads");

        catalog.registerNodeGauge("jvm_gc_collection_count", "Count of GC collections", "gc");
        catalog.registerNodeGauge("jvm_gc_collection_time_seconds", "Time spent for GC collections", "gc");

        catalog.registerNodeGauge("jvm_bufferpool_number", "Number of buffer pools", "bufferpool");
        catalog.registerNodeGauge("jvm_bufferpool_total_capacity_bytes", "Total capacity provided by buffer pools",
                "bufferpool");
        catalog.registerNodeGauge("jvm_bufferpool_used_bytes", "Used memory in buffer pools", "bufferpool");

        catalog.registerNodeGauge("jvm_classes_loaded_number", "Count of loaded classes");
        catalog.registerNodeGauge("jvm_classes_total_loaded_number", "Total count of loaded classes");
        catalog.registerNodeGauge("jvm_classes_unloaded_number", "Count of unloaded classes");
    }

    private void updateJVMMetrics(JvmStats jvm) {
        if (jvm != null) {
            catalog.setNodeGauge("jvm_uptime_seconds", jvm.getUptime().getSeconds());

            catalog.setNodeGauge("jvm_mem_heap_max_bytes", jvm.getMem().getHeapMax().getBytes());
            catalog.setNodeGauge("jvm_mem_heap_used_bytes", jvm.getMem().getHeapUsed().getBytes());
            catalog.setNodeGauge("jvm_mem_heap_used_percent", jvm.getMem().getHeapUsedPercent());
            catalog.setNodeGauge("jvm_mem_nonheap_used_bytes", jvm.getMem().getNonHeapUsed().getBytes());
            catalog.setNodeGauge("jvm_mem_heap_committed_bytes", jvm.getMem().getHeapCommitted().getBytes());
            catalog.setNodeGauge("jvm_mem_nonheap_committed_bytes", jvm.getMem().getNonHeapCommitted().getBytes());

            for (JvmStats.MemoryPool mp : jvm.getMem()) {
                String name = mp.getName();
                catalog.setNodeGauge("jvm_mem_pool_max_bytes", mp.getMax().getBytes(), name);
                catalog.setNodeGauge("jvm_mem_pool_peak_max_bytes", mp.getPeakMax().getBytes(), name);
                catalog.setNodeGauge("jvm_mem_pool_used_bytes", mp.getUsed().getBytes(), name);
                catalog.setNodeGauge("jvm_mem_pool_peak_used_bytes", mp.getPeakUsed().getBytes(), name);
            }

            catalog.setNodeGauge("jvm_threads_number", jvm.getThreads().getCount());
            catalog.setNodeGauge("jvm_threads_peak_number", jvm.getThreads().getPeakCount());

            for (JvmStats.GarbageCollector gc : jvm.getGc().getCollectors()) {
                String name = gc.getName();
                catalog.setNodeGauge("jvm_gc_collection_count", gc.getCollectionCount(), name);
                catalog.setNodeGauge("jvm_gc_collection_time_seconds", gc.getCollectionTime().getSeconds(), name);
            }

            for (JvmStats.BufferPool bp : jvm.getBufferPools()) {
                String name = bp.getName();
                catalog.setNodeGauge("jvm_bufferpool_number", bp.getCount(), name);
                catalog.setNodeGauge("jvm_bufferpool_total_capacity_bytes", bp.getTotalCapacity().getBytes(), name);
                catalog.setNodeGauge("jvm_bufferpool_used_bytes", bp.getUsed().getBytes(), name);
            }
            if (jvm.getClasses() != null) {
                catalog.setNodeGauge("jvm_classes_loaded_number", jvm.getClasses().getLoadedClassCount());
                catalog.setNodeGauge("jvm_classes_total_loaded_number", jvm.getClasses().getTotalLoadedClassCount());
                catalog.setNodeGauge("jvm_classes_unloaded_number", jvm.getClasses().getUnloadedClassCount());
            }
        }
    }

    private void registerOsMetrics() {
        catalog.registerNodeGauge("os_cpu_percent", "CPU usage in percent");

        catalog.registerNodeGauge("os_load_average_one_minute", "CPU load");
        catalog.registerNodeGauge("os_load_average_five_minutes", "CPU load");
        catalog.registerNodeGauge("os_load_average_fifteen_minutes", "CPU load");

        catalog.registerNodeGauge("os_mem_free_bytes", "Memory free");
        catalog.registerNodeGauge("os_mem_free_percent", "Memory free in percent");
        catalog.registerNodeGauge("os_mem_used_bytes", "Memory used");
        catalog.registerNodeGauge("os_mem_used_percent", "Memory used in percent");
        catalog.registerNodeGauge("os_mem_total_bytes", "Total memory size");

        catalog.registerNodeGauge("os_swap_free_bytes", "Swap free");
        catalog.registerNodeGauge("os_swap_used_bytes", "Swap used");
        catalog.registerNodeGauge("os_swap_total_bytes", "Total swap size");
    }

    private void updateOsMetrics(OsStats os) {
        if (os != null) {
            if (os.getCpu() != null) {
                catalog.setNodeGauge("os_cpu_percent", os.getCpu().getPercent());
                double[] loadAverage = os.getCpu().getLoadAverage();
                if (loadAverage != null && loadAverage.length == 3) {
                    catalog.setNodeGauge("os_load_average_one_minute", os.getCpu().getLoadAverage()[0]);
                    catalog.setNodeGauge("os_load_average_five_minutes", os.getCpu().getLoadAverage()[1]);
                    catalog.setNodeGauge("os_load_average_fifteen_minutes", os.getCpu().getLoadAverage()[2]);
                }
            }

            if (os.getMem() != null) {
                catalog.setNodeGauge("os_mem_free_bytes", os.getMem().getFree().getBytes());
                catalog.setNodeGauge("os_mem_free_percent", os.getMem().getFreePercent());
                catalog.setNodeGauge("os_mem_used_bytes", os.getMem().getUsed().getBytes());
                catalog.setNodeGauge("os_mem_used_percent", os.getMem().getUsedPercent());
                catalog.setNodeGauge("os_mem_total_bytes", os.getMem().getTotal().getBytes());
            }

            if (os.getSwap() != null) {
                catalog.setNodeGauge("os_swap_free_bytes", os.getSwap().getFree().getBytes());
                catalog.setNodeGauge("os_swap_used_bytes", os.getSwap().getUsed().getBytes());
                catalog.setNodeGauge("os_swap_total_bytes", os.getSwap().getTotal().getBytes());
            }
        }
    }

    private void registerFsMetrics() {
        catalog.registerNodeGauge("fs_total_total_bytes", "Total disk space for all mount points");
        catalog.registerNodeGauge("fs_total_available_bytes", "Available disk space for all mount points");
        catalog.registerNodeGauge("fs_total_free_bytes", "Free disk space for all mountpoints");

        catalog.registerNodeGauge("fs_most_usage_free_bytes", "Free disk space for most used mountpoint", "path");
        catalog.registerNodeGauge("fs_most_usage_total_bytes", "Total disk space for most used mountpoint", "path");

        catalog.registerNodeGauge("fs_least_usage_free_bytes", "Free disk space for least used mountpoint", "path");
        catalog.registerNodeGauge("fs_least_usage_total_bytes", "Total disk space for least used mountpoint", "path");

        catalog.registerNodeGauge("fs_path_total_bytes", "Total disk space", "path", "mount", "type");
        catalog.registerNodeGauge("fs_path_available_bytes", "Available disk space", "path", "mount", "type");
        catalog.registerNodeGauge("fs_path_free_bytes", "Free disk space", "path", "mount", "type");

        catalog.registerNodeGauge("fs_io_total_operations", "Total IO operations");
        catalog.registerNodeGauge("fs_io_total_read_operations", "Total IO read operations");
        catalog.registerNodeGauge("fs_io_total_write_operations", "Total IO write operations");
        catalog.registerNodeGauge("fs_io_total_read_bytes", "Total IO read bytes");
        catalog.registerNodeGauge("fs_io_total_write_bytes", "Total IO write bytes");
    }

    private void updateFsMetrics(FsInfo fs) {
        if (fs != null) {
            catalog.setNodeGauge("fs_total_total_bytes", fs.getTotal().getTotal().getBytes());
            catalog.setNodeGauge("fs_total_available_bytes", fs.getTotal().getAvailable().getBytes());
            catalog.setNodeGauge("fs_total_free_bytes", fs.getTotal().getFree().getBytes());

            if (fs.getMostDiskEstimate() != null) {
                catalog.setNodeGauge("fs_most_usage_free_bytes", fs.getMostDiskEstimate().getFreeBytes(),
                        fs.getMostDiskEstimate().getPath());
                catalog.setNodeGauge("fs_most_usage_total_bytes", fs.getMostDiskEstimate().getTotalBytes(),
                        fs.getMostDiskEstimate().getPath());
            }

            if (fs.getLeastDiskEstimate() != null) {
                catalog.setNodeGauge("fs_least_usage_free_bytes", fs.getLeastDiskEstimate().getFreeBytes(),
                        fs.getLeastDiskEstimate().getPath());
                catalog.setNodeGauge("fs_least_usage_total_bytes", fs.getLeastDiskEstimate().getTotalBytes(),
                        fs.getLeastDiskEstimate().getPath());
            }

            for (FsInfo.Path fspath : fs) {
                String path = fspath.getPath();
                String mount = fspath.getMount();
                String type = fspath.getType();
                catalog.setNodeGauge("fs_path_total_bytes", fspath.getTotal().getBytes(), path, mount, type);
                catalog.setNodeGauge("fs_path_available_bytes", fspath.getAvailable().getBytes(), path, mount, type);
                catalog.setNodeGauge("fs_path_free_bytes", fspath.getFree().getBytes(), path, mount, type);
            }

            FsInfo.IoStats ioStats = fs.getIoStats();
            if (ioStats != null) {
                catalog.setNodeGauge("fs_io_total_operations", fs.getIoStats().getTotalOperations());
                catalog.setNodeGauge("fs_io_total_read_operations", fs.getIoStats().getTotalReadOperations());
                catalog.setNodeGauge("fs_io_total_write_operations", fs.getIoStats().getTotalWriteOperations());
                catalog.setNodeGauge("fs_io_total_read_bytes", fs.getIoStats().getTotalReadKilobytes() * 1024);
                catalog.setNodeGauge("fs_io_total_write_bytes", fs.getIoStats().getTotalWriteKilobytes() * 1024);
            }
        }
    }

    @SuppressWarnings("checkstyle:LineLength")
    private void registerESSettings() {
        catalog.registerClusterGauge("cluster_routing_allocation_disk_threshold_enabled", "Disk allocation decider is enabled");
        //
        catalog.registerClusterGauge("cluster_routing_allocation_disk_watermark_low_bytes", "Low watermark for disk usage in bytes");
        catalog.registerClusterGauge("cluster_routing_allocation_disk_watermark_high_bytes", "High watermark for disk usage in bytes");
        catalog.registerClusterGauge("cluster_routing_allocation_disk_watermark_flood_stage_bytes", "Flood stage for disk usage in bytes");
        //
        catalog.registerClusterGauge("cluster_routing_allocation_disk_watermark_low_pct", "Low watermark for disk usage in pct");
        catalog.registerClusterGauge("cluster_routing_allocation_disk_watermark_high_pct", "High watermark for disk usage in pct");
        catalog.registerClusterGauge("cluster_routing_allocation_disk_watermark_flood_stage_pct", "Flood stage watermark for disk usage in pct");
    }

    @SuppressWarnings({"checkstyle:LineLength", "checkstyle:LeftCurly"})
    private void updateESSettings(ClusterStatsData stats) {
        if (stats != null) {
            catalog.setClusterGauge("cluster_routing_allocation_disk_threshold_enabled", Boolean.TRUE.equals(stats.getThresholdEnabled()) ? 1 : 0);
            // According to Elasticsearch documentation the following settings must be set either in pct or bytes size.
            // Mixing is not allowed. We rely on Elasticsearch to do all necessary checks and we simply
            // output all those metrics that are not null. If this will lead to mixed metric then we do not
            // consider it our fault.
            if (stats.getDiskLowInBytes() != null) { catalog.setClusterGauge("cluster_routing_allocation_disk_watermark_low_bytes", stats.getDiskLowInBytes()); }
            if (stats.getDiskHighInBytes() != null) { catalog.setClusterGauge("cluster_routing_allocation_disk_watermark_high_bytes", stats.getDiskHighInBytes()); }
            if (stats.getFloodStageInBytes() != null) { catalog.setClusterGauge("cluster_routing_allocation_disk_watermark_flood_stage_bytes", stats.getFloodStageInBytes()); }
            //
            if (stats.getDiskLowInPct() != null) { catalog.setClusterGauge("cluster_routing_allocation_disk_watermark_low_pct", stats.getDiskLowInPct()); }
            if (stats.getDiskHighInPct() != null) { catalog.setClusterGauge("cluster_routing_allocation_disk_watermark_high_pct", stats.getDiskHighInPct()); }
            if (stats.getFloodStageInPct() != null) { catalog.setClusterGauge("cluster_routing_allocation_disk_watermark_flood_stage_pct", stats.getFloodStageInPct()); }
        }
    }

    public void updateMetrics(ClusterHealthResponse clusterHealthResponse, NodeStats nodeStats,
                              IndicesStatsResponse indicesStats, ClusterStatsData clusterStatsData) {
        Summary.Timer timer = catalog.startSummaryTimer("metrics_generate_time_seconds");

        updateClusterMetrics(clusterHealthResponse);
        updateNodeMetrics(nodeStats);
        updateIndicesMetrics(nodeStats.getIndices());
        if (isPrometheusIndices) {
            updatePerIndexMetrics(clusterHealthResponse, indicesStats);
        }
        updateTransportMetrics(nodeStats.getTransport());
        updateHTTPMetrics(nodeStats.getHttp());
        updateThreadPoolMetrics(nodeStats.getThreadPool());
        updateIngestMetrics(nodeStats.getIngestStats());
        updateCircuitBreakersMetrics(nodeStats.getBreaker());
        updateScriptMetrics(nodeStats.getScriptStats());
        updateProcessMetrics(nodeStats.getProcess());
        updateJVMMetrics(nodeStats.getJvm());
        updateOsMetrics(nodeStats.getOs());
        updateFsMetrics(nodeStats.getFs());
        if (isPrometheusClusterSettings) {
            updateESSettings(clusterStatsData);
        }

        timer.observeDuration();
    }

    public PrometheusMetricsCatalog getCatalog() {
        return catalog;
    }
}
