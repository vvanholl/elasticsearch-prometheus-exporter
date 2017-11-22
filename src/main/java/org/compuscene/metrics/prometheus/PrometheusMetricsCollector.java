package org.compuscene.metrics.prometheus;

import io.prometheus.client.Summary;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.action.ActionFuture;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthRequest;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.admin.cluster.node.stats.NodeStats;
import org.elasticsearch.action.admin.cluster.node.stats.NodesStatsRequest;
import org.elasticsearch.action.admin.cluster.node.stats.NodesStatsResponse;
import org.elasticsearch.action.admin.indices.stats.CommonStats;
import org.elasticsearch.action.admin.indices.stats.IndexStats;
import org.elasticsearch.action.admin.indices.stats.IndicesStatsRequest;
import org.elasticsearch.action.admin.indices.stats.IndicesStatsResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.cluster.health.ClusterIndexHealth;
import org.elasticsearch.cluster.node.DiscoveryNode;
import org.elasticsearch.cluster.node.DiscoveryNode.Role;
import org.elasticsearch.common.collect.HppcMaps;
import org.elasticsearch.common.logging.ESLoggerFactory;
import org.elasticsearch.common.settings.Setting;
import org.elasticsearch.common.settings.Setting.Property;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.http.HttpStats;
import org.elasticsearch.indices.NodeIndicesStats;
import org.elasticsearch.indices.breaker.AllCircuitBreakerStats;
import org.elasticsearch.indices.breaker.CircuitBreakerStats;
import org.elasticsearch.ingest.IngestStats;
import org.elasticsearch.monitor.fs.FsInfo;
import org.elasticsearch.monitor.jvm.JvmStats;
import org.elasticsearch.monitor.os.OsStats;
import org.elasticsearch.monitor.process.ProcessStats;
import org.elasticsearch.rest.prometheus.RestPrometheusMetricsAction;
import org.elasticsearch.script.ScriptStats;
import org.elasticsearch.threadpool.ThreadPoolStats;
import org.elasticsearch.transport.TransportStats;

import java.util.HashMap;
import java.util.Map;

public class PrometheusMetricsCollector {
    public static final Setting<Boolean> PROMETHEUS_INDICES = Setting.boolSetting("prometheus.indices", true, Property.NodeScope);

    private final static Logger logger = ESLoggerFactory.getLogger(RestPrometheusMetricsAction.class.getSimpleName());

    private final Settings settings;
    private final Client client;

    private String cluster;
    private String node;

    private PrometheusMetricsCatalog catalog;

    public PrometheusMetricsCollector(Settings settings, final Client client) {
        this.settings = settings;
        this.client = client;

        NodesStatsRequest nodesStatsRequest = new NodesStatsRequest("_local").all();
        NodesStatsResponse nodesStatsResponse = this.client.admin().cluster().nodesStats(nodesStatsRequest).actionGet();

        cluster = nodesStatsResponse.getClusterName().value();
        node = nodesStatsResponse.getNodes().get(0).getNode().getName();
        catalog = new PrometheusMetricsCatalog(cluster, "es_");

        registerMetrics();
    }

    private void registerMetrics() {
        catalog.registerSummaryTimer("metrics_generate_time_seconds", "Time spent while generating metrics", "node");

        registerClusterMetrics();
        registerNodeMetrics();
        registerJVMMetrics();
        registerIndicesMetrics();
        registerIngestMetrics();
        registerTransportMetrics();
        registerHTTPMetrics();
        registerScriptMetrics();
        registerProcessMetrics();
        registerOsMetrics();
        registerCircuitBreakerMetrics();
        registerThreadPoolMetrics();
        registerFsMetrics();
        if (PROMETHEUS_INDICES.get(settings)) {
            registerPerIndexMetrics();
        }
    }

    private void registerClusterMetrics() {
        catalog.registerGauge("cluster_status", "Cluster status");
        catalog.registerGauge("cluster_nodes_number", "Number of nodes in the cluster");
        catalog.registerGauge("cluster_datanodes_number", "Number of data nodes in the cluster");
        catalog.registerGauge("cluster_shards_active_percent", "Percent of active shards");
        catalog.registerGauge("cluster_shards_number", "Number of shards", "type");
        catalog.registerGauge("cluster_pending_tasks_number", "Number of pending tasks");
        catalog.registerGauge("cluster_task_max_waiting_time_seconds", "Max waiting time for tasks");
        catalog.registerGauge("cluster_is_timedout_bool", "Is the cluster timed out ?");
        catalog.registerGauge("cluster_inflight_fetch_number", "Number of in flight fetches");
    }

    private void updateClusterMetrics(ClusterHealthResponse res) {
        catalog.setGauge("cluster_status", res.getStatus().value());

        catalog.setGauge("cluster_nodes_number", res.getNumberOfNodes());
        catalog.setGauge("cluster_datanodes_number", res.getNumberOfDataNodes());

        catalog.setGauge("cluster_shards_active_percent", res.getActiveShardsPercent());

        catalog.setGauge("cluster_shards_number", res.getActiveShards(), "active");
        catalog.setGauge("cluster_shards_number", res.getActivePrimaryShards(), "active_primary");
        catalog.setGauge("cluster_shards_number", res.getDelayedUnassignedShards(), "unassigned");
        catalog.setGauge("cluster_shards_number", res.getInitializingShards(), "initializing");
        catalog.setGauge("cluster_shards_number", res.getRelocatingShards(), "relocating");
        catalog.setGauge("cluster_shards_number", res.getUnassignedShards(), "unassigned");

        catalog.setGauge("cluster_pending_tasks_number", res.getNumberOfPendingTasks());
        catalog.setGauge("cluster_task_max_waiting_time_seconds", res.getTaskMaxWaitingTime().getSeconds());

        catalog.setGauge("cluster_is_timedout_bool", res.isTimedOut() ? 1 : 0);

        catalog.setGauge("cluster_inflight_fetch_number", res.getNumberOfInFlightFetch());
    }

    private void registerNodeMetrics() {
        catalog.registerGauge("node_role", "Node role", "node", "role");
    }

    private void updateNodeMetrics(NodeStats ns) {
        Map<String, Integer> roles = new HashMap<>();

        roles.put("master", 0);
        roles.put("data", 0);
        roles.put("ingest", 0);

        for (Role r : ns.getNode().getRoles()) {
            roles.put(r.getRoleName(), 1);
        }

        for (String k : roles.keySet()) {
            catalog.setGauge("node_role", roles.get(k), node, k);
        }
    }

    private void registerJVMMetrics() {
        catalog.registerGauge("jvm_uptime_seconds", "JVM uptime", "node");
        catalog.registerGauge("jvm_mem_heap_max_bytes", "Maximum used memory in heap", "node");
        catalog.registerGauge("jvm_mem_heap_used_bytes", "Memory used in heap", "node");
        catalog.registerGauge("jvm_mem_heap_used_percent", "Percentage of memory used in heap", "node");
        catalog.registerGauge("jvm_mem_nonheap_used_bytes", "Memory used apart from heap", "node");
        catalog.registerGauge("jvm_mem_heap_committed_bytes", "Committed bytes in heap", "node");
        catalog.registerGauge("jvm_mem_nonheap_committed_bytes", "Committed bytes apart from heap", "node");

        catalog.registerGauge("jvm_mem_pool_max_bytes", "Maximum usage of memory pool", "node", "pool");
        catalog.registerGauge("jvm_mem_pool_peak_max_bytes", "Maximum usage peak of memory pool", "node", "pool");
        catalog.registerGauge("jvm_mem_pool_used_bytes", "Used memory in memory pool", "node", "pool");
        catalog.registerGauge("jvm_mem_pool_peak_used_bytes", "Used memory peak in memory pool", "node", "pool");

        catalog.registerGauge("jvm_threads_number", "Number of threads", "node");
        catalog.registerGauge("jvm_threads_peak_number", "Peak number of threads", "node");

        catalog.registerGauge("jvm_gc_collection_count", "Count of GC collections", "node", "gc");
        catalog.registerGauge("jvm_gc_collection_time_seconds", "Time spent for GC collections", "node", "gc");

        catalog.registerGauge("jvm_bufferpool_number", "Number of buffer pools", "node", "bufferpool");
        catalog.registerGauge("jvm_bufferpool_total_capacity_bytes", "Total capacity provided by buffer pools", "node", "bufferpool");
        catalog.registerGauge("jvm_bufferpool_used_bytes", "Used memory in buffer pools", "node", "bufferpool");

        catalog.registerGauge("jvm_classes_loaded_number", "Count of loaded classes", "node");
        catalog.registerGauge("jvm_classes_total_loaded_number", "Total count of loaded classes", "node");
        catalog.registerGauge("jvm_classes_unloaded_number", "Count of unloaded classes", "node");
    }

