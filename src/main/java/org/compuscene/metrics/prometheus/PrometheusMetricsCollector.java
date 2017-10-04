package org.compuscene.metrics.prometheus;

import io.prometheus.client.Summary;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.admin.cluster.node.stats.NodeStats;
import org.elasticsearch.http.HttpStats;
import org.elasticsearch.indices.NodeIndicesStats;
import org.elasticsearch.indices.breaker.AllCircuitBreakerStats;
import org.elasticsearch.indices.breaker.CircuitBreakerStats;
import org.elasticsearch.monitor.fs.FsInfo;
import org.elasticsearch.monitor.jvm.JvmStats;
import org.elasticsearch.monitor.os.OsStats;
import org.elasticsearch.monitor.process.ProcessStats;
import org.elasticsearch.script.ScriptStats;
import org.elasticsearch.threadpool.ThreadPoolStats;
import org.elasticsearch.transport.TransportStats;

public class PrometheusMetricsCollector {

    private String cluster;
    private String node;
    private String nodeId;

    private PrometheusMetricsCatalog catalog;

    public PrometheusMetricsCollector (final String clusterName, final String nodeName, final String nodeId) {
        this.cluster = clusterName;
        this.node = nodeName;
        this.nodeId = nodeId;
        catalog = new PrometheusMetricsCatalog(cluster, "es_");
        registerMetrics();
    }

