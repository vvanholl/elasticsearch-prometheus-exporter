package org.compuscene.metrics.prometheus;

import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.admin.cluster.node.stats.NodeStats;
import org.elasticsearch.cluster.health.ClusterIndexHealth;
import org.elasticsearch.cluster.node.DiscoveryNode.Role;
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
import org.elasticsearch.script.ScriptStats;
import org.elasticsearch.threadpool.ThreadPoolStats;
import org.elasticsearch.transport.TransportStats;
import java.util.HashMap;
import java.util.Map;
import io.prometheus.client.Summary;


public class PrometheusMetricsCollector {
    public static final Setting<Boolean> PROMETHEUS_INDICES = Setting.boolSetting("prometheus.indices", true, Property.NodeScope);

    private final Settings settings;

    private String node;
    private String nodeid;

    private PrometheusMetricsCatalog catalog;

    public PrometheusMetricsCollector(Settings settings, String cluster, String node, String nodeid) {
        this.settings = settings;
        this.node = node;
        this.nodeid = nodeid;

        catalog = new PrometheusMetricsCatalog(cluster, "es_");

        registerMetrics();
    }

    private void registerMetrics() {
        catalog.registerSummaryTimer("metrics_generate_time_seconds", "Time spent while generating metrics", "node", "nodeid");

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
        catalog.registerGauge("node_role", "Node role", "node", "nodeid", "role");
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
            catalog.setGauge("node_role", roles.get(k), node, nodeid, k);
        }
    }

    private void registerJVMMetrics() {
        catalog.registerGauge("jvm_uptime_seconds", "JVM uptime", "node", "nodeid");
        catalog.registerGauge("jvm_mem_heap_max_bytes", "Maximum used memory in heap", "node", "nodeid");
        catalog.registerGauge("jvm_mem_heap_used_bytes", "Memory used in heap", "node", "nodeid");
        catalog.registerGauge("jvm_mem_heap_used_percent", "Percentage of memory used in heap", "node", "nodeid");
        catalog.registerGauge("jvm_mem_nonheap_used_bytes", "Memory used apart from heap", "node", "nodeid");
        catalog.registerGauge("jvm_mem_heap_committed_bytes", "Committed bytes in heap", "node", "nodeid");
        catalog.registerGauge("jvm_mem_nonheap_committed_bytes", "Committed bytes apart from heap", "node", "nodeid");

        catalog.registerGauge("jvm_mem_pool_max_bytes", "Maximum usage of memory pool", "node", "nodeid", "pool");
        catalog.registerGauge("jvm_mem_pool_peak_max_bytes", "Maximum usage peak of memory pool", "node", "nodeid", "pool");
        catalog.registerGauge("jvm_mem_pool_used_bytes", "Used memory in memory pool", "node", "nodeid", "pool");
        catalog.registerGauge("jvm_mem_pool_peak_used_bytes", "Used memory peak in memory pool", "node", "nodeid", "pool");

        catalog.registerGauge("jvm_threads_number", "Number of threads", "node", "nodeid");
        catalog.registerGauge("jvm_threads_peak_number", "Peak number of threads", "node", "nodeid");

        catalog.registerGauge("jvm_gc_collection_count", "Count of GC collections", "node", "nodeid", "gc");
        catalog.registerGauge("jvm_gc_collection_time_seconds", "Time spent for GC collections", "node", "nodeid", "gc");

        catalog.registerGauge("jvm_bufferpool_number", "Number of buffer pools", "node", "nodeid", "bufferpool");
        catalog.registerGauge("jvm_bufferpool_total_capacity_bytes", "Total capacity provided by buffer pools", "node", "nodeid", "bufferpool");
        catalog.registerGauge("jvm_bufferpool_used_bytes", "Used memory in buffer pools", "node", "nodeid", "bufferpool");

        catalog.registerGauge("jvm_classes_loaded_number", "Count of loaded classes", "node", "nodeid");
        catalog.registerGauge("jvm_classes_total_loaded_number", "Total count of loaded classes", "node", "nodeid");
        catalog.registerGauge("jvm_classes_unloaded_number", "Count of unloaded classes", "node", "nodeid");
    }

    private void updateJVMMetrics(JvmStats jvm) {
        if (jvm != null) {
            catalog.setGauge("jvm_uptime_seconds", jvm.getUptime().getSeconds(), node, nodeid);

            catalog.setGauge("jvm_mem_heap_max_bytes", jvm.getMem().getHeapMax().getBytes(), node, nodeid);
            catalog.setGauge("jvm_mem_heap_used_bytes", jvm.getMem().getHeapUsed().getBytes(), node, nodeid);
            catalog.setGauge("jvm_mem_heap_used_percent", jvm.getMem().getHeapUsedPercent(), node, nodeid);
            catalog.setGauge("jvm_mem_nonheap_used_bytes", jvm.getMem().getNonHeapUsed().getBytes(), node, nodeid);
            catalog.setGauge("jvm_mem_heap_committed_bytes", jvm.getMem().getHeapCommitted().getBytes(), node, nodeid);
            catalog.setGauge("jvm_mem_nonheap_committed_bytes", jvm.getMem().getNonHeapCommitted().getBytes(), node, nodeid);

            for (JvmStats.MemoryPool mp : jvm.getMem()) {
                String name = mp.getName();
                catalog.setGauge("jvm_mem_pool_max_bytes", mp.getMax().getBytes(), node, nodeid, name);
                catalog.setGauge("jvm_mem_pool_peak_max_bytes", mp.getPeakMax().getBytes(), node, nodeid, name);
                catalog.setGauge("jvm_mem_pool_used_bytes", mp.getUsed().getBytes(), node, nodeid, name);
                catalog.setGauge("jvm_mem_pool_peak_used_bytes", mp.getPeakUsed().getBytes(), node, nodeid, name);
            }

            catalog.setGauge("jvm_threads_number", jvm.getThreads().getCount(), node, nodeid);
            catalog.setGauge("jvm_threads_peak_number", jvm.getThreads().getPeakCount(), node, nodeid);

            for (JvmStats.GarbageCollector gc : jvm.getGc().getCollectors()) {
                String name = gc.getName();
                catalog.setGauge("jvm_gc_collection_count", gc.getCollectionCount(), node, nodeid, name);
                catalog.setGauge("jvm_gc_collection_time_seconds", gc.getCollectionTime().getSeconds(), node, nodeid, name);
            }

            for (JvmStats.BufferPool bp : jvm.getBufferPools()) {
                String name = bp.getName();
                catalog.setGauge("jvm_bufferpool_number", bp.getCount(), node, nodeid, name);
                catalog.setGauge("jvm_bufferpool_total_capacity_bytes", bp.getTotalCapacity().getBytes(), node, nodeid, name);
                catalog.setGauge("jvm_bufferpool_used_bytes", bp.getUsed().getBytes(), node, nodeid, name);
            }
            if (jvm.getClasses() != null) {
                catalog.setGauge("jvm_classes_loaded_number", jvm.getClasses().getLoadedClassCount(), node, nodeid);
                catalog.setGauge("jvm_classes_total_loaded_number", jvm.getClasses().getTotalLoadedClassCount(), node, nodeid);
                catalog.setGauge("jvm_classes_unloaded_number", jvm.getClasses().getUnloadedClassCount(), node, nodeid);
            }
        }
    }

    private void registerIndicesMetrics() {
        catalog.registerGauge("indices_doc_number", "Total number of documents", "node", "nodeid");
        catalog.registerGauge("indices_doc_deleted_number", "Number of deleted documents", "node", "nodeid");

        catalog.registerGauge("indices_store_size_bytes", "Store size of the indices in bytes", "node", "nodeid");

        catalog.registerGauge("indices_indexing_delete_count", "Count of documents deleted", "node", "nodeid");
        catalog.registerGauge("indices_indexing_delete_current_number", "Current rate of documents deleted", "node", "nodeid");
        catalog.registerGauge("indices_indexing_delete_time_seconds", "Time spent while deleting documents", "node", "nodeid");
        catalog.registerGauge("indices_indexing_index_count", "Count of documents indexed", "node", "nodeid");
        catalog.registerGauge("indices_indexing_index_current_number", "Current rate of documents indexed", "node", "nodeid");
        catalog.registerGauge("indices_indexing_index_failed_count", "Count of failed to index documents", "node", "nodeid");
        catalog.registerGauge("indices_indexing_index_time_seconds", "Time spent while indexing documents", "node", "nodeid");
        catalog.registerGauge("indices_indexing_noop_update_count", "Count of noop document updates", "node", "nodeid");
        catalog.registerGauge("indices_indexing_is_throttled_bool", "Is indexing throttling ?", "node", "nodeid");
        catalog.registerGauge("indices_indexing_throttle_time_seconds", "Time spent while throttling", "node", "nodeid");

        catalog.registerGauge("indices_get_count", "Count of get commands", "node", "nodeid");
        catalog.registerGauge("indices_get_time_seconds", "Time spent while get commands", "node", "nodeid");
        catalog.registerGauge("indices_get_exists_count", "Count of existing documents when get command", "node", "nodeid");
        catalog.registerGauge("indices_get_exists_time_seconds", "Time spent while existing documents get command", "node", "nodeid");
        catalog.registerGauge("indices_get_missing_count", "Count of missing documents when get command", "node", "nodeid");
        catalog.registerGauge("indices_get_missing_time_seconds", "Time spent while missing documents get command", "node", "nodeid");
        catalog.registerGauge("indices_get_current_number", "Current rate of get commands", "node", "nodeid");

        catalog.registerGauge("indices_search_open_contexts_number", "Number of search open contexts", "node", "nodeid");
        catalog.registerGauge("indices_search_fetch_count", "Count of search fetches", "node", "nodeid");
        catalog.registerGauge("indices_search_fetch_current_number", "Current rate of search fetches", "node", "nodeid");
        catalog.registerGauge("indices_search_fetch_time_seconds", "Time spent while search fetches", "node", "nodeid");
        catalog.registerGauge("indices_search_query_count", "Count of search queries", "node", "nodeid");
        catalog.registerGauge("indices_search_query_current_number", "Current rate of search queries", "node", "nodeid");
        catalog.registerGauge("indices_search_query_time_seconds", "Time spent while search queries", "node", "nodeid");
        catalog.registerGauge("indices_search_scroll_count", "Count of search scrolls", "node", "nodeid");
        catalog.registerGauge("indices_search_scroll_current_number", "Current rate of search scrolls", "node", "nodeid");
        catalog.registerGauge("indices_search_scroll_time_seconds", "Time spent while search scrolls", "node", "nodeid");

        catalog.registerGauge("indices_merges_current_number", "Current rate of merges", "node", "nodeid");
        catalog.registerGauge("indices_merges_current_docs_number", "Current rate of documents merged", "node", "nodeid");
        catalog.registerGauge("indices_merges_current_size_bytes", "Current rate of bytes merged", "node", "nodeid");
        catalog.registerGauge("indices_merges_total_number", "Count of merges", "node", "nodeid");
        catalog.registerGauge("indices_merges_total_time_seconds", "Time spent while merging", "node", "nodeid");
        catalog.registerGauge("indices_merges_total_docs_count", "Count of documents merged", "node", "nodeid");
        catalog.registerGauge("indices_merges_total_size_bytes", "Count of bytes of merged documents", "node", "nodeid");
        catalog.registerGauge("indices_merges_total_stopped_time_seconds", "Time spent while merge process stopped", "node", "nodeid");
        catalog.registerGauge("indices_merges_total_throttled_time_seconds", "Time spent while merging when throttling", "node", "nodeid");
        catalog.registerGauge("indices_merges_total_auto_throttle_bytes", "Bytes merged while throttling", "node", "nodeid");

        catalog.registerGauge("indices_refresh_total_count", "Count of refreshes", "node", "nodeid");
        catalog.registerGauge("indices_refresh_total_time_seconds", "Time spent while refreshes", "node", "nodeid");
        catalog.registerGauge("indices_refresh_listeners_number", "Number of refresh listeners", "node", "nodeid");

        catalog.registerGauge("indices_flush_total_count", "Count of flushes", "node", "nodeid");
        catalog.registerGauge("indices_flush_total_time_seconds", "Total time spent while flushes", "node", "nodeid");

        catalog.registerGauge("indices_querycache_cache_count", "Count of queries in cache", "node", "nodeid");
        catalog.registerGauge("indices_querycache_cache_size_bytes", "Query cache size", "node", "nodeid");
        catalog.registerGauge("indices_querycache_evictions_count", "Count of evictions in query cache", "node", "nodeid");
        catalog.registerGauge("indices_querycache_hit_count", "Count of hits in query cache", "node", "nodeid");
        catalog.registerGauge("indices_querycache_memory_size_bytes", "Memory usage of query cache", "node", "nodeid");
        catalog.registerGauge("indices_querycache_miss_number", "Count of misses in query cache", "node", "nodeid");
        catalog.registerGauge("indices_querycache_total_number", "Count of usages of query cache", "node", "nodeid");

        catalog.registerGauge("indices_fielddata_memory_size_bytes", "Memory usage of field date cache", "node", "nodeid");
        catalog.registerGauge("indices_fielddata_evictions_count", "Count of evictions in field data cache", "node", "nodeid");

        catalog.registerGauge("indices_percolate_count", "Count of percolates", "node", "nodeid");
        catalog.registerGauge("indices_percolate_current_number", "Rate of percolates", "node", "nodeid");
        catalog.registerGauge("indices_percolate_memory_size_bytes", "Percolate memory size", "node", "nodeid");
        catalog.registerGauge("indices_percolate_queries_count", "Count of queries percolated", "node", "nodeid");
        catalog.registerGauge("indices_percolate_time_seconds", "Time spent while percolating", "node", "nodeid");

        catalog.registerGauge("indices_completion_size_bytes", "Size of completion suggest statistics", "node", "nodeid");

        catalog.registerGauge("indices_segments_number", "Current number of segments", "node", "nodeid");
        catalog.registerGauge("indices_segments_memory_bytes", "Memory used by segments", "node", "nodeid", "type");

        catalog.registerGauge("indices_suggest_current_number", "Current rate of suggests", "node", "nodeid");
        catalog.registerGauge("indices_suggest_count", "Count of suggests", "node", "nodeid");
        catalog.registerGauge("indices_suggest_time_seconds", "Time spent while making suggests", "node", "nodeid");

        catalog.registerGauge("indices_requestcache_memory_size_bytes", "Memory used for request cache", "node", "nodeid");
        catalog.registerGauge("indices_requestcache_hit_count", "Number of hits in request cache", "node", "nodeid");
        catalog.registerGauge("indices_requestcache_miss_count", "Number of misses in request cache", "node", "nodeid");
        catalog.registerGauge("indices_requestcache_evictions_count", "Number of evictions in request cache", "node", "nodeid");

        catalog.registerGauge("indices_recovery_current_number", "Current number of recoveries", "node", "nodeid", "type");
        catalog.registerGauge("indices_recovery_throttle_time_seconds", "Time spent while throttling recoveries", "node", "nodeid");
    }

    private void updateIndicesMetrics(NodeIndicesStats idx) {
        if (idx != null) {
            catalog.setGauge("indices_doc_number", idx.getDocs().getCount(), node, nodeid);
            catalog.setGauge("indices_doc_deleted_number", idx.getDocs().getDeleted(), node, nodeid);

            catalog.setGauge("indices_store_size_bytes", idx.getStore().getSizeInBytes(), node, nodeid);

            catalog.setGauge("indices_indexing_delete_count", idx.getIndexing().getTotal().getDeleteCount(), node, nodeid);
            catalog.setGauge("indices_indexing_delete_current_number", idx.getIndexing().getTotal().getDeleteCurrent(), node, nodeid);
            catalog.setGauge("indices_indexing_delete_time_seconds", idx.getIndexing().getTotal().getDeleteTime().seconds(), node, nodeid);
            catalog.setGauge("indices_indexing_index_count", idx.getIndexing().getTotal().getIndexCount(), node, nodeid);
            catalog.setGauge("indices_indexing_index_current_number", idx.getIndexing().getTotal().getIndexCurrent(), node, nodeid);
            catalog.setGauge("indices_indexing_index_failed_count", idx.getIndexing().getTotal().getIndexFailedCount(), node, nodeid);
            catalog.setGauge("indices_indexing_index_time_seconds", idx.getIndexing().getTotal().getIndexTime().seconds(), node, nodeid);
            catalog.setGauge("indices_indexing_noop_update_count", idx.getIndexing().getTotal().getNoopUpdateCount(), node, nodeid);
            catalog.setGauge("indices_indexing_is_throttled_bool", idx.getIndexing().getTotal().isThrottled() ? 1 : 0, node, nodeid);
            catalog.setGauge("indices_indexing_throttle_time_seconds", idx.getIndexing().getTotal().getThrottleTime().seconds(), node, nodeid);

            catalog.setGauge("indices_get_count", idx.getGet().getCount(), node, nodeid);
            catalog.setGauge("indices_get_time_seconds", idx.getGet().getTimeInMillis() / 1000.0, node, nodeid);
            catalog.setGauge("indices_get_exists_count", idx.getGet().getExistsCount(), node, nodeid);
            catalog.setGauge("indices_get_exists_time_seconds", idx.getGet().getExistsTimeInMillis() / 1000.0, node, nodeid);
            catalog.setGauge("indices_get_missing_count", idx.getGet().getMissingCount(), node, nodeid);
            catalog.setGauge("indices_get_missing_time_seconds", idx.getGet().getMissingTimeInMillis() / 1000.0, node, nodeid);
            catalog.setGauge("indices_get_current_number", idx.getGet().current(), node, nodeid);

            catalog.setGauge("indices_search_open_contexts_number", idx.getSearch().getOpenContexts(), node, nodeid);
            catalog.setGauge("indices_search_fetch_count", idx.getSearch().getTotal().getFetchCount(), node, nodeid);
            catalog.setGauge("indices_search_fetch_current_number", idx.getSearch().getTotal().getFetchCurrent(), node, nodeid);
            catalog.setGauge("indices_search_fetch_time_seconds", idx.getSearch().getTotal().getFetchTimeInMillis() / 1000.0, node, nodeid);
            catalog.setGauge("indices_search_query_count", idx.getSearch().getTotal().getQueryCount(), node, nodeid);
            catalog.setGauge("indices_search_query_current_number", idx.getSearch().getTotal().getQueryCurrent(), node, nodeid);
            catalog.setGauge("indices_search_query_time_seconds", idx.getSearch().getTotal().getQueryTimeInMillis() / 1000.0, node, nodeid);
            catalog.setGauge("indices_search_scroll_count", idx.getSearch().getTotal().getScrollCount(), node, nodeid);
            catalog.setGauge("indices_search_scroll_current_number", idx.getSearch().getTotal().getScrollCurrent(), node, nodeid);
            catalog.setGauge("indices_search_scroll_time_seconds", idx.getSearch().getTotal().getScrollTimeInMillis() / 1000.0, node, nodeid);

            catalog.setGauge("indices_merges_current_number", idx.getMerge().getCurrent(), node, nodeid);
            catalog.setGauge("indices_merges_current_docs_number", idx.getMerge().getCurrentNumDocs(), node, nodeid);
            catalog.setGauge("indices_merges_current_size_bytes", idx.getMerge().getCurrentSizeInBytes(), node, nodeid);
            catalog.setGauge("indices_merges_total_number", idx.getMerge().getTotal(), node, nodeid);
            catalog.setGauge("indices_merges_total_time_seconds", idx.getMerge().getTotalTimeInMillis() / 1000.0, node, nodeid);
            catalog.setGauge("indices_merges_total_docs_count", idx.getMerge().getTotalNumDocs(), node, nodeid);
            catalog.setGauge("indices_merges_total_size_bytes", idx.getMerge().getTotalSizeInBytes(), node, nodeid);
            catalog.setGauge("indices_merges_total_stopped_time_seconds", idx.getMerge().getTotalStoppedTimeInMillis() / 1000.0, node, nodeid);
            catalog.setGauge("indices_merges_total_throttled_time_seconds", idx.getMerge().getTotalThrottledTimeInMillis() / 1000.0, node, nodeid);
            catalog.setGauge("indices_merges_total_auto_throttle_bytes", idx.getMerge().getTotalBytesPerSecAutoThrottle(), node, nodeid);

            catalog.setGauge("indices_refresh_total_count", idx.getRefresh().getTotal(), node, nodeid);
            catalog.setGauge("indices_refresh_total_time_seconds", idx.getRefresh().getTotalTimeInMillis() / 1000.0, node, nodeid);
            catalog.setGauge("indices_refresh_listeners_number", idx.getRefresh().getListeners(), node, nodeid);

            catalog.setGauge("indices_flush_total_count", idx.getFlush().getTotal(), node, nodeid);
            catalog.setGauge("indices_flush_total_time_seconds", idx.getFlush().getTotalTimeInMillis() / 1000.0, node, nodeid);

            catalog.setGauge("indices_querycache_cache_count", idx.getQueryCache().getCacheCount(), node, nodeid);
            catalog.setGauge("indices_querycache_cache_size_bytes", idx.getQueryCache().getCacheSize(), node, nodeid);
            catalog.setGauge("indices_querycache_evictions_count", idx.getQueryCache().getEvictions(), node, nodeid);
            catalog.setGauge("indices_querycache_hit_count", idx.getQueryCache().getHitCount(), node, nodeid);
            catalog.setGauge("indices_querycache_memory_size_bytes", idx.getQueryCache().getMemorySizeInBytes(), node, nodeid);
            catalog.setGauge("indices_querycache_miss_number", idx.getQueryCache().getMissCount(), node, nodeid);
            catalog.setGauge("indices_querycache_total_number", idx.getQueryCache().getTotalCount(), node, nodeid);

            catalog.setGauge("indices_fielddata_memory_size_bytes", idx.getFieldData().getMemorySizeInBytes(), node, nodeid);
            catalog.setGauge("indices_fielddata_evictions_count", idx.getFieldData().getEvictions(), node, nodeid);

            catalog.setGauge("indices_completion_size_bytes", idx.getCompletion().getSizeInBytes(), node, nodeid);

            catalog.setGauge("indices_segments_number", idx.getSegments().getCount(), node, nodeid);
            catalog.setGauge("indices_segments_memory_bytes", idx.getSegments().getMemoryInBytes(), node, nodeid, "all");
            catalog.setGauge("indices_segments_memory_bytes", idx.getSegments().getBitsetMemoryInBytes(), node, nodeid, "bitset");
            catalog.setGauge("indices_segments_memory_bytes", idx.getSegments().getDocValuesMemoryInBytes(), node, nodeid, "docvalues");
            catalog.setGauge("indices_segments_memory_bytes", idx.getSegments().getIndexWriterMemoryInBytes(), node, nodeid, "indexwriter");
            catalog.setGauge("indices_segments_memory_bytes", idx.getSegments().getNormsMemoryInBytes(), node, nodeid, "norms");
            catalog.setGauge("indices_segments_memory_bytes", idx.getSegments().getStoredFieldsMemoryInBytes(), node, nodeid, "storefields");
            catalog.setGauge("indices_segments_memory_bytes", idx.getSegments().getTermsMemoryInBytes(), node, nodeid, "terms");
            catalog.setGauge("indices_segments_memory_bytes", idx.getSegments().getTermVectorsMemoryInBytes(), node, nodeid, "termvectors");
            catalog.setGauge("indices_segments_memory_bytes", idx.getSegments().getVersionMapMemoryInBytes(), node, nodeid, "versionmap");
            catalog.setGauge("indices_segments_memory_bytes", idx.getSegments().getPointsMemoryInBytes(), node, nodeid, "points");

            catalog.setGauge("indices_suggest_current_number", idx.getSearch().getTotal().getSuggestCurrent(), node, nodeid);
            catalog.setGauge("indices_suggest_count", idx.getSearch().getTotal().getSuggestCount(), node, nodeid);
            catalog.setGauge("indices_suggest_time_seconds", idx.getSearch().getTotal().getSuggestTimeInMillis() / 1000.0, node, nodeid);

            catalog.setGauge("indices_requestcache_memory_size_bytes", idx.getRequestCache().getMemorySizeInBytes(), node, nodeid);
            catalog.setGauge("indices_requestcache_hit_count", idx.getRequestCache().getHitCount(), node, nodeid);
            catalog.setGauge("indices_requestcache_miss_count", idx.getRequestCache().getMissCount(), node, nodeid);
            catalog.setGauge("indices_requestcache_evictions_count", idx.getRequestCache().getEvictions(), node, nodeid);

            catalog.setGauge("indices_recovery_current_number", idx.getRecoveryStats().currentAsSource(), node, nodeid, "source");
            catalog.setGauge("indices_recovery_current_number", idx.getRecoveryStats().currentAsTarget(), node, nodeid, "target");
            catalog.setGauge("indices_recovery_throttle_time_seconds", idx.getRecoveryStats().throttleTime().getSeconds(), node, nodeid);
        }
    }

    private void registerIngestMetrics() {
        catalog.registerGauge("ingest_total_count", "Ingestion total number", "node", "nodeid");
        catalog.registerGauge("ingest_total_time_seconds", "Ingestion total time in seconds", "node", "nodeid");
        catalog.registerGauge("ingest_total_current", "Ingestion total current", "node", "nodeid");
        catalog.registerGauge("ingest_total_failed_count", "Ingestion total failed", "node", "nodeid");

        catalog.registerGauge("ingest_pipeline_total_count", "Ingestion total number", "node", "nodeid", "pipeline");
        catalog.registerGauge("ingest_pipeline_total_time_seconds", "Ingestion total time in seconds", "node", "nodeid", "pipeline");
        catalog.registerGauge("ingest_pipeline_total_current", "Ingestion total current", "node", "nodeid", "pipeline");
        catalog.registerGauge("ingest_pipeline_total_failed_count", "Ingestion total failed", "node", "nodeid", "pipeline");
    }

    private void updateIngestMetrics(IngestStats is) {
        catalog.setGauge("ingest_total_count", is.getTotalStats().getIngestCount(), node, nodeid);
        catalog.setGauge("ingest_total_time_seconds", is.getTotalStats().getIngestTimeInMillis() / 1000.0, node, nodeid);
        catalog.setGauge("ingest_total_current", is.getTotalStats().getIngestCurrent(), node, nodeid);
        catalog.setGauge("ingest_total_failed_count", is.getTotalStats().getIngestFailedCount(), node, nodeid);

        for (Map.Entry<String, IngestStats.Stats> entry : is.getStatsPerPipeline().entrySet()) {
            String pipeline = entry.getKey();
            catalog.setGauge("ingest_pipeline_total_count", entry.getValue().getIngestCount(), node, nodeid, pipeline);
            catalog.setGauge("ingest_pipeline_total_time_seconds", entry.getValue().getIngestTimeInMillis() / 1000.0, node, nodeid, pipeline);
            catalog.setGauge("ingest_pipeline_total_current", entry.getValue().getIngestCurrent(), node, nodeid, pipeline);
            catalog.setGauge("ingest_pipeline_total_failed_count", entry.getValue().getIngestFailedCount(), node, nodeid, pipeline);
        }
    }

    private void registerTransportMetrics() {
        catalog.registerGauge("transport_server_open_number", "Opened server connections", "node", "nodeid");
        catalog.registerGauge("transport_rx_packets_count", "Received packets", "node", "nodeid");
        catalog.registerGauge("transport_tx_packets_count", "Sent packets", "node", "nodeid");
        catalog.registerGauge("transport_rx_bytes_count", "Bytes received", "node", "nodeid");
        catalog.registerGauge("transport_tx_bytes_count", "Bytes sent", "node", "nodeid");
    }

    private void updateTransportMetrics(TransportStats ts) {
        if (ts != null) {
            catalog.setGauge("transport_server_open_number", ts.getServerOpen(), node, nodeid);
            catalog.setGauge("transport_rx_packets_count", ts.getRxCount(), node, nodeid);
            catalog.setGauge("transport_tx_packets_count", ts.getTxCount(), node, nodeid);
            catalog.setGauge("transport_rx_bytes_count", ts.getRxSize().getBytes(), node, nodeid);
            catalog.setGauge("transport_tx_bytes_count", ts.getTxSize().getBytes(), node, nodeid);
        }
    }

    private void registerHTTPMetrics() {
        catalog.registerGauge("http_open_server_number", "Number of open server connections", "node", "nodeid");
        catalog.registerGauge("http_open_total_count", "Count of opened connections", "node", "nodeid");
    }

    private void updateHTTPMetrics(HttpStats http) {
        if (http != null) {
            catalog.setGauge("http_open_server_number", http.getServerOpen(), node, nodeid);
            catalog.setGauge("http_open_total_count", http.getTotalOpen(), node, nodeid);
        }
    }

    private void registerScriptMetrics() {
        catalog.registerGauge("script_cache_evictions_count", "Number of evictions in scripts cache", "node", "nodeid");
        catalog.registerGauge("script_compilations_count", "Number of scripts compilations", "node", "nodeid");
    }

    private void updateScriptMetrics(ScriptStats sc) {
        if (sc != null) {
            catalog.setGauge("script_cache_evictions_count", sc.getCacheEvictions(), node, nodeid);
            catalog.setGauge("script_compilations_count", sc.getCompilations(), node, nodeid);
        }
    }

    private void registerProcessMetrics() {
        catalog.registerGauge("process_cpu_percent", "CPU percentage used by ES process", "node", "nodeid");
        catalog.registerGauge("process_cpu_time_seconds", "CPU time used by ES process", "node", "nodeid");
        catalog.registerGauge("process_mem_total_virtual_bytes", "Memory used by ES process", "node", "nodeid");
        catalog.registerGauge("process_file_descriptors_open_number", "Open file descriptors", "node", "nodeid");
        catalog.registerGauge("process_file_descriptors_max_number", "Max file descriptors", "node", "nodeid");
    }

    private void updateProcessMetrics(ProcessStats ps) {
        if (ps != null) {
            catalog.setGauge("process_cpu_percent", ps.getCpu().getPercent(), node, nodeid);
            catalog.setGauge("process_cpu_time_seconds", ps.getCpu().getTotal().getSeconds(), node, nodeid);
            catalog.setGauge("process_mem_total_virtual_bytes", ps.getMem().getTotalVirtual().getBytes(), node, nodeid);
            catalog.setGauge("process_file_descriptors_open_number", ps.getOpenFileDescriptors(), node, nodeid);
            catalog.setGauge("process_file_descriptors_max_number", ps.getMaxFileDescriptors(), node, nodeid);
        }
    }

    private void registerOsMetrics() {
        catalog.registerGauge("os_cpu_percent", "CPU usage in percent", "node", "nodeid");
        catalog.registerGauge("os_load_average_one_minute", "CPU load", "node", "nodeid");
        catalog.registerGauge("os_load_average_five_minutes", "CPU load", "node", "nodeid");
        catalog.registerGauge("os_load_average_fifteen_minutes", "CPU load", "node", "nodeid");
        catalog.registerGauge("os_mem_free_bytes", "Memory free", "node", "nodeid");
        catalog.registerGauge("os_mem_free_percent", "Memory free in percent", "node", "nodeid");
        catalog.registerGauge("os_mem_used_bytes", "Memory used", "node", "nodeid");
        catalog.registerGauge("os_mem_used_percent", "Memory used in percent", "node", "nodeid");
        catalog.registerGauge("os_mem_total_bytes", "Total memory size", "node", "nodeid");
        catalog.registerGauge("os_swap_free_bytes", "Swap free", "node", "nodeid");
        catalog.registerGauge("os_swap_used_bytes", "Swap used", "node", "nodeid");
        catalog.registerGauge("os_swap_total_bytes", "Total swap size", "node", "nodeid");
    }

    private void updateOsMetrics(OsStats os) {
        if (os != null) {
            if (os.getCpu() != null) {
                catalog.setGauge("os_cpu_percent", os.getCpu().getPercent(), node, nodeid);
                double[] loadAverage = os.getCpu().getLoadAverage();
                if (loadAverage != null && loadAverage.length == 3) {
                    catalog.setGauge("os_load_average_one_minute", os.getCpu().getLoadAverage()[0], node, nodeid);
                    catalog.setGauge("os_load_average_five_minutes", os.getCpu().getLoadAverage()[1], node, nodeid);
                    catalog.setGauge("os_load_average_fifteen_minutes", os.getCpu().getLoadAverage()[2], node, nodeid);
                }
            }

            if (os.getMem() != null) {
                catalog.setGauge("os_mem_free_bytes", os.getMem().getFree().getBytes(), node, nodeid);
                catalog.setGauge("os_mem_free_percent", os.getMem().getFreePercent(), node, nodeid);
                catalog.setGauge("os_mem_used_bytes", os.getMem().getUsed().getBytes(), node, nodeid);
                catalog.setGauge("os_mem_used_percent", os.getMem().getUsedPercent(), node, nodeid);
                catalog.setGauge("os_mem_total_bytes", os.getMem().getTotal().getBytes(), node, nodeid);
            }

            if (os.getSwap() != null) {
                catalog.setGauge("os_swap_free_bytes", os.getSwap().getFree().getBytes(), node, nodeid);
                catalog.setGauge("os_swap_used_bytes", os.getSwap().getUsed().getBytes(), node, nodeid);
                catalog.setGauge("os_swap_total_bytes", os.getSwap().getTotal().getBytes(), node, nodeid);
            }
        }
    }

    private void registerCircuitBreakerMetrics() {
        catalog.registerGauge("circuitbreaker_estimated_bytes", "Circuit breaker estimated size", "node", "nodeid", "name");
        catalog.registerGauge("circuitbreaker_limit_bytes", "Circuit breaker size limit", "node", "nodeid", "name");
        catalog.registerGauge("circuitbreaker_overhead_ratio", "Circuit breaker overhead ratio", "node", "nodeid", "name");
        catalog.registerGauge("circuitbreaker_tripped_count", "Circuit breaker tripped count", "node", "nodeid", "name");
    }

    private void updateCircuitBreakersMetrics(AllCircuitBreakerStats acbs) {
        if (acbs != null) {
            for (CircuitBreakerStats cbs : acbs.getAllStats()) {
                String name = cbs.getName();
                catalog.setGauge("circuitbreaker_estimated_bytes", cbs.getEstimated(), node, nodeid, name);
                catalog.setGauge("circuitbreaker_limit_bytes", cbs.getLimit(), node, nodeid, name);
                catalog.setGauge("circuitbreaker_overhead_ratio", cbs.getOverhead(), node, nodeid, name);
                catalog.setGauge("circuitbreaker_tripped_count", cbs.getTrippedCount(), node, nodeid, name);
            }
        }
    }

    private void registerThreadPoolMetrics() {
        catalog.registerGauge("threadpool_threads_number", "Number of threads in thread pool", "node", "nodeid", "name", "type");
        catalog.registerGauge("threadpool_threads_count", "Count of threads in thread pool", "node", "nodeid", "name", "type");
        catalog.registerGauge("threadpool_tasks_number", "Number of tasks in thread pool", "node", "nodeid", "name", "type");
    }

    private void updateThreadPoolMetrics(ThreadPoolStats tps) {
        if (tps != null) {
            for (ThreadPoolStats.Stats st : tps) {
                String name = st.getName();
                catalog.setGauge("threadpool_threads_number", st.getThreads(), node, nodeid, name, "threads");
                catalog.setGauge("threadpool_threads_number", st.getActive(), node, nodeid, name, "active");
                catalog.setGauge("threadpool_threads_number", st.getLargest(), node, nodeid, name, "largest");
                catalog.setGauge("threadpool_threads_count", st.getCompleted(), node, nodeid, name, "completed");
                catalog.setGauge("threadpool_threads_count", st.getRejected(), node, nodeid, name, "rejected");
                catalog.setGauge("threadpool_tasks_number", st.getQueue(), node, nodeid, name, "queue");
            }
        }
    }

    private void registerFsMetrics() {
        catalog.registerGauge("fs_total_total_bytes", "Total disk space for all mount points", "node", "nodeid");
        catalog.registerGauge("fs_total_available_bytes", "Available disk space for all mount points", "node", "nodeid");
        catalog.registerGauge("fs_total_free_bytes", "Free disk space for all mountpoints", "node", "nodeid");

        catalog.registerGauge("fs_most_usage_free_bytes", "Free disk space for most used mountpoint", "node", "nodeid", "path");
        catalog.registerGauge("fs_most_usage_total_bytes", "Total disk space for most used mountpoint", "node", "nodeid", "path");

        catalog.registerGauge("fs_least_usage_free_bytes", "Free disk space for least used mountpoint", "node", "nodeid", "path");
        catalog.registerGauge("fs_least_usage_total_bytes", "Total disk space for least used mountpoint", "node", "nodeid", "path");

        catalog.registerGauge("fs_path_total_bytes", "Total disk space", "node", "nodeid", "path", "mount", "type");
        catalog.registerGauge("fs_path_available_bytes", "Available disk space", "node", "nodeid", "path", "mount", "type");
        catalog.registerGauge("fs_path_free_bytes", "Free disk space", "node", "nodeid", "path", "mount", "type");

        catalog.registerGauge("fs_io_total_operations", "Total IO operations", "node", "nodeid");
        catalog.registerGauge("fs_io_total_read_operations", "Total IO read operations", "node", "nodeid");
        catalog.registerGauge("fs_io_total_write_operations", "Total IO write operations", "node", "nodeid");
        catalog.registerGauge("fs_io_total_read_bytes", "Total IO read bytes", "node", "nodeid");
        catalog.registerGauge("fs_io_total_write_bytes", "Total IO write bytes", "node", "nodeid");
    }

    private void updateFsMetrics(FsInfo fs) {
        if (fs != null) {
            catalog.setGauge("fs_total_total_bytes", fs.getTotal().getTotal().getBytes(), node, nodeid);
            catalog.setGauge("fs_total_available_bytes", fs.getTotal().getAvailable().getBytes(), node, nodeid);
            catalog.setGauge("fs_total_free_bytes", fs.getTotal().getFree().getBytes(), node, nodeid);

            if (fs.getMostDiskEstimate() != null) {
                catalog.setGauge("fs_most_usage_free_bytes", fs.getMostDiskEstimate().getFreeBytes(), node, nodeid, fs.getMostDiskEstimate().getPath());
                catalog.setGauge("fs_most_usage_total_bytes", fs.getMostDiskEstimate().getTotalBytes(), node, nodeid, fs.getMostDiskEstimate().getPath());
            }

            if (fs.getLeastDiskEstimate() != null) {
                catalog.setGauge("fs_least_usage_free_bytes", fs.getLeastDiskEstimate().getFreeBytes(), node, nodeid, fs.getLeastDiskEstimate().getPath());
                catalog.setGauge("fs_least_usage_total_bytes", fs.getLeastDiskEstimate().getTotalBytes(), node, nodeid, fs.getLeastDiskEstimate().getPath());
            }

            for (FsInfo.Path fspath : fs) {
                String path = fspath.getPath();
                String mount = fspath.getMount();
                String type = fspath.getType();
                catalog.setGauge("fs_path_total_bytes", fspath.getTotal().getBytes(), node, nodeid, path, mount, type);
                catalog.setGauge("fs_path_available_bytes", fspath.getAvailable().getBytes(), node, nodeid, path, mount, type);
                catalog.setGauge("fs_path_free_bytes", fspath.getFree().getBytes(), node, nodeid, path, mount, type);
            }

            FsInfo.IoStats ioStats = fs.getIoStats();
            if (ioStats != null) {
                catalog.setGauge("fs_io_total_operations", fs.getIoStats().getTotalOperations(), node, nodeid);
                catalog.setGauge("fs_io_total_read_operations", fs.getIoStats().getTotalReadOperations(), node, nodeid);
                catalog.setGauge("fs_io_total_write_operations", fs.getIoStats().getTotalWriteOperations(), node, nodeid);
                catalog.setGauge("fs_io_total_read_bytes", fs.getIoStats().getTotalReadKilobytes() * 1024, node, nodeid);
                catalog.setGauge("fs_io_total_write_bytes", fs.getIoStats().getTotalWriteKilobytes() * 1024, node, nodeid);
            }
        }
    }

    private void registerPerIndexMetrics() {
        catalog.registerGauge("index_status", "Index status", "index");
        catalog.registerGauge("index_replicas_number", "Number of replicas", "index");
        catalog.registerGauge("index_shards_number", "Number of shards", "type", "index");
    }

    private void updatePerIndexMetrics(ClusterHealthResponse chr) {
        for (String index_name : chr.getIndices().keySet()) {
            ClusterIndexHealth cih = chr.getIndices().get(index_name);
            catalog.setGauge("index_status", cih.getStatus().value(), index_name);
            catalog.setGauge("index_replicas_number", cih.getNumberOfReplicas(), index_name);
            catalog.setGauge("index_shards_number", cih.getActiveShards(), "active", index_name);
            catalog.setGauge("index_shards_number", cih.getNumberOfShards(), "shards", index_name);
            catalog.setGauge("index_shards_number", cih.getActivePrimaryShards(), "active_primary", index_name);
            catalog.setGauge("index_shards_number", cih.getInitializingShards(), "initializing", index_name);
            catalog.setGauge("index_shards_number", cih.getRelocatingShards(), "relocating", index_name);
            catalog.setGauge("index_shards_number", cih.getUnassignedShards(), "unassigned", index_name);
        }
    }

    public void updateMetrics(ClusterHealthResponse clusterHealthResponse, NodeStats nodeStats) {
        Summary.Timer timer = catalog.startSummaryTimer("metrics_generate_time_seconds", node, nodeid);

        updateClusterMetrics(clusterHealthResponse);
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
            updatePerIndexMetrics(clusterHealthResponse);
        }

        timer.observeDuration();
    }

    public PrometheusMetricsCatalog getCatalog() {
        return catalog;
    }
}