    private void updateJVMMetrics(JvmStats jvm) {
        if (jvm != null) {
            catalog.setGauge("jvm_uptime_seconds", jvm.getUptime().getSeconds(), node);

            catalog.setGauge("jvm_mem_heap_max_bytes", jvm.getMem().getHeapMax().getBytes(), node);
            catalog.setGauge("jvm_mem_heap_used_bytes", jvm.getMem().getHeapUsed().getBytes(), node);
            catalog.setGauge("jvm_mem_heap_used_percent", jvm.getMem().getHeapUsedPercent(), node);
            catalog.setGauge("jvm_mem_nonheap_used_bytes", jvm.getMem().getNonHeapUsed().getBytes(), node);
            catalog.setGauge("jvm_mem_heap_committed_bytes", jvm.getMem().getHeapCommitted().getBytes(), node);
            catalog.setGauge("jvm_mem_nonheap_committed_bytes", jvm.getMem().getNonHeapCommitted().getBytes(), node);

            for (JvmStats.MemoryPool mp : jvm.getMem()) {
                String name = mp.getName();
                catalog.setGauge("jvm_mem_pool_max_bytes", mp.getMax().getBytes(), node, name);
                catalog.setGauge("jvm_mem_pool_peak_max_bytes", mp.getPeakMax().getBytes(), node, name);
                catalog.setGauge("jvm_mem_pool_used_bytes", mp.getUsed().getBytes(), node, name);
                catalog.setGauge("jvm_mem_pool_peak_used_bytes", mp.getPeakUsed().getBytes(), node, name);
            }

            catalog.setGauge("jvm_threads_number", jvm.getThreads().getCount(), node);
            catalog.setGauge("jvm_threads_peak_number", jvm.getThreads().getPeakCount(), node);

            for (JvmStats.GarbageCollector gc : jvm.getGc().getCollectors()) {
                String name = gc.getName();
                catalog.setGauge("jvm_gc_collection_count", gc.getCollectionCount(), node, name);
                catalog.setGauge("jvm_gc_collection_time_seconds", gc.getCollectionTime().getSeconds(), node, name);
            }

            for (JvmStats.BufferPool bp : jvm.getBufferPools()) {
                String name = bp.getName();
                catalog.setGauge("jvm_bufferpool_number", bp.getCount(), node, name);
                catalog.setGauge("jvm_bufferpool_total_capacity_bytes", bp.getTotalCapacity().getBytes(), node, name);
                catalog.setGauge("jvm_bufferpool_used_bytes", bp.getUsed().getBytes(), node, name);
            }
            if (jvm.getClasses() != null) {
                catalog.setGauge("jvm_classes_loaded_number", jvm.getClasses().getLoadedClassCount(), node);
                catalog.setGauge("jvm_classes_total_loaded_number", jvm.getClasses().getTotalLoadedClassCount(), node);
                catalog.setGauge("jvm_classes_unloaded_number", jvm.getClasses().getUnloadedClassCount(), node);
            }
        }
    }

    private void registerIndicesMetrics() {
        catalog.registerGauge("indices_doc_number", "Total number of documents", "node");
        catalog.registerGauge("indices_doc_deleted_number", "Number of deleted documents", "node");

        catalog.registerGauge("indices_store_size_bytes", "Store size of the indices in bytes", "node");

        catalog.registerGauge("indices_indexing_delete_count", "Count of documents deleted", "node");
        catalog.registerGauge("indices_indexing_delete_current_number", "Current rate of documents deleted", "node");
        catalog.registerGauge("indices_indexing_delete_time_seconds", "Time spent while deleting documents", "node");
        catalog.registerGauge("indices_indexing_index_count", "Count of documents indexed", "node");
        catalog.registerGauge("indices_indexing_index_current_number", "Current rate of documents indexed", "node");
        catalog.registerGauge("indices_indexing_index_failed_count", "Count of failed to index documents", "node");
        catalog.registerGauge("indices_indexing_index_time_seconds", "Time spent while indexing documents", "node");
        catalog.registerGauge("indices_indexing_noop_update_count", "Count of noop document updates", "node");
        catalog.registerGauge("indices_indexing_is_throttled_bool", "Is indexing throttling ?", "node");
        catalog.registerGauge("indices_indexing_throttle_time_seconds", "Time spent while throttling", "node");

        catalog.registerGauge("indices_get_count", "Count of get commands", "node");
        catalog.registerGauge("indices_get_time_seconds", "Time spent while get commands", "node");
        catalog.registerGauge("indices_get_exists_count", "Count of existing documents when get command", "node");
        catalog.registerGauge("indices_get_exists_time_seconds", "Time spent while existing documents get command", "node");
        catalog.registerGauge("indices_get_missing_count", "Count of missing documents when get command", "node");
        catalog.registerGauge("indices_get_missing_time_seconds", "Time spent while missing documents get command", "node");
        catalog.registerGauge("indices_get_current_number", "Current rate of get commands", "node");

        catalog.registerGauge("indices_search_open_contexts_number", "Number of search open contexts", "node");
        catalog.registerGauge("indices_search_fetch_count", "Count of search fetches", "node");
        catalog.registerGauge("indices_search_fetch_current_number", "Current rate of search fetches", "node");
        catalog.registerGauge("indices_search_fetch_time_seconds", "Time spent while search fetches", "node");
        catalog.registerGauge("indices_search_query_count", "Count of search queries", "node");
        catalog.registerGauge("indices_search_query_current_number", "Current rate of search queries", "node");
        catalog.registerGauge("indices_search_query_time_seconds", "Time spent while search queries", "node");
        catalog.registerGauge("indices_search_scroll_count", "Count of search scrolls", "node");
        catalog.registerGauge("indices_search_scroll_current_number", "Current rate of search scrolls", "node");
        catalog.registerGauge("indices_search_scroll_time_seconds", "Time spent while search scrolls", "node");

        catalog.registerGauge("indices_merges_current_number", "Current rate of merges", "node");
        catalog.registerGauge("indices_merges_current_docs_number", "Current rate of documents merged", "node");
        catalog.registerGauge("indices_merges_current_size_bytes", "Current rate of bytes merged", "node");
        catalog.registerGauge("indices_merges_total_number", "Count of merges", "node");
        catalog.registerGauge("indices_merges_total_time_seconds", "Time spent while merging", "node");
        catalog.registerGauge("indices_merges_total_docs_count", "Count of documents merged", "node");
        catalog.registerGauge("indices_merges_total_size_bytes", "Count of bytes of merged documents", "node");
        catalog.registerGauge("indices_merges_total_stopped_time_seconds", "Time spent while merge process stopped", "node");
        catalog.registerGauge("indices_merges_total_throttled_time_seconds", "Time spent while merging when throttling", "node");
        catalog.registerGauge("indices_merges_total_auto_throttle_bytes", "Bytes merged while throttling", "node");

        catalog.registerGauge("indices_refresh_total_count", "Count of refreshes", "node");
        catalog.registerGauge("indices_refresh_total_time_seconds", "Time spent while refreshes", "node");
        catalog.registerGauge("indices_refresh_listeners_number", "Number of refresh listeners", "node");

        catalog.registerGauge("indices_flush_total_count", "Count of flushes", "node");
        catalog.registerGauge("indices_flush_total_time_seconds", "Total time spent while flushes", "node");

        catalog.registerGauge("indices_querycache_cache_count", "Count of queries in cache", "node");
        catalog.registerGauge("indices_querycache_cache_size_bytes", "Query cache size", "node");
        catalog.registerGauge("indices_querycache_evictions_count", "Count of evictions in query cache", "node");
        catalog.registerGauge("indices_querycache_hit_count", "Count of hits in query cache", "node");
        catalog.registerGauge("indices_querycache_memory_size_bytes", "Memory usage of query cache", "node");
        catalog.registerGauge("indices_querycache_miss_number", "Count of misses in query cache", "node");
        catalog.registerGauge("indices_querycache_total_number", "Count of usages of query cache", "node");

        catalog.registerGauge("indices_fielddata_memory_size_bytes", "Memory usage of field date cache", "node");
        catalog.registerGauge("indices_fielddata_evictions_count", "Count of evictions in field data cache", "node");

        catalog.registerGauge("indices_percolate_count", "Count of percolates", "node");
        catalog.registerGauge("indices_percolate_current_number", "Rate of percolates", "node");
        catalog.registerGauge("indices_percolate_memory_size_bytes", "Percolate memory size", "node");
        catalog.registerGauge("indices_percolate_queries_count", "Count of queries percolated", "node");
        catalog.registerGauge("indices_percolate_time_seconds", "Time spent while percolating", "node");

        catalog.registerGauge("indices_completion_size_bytes", "Size of completion suggest statistics", "node");

        catalog.registerGauge("indices_segments_number", "Current number of segments", "node");
        catalog.registerGauge("indices_segments_memory_bytes", "Memory used by segments", "node", "type");

        catalog.registerGauge("indices_suggest_current_number", "Current rate of suggests", "node");
        catalog.registerGauge("indices_suggest_count", "Count of suggests", "node");
        catalog.registerGauge("indices_suggest_time_seconds", "Time spent while making suggests", "node");

        catalog.registerGauge("indices_requestcache_memory_size_bytes", "Memory used for request cache", "node");
        catalog.registerGauge("indices_requestcache_hit_count", "Number of hits in request cache", "node");
        catalog.registerGauge("indices_requestcache_miss_count", "Number of misses in request cache", "node");
        catalog.registerGauge("indices_requestcache_evictions_count", "Number of evictions in request cache", "node");

        catalog.registerGauge("indices_recovery_current_number", "Current number of recoveries", "node", "type");
        catalog.registerGauge("indices_recovery_throttle_time_seconds", "Time spent while throttling recoveries", "node");
    }