    private void registerMetrics() {
        catalog.registerSummaryTimer("metrics_generate_time_seconds", "Time spent while generating metrics", "node", "nodeId");

        registerClusterMetrics();
        registerJVMMetrics();
        registerIndicesMetrics();
        registerTransportMetrics();
        registerHTTPMetrics();
        registerScriptMetrics();
        registerProcessMetrics();
        registerOsMetrics();
        registerCircuitBreakerMetrics();
        registerThreadPoolMetrics();
        registerFsMetrics();
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

    private void registerJVMMetrics() {
        catalog.registerCounter("jvm_uptime_seconds", "JVM uptime", "node", "nodeId");
        catalog.registerGauge("jvm_mem_heap_max_bytes", "Maximum used memory in heap", "node", "nodeId");
        catalog.registerGauge("jvm_mem_heap_used_bytes", "Memory used in heap", "node", "nodeId");
        catalog.registerGauge("jvm_mem_heap_used_percent", "Percentage of memory used in heap", "node", "nodeId");
        catalog.registerGauge("jvm_mem_nonheap_used_bytes", "Memory used apart from heap", "node", "nodeId");
        catalog.registerGauge("jvm_mem_heap_committed_bytes", "Committed bytes in heap", "node", "nodeId");
        catalog.registerGauge("jvm_mem_nonheap_committed_bytes", "Committed bytes apart from heap", "node", "nodeId");

        catalog.registerGauge("jvm_mem_pool_max_bytes", "Maximum usage of memory pool", "node", "nodeId", "pool");
        catalog.registerGauge("jvm_mem_pool_peak_max_bytes", "Maximum usage peak of memory pool", "node", "nodeId", "pool");
        catalog.registerGauge("jvm_mem_pool_used_bytes", "Used memory in memory pool", "node", "nodeId", "pool");
        catalog.registerGauge("jvm_mem_pool_peak_used_bytes", "Used memory peak in memory pool", "node", "nodeId", "pool");

        catalog.registerGauge("jvm_threads_number", "Number of threads", "node", "nodeId");
        catalog.registerGauge("jvm_threads_peak_number", "Peak number of threads", "node", "nodeId");

        catalog.registerCounter("jvm_gc_collection_count", "Count of GC collections", "node", "nodeId", "gc");
        catalog.registerCounter("jvm_gc_collection_time_seconds", "Time spent for GC collections", "node", "nodeId", "gc");

        catalog.registerGauge("jvm_bufferpool_number", "Number of buffer pools", "node", "nodeId", "bufferpool");
        catalog.registerGauge("jvm_bufferpool_total_capacity_bytes", "Total capacity provided by buffer pools", "node", "nodeId", "bufferpool");
        catalog.registerGauge("jvm_bufferpool_used_bytes", "Used memory in buffer pools", "node", "nodeId", "bufferpool");

        catalog.registerGauge("jvm_classes_loaded_number", "Count of loaded classes", "node", "nodeId");
        catalog.registerGauge("jvm_classes_total_loaded_number", "Total count of loaded classes", "node", "nodeId");
        catalog.registerGauge("jvm_classes_unloaded_number", "Count of unloaded classes", "node", "nodeId");
    }

    private void updateJVMMetrics(JvmStats jvm) {
        if (jvm != null) {
            catalog.setCounter("jvm_uptime_seconds", jvm.getUptime().getSeconds(), node, nodeId);

            catalog.setGauge("jvm_mem_heap_max_bytes", jvm.getMem().getHeapMax().bytes(), node, nodeId);
            catalog.setGauge("jvm_mem_heap_used_bytes", jvm.getMem().getHeapUsed().bytes(), node, nodeId);
            catalog.setGauge("jvm_mem_heap_used_percent", jvm.getMem().getHeapUsedPercent(), node, nodeId);
            catalog.setGauge("jvm_mem_nonheap_used_bytes", jvm.getMem().getNonHeapUsed().bytes(), node, nodeId);
            catalog.setGauge("jvm_mem_heap_committed_bytes", jvm.getMem().getHeapCommitted().bytes(), node, nodeId);
            catalog.setGauge("jvm_mem_nonheap_committed_bytes", jvm.getMem().getNonHeapCommitted().bytes(), node, nodeId);

            for (JvmStats.MemoryPool mp : jvm.getMem()) {
                String name = mp.getName();
                catalog.setGauge("jvm_mem_pool_max_bytes", mp.getMax().bytes(), node, nodeId, name);
                catalog.setGauge("jvm_mem_pool_peak_max_bytes", mp.getPeakMax().bytes(), node, nodeId, name);
                catalog.setGauge("jvm_mem_pool_used_bytes", mp.getUsed().bytes(), node, nodeId, name);
                catalog.setGauge("jvm_mem_pool_peak_used_bytes", mp.getPeakUsed().bytes(), node, nodeId, name);
            }

            catalog.setGauge("jvm_threads_number", jvm.getThreads().getCount(), node, nodeId);
            catalog.setGauge("jvm_threads_peak_number", jvm.getThreads().getPeakCount(), node, nodeId);

            for (JvmStats.GarbageCollector gc : jvm.getGc().getCollectors()) {
                String name = gc.getName();
                catalog.setCounter("jvm_gc_collection_count", gc.getCollectionCount(), node, nodeId, name);
                catalog.setCounter("jvm_gc_collection_time_seconds", gc.getCollectionTime().getSeconds(), node, nodeId, name);
            }

            for (JvmStats.BufferPool bp : jvm.getBufferPools()) {
                String name = bp.getName();
                catalog.setGauge("jvm_bufferpool_number", bp.getCount(), node, nodeId, name);
                catalog.setGauge("jvm_bufferpool_total_capacity_bytes", bp.getTotalCapacity().bytes(), node, nodeId, name);
                catalog.setGauge("jvm_bufferpool_used_bytes", bp.getUsed().bytes(), node, nodeId, name);
            }
            if (jvm.getClasses() != null) {
                catalog.setGauge("jvm_classes_loaded_number", jvm.getClasses().getLoadedClassCount(), node, nodeId);
                catalog.setGauge("jvm_classes_total_loaded_number", jvm.getClasses().getTotalLoadedClassCount(), node, nodeId);
                catalog.setGauge("jvm_classes_unloaded_number", jvm.getClasses().getUnloadedClassCount(), node, nodeId);
            }
        }
    }

    private void registerIndicesMetrics() {
        catalog.registerGauge("indices_doc_number", "Total number of documents", "node", "nodeId");
        catalog.registerGauge("indices_doc_deleted_number", "Number of deleted documents", "node", "nodeId");

        catalog.registerGauge("indices_store_size_bytes", "Store size of the indices in bytes", "node", "nodeId");
        catalog.registerCounter("indices_store_throttle_time_seconds", "Time spent while storing into indices when throttling", "node", "nodeId");

        catalog.registerCounter("indices_indexing_delete_count", "Count of documents deleted", "node", "nodeId");
        catalog.registerGauge("indices_indexing_delete_current_number", "Current rate of documents deleted", "node", "nodeId");
        catalog.registerCounter("indices_indexing_delete_time_seconds", "Time spent while deleting documents", "node", "nodeId");
        catalog.registerCounter("indices_indexing_index_count", "Count of documents indexed", "node", "nodeId");
        catalog.registerGauge("indices_indexing_index_current_number", "Current rate of documents indexed", "node", "nodeId");
        catalog.registerCounter("indices_indexing_index_failed_count", "Count of failed to index documents", "node", "nodeId");
        catalog.registerCounter("indices_indexing_index_time_seconds", "Time spent while indexing documents", "node", "nodeId");
        catalog.registerCounter("indices_indexing_noop_update_count", "Count of noop document updates", "node", "nodeId");
        catalog.registerGauge("indices_indexing_is_throttled_bool", "Is indexing throttling ?", "node", "nodeId");
        catalog.registerCounter("indices_indexing_throttle_time_seconds", "Time spent while throttling", "node", "nodeId");

        catalog.registerCounter("indices_get_count", "Count of get commands", "node", "nodeId");
        catalog.registerCounter("indices_get_time_seconds", "Time spent while get commands", "node", "nodeId");
        catalog.registerCounter("indices_get_exists_count", "Count of existing documents when get command", "node", "nodeId");
        catalog.registerCounter("indices_get_exists_time_seconds", "Time spent while existing documents get command", "node", "nodeId");
        catalog.registerCounter("indices_get_missing_count", "Count of missing documents when get command", "node", "nodeId");
        catalog.registerCounter("indices_get_missing_time_seconds", "Time spent while missing documents get command", "node", "nodeId");

        catalog.registerGauge("indices_search_open_contexts_number", "Number of search open contexts", "node", "nodeId");
        catalog.registerCounter("indices_search_fetch_count", "Count of search fetches", "node", "nodeId");
        catalog.registerGauge("indices_search_fetch_current_number", "Current rate of search fetches", "node", "nodeId");
        catalog.registerCounter("indices_search_fetch_time_seconds", "Time spent while search fetches", "node", "nodeId");
        catalog.registerCounter("indices_search_query_count", "Count of search queries", "node", "nodeId");
        catalog.registerGauge("indices_search_query_current_number", "Current rate of search queries", "node", "nodeId");
        catalog.registerCounter("indices_search_query_time_seconds", "Time spent while search queries", "node", "nodeId");
        catalog.registerCounter("indices_search_scroll_count", "Count of search scrolls", "node", "nodeId");
        catalog.registerGauge("indices_search_scroll_current_number", "Current rate of search scrolls", "node", "nodeId");
        catalog.registerCounter("indices_search_scroll_time_seconds", "Time spent while search scrolls", "node", "nodeId");


        catalog.registerGauge("indices_merges_current_number", "Current rate of merges", "node", "nodeId");
        catalog.registerGauge("indices_merges_current_docs_number", "Current rate of documents merged", "node", "nodeId");
        catalog.registerGauge("indices_merges_current_size_bytes", "Current rate of bytes merged", "node", "nodeId");
        catalog.registerCounter("indices_merges_total_number", "Count of merges", "node", "nodeId");
        catalog.registerCounter("indices_merges_total_time_seconds", "Time spent while merging", "node", "nodeId");
        catalog.registerCounter("indices_merges_total_docs_count", "Count of documents merged", "node", "nodeId");
        catalog.registerCounter("indices_merges_total_size_bytes", "Count of bytes of merged documents", "node", "nodeId");
        catalog.registerCounter("indices_merges_total_stopped_time_seconds", "Time spent while merge process stopped", "node", "nodeId");
        catalog.registerCounter("indices_merges_total_throttled_time_seconds", "Time spent while merging when throttling", "node", "nodeId");
        catalog.registerGauge("indices_merges_total_auto_throttle_bytes", "Bytes merged while throttling", "node", "nodeId");

        catalog.registerCounter("indices_refresh_total_count", "Count of refreshes", "node", "nodeId");
        catalog.registerCounter("indices_refresh_total_time_seconds", "Time spent while refreshes", "node", "nodeId");

        catalog.registerCounter("indices_flush_total_count", "Count of flushes", "node", "nodeId");
        catalog.registerCounter("indices_flush_total_time_seconds", "Total time spent while flushes", "node", "nodeId");

        catalog.registerCounter("indices_querycache_cache_count", "Count of queries in cache", "node", "nodeId");
        catalog.registerGauge("indices_querycache_cache_size_bytes", "Query cache size", "node", "nodeId");
        catalog.registerCounter("indices_querycache_evictions_count", "Count of evictions in query cache", "node", "nodeId");
        catalog.registerCounter("indices_querycache_hit_count", "Count of hits in query cache", "node", "nodeId");
        catalog.registerGauge("indices_querycache_memory_size_bytes", "Memory usage of query cache", "node", "nodeId");
        catalog.registerGauge("indices_querycache_miss_number", "Count of misses in query cache", "node", "nodeId");
        catalog.registerGauge("indices_querycache_total_number", "Count of usages of query cache", "node", "nodeId");

        catalog.registerGauge("indices_fielddata_memory_size_bytes", "Memory usage of field date cache", "node", "nodeId");
        catalog.registerCounter("indices_fielddata_evictions_count", "Count of evictions in field data cache", "node", "nodeId");

        catalog.registerCounter("indices_percolate_count", "Count of percolates", "node", "nodeId");
        catalog.registerGauge("indices_percolate_current_number", "Rate of percolates", "node", "nodeId");
        catalog.registerGauge("indices_percolate_memory_size_bytes", "Percolate memory size", "node", "nodeId");
        catalog.registerCounter("indices_percolate_queries_count", "Count of queries percolated", "node", "nodeId");
        catalog.registerCounter("indices_percolate_time_seconds", "Time spent while percolating", "node", "nodeId");

        catalog.registerGauge("indices_completion_size_bytes", "Size of completion suggest statistics", "node", "nodeId");

        catalog.registerGauge("indices_segments_number", "Current number of segments", "node", "nodeId");
        catalog.registerGauge("indices_segments_memory_bytes", "Memory used by segments", "node", "nodeId", "type");

        catalog.registerGauge("indices_suggest_current_number", "Current rate of suggests", "node", "nodeId");
        catalog.registerCounter("indices_suggest_count", "Count of suggests", "node", "nodeId");
        catalog.registerCounter("indices_suggest_time_seconds", "Time spent while making suggests", "node", "nodeId");

        catalog.registerGauge("indices_requestcache_memory_size_bytes", "Memory used for request cache", "node", "nodeId");
        catalog.registerCounter("indices_requestcache_hit_count", "Number of hits in request cache", "node", "nodeId");
        catalog.registerCounter("indices_requestcache_miss_count", "Number of misses in request cache", "node", "nodeId");
        catalog.registerCounter("indices_requestcache_evictions_count", "Number of evictions in request cache", "node", "nodeId");

        catalog.registerGauge("indices_recovery_current_number", "Current number of recoveries", "node", "nodeId", "type");
        catalog.registerCounter("indices_recovery_throttle_time_seconds", "Time spent while throttling recoveries", "node", "nodeId");
    }

    private void updateIndicesMetrics(NodeIndicesStats idx) {
        if (idx != null) {
            catalog.setGauge("indices_doc_number", idx.getDocs().getCount(), node, nodeId);
            catalog.setGauge("indices_doc_deleted_number", idx.getDocs().getDeleted(), node, nodeId);

            catalog.setGauge("indices_store_size_bytes", idx.getStore().getSizeInBytes(), node, nodeId);
            catalog.setCounter("indices_store_throttle_time_seconds", idx.getStore().getThrottleTime().millis() / 1000.0, node, nodeId);

            catalog.setCounter("indices_indexing_delete_count", idx.getIndexing().getTotal().getDeleteCount(), node, nodeId);
            catalog.setGauge("indices_indexing_delete_current_number", idx.getIndexing().getTotal().getDeleteCurrent(), node, nodeId);
            catalog.setCounter("indices_indexing_delete_time_seconds", idx.getIndexing().getTotal().getDeleteTimeInMillis() / 1000.0, node, nodeId);
            catalog.setCounter("indices_indexing_index_count", idx.getIndexing().getTotal().getIndexCount(), node, nodeId);
            catalog.setGauge("indices_indexing_index_current_number", idx.getIndexing().getTotal().getIndexCurrent(), node, nodeId);
            catalog.setCounter("indices_indexing_index_failed_count", idx.getIndexing().getTotal().getIndexFailedCount(), node, nodeId);
            catalog.setCounter("indices_indexing_index_time_seconds", idx.getIndexing().getTotal().getIndexTimeInMillis() / 1000.0, node, nodeId);
            catalog.setCounter("indices_indexing_noop_update_count", idx.getIndexing().getTotal().getNoopUpdateCount(), node, nodeId);
            catalog.setGauge("indices_indexing_is_throttled_bool", idx.getIndexing().getTotal().isThrottled() ? 1 : 0, node, nodeId);
            catalog.setCounter("indices_indexing_throttle_time_seconds", idx.getIndexing().getTotal().getThrottleTimeInMillis() / 1000.0, node, nodeId);

            catalog.setCounter("indices_get_count", idx.getGet().getCount(), node, nodeId);
            catalog.setCounter("indices_get_time_seconds", idx.getGet().getTimeInMillis() / 1000.0, node, nodeId);
            catalog.setCounter("indices_get_exists_count", idx.getGet().getExistsCount(), node, nodeId);
            catalog.setCounter("indices_get_exists_time_seconds", idx.getGet().getExistsTimeInMillis() / 1000.0, node, nodeId);
            catalog.setCounter("indices_get_missing_count", idx.getGet().getMissingCount(), node, nodeId);
            catalog.setCounter("indices_get_missing_time_seconds", idx.getGet().getMissingTimeInMillis() / 1000.0, node, nodeId);

            catalog.setGauge("indices_search_open_contexts_number", idx.getSearch().getOpenContexts(), node, nodeId);
            catalog.setCounter("indices_search_fetch_count", idx.getSearch().getTotal().getFetchCount(), node, nodeId);
            catalog.setGauge("indices_search_fetch_current_number", idx.getSearch().getTotal().getFetchCurrent(), node, nodeId);
            catalog.setCounter("indices_search_fetch_time_seconds", idx.getSearch().getTotal().getFetchTimeInMillis() / 1000.0, node, nodeId);
            catalog.setCounter("indices_search_query_count", idx.getSearch().getTotal().getQueryCount(), node, nodeId);
            catalog.setGauge("indices_search_query_current_number", idx.getSearch().getTotal().getQueryCurrent(), node, nodeId);
            catalog.setCounter("indices_search_query_time_seconds", idx.getSearch().getTotal().getQueryTimeInMillis() / 1000.0, node, nodeId);
            catalog.setCounter("indices_search_scroll_count", idx.getSearch().getTotal().getScrollCount(), node, nodeId);
            catalog.setGauge("indices_search_scroll_current_number", idx.getSearch().getTotal().getScrollCurrent(), node, nodeId);
            catalog.setCounter("indices_search_scroll_time_seconds", idx.getSearch().getTotal().getScrollTimeInMillis() / 1000.0, node, nodeId);

            catalog.setGauge("indices_merges_current_number", idx.getMerge().getCurrent(), node, nodeId);
            catalog.setGauge("indices_merges_current_docs_number", idx.getMerge().getCurrentNumDocs(), node, nodeId);
            catalog.setGauge("indices_merges_current_size_bytes", idx.getMerge().getCurrentSizeInBytes(), node, nodeId);
            catalog.setCounter("indices_merges_total_number", idx.getMerge().getTotal(), node, nodeId);
            catalog.setCounter("indices_merges_total_time_seconds", idx.getMerge().getTotalTimeInMillis() / 1000.0, node, nodeId);
            catalog.setCounter("indices_merges_total_docs_count", idx.getMerge().getTotalNumDocs(), node, nodeId);
            catalog.setCounter("indices_merges_total_size_bytes", idx.getMerge().getTotalSizeInBytes(), node, nodeId);
            catalog.setCounter("indices_merges_total_stopped_time_seconds", idx.getMerge().getTotalStoppedTimeInMillis() / 1000.0, node, nodeId);
            catalog.setCounter("indices_merges_total_throttled_time_seconds", idx.getMerge().getTotalThrottledTimeInMillis() / 1000.0, node, nodeId);
            catalog.setGauge("indices_merges_total_auto_throttle_bytes", idx.getMerge().getTotalBytesPerSecAutoThrottle(), node, nodeId);

            catalog.setCounter("indices_refresh_total_count", idx.getRefresh().getTotal(), node, nodeId);
            catalog.setCounter("indices_refresh_total_time_seconds", idx.getRefresh().getTotalTimeInMillis() / 1000.0, node, nodeId);

            catalog.setCounter("indices_flush_total_count", idx.getFlush().getTotal(), node, nodeId);
            catalog.setCounter("indices_flush_total_time_seconds", idx.getFlush().getTotalTimeInMillis() / 1000.0, node, nodeId);

            catalog.setCounter("indices_querycache_cache_count", idx.getQueryCache().getCacheCount(), node, nodeId);
            catalog.setGauge("indices_querycache_cache_size_bytes", idx.getQueryCache().getCacheSize(), node, nodeId);
            catalog.setCounter("indices_querycache_evictions_count", idx.getQueryCache().getEvictions(), node, nodeId);
            catalog.setCounter("indices_querycache_hit_count", idx.getQueryCache().getHitCount(), node, nodeId);
            catalog.setGauge("indices_querycache_memory_size_bytes", idx.getQueryCache().getMemorySizeInBytes(), node, nodeId);
            catalog.setGauge("indices_querycache_miss_number", idx.getQueryCache().getMissCount(), node, nodeId);
            catalog.setGauge("indices_querycache_total_number", idx.getQueryCache().getTotalCount(), node, nodeId);

            catalog.setGauge("indices_fielddata_memory_size_bytes", idx.getFieldData().getMemorySizeInBytes(), node, nodeId);
            catalog.setCounter("indices_fielddata_evictions_count", idx.getFieldData().getEvictions(), node, nodeId);

            catalog.setCounter("indices_percolate_count", idx.getPercolate().getCount(), node, nodeId);
            catalog.setGauge("indices_percolate_current_number", idx.getPercolate().getCurrent(), node, nodeId);
            catalog.setGauge("indices_percolate_memory_size_bytes", idx.getPercolate().getMemorySizeInBytes(), node, nodeId);
            catalog.setCounter("indices_percolate_queries_count", idx.getPercolate().getNumQueries(), node, nodeId);
            catalog.setCounter("indices_percolate_time_seconds", idx.getPercolate().getTimeInMillis() / 1000.0, node, nodeId);

            catalog.setGauge("indices_completion_size_bytes", idx.getCompletion().getSizeInBytes(), node, nodeId);

            catalog.setGauge("indices_segments_number", idx.getSegments().getCount(), node, nodeId);
            catalog.setGauge("indices_segments_memory_bytes", idx.getSegments().getMemoryInBytes(), node, nodeId, "all");
            catalog.setGauge("indices_segments_memory_bytes", idx.getSegments().getBitsetMemoryInBytes(), node, nodeId, "bitset");
            catalog.setGauge("indices_segments_memory_bytes", idx.getSegments().getDocValuesMemoryInBytes(), node, nodeId, "docvalues");
            catalog.setGauge("indices_segments_memory_bytes", idx.getSegments().getIndexWriterMaxMemoryInBytes(), node, nodeId, "indexwriter_max");
            catalog.setGauge("indices_segments_memory_bytes", idx.getSegments().getIndexWriterMemoryInBytes(), node, nodeId, "indexwriter");
            catalog.setGauge("indices_segments_memory_bytes", idx.getSegments().getNormsMemoryInBytes(), node, nodeId, "norms");
            catalog.setGauge("indices_segments_memory_bytes", idx.getSegments().getStoredFieldsMemoryInBytes(), node, nodeId, "storefields");
            catalog.setGauge("indices_segments_memory_bytes", idx.getSegments().getTermsMemoryInBytes(), node, nodeId, "terms");
            catalog.setGauge("indices_segments_memory_bytes", idx.getSegments().getTermVectorsMemoryInBytes(), node, nodeId, "termvectors");
            catalog.setGauge("indices_segments_memory_bytes", idx.getSegments().getVersionMapMemoryInBytes(), node, nodeId, "versionmap");

            catalog.setGauge("indices_suggest_current_number", idx.getSuggest().getCurrent(), node, nodeId);
            catalog.setCounter("indices_suggest_count", idx.getSuggest().getCount(), node, nodeId);
            catalog.setCounter("indices_suggest_time_seconds", idx.getSuggest().getTimeInMillis() / 1000.0, node, nodeId);

            catalog.setGauge("indices_requestcache_memory_size_bytes", idx.getRequestCache().getMemorySizeInBytes(), node, nodeId);
            catalog.setCounter("indices_requestcache_hit_count", idx.getRequestCache().getHitCount(), node, nodeId);
            catalog.setCounter("indices_requestcache_miss_count", idx.getRequestCache().getMissCount(), node, nodeId);
            catalog.setCounter("indices_requestcache_evictions_count", idx.getRequestCache().getEvictions(), node, nodeId);

            catalog.setGauge("indices_recovery_current_number", idx.getRecoveryStats().currentAsSource(), node, nodeId, "source");
            catalog.setGauge("indices_recovery_current_number", idx.getRecoveryStats().currentAsTarget(), node, nodeId, "target");
            catalog.setCounter("indices_recovery_throttle_time_seconds", idx.getRecoveryStats().throttleTime().getSeconds(), node, nodeId);
        }
    }

    private void registerTransportMetrics() {
        catalog.registerGauge("transport_server_open_number", "Opened server connections", "node", "nodeId");
        catalog.registerCounter("transport_rx_packets_count", "Received packets", "node", "nodeId");
        catalog.registerCounter("transport_tx_packets_count", "Sent packets", "node", "nodeId");
        catalog.registerCounter("transport_rx_bytes_count", "Bytes received", "node", "nodeId");
        catalog.registerCounter("transport_tx_bytes_count", "Bytes sent", "node", "nodeId");
    }

    private void updateTransportMetrics(TransportStats ts) {
        if (ts != null) {
            catalog.setGauge("transport_server_open_number", ts.getServerOpen(), node, nodeId);
            catalog.setCounter("transport_rx_packets_count", ts.getRxCount(), node, nodeId);
            catalog.setCounter("transport_tx_packets_count", ts.getTxCount(), node, nodeId);
            catalog.setCounter("transport_rx_bytes_count", ts.getRxSize().bytes(), node, nodeId);
            catalog.setCounter("transport_tx_bytes_count", ts.getTxSize().bytes(), node, nodeId);
        }
    }

    private void registerHTTPMetrics() {
        catalog.registerGauge("http_open_server_number", "Number of open server connections", "node", "nodeId");
        catalog.registerCounter("http_open_total_count", "Count of opened connections", "node", "nodeId");
    }

    private void updateHTTPMetrics(HttpStats http) {
        if (http != null) {
            catalog.setGauge("http_open_server_number", http.getServerOpen(), node, nodeId);
            catalog.setCounter("http_open_total_count", http.getTotalOpen(), node, nodeId);
        }
    }

    private void registerScriptMetrics() {
        catalog.registerCounter("script_cache_evictions_count", "Number of evictions in scripts cache", "node", "nodeId");
        catalog.registerCounter("script_compilations_count", "Number of scripts compilations", "node", "nodeId");
    }

    private void updateScriptMetrics(ScriptStats sc) {
        if (sc != null) {
            catalog.setCounter("script_cache_evictions_count", sc.getCacheEvictions(), node, nodeId);
            catalog.setCounter("script_compilations_count", sc.getCompilations(), node, nodeId);
        }
    }

    private void registerProcessMetrics() {
        catalog.registerGauge("process_cpu_percent", "CPU percentage used by ES process", "node", "nodeId");
        catalog.registerGauge("process_cpu_time_seconds", "CPU time used by ES process", "node", "nodeId");
        catalog.registerGauge("process_mem_total_virtual_bytes", "Memory used by ES process", "node", "nodeId");
        catalog.registerGauge("process_file_descriptors_open_number", "Open file descriptors", "node", "nodeId");
        catalog.registerGauge("process_file_descriptors_max_number", "Max file descriptors", "node", "nodeId");
    }

    private void updateProcessMetrics(ProcessStats ps) {
        if (ps != null) {
            catalog.setGauge("process_cpu_percent", ps.getCpu().getPercent(), node, nodeId);
            catalog.setGauge("process_cpu_time_seconds", ps.getCpu().getTotal().getSeconds(), node, nodeId);
            catalog.setGauge("process_mem_total_virtual_bytes", ps.getMem().getTotalVirtual().bytes(), node, nodeId);
            catalog.setGauge("process_file_descriptors_open_number", ps.getOpenFileDescriptors(), node, nodeId);
            catalog.setGauge("process_file_descriptors_max_number", ps.getMaxFileDescriptors(), node, nodeId);
        }
    }

    private void registerOsMetrics() {
        catalog.registerGauge("os_cpu_percent", "CPU usage in percent", "node", "nodeId");
        catalog.registerGauge("os_load_average", "CPU load", "node", "nodeId");
        catalog.registerGauge("os_mem_free_bytes", "Memory free", "node", "nodeId");
        catalog.registerGauge("os_mem_free_percent", "Memory free in percent", "node", "nodeId");
        catalog.registerGauge("os_mem_used_bytes", "Memory used", "node", "nodeId");
        catalog.registerGauge("os_mem_used_percent", "Memory used in percent", "node", "nodeId");
        catalog.registerGauge("os_mem_total_bytes", "Total memory size", "node", "nodeId");
        catalog.registerGauge("os_swap_free_bytes", "Swap free", "node", "nodeId");
        catalog.registerGauge("os_swap_used_bytes", "Swap used", "node", "nodeId");
        catalog.registerGauge("os_swap_total_bytes", "Total swap size", "node", "nodeId");
    }

    private void updateOsMetrics(OsStats os) {
        if (os != null) {
            catalog.setGauge("os_cpu_percent", os.getCpuPercent(), node, nodeId);
            catalog.setGauge("os_load_average", os.getLoadAverage(), node, nodeId);
            catalog.setGauge("os_mem_free_bytes", os.getMem().getFree().bytes(), node, nodeId);
            catalog.setGauge("os_mem_free_percent", os.getMem().getFreePercent(), node, nodeId);
            catalog.setGauge("os_mem_used_bytes", os.getMem().getUsed().bytes(), node, nodeId);
            catalog.setGauge("os_mem_used_percent", os.getMem().getUsedPercent(), node, nodeId);
            catalog.setGauge("os_mem_total_bytes", os.getMem().getTotal().bytes(), node, nodeId);
            catalog.setGauge("os_swap_free_bytes", os.getSwap().getFree().bytes(), node, nodeId);
            catalog.setGauge("os_swap_used_bytes", os.getSwap().getUsed().bytes(), node, nodeId);
            catalog.setGauge("os_swap_total_bytes", os.getSwap().getTotal().bytes(), node, nodeId);
        }
    }

    private void registerCircuitBreakerMetrics() {
        catalog.registerGauge("circuitbreaker_estimated_bytes", "Circuit breaker estimated size", "node", "nodeId", "name");
        catalog.registerGauge("circuitbreaker_limit_bytes", "Circuit breaker size limit", "node", "nodeId", "name");
        catalog.registerGauge("circuitbreaker_overhead_ratio", "Circuit breaker overhead ratio", "node", "nodeId", "name");
        catalog.registerCounter("circuitbreaker_tripped_count", "Circuit breaker tripped count", "node", "nodeId", "name");
    }

    private void updateCircuitBreakersMetrics(AllCircuitBreakerStats acbs) {
        if (acbs != null) {
            for (CircuitBreakerStats cbs : acbs.getAllStats()) {
                String name = cbs.getName();
                catalog.setGauge("circuitbreaker_estimated_bytes", cbs.getEstimated(), node, nodeId, name);
                catalog.setGauge("circuitbreaker_limit_bytes", cbs.getLimit(), node, nodeId, name);
                catalog.setGauge("circuitbreaker_overhead_ratio", cbs.getOverhead(), node, nodeId, name);
                catalog.setCounter("circuitbreaker_tripped_count", cbs.getTrippedCount(), node, nodeId, name);
            }
        }
    }

    private void registerThreadPoolMetrics() {
        catalog.registerGauge("threadpool_threads_number", "Number of threads in thread pool", "node", "nodeId", "name", "type");
        catalog.registerCounter("threadpool_threads_count", "Count of threads in thread pool", "node", "nodeId", "name", "type");
        catalog.registerGauge("threadpool_tasks_number", "Number of tasks in thread pool", "node", "nodeId", "name", "type");
    }

    private void updateThreadPoolMetrics(ThreadPoolStats tps) {
        if (tps != null) {
            for (ThreadPoolStats.Stats st : tps) {
                String name = st.getName();
                catalog.setGauge("threadpool_threads_number", st.getThreads(), node, nodeId, name, "threads");
                catalog.setGauge("threadpool_threads_number", st.getActive(), node, nodeId, name, "active");
                catalog.setGauge("threadpool_threads_number", st.getLargest(), node, nodeId, name, "largest");
                catalog.setCounter("threadpool_threads_count", st.getCompleted(), node, nodeId, name, "completed");
                catalog.setCounter("threadpool_threads_count", st.getRejected(), node, nodeId, name, "rejected");
                catalog.setGauge("threadpool_tasks_number", st.getQueue(), node, nodeId, name, "queue");
            }
        }
    }

    private void registerFsMetrics() {
        catalog.registerGauge("fs_total_total_bytes", "Total disk space for all mount points", "node", "nodeId");
        catalog.registerGauge("fs_total_available_bytes", "Available disk space for all mount points", "node", "nodeId");
        catalog.registerGauge("fs_total_free_bytes", "Free disk space for all mountpoints", "node", "nodeId");
        catalog.registerGauge("fs_total_is_spinning_bool", "Is it a spinning disk ?", "node", "nodeId");

        catalog.registerGauge("fs_path_total_bytes", "Total disk space", "node", "nodeId", "path", "mount", "type");
        catalog.registerGauge("fs_path_available_bytes", "Available disk space", "node", "nodeId", "path", "mount", "type");
        catalog.registerGauge("fs_path_free_bytes", "Free disk space", "node", "nodeId", "path", "mount", "type");
        catalog.registerGauge("fs_path_is_spinning_bool", "Is it a spinning disk ?", "node", "nodeId", "path", "mount", "type");
    }

    private void updateFsMetrics(FsInfo fs) {
        if (fs != null) {
            catalog.setGauge("fs_total_total_bytes", fs.getTotal().getTotal().bytes(), node, nodeId);
            catalog.setGauge("fs_total_available_bytes", fs.getTotal().getAvailable().bytes(), node, nodeId);
            catalog.setGauge("fs_total_free_bytes", fs.getTotal().getFree().bytes(), node, nodeId);
            if (fs.getTotal() != null && fs.getTotal().getSpins() != null)
                catalog.setGauge("fs_total_is_spinning_bool", fs.getTotal().getSpins() ? 1 : 0, node, nodeId);

            for (FsInfo.Path fspath : fs) {
                String path = fspath.getPath();
                String mount = fspath.getMount();
                String type = fspath.getType();
                catalog.setGauge("fs_path_total_bytes", fspath.getTotal().bytes(), node, nodeId, path, mount, type);
                catalog.setGauge("fs_path_available_bytes", fspath.getAvailable().bytes(), node, nodeId, path, mount, type);
                catalog.setGauge("fs_path_free_bytes", fspath.getFree().bytes(), node, nodeId, path, mount, type);
                if (fspath.getSpins() != null)
                    catalog.setGauge("fs_path_is_spinning_bool", fspath.getSpins() ? 1 : 0, node, nodeId, path, mount, type);
            }
        }
    }

    public void updateMetrics(ClusterHealthResponse clusterHealthResponse, NodeStats nodeStats) {
        Summary.Timer timer = catalog.startSummaryTimer("metrics_generate_time_seconds", node, nodeId);

        updateClusterMetrics(clusterHealthResponse);

        updateJVMMetrics(nodeStats.getJvm());
        updateIndicesMetrics(nodeStats.getIndices());
        updateTransportMetrics(nodeStats.getTransport());
        updateHTTPMetrics(nodeStats.getHttp());
        updateScriptMetrics(nodeStats.getScriptStats());
        updateProcessMetrics(nodeStats.getProcess());
        updateOsMetrics(nodeStats.getOs());
        updateCircuitBreakersMetrics(nodeStats.getBreaker());
        updateThreadPoolMetrics(nodeStats.getThreadPool());
        updateFsMetrics(nodeStats.getFs());

        timer.observeDuration();
    }

    public PrometheusMetricsCatalog getCatalog() {
        return catalog;
    }
}