    private void updateIndicesMetrics(NodeIndicesStats idx) {
        if (idx != null) {
            catalog.setGauge("indices_doc_number", idx.getDocs().getCount(), node);
            catalog.setGauge("indices_doc_deleted_number", idx.getDocs().getDeleted(), node);

            catalog.setGauge("indices_store_size_bytes", idx.getStore().getSizeInBytes(), node);

            catalog.setGauge("indices_indexing_delete_count", idx.getIndexing().getTotal().getDeleteCount(), node);
            catalog.setGauge("indices_indexing_delete_current_number", idx.getIndexing().getTotal().getDeleteCurrent(), node);
            catalog.setGauge("indices_indexing_delete_time_seconds", idx.getIndexing().getTotal().getDeleteTime().seconds(), node);
            catalog.setGauge("indices_indexing_index_count", idx.getIndexing().getTotal().getIndexCount(), node);
            catalog.setGauge("indices_indexing_index_current_number", idx.getIndexing().getTotal().getIndexCurrent(), node);
            catalog.setGauge("indices_indexing_index_failed_count", idx.getIndexing().getTotal().getIndexFailedCount(), node);
            catalog.setGauge("indices_indexing_index_time_seconds", idx.getIndexing().getTotal().getIndexTime().seconds(), node);
            catalog.setGauge("indices_indexing_noop_update_count", idx.getIndexing().getTotal().getNoopUpdateCount(), node);
            catalog.setGauge("indices_indexing_is_throttled_bool", idx.getIndexing().getTotal().isThrottled() ? 1 : 0, node);
            catalog.setGauge("indices_indexing_throttle_time_seconds", idx.getIndexing().getTotal().getThrottleTime().seconds(), node);

            catalog.setGauge("indices_get_count", idx.getGet().getCount(), node);
            catalog.setGauge("indices_get_time_seconds", idx.getGet().getTimeInMillis() / 1000.0, node);
            catalog.setGauge("indices_get_exists_count", idx.getGet().getExistsCount(), node);
            catalog.setGauge("indices_get_exists_time_seconds", idx.getGet().getExistsTimeInMillis() / 1000.0, node);
            catalog.setGauge("indices_get_missing_count", idx.getGet().getMissingCount(), node);
            catalog.setGauge("indices_get_missing_time_seconds", idx.getGet().getMissingTimeInMillis() / 1000.0, node);
            catalog.setGauge("indices_get_current_number", idx.getGet().current(), node);

            catalog.setGauge("indices_search_open_contexts_number", idx.getSearch().getOpenContexts(), node);
            catalog.setGauge("indices_search_fetch_count", idx.getSearch().getTotal().getFetchCount(), node);
            catalog.setGauge("indices_search_fetch_current_number", idx.getSearch().getTotal().getFetchCurrent(), node);
            catalog.setGauge("indices_search_fetch_time_seconds", idx.getSearch().getTotal().getFetchTimeInMillis() / 1000.0, node);
            catalog.setGauge("indices_search_query_count", idx.getSearch().getTotal().getQueryCount(), node);
            catalog.setGauge("indices_search_query_current_number", idx.getSearch().getTotal().getQueryCurrent(), node);
            catalog.setGauge("indices_search_query_time_seconds", idx.getSearch().getTotal().getQueryTimeInMillis() / 1000.0, node);
            catalog.setGauge("indices_search_scroll_count", idx.getSearch().getTotal().getScrollCount(), node);
            catalog.setGauge("indices_search_scroll_current_number", idx.getSearch().getTotal().getScrollCurrent(), node);
            catalog.setGauge("indices_search_scroll_time_seconds", idx.getSearch().getTotal().getScrollTimeInMillis() / 1000.0, node);

            catalog.setGauge("indices_merges_current_number", idx.getMerge().getCurrent(), node);
            catalog.setGauge("indices_merges_current_docs_number", idx.getMerge().getCurrentNumDocs(), node);
            catalog.setGauge("indices_merges_current_size_bytes", idx.getMerge().getCurrentSizeInBytes(), node);
            catalog.setGauge("indices_merges_total_number", idx.getMerge().getTotal(), node);
            catalog.setGauge("indices_merges_total_time_seconds", idx.getMerge().getTotalTimeInMillis() / 1000.0, node);
            catalog.setGauge("indices_merges_total_docs_count", idx.getMerge().getTotalNumDocs(), node);
            catalog.setGauge("indices_merges_total_size_bytes", idx.getMerge().getTotalSizeInBytes(), node);
            catalog.setGauge("indices_merges_total_stopped_time_seconds", idx.getMerge().getTotalStoppedTimeInMillis() / 1000.0, node);
            catalog.setGauge("indices_merges_total_throttled_time_seconds", idx.getMerge().getTotalThrottledTimeInMillis() / 1000.0, node);
            catalog.setGauge("indices_merges_total_auto_throttle_bytes", idx.getMerge().getTotalBytesPerSecAutoThrottle(), node);

            catalog.setGauge("indices_refresh_total_count", idx.getRefresh().getTotal(), node);
            catalog.setGauge("indices_refresh_total_time_seconds", idx.getRefresh().getTotalTimeInMillis() / 1000.0, node);
            catalog.setGauge("indices_refresh_listeners_number", idx.getRefresh().getListeners(), node);

            catalog.setGauge("indices_flush_total_count", idx.getFlush().getTotal(), node);
            catalog.setGauge("indices_flush_total_time_seconds", idx.getFlush().getTotalTimeInMillis() / 1000.0, node);

            catalog.setGauge("indices_querycache_cache_count", idx.getQueryCache().getCacheCount(), node);
            catalog.setGauge("indices_querycache_cache_size_bytes", idx.getQueryCache().getCacheSize(), node);
            catalog.setGauge("indices_querycache_evictions_count", idx.getQueryCache().getEvictions(), node);
            catalog.setGauge("indices_querycache_hit_count", idx.getQueryCache().getHitCount(), node);
            catalog.setGauge("indices_querycache_memory_size_bytes", idx.getQueryCache().getMemorySizeInBytes(), node);
            catalog.setGauge("indices_querycache_miss_number", idx.getQueryCache().getMissCount(), node);
            catalog.setGauge("indices_querycache_total_number", idx.getQueryCache().getTotalCount(), node);

            catalog.setGauge("indices_fielddata_memory_size_bytes", idx.getFieldData().getMemorySizeInBytes(), node);
            catalog.setGauge("indices_fielddata_evictions_count", idx.getFieldData().getEvictions(), node);

            catalog.setGauge("indices_completion_size_bytes", idx.getCompletion().getSizeInBytes(), node);

            catalog.setGauge("indices_segments_number", idx.getSegments().getCount(), node);
            catalog.setGauge("indices_segments_memory_bytes", idx.getSegments().getMemoryInBytes(), node, "all");
            catalog.setGauge("indices_segments_memory_bytes", idx.getSegments().getBitsetMemoryInBytes(), node, "bitset");
            catalog.setGauge("indices_segments_memory_bytes", idx.getSegments().getDocValuesMemoryInBytes(), node, "docvalues");
            catalog.setGauge("indices_segments_memory_bytes", idx.getSegments().getIndexWriterMemoryInBytes(), node, "indexwriter");
            catalog.setGauge("indices_segments_memory_bytes", idx.getSegments().getNormsMemoryInBytes(), node, "norms");
            catalog.setGauge("indices_segments_memory_bytes", idx.getSegments().getStoredFieldsMemoryInBytes(), node, "storefields");
            catalog.setGauge("indices_segments_memory_bytes", idx.getSegments().getTermsMemoryInBytes(), node, "terms");
            catalog.setGauge("indices_segments_memory_bytes", idx.getSegments().getTermVectorsMemoryInBytes(), node, "termvectors");
            catalog.setGauge("indices_segments_memory_bytes", idx.getSegments().getVersionMapMemoryInBytes(), node, "versionmap");
            catalog.setGauge("indices_segments_memory_bytes", idx.getSegments().getPointsMemoryInBytes(), node, "points");

            catalog.setGauge("indices_suggest_current_number", idx.getSearch().getTotal().getSuggestCurrent(), node);
            catalog.setGauge("indices_suggest_count", idx.getSearch().getTotal().getSuggestCount(), node);
            catalog.setGauge("indices_suggest_time_seconds", idx.getSearch().getTotal().getSuggestTimeInMillis() / 1000.0, node);

            catalog.setGauge("indices_requestcache_memory_size_bytes", idx.getRequestCache().getMemorySizeInBytes(), node);
            catalog.setGauge("indices_requestcache_hit_count", idx.getRequestCache().getHitCount(), node);
            catalog.setGauge("indices_requestcache_miss_count", idx.getRequestCache().getMissCount(), node);
            catalog.setGauge("indices_requestcache_evictions_count", idx.getRequestCache().getEvictions(), node);

            catalog.setGauge("indices_recovery_current_number", idx.getRecoveryStats().currentAsSource(), node, "source");
            catalog.setGauge("indices_recovery_current_number", idx.getRecoveryStats().currentAsTarget(), node, "target");
            catalog.setGauge("indices_recovery_throttle_time_seconds", idx.getRecoveryStats().throttleTime().getSeconds(), node);
        }
    }

    private void registerIngestMetrics() {
        catalog.registerGauge("ingest_total_count", "Ingestion total number", "node");
        catalog.registerGauge("ingest_total_time_seconds", "Ingestion total time in seconds", "node");
        catalog.registerGauge("ingest_total_current", "Ingestion total current", "node");
        catalog.registerGauge("ingest_total_failed_count", "Ingestion total failed", "node");

        catalog.registerGauge("ingest_pipeline_total_count", "Ingestion total number", "node", "pipeline");
        catalog.registerGauge("ingest_pipeline_total_time_seconds", "Ingestion total time in seconds", "node", "pipeline");
        catalog.registerGauge("ingest_pipeline_total_current", "Ingestion total current", "node", "pipeline");
        catalog.registerGauge("ingest_pipeline_total_failed_count", "Ingestion total failed", "node", "pipeline");
    }

    private void updateIngestMetrics(IngestStats is) {
        catalog.setGauge("ingest_total_count", is.getTotalStats().getIngestCount(), node);
        catalog.setGauge("ingest_total_time_seconds", is.getTotalStats().getIngestTimeInMillis() / 1000.0, node);
        catalog.setGauge("ingest_total_current", is.getTotalStats().getIngestCurrent(), node);
        catalog.setGauge("ingest_total_failed_count", is.getTotalStats().getIngestFailedCount(), node);

        for (Map.Entry<String, IngestStats.Stats> entry : is.getStatsPerPipeline().entrySet()) {
            String pipeline = entry.getKey();
            catalog.setGauge("ingest_pipeline_total_count", entry.getValue().getIngestCount(), node, pipeline);
            catalog.setGauge("ingest_pipeline_total_time_seconds", entry.getValue().getIngestTimeInMillis() / 1000.0, node, pipeline);
            catalog.setGauge("ingest_pipeline_total_current", entry.getValue().getIngestCurrent(), node, pipeline);
            catalog.setGauge("ingest_pipeline_total_failed_count", entry.getValue().getIngestFailedCount(), node, pipeline);
        }
    }

    private void registerTransportMetrics() {
        catalog.registerGauge("transport_server_open_number", "Opened server connections", "node");
        catalog.registerGauge("transport_rx_packets_count", "Received packets", "node");
        catalog.registerGauge("transport_tx_packets_count", "Sent packets", "node");
        catalog.registerGauge("transport_rx_bytes_count", "Bytes received", "node");
        catalog.registerGauge("transport_tx_bytes_count", "Bytes sent", "node");
    }

    private void updateTransportMetrics(TransportStats ts) {
        if (ts != null) {
            catalog.setGauge("transport_server_open_number", ts.getServerOpen(), node);
            catalog.setGauge("transport_rx_packets_count", ts.getRxCount(), node);
            catalog.setGauge("transport_tx_packets_count", ts.getTxCount(), node);
            catalog.setGauge("transport_rx_bytes_count", ts.getRxSize().getBytes(), node);
            catalog.setGauge("transport_tx_bytes_count", ts.getTxSize().getBytes(), node);
        }
    }

    private void registerHTTPMetrics() {
        catalog.registerGauge("http_open_server_number", "Number of open server connections", "node");
        catalog.registerGauge("http_open_total_count", "Count of opened connections", "node");
    }

    private void updateHTTPMetrics(HttpStats http) {
        if (http != null) {
            catalog.setGauge("http_open_server_number", http.getServerOpen(), node);
            catalog.setGauge("http_open_total_count", http.getTotalOpen(), node);
        }
    }

    private void registerScriptMetrics() {
        catalog.registerGauge("script_cache_evictions_count", "Number of evictions in scripts cache", "node");
        catalog.registerGauge("script_compilations_count", "Number of scripts compilations", "node");
    }

    private void updateScriptMetrics(ScriptStats sc) {
        if (sc != null) {
            catalog.setGauge("script_cache_evictions_count", sc.getCacheEvictions(), node);
            catalog.setGauge("script_compilations_count", sc.getCompilations(), node);
        }
    }

    private void registerProcessMetrics() {
        catalog.registerGauge("process_cpu_percent", "CPU percentage used by ES process", "node");
        catalog.registerGauge("process_cpu_time_seconds", "CPU time used by ES process", "node");
        catalog.registerGauge("process_mem_total_virtual_bytes", "Memory used by ES process", "node");
        catalog.registerGauge("process_file_descriptors_open_number", "Open file descriptors", "node");
        catalog.registerGauge("process_file_descriptors_max_number", "Max file descriptors", "node");
    }

    private void updateProcessMetrics(ProcessStats ps) {
        if (ps != null) {
            catalog.setGauge("process_cpu_percent", ps.getCpu().getPercent(), node);
            catalog.setGauge("process_cpu_time_seconds", ps.getCpu().getTotal().getSeconds(), node);
            catalog.setGauge("process_mem_total_virtual_bytes", ps.getMem().getTotalVirtual().getBytes(), node);
            catalog.setGauge("process_file_descriptors_open_number", ps.getOpenFileDescriptors(), node);
            catalog.setGauge("process_file_descriptors_max_number", ps.getMaxFileDescriptors(), node);
        }
    }

    private void registerOsMetrics() {
        catalog.registerGauge("os_cpu_percent", "CPU usage in percent", "node");
        catalog.registerGauge("os_load_average_one_minute", "CPU load", "node");
        catalog.registerGauge("os_load_average_five_minutes", "CPU load", "node");
        catalog.registerGauge("os_load_average_fifteen_minutes", "CPU load", "node");
        catalog.registerGauge("os_mem_free_bytes", "Memory free", "node");
        catalog.registerGauge("os_mem_free_percent", "Memory free in percent", "node");
        catalog.registerGauge("os_mem_used_bytes", "Memory used", "node");
        catalog.registerGauge("os_mem_used_percent", "Memory used in percent", "node");
        catalog.registerGauge("os_mem_total_bytes", "Total memory size", "node");
        catalog.registerGauge("os_swap_free_bytes", "Swap free", "node");
        catalog.registerGauge("os_swap_used_bytes", "Swap used", "node");
        catalog.registerGauge("os_swap_total_bytes", "Total swap size", "node");
    }

    private void updateOsMetrics(OsStats os) {
        if (os != null) {
            if (os.getCpu() != null) {
                catalog.setGauge("os_cpu_percent", os.getCpu().getPercent(), node);
                double[] loadAverage = os.getCpu().getLoadAverage();
                if (loadAverage != null && loadAverage.length == 3) {
                    catalog.setGauge("os_load_average_one_minute", os.getCpu().getLoadAverage()[0], node);
                    catalog.setGauge("os_load_average_five_minutes", os.getCpu().getLoadAverage()[1], node);
                    catalog.setGauge("os_load_average_fifteen_minutes", os.getCpu().getLoadAverage()[2], node);
                }
            }

            if (os.getMem() != null) {
                catalog.setGauge("os_mem_free_bytes", os.getMem().getFree().getBytes(), node);
                catalog.setGauge("os_mem_free_percent", os.getMem().getFreePercent(), node);
                catalog.setGauge("os_mem_used_bytes", os.getMem().getUsed().getBytes(), node);
                catalog.setGauge("os_mem_used_percent", os.getMem().getUsedPercent(), node);
                catalog.setGauge("os_mem_total_bytes", os.getMem().getTotal().getBytes(), node);
            }

            if (os.getSwap() != null) {
                catalog.setGauge("os_swap_free_bytes", os.getSwap().getFree().getBytes(), node);
                catalog.setGauge("os_swap_used_bytes", os.getSwap().getUsed().getBytes(), node);
                catalog.setGauge("os_swap_total_bytes", os.getSwap().getTotal().getBytes(), node);
            }
        }
    }

    private void registerCircuitBreakerMetrics() {
        catalog.registerGauge("circuitbreaker_estimated_bytes", "Circuit breaker estimated size", "node", "name");
        catalog.registerGauge("circuitbreaker_limit_bytes", "Circuit breaker size limit", "node", "name");
        catalog.registerGauge("circuitbreaker_overhead_ratio", "Circuit breaker overhead ratio", "node", "name");
        catalog.registerGauge("circuitbreaker_tripped_count", "Circuit breaker tripped count", "node", "name");
    }

    private void updateCircuitBreakersMetrics(AllCircuitBreakerStats acbs) {
        if (acbs != null) {
            for (CircuitBreakerStats cbs : acbs.getAllStats()) {
                String name = cbs.getName();
                catalog.setGauge("circuitbreaker_estimated_bytes", cbs.getEstimated(), node, name);
                catalog.setGauge("circuitbreaker_limit_bytes", cbs.getLimit(), node, name);
                catalog.setGauge("circuitbreaker_overhead_ratio", cbs.getOverhead(), node, name);
                catalog.setGauge("circuitbreaker_tripped_count", cbs.getTrippedCount(), node, name);
            }
        }
    }

    private void registerThreadPoolMetrics() {
        catalog.registerGauge("threadpool_threads_number", "Number of threads in thread pool", "node", "name", "type");
        catalog.registerGauge("threadpool_threads_count", "Count of threads in thread pool", "node", "name", "type");
        catalog.registerGauge("threadpool_tasks_number", "Number of tasks in thread pool", "node", "name", "type");
    }

    private void updateThreadPoolMetrics(ThreadPoolStats tps) {
        if (tps != null) {
            for (ThreadPoolStats.Stats st : tps) {
                String name = st.getName();
                catalog.setGauge("threadpool_threads_number", st.getThreads(), node, name, "threads");
                catalog.setGauge("threadpool_threads_number", st.getActive(), node, name, "active");
                catalog.setGauge("threadpool_threads_number", st.getLargest(), node, name, "largest");
                catalog.setGauge("threadpool_threads_count", st.getCompleted(), node, name, "completed");
                catalog.setGauge("threadpool_threads_count", st.getRejected(), node, name, "rejected");
                catalog.setGauge("threadpool_tasks_number", st.getQueue(), node, name, "queue");
            }
        }
    }

    private void registerFsMetrics() {
        catalog.registerGauge("fs_total_total_bytes", "Total disk space for all mount points", "node");
        catalog.registerGauge("fs_total_available_bytes", "Available disk space for all mount points", "node");
        catalog.registerGauge("fs_total_free_bytes", "Free disk space for all mountpoints", "node");

        catalog.registerGauge("fs_most_usage_free_bytes", "Free disk space for most used mountpoint", "node", "path");
        catalog.registerGauge("fs_most_usage_total_bytes", "Total disk space for most used mountpoint", "node", "path");

        catalog.registerGauge("fs_least_usage_free_bytes", "Free disk space for least used mountpoint", "node", "path");
        catalog.registerGauge("fs_least_usage_total_bytes", "Total disk space for least used mountpoint", "node", "path");

        catalog.registerGauge("fs_path_total_bytes", "Total disk space", "node", "path", "mount", "type");
        catalog.registerGauge("fs_path_available_bytes", "Available disk space", "node", "path", "mount", "type");
        catalog.registerGauge("fs_path_free_bytes", "Free disk space", "node", "path", "mount", "type");

        catalog.registerGauge("fs_io_total_operations", "Total IO operations", "node");
        catalog.registerGauge("fs_io_total_read_operations", "Total IO read operations", "node");
        catalog.registerGauge("fs_io_total_write_operations", "Total IO write operations", "node");
        catalog.registerGauge("fs_io_total_read_bytes", "Total IO read bytes", "node");
        catalog.registerGauge("fs_io_total_write_bytes", "Total IO write bytes", "node");
    }

    private void updateFsMetrics(FsInfo fs) {
        if (fs != null) {
            catalog.setGauge("fs_total_total_bytes", fs.getTotal().getTotal().getBytes(), node);
            catalog.setGauge("fs_total_available_bytes", fs.getTotal().getAvailable().getBytes(), node);
            catalog.setGauge("fs_total_free_bytes", fs.getTotal().getFree().getBytes(), node);

            if (fs.getMostDiskEstimate() != null) {
                catalog.setGauge("fs_most_usage_free_bytes", fs.getMostDiskEstimate().getFreeBytes(), node, fs.getMostDiskEstimate().getPath());
                catalog.setGauge("fs_most_usage_total_bytes", fs.getMostDiskEstimate().getTotalBytes(), node, fs.getMostDiskEstimate().getPath());
            }

            if (fs.getLeastDiskEstimate() != null) {
                catalog.setGauge("fs_least_usage_free_bytes", fs.getLeastDiskEstimate().getFreeBytes(), node, fs.getLeastDiskEstimate().getPath());
                catalog.setGauge("fs_least_usage_total_bytes", fs.getLeastDiskEstimate().getTotalBytes(), node, fs.getLeastDiskEstimate().getPath());
            }

            for (FsInfo.Path fspath : fs) {
                String path = fspath.getPath();
                String mount = fspath.getMount();
                String type = fspath.getType();
                catalog.setGauge("fs_path_total_bytes", fspath.getTotal().getBytes(), node, path, mount, type);
                catalog.setGauge("fs_path_available_bytes", fspath.getAvailable().getBytes(), node, path, mount, type);
                catalog.setGauge("fs_path_free_bytes", fspath.getFree().getBytes(), node, path, mount, type);
            }

            FsInfo.IoStats ioStats = fs.getIoStats();
            if (ioStats != null) {
                catalog.setGauge("fs_io_total_operations", fs.getIoStats().getTotalOperations(), node);
                catalog.setGauge("fs_io_total_read_operations", fs.getIoStats().getTotalReadOperations(), node);
                catalog.setGauge("fs_io_total_write_operations", fs.getIoStats().getTotalWriteOperations(), node);
                catalog.setGauge("fs_io_total_read_bytes", fs.getIoStats().getTotalReadKilobytes() * 1024, node);
                catalog.setGauge("fs_io_total_write_bytes", fs.getIoStats().getTotalWriteKilobytes() * 1024, node);
            }
        }
    }

    private void registerPerIndexMetrics() {
        catalog.registerGauge("index_status", "Index status", "index");
        catalog.registerGauge("index_replicas_number", "Number of replicas", "index");
        catalog.registerGauge("index_shards_number", "Number of shards", "type", "index");

        catalog.registerGauge("index_doc_number", "Total number of documents", "node", "index", "context");
        catalog.registerGauge("index_doc_deleted_number", "Number of deleted documents", "node", "index", "context");

        catalog.registerGauge("index_store_size_bytes", "Store size of the indices in bytes", "node", "index", "context");

        catalog.registerGauge("index_indexing_delete_count", "Count of documents deleted", "node", "index", "context");
        catalog.registerGauge("index_indexing_delete_current_number", "Current rate of documents deleted", "node", "index", "context");
        catalog.registerGauge("index_indexing_delete_time_seconds", "Time spent while deleting documents", "node", "index", "context");
        catalog.registerGauge("index_indexing_index_count", "Count of documents indexed", "node", "index", "context");
        catalog.registerGauge("index_indexing_index_current_number", "Current rate of documents indexed", "node", "index", "context");
        catalog.registerGauge("index_indexing_index_failed_count", "Count of failed to index documents", "node", "index", "context");
        catalog.registerGauge("index_indexing_index_time_seconds", "Time spent while indexing documents", "node", "index", "context");
        catalog.registerGauge("index_indexing_noop_update_count", "Count of noop document updates", "node", "index", "context");
        catalog.registerGauge("index_indexing_is_throttled_bool", "Is indexing throttling ?", "node", "index", "context");
        catalog.registerGauge("index_indexing_throttle_time_seconds", "Time spent while throttling", "node", "index", "context");

        catalog.registerGauge("index_get_count", "Count of get commands", "node", "index", "context");
        catalog.registerGauge("index_get_time_seconds", "Time spent while get commands", "node", "index", "context");
        catalog.registerGauge("index_get_exists_count", "Count of existing documents when get command", "node", "index", "context");
        catalog.registerGauge("index_get_exists_time_seconds", "Time spent while existing documents get command", "node", "index", "context");
        catalog.registerGauge("index_get_missing_count", "Count of missing documents when get command", "node", "index", "context");
        catalog.registerGauge("index_get_missing_time_seconds", "Time spent while missing documents get command", "node", "index", "context");
        catalog.registerGauge("index_get_current_number", "Current rate of get commands", "node", "index", "context");

        catalog.registerGauge("index_search_open_contexts_number", "Number of search open contexts", "node", "index", "context");
        catalog.registerGauge("index_search_fetch_count", "Count of search fetches", "node", "index", "context");
        catalog.registerGauge("index_search_fetch_current_number", "Current rate of search fetches", "node", "index", "context");
        catalog.registerGauge("index_search_fetch_time_seconds", "Time spent while search fetches", "node", "index", "context");
        catalog.registerGauge("index_search_query_count", "Count of search queries", "node", "index", "context");
        catalog.registerGauge("index_search_query_current_number", "Current rate of search queries", "node", "index", "context");
        catalog.registerGauge("index_search_query_time_seconds", "Time spent while search queries", "node", "index", "context");
        catalog.registerGauge("index_search_scroll_count", "Count of search scrolls", "node", "index", "context");
        catalog.registerGauge("index_search_scroll_current_number", "Current rate of search scrolls", "node", "index", "context");
        catalog.registerGauge("index_search_scroll_time_seconds", "Time spent while search scrolls", "node", "index", "context");

        catalog.registerGauge("index_merges_current_number", "Current rate of merges", "node", "index", "context");
        catalog.registerGauge("index_merges_current_docs_number", "Current rate of documents merged", "node", "index", "context");
        catalog.registerGauge("index_merges_current_size_bytes", "Current rate of bytes merged", "node", "index", "context");
        catalog.registerGauge("index_merges_total_number", "Count of merges", "node", "index", "context");
        catalog.registerGauge("index_merges_total_time_seconds", "Time spent while merging", "node", "index", "context");
        catalog.registerGauge("index_merges_total_docs_count", "Count of documents merged", "node", "index", "context");
        catalog.registerGauge("index_merges_total_size_bytes", "Count of bytes of merged documents", "node", "index", "context");
        catalog.registerGauge("index_merges_total_stopped_time_seconds", "Time spent while merge process stopped", "node", "index", "context");
        catalog.registerGauge("index_merges_total_throttled_time_seconds", "Time spent while merging when throttling", "node", "index", "context");
        catalog.registerGauge("index_merges_total_auto_throttle_bytes", "Bytes merged while throttling", "node", "index", "context");

        catalog.registerGauge("index_refresh_total_count", "Count of refreshes", "node", "index", "context");
        catalog.registerGauge("index_refresh_total_time_seconds", "Time spent while refreshes", "node", "index", "context");
        catalog.registerGauge("index_refresh_listeners_number", "Number of refresh listeners", "node", "index", "context");

        catalog.registerGauge("index_flush_total_count", "Count of flushes", "node", "index", "context");
        catalog.registerGauge("index_flush_total_time_seconds", "Total time spent while flushes", "node", "index", "context");

        catalog.registerGauge("index_querycache_cache_count", "Count of queries in cache", "node", "index", "context");
        catalog.registerGauge("index_querycache_cache_size_bytes", "Query cache size", "node", "index", "context");
        catalog.registerGauge("index_querycache_evictions_count", "Count of evictions in query cache", "node", "index", "context");
        catalog.registerGauge("index_querycache_hit_count", "Count of hits in query cache", "node", "index", "context");
        catalog.registerGauge("index_querycache_memory_size_bytes", "Memory usage of query cache", "node", "index", "context");
        catalog.registerGauge("index_querycache_miss_number", "Count of misses in query cache", "node", "index", "context");
        catalog.registerGauge("index_querycache_total_number", "Count of usages of query cache", "node", "index", "context");

        catalog.registerGauge("index_fielddata_memory_size_bytes", "Memory usage of field date cache", "node", "index", "context");
        catalog.registerGauge("index_fielddata_evictions_count", "Count of evictions in field data cache", "node", "index", "context");

        catalog.registerGauge("index_percolate_count", "Count of percolates", "node", "index", "context");
        catalog.registerGauge("index_percolate_current_number", "Rate of percolates", "node", "index", "context");
        catalog.registerGauge("index_percolate_memory_size_bytes", "Percolate memory size", "node", "index", "context");
        catalog.registerGauge("index_percolate_queries_count", "Count of queries percolated", "node", "index", "context");
        catalog.registerGauge("index_percolate_time_seconds", "Time spent while percolating", "node", "index", "context");

        catalog.registerGauge("index_completion_size_bytes", "Size of completion suggest statistics", "node", "index", "context");

        catalog.registerGauge("index_segments_number", "Current number of segments", "node", "index", "context");
        catalog.registerGauge("index_segments_memory_bytes", "Memory used by segments", "node", "type", "index", "context");

        catalog.registerGauge("index_suggest_current_number", "Current rate of suggests", "node", "index", "context");
        catalog.registerGauge("index_suggest_count", "Count of suggests", "node", "index", "context");
        catalog.registerGauge("index_suggest_time_seconds", "Time spent while making suggests", "node", "index", "context");

        catalog.registerGauge("index_requestcache_memory_size_bytes", "Memory used for request cache", "node", "index", "context");
        catalog.registerGauge("index_requestcache_hit_count", "Number of hits in request cache", "node", "index", "context");
        catalog.registerGauge("index_requestcache_miss_count", "Number of misses in request cache", "node", "index", "context");
        catalog.registerGauge("index_requestcache_evictions_count", "Number of evictions in request cache", "node", "index", "context");

        catalog.registerGauge("index_recovery_current_number", "Current number of recoveries", "node", "type", "index", "context");
        catalog.registerGauge("index_recovery_throttle_time_seconds", "Time spent while throttling recoveries", "node", "index", "context");

        catalog.registerGauge("index_translog_operations_number", "Current number of translog operations", "node", "index", "context");
        catalog.registerGauge("index_translog_size_bytes", "Translog size", "node", "index", "context");
        catalog.registerGauge("index_translog_uncommited_operations_number", "Current number of uncommited translog operations", "node", "index", "context");
        catalog.registerGauge("index_translog_uncommited_size_bytes", "Translog uncommited size", "node", "index", "context");

        catalog.registerGauge("index_warmer_current_number", "Current number of warmer", "node", "index", "context");
        catalog.registerGauge("index_warmer_time_seconds", "Time spent during warmers", "node", "index", "context");
        catalog.registerGauge("index_warmer_count", "Counter of warmers", "node", "index", "context");
    }

    public void updatePerIndexContextMetrics(String index_name, String context, CommonStats idx) {
        catalog.setGauge("index_doc_number", idx.getDocs().getCount(), node, index_name, context);
        catalog.setGauge("index_doc_deleted_number", idx.getDocs().getDeleted(), node, index_name, context);

        catalog.setGauge("index_store_size_bytes", idx.getStore().getSizeInBytes(), node, index_name, context);

        catalog.setGauge("index_indexing_delete_count", idx.getIndexing().getTotal().getDeleteCount(), node, index_name, context);
        catalog.setGauge("index_indexing_delete_current_number", idx.getIndexing().getTotal().getDeleteCurrent(), node, index_name, context);
        catalog.setGauge("index_indexing_delete_time_seconds", idx.getIndexing().getTotal().getDeleteTime().seconds(), node, index_name, context);
        catalog.setGauge("index_indexing_index_count", idx.getIndexing().getTotal().getIndexCount(), node, index_name, context);
        catalog.setGauge("index_indexing_index_current_number", idx.getIndexing().getTotal().getIndexCurrent(), node, index_name, context);
        catalog.setGauge("index_indexing_index_failed_count", idx.getIndexing().getTotal().getIndexFailedCount(), node, index_name, context);
        catalog.setGauge("index_indexing_index_time_seconds", idx.getIndexing().getTotal().getIndexTime().seconds(), node, index_name, context);
        catalog.setGauge("index_indexing_noop_update_count", idx.getIndexing().getTotal().getNoopUpdateCount(), node, index_name, context);
        catalog.setGauge("index_indexing_is_throttled_bool", idx.getIndexing().getTotal().isThrottled() ? 1 : 0, node, index_name, context);
        catalog.setGauge("index_indexing_throttle_time_seconds", idx.getIndexing().getTotal().getThrottleTime().seconds(), node, index_name, context);

        catalog.setGauge("index_get_count", idx.getGet().getCount(), node, index_name, context);
        catalog.setGauge("index_get_time_seconds", idx.getGet().getTimeInMillis() / 1000.0, node, index_name, context);
        catalog.setGauge("index_get_exists_count", idx.getGet().getExistsCount(), node, index_name, context);
        catalog.setGauge("index_get_exists_time_seconds", idx.getGet().getExistsTimeInMillis() / 1000.0, node, index_name, context);
        catalog.setGauge("index_get_missing_count", idx.getGet().getMissingCount(), node, index_name, context);
        catalog.setGauge("index_get_missing_time_seconds", idx.getGet().getMissingTimeInMillis() / 1000.0, node, index_name, context);
        catalog.setGauge("index_get_current_number", idx.getGet().current(), node, index_name, context);

        catalog.setGauge("index_search_open_contexts_number", idx.getSearch().getOpenContexts(), node, index_name, context);
        catalog.setGauge("index_search_fetch_count", idx.getSearch().getTotal().getFetchCount(), node, index_name, context);
        catalog.setGauge("index_search_fetch_current_number", idx.getSearch().getTotal().getFetchCurrent(), node, index_name, context);
        catalog.setGauge("index_search_fetch_time_seconds", idx.getSearch().getTotal().getFetchTimeInMillis() / 1000.0, node, index_name, context);
        catalog.setGauge("index_search_query_count", idx.getSearch().getTotal().getQueryCount(), node, index_name, context);
        catalog.setGauge("index_search_query_current_number", idx.getSearch().getTotal().getQueryCurrent(), node, index_name, context);
        catalog.setGauge("index_search_query_time_seconds", idx.getSearch().getTotal().getQueryTimeInMillis() / 1000.0, node, index_name, context);
        catalog.setGauge("index_search_scroll_count", idx.getSearch().getTotal().getScrollCount(), node, index_name, context);
        catalog.setGauge("index_search_scroll_current_number", idx.getSearch().getTotal().getScrollCurrent(), node, index_name, context);
        catalog.setGauge("index_search_scroll_time_seconds", idx.getSearch().getTotal().getScrollTimeInMillis() / 1000.0, node, index_name, context);

        catalog.setGauge("index_merges_current_number", idx.getMerge().getCurrent(), node, index_name, context);
        catalog.setGauge("index_merges_current_docs_number", idx.getMerge().getCurrentNumDocs(), node, index_name, context);
        catalog.setGauge("index_merges_current_size_bytes", idx.getMerge().getCurrentSizeInBytes(), node, index_name, context);
        catalog.setGauge("index_merges_total_number", idx.getMerge().getTotal(), node, index_name, context);
        catalog.setGauge("index_merges_total_time_seconds", idx.getMerge().getTotalTimeInMillis() / 1000.0, node, index_name, context);
        catalog.setGauge("index_merges_total_docs_count", idx.getMerge().getTotalNumDocs(), node, index_name, context);
        catalog.setGauge("index_merges_total_size_bytes", idx.getMerge().getTotalSizeInBytes(), node, index_name, context);
        catalog.setGauge("index_merges_total_stopped_time_seconds", idx.getMerge().getTotalStoppedTimeInMillis() / 1000.0, node, index_name, context);
        catalog.setGauge("index_merges_total_throttled_time_seconds", idx.getMerge().getTotalThrottledTimeInMillis() / 1000.0, node, index_name, context);
        catalog.setGauge("index_merges_total_auto_throttle_bytes", idx.getMerge().getTotalBytesPerSecAutoThrottle(), node, index_name, context);

        catalog.setGauge("index_refresh_total_count", idx.getRefresh().getTotal(), node, index_name, context);
        catalog.setGauge("index_refresh_total_time_seconds", idx.getRefresh().getTotalTimeInMillis() / 1000.0, node, index_name, context);
        catalog.setGauge("index_refresh_listeners_number", idx.getRefresh().getListeners(), node, index_name, context);

        catalog.setGauge("index_flush_total_count", idx.getFlush().getTotal(), node, index_name, context);
        catalog.setGauge("index_flush_total_time_seconds", idx.getFlush().getTotalTimeInMillis() / 1000.0, node, index_name, context);

        catalog.setGauge("index_querycache_cache_count", idx.getQueryCache().getCacheCount(), node, index_name, context);
        catalog.setGauge("index_querycache_cache_size_bytes", idx.getQueryCache().getCacheSize(), node, index_name, context);
        catalog.setGauge("index_querycache_evictions_count", idx.getQueryCache().getEvictions(), node, index_name, context);
        catalog.setGauge("index_querycache_hit_count", idx.getQueryCache().getHitCount(), node, index_name, context);
        catalog.setGauge("index_querycache_memory_size_bytes", idx.getQueryCache().getMemorySizeInBytes(), node, index_name, context);
        catalog.setGauge("index_querycache_miss_number", idx.getQueryCache().getMissCount(), node, index_name, context);
        catalog.setGauge("index_querycache_total_number", idx.getQueryCache().getTotalCount(), node, index_name, context);

        catalog.setGauge("index_fielddata_memory_size_bytes", idx.getFieldData().getMemorySizeInBytes(), node, index_name, context);
        catalog.setGauge("index_fielddata_evictions_count", idx.getFieldData().getEvictions(), node, index_name, context);

        catalog.setGauge("index_completion_size_bytes", idx.getCompletion().getSizeInBytes(), node, index_name, context);

        catalog.setGauge("index_segments_number", idx.getSegments().getCount(), node, index_name, context);
        catalog.setGauge("index_segments_memory_bytes", idx.getSegments().getMemoryInBytes(), node, "all", index_name, context);
        catalog.setGauge("index_segments_memory_bytes", idx.getSegments().getBitsetMemoryInBytes(), node, "bitset", index_name, context);
        catalog.setGauge("index_segments_memory_bytes", idx.getSegments().getDocValuesMemoryInBytes(), node, "docvalues", index_name, context);
        catalog.setGauge("index_segments_memory_bytes", idx.getSegments().getIndexWriterMemoryInBytes(), node, "indexwriter", index_name, context);
        catalog.setGauge("index_segments_memory_bytes", idx.getSegments().getNormsMemoryInBytes(), node, "norms", index_name, context);
        catalog.setGauge("index_segments_memory_bytes", idx.getSegments().getStoredFieldsMemoryInBytes(), node, "storefields", index_name, context);
        catalog.setGauge("index_segments_memory_bytes", idx.getSegments().getTermsMemoryInBytes(), node, "terms", index_name, context);
        catalog.setGauge("index_segments_memory_bytes", idx.getSegments().getTermVectorsMemoryInBytes(), node, "termvectors", index_name, context);
        catalog.setGauge("index_segments_memory_bytes", idx.getSegments().getVersionMapMemoryInBytes(), node, "versionmap", index_name, context);
        catalog.setGauge("index_segments_memory_bytes", idx.getSegments().getPointsMemoryInBytes(), node, "points", index_name, context);

        catalog.setGauge("index_suggest_current_number", idx.getSearch().getTotal().getSuggestCurrent(), node, index_name, context);
        catalog.setGauge("index_suggest_count", idx.getSearch().getTotal().getSuggestCount(), node, index_name, context);
        catalog.setGauge("index_suggest_time_seconds", idx.getSearch().getTotal().getSuggestTimeInMillis() / 1000.0, node, index_name, context);

        catalog.setGauge("index_requestcache_memory_size_bytes", idx.getRequestCache().getMemorySizeInBytes(), node, index_name, context);
        catalog.setGauge("index_requestcache_hit_count", idx.getRequestCache().getHitCount(), node, index_name, context);
        catalog.setGauge("index_requestcache_miss_count", idx.getRequestCache().getMissCount(), node, index_name, context);
        catalog.setGauge("index_requestcache_evictions_count", idx.getRequestCache().getEvictions(), node, index_name, context);

        catalog.setGauge("index_recovery_current_number", idx.getRecoveryStats().currentAsSource(), node, "source", index_name, context);
        catalog.setGauge("index_recovery_current_number", idx.getRecoveryStats().currentAsTarget(), node, "target", index_name, context);
        catalog.setGauge("index_recovery_throttle_time_seconds", idx.getRecoveryStats().throttleTime().getSeconds(), node, index_name, context);

        catalog.setGauge("index_translog_operations_number", idx.getTranslog().estimatedNumberOfOperations(), node, index_name, context);
        catalog.setGauge("index_translog_size_bytes", idx.getTranslog().getTranslogSizeInBytes(), node, index_name, context);
        catalog.setGauge("index_translog_uncommited_operations_number", idx.getTranslog().getUncommittedOperations(), node, index_name, context);
        catalog.setGauge("index_translog_uncommited_size_bytes", idx.getTranslog().getUncommittedSizeInBytes(), node, index_name, context);

        catalog.setGauge("index_warmer_current_number", idx.getWarmer().current(), node, index_name, context);
        catalog.setGauge("index_warmer_time_seconds", idx.getWarmer().totalTimeInMillis(), node, index_name, context);
        catalog.setGauge("index_warmer_count", idx.getWarmer().total(), node, index_name, context);
    }

    private void updatePerIndexMetrics(ClusterHealthResponse chr, IndicesStatsResponse isr) {

        for (Map.Entry<String, IndexStats> entry : isr.getIndices().entrySet()) {
            String index_name = entry.getKey();

            ClusterIndexHealth cih = chr.getIndices().get(index_name);
            catalog.setGauge("index_status", cih.getStatus().value(), index_name);
            catalog.setGauge("index_replicas_number", cih.getNumberOfReplicas(), index_name);
            catalog.setGauge("index_shards_number", cih.getActiveShards(), "active", index_name);
            catalog.setGauge("index_shards_number", cih.getNumberOfShards(), "shards", index_name);
            catalog.setGauge("index_shards_number", cih.getActivePrimaryShards(), "active_primary", index_name);
            catalog.setGauge("index_shards_number", cih.getInitializingShards(), "initializing", index_name);
            catalog.setGauge("index_shards_number", cih.getRelocatingShards(), "relocating", index_name);
            catalog.setGauge("index_shards_number", cih.getUnassignedShards(), "unassigned", index_name);

            IndexStats index_stats = entry.getValue();
            updatePerIndexContextMetrics(index_name, "total", index_stats.getTotal());
            updatePerIndexContextMetrics(index_name, "primaries", index_stats.getPrimaries());
        }
    }

    public void updateMetrics() {
        Summary.Timer timer = catalog.startSummaryTimer("metrics_generate_time_seconds", node);

        ClusterHealthRequest clusterHealthRequest = new ClusterHealthRequest();
        ClusterHealthResponse clusterHealthResponse = client.admin().cluster().health(clusterHealthRequest).actionGet();

        updateClusterMetrics(clusterHealthResponse);

        NodesStatsRequest nodesStatsRequest = new NodesStatsRequest("_local").all();
        NodesStatsResponse nodesStatsResponse = client.admin().cluster().nodesStats(nodesStatsRequest).actionGet();

        NodeStats nodeStats = nodesStatsResponse.getNodes().get(0);

        updateNodeMetrics(nodeStats);
        updateJVMMetrics(nodeStats.getJvm());
        updateIndicesMetrics(nodeStats.getIndices());
        updateIngestMetrics(nodeStats.getIngestStats());
        updateTransportMetrics(nodeStats.getTransport());
        updateHTTPMetrics(nodeStats.getHttp());
        updateScriptMetrics(nodeStats.getScriptStats());
        updateProcessMetrics(nodeStats.getProcess());
        updateOsMetrics(nodeStats.getOs());
        updateCircuitBreakersMetrics(nodeStats.getBreaker());
        updateThreadPoolMetrics(nodeStats.getThreadPool());
        updateFsMetrics(nodeStats.getFs());

        if (PROMETHEUS_INDICES.get(settings)) {
            IndicesStatsRequest indicesStatsRequest = new IndicesStatsRequest();
            ActionFuture<IndicesStatsResponse> f = client.admin().indices().stats(indicesStatsRequest);
            try {
                updatePerIndexMetrics(clusterHealthResponse, f.get());
            } catch (Exception e) {
                logger.warn("Could not get indices statistics");
            }

        }
        timer.observeDuration();
    }

    public PrometheusMetricsCatalog getCatalog() {
        return catalog;
    }
}
