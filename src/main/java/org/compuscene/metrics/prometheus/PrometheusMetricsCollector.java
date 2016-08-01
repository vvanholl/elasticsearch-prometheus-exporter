package org.compuscene.metrics.prometheus;

import org.elasticsearch.action.admin.cluster.health.ClusterHealthRequest;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.admin.cluster.node.stats.NodeStats;
import org.elasticsearch.action.admin.cluster.node.stats.NodesStatsRequest;
import org.elasticsearch.action.admin.cluster.node.stats.NodesStatsResponse;
import org.elasticsearch.client.Client;
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

    private final Client client;
    private PrometheusMetricsCatalog catalog;


    public PrometheusMetricsCollector(final Client client) {
        this.client = client;

        NodesStatsRequest nodesStatsRequest = new NodesStatsRequest().all();
        NodesStatsResponse nodesStatsResponse = this.client.admin().cluster().nodesStats(nodesStatsRequest).actionGet();

        String cluster = nodesStatsResponse.getClusterNameAsString();

        this.catalog = new PrometheusMetricsCatalog(cluster, "es_");

        this.registerClusterMetrics();
        this.registerJVMMetrics();
        this.registerIndicesMetrics();
        this.registerTransportMetrics();
        this.registerHTTPMetrics();
        this.registerScriptMetrics();
        this.registerProcessMetrics();
        this.registerOsMetrics();
        this.registerCircuitBreakerMetrics();
        this.registerThreadPoolMetrics();
        this.registerFsMetrics();
    }

    private void registerClusterMetrics() {
        this.catalog.registerGauge("cluster_status", "Cluster status");
        this.catalog.registerGauge("cluster_nodes_number", "Number of nodes in the cluster");
        this.catalog.registerGauge("cluster_datanodes_number", "Number of data nodes in the cluster");
        this.catalog.registerGauge("cluster_shards_active_percent", "Percent of active shards");
        this.catalog.registerGauge("cluster_shards_number", "Number of shards", "type");
        this.catalog.registerGauge("cluster_pending_tasks_number", "Number of pending tasks");
        this.catalog.registerGauge("cluster_task_max_waiting_time_seconds", "Max waiting time in seconds for tasks");
        this.catalog.registerGauge("cluster_is_timedout_bool", "Is the cluster in timed out status ?");
        this.catalog.registerGauge("cluster_inflight_fetch_number", "Number of in flight fetches");
    }

    private void updateClusterMetrics(ClusterHealthResponse res) {
        this.catalog.setGauge("cluster_status", res.getStatus().value());

        this.catalog.setGauge("cluster_nodes_number", res.getNumberOfNodes());
        this.catalog.setGauge("cluster_datanodes_number", res.getNumberOfDataNodes());

        this.catalog.setGauge("cluster_shards_active_percent", res.getActiveShardsPercent());

        this.catalog.setGauge("cluster_shards_number", res.getActiveShards(), "active");
        this.catalog.setGauge("cluster_shards_number", res.getActivePrimaryShards(), "active_primary");
        this.catalog.setGauge("cluster_shards_number", res.getDelayedUnassignedShards(), "unassigned");
        this.catalog.setGauge("cluster_shards_number", res.getInitializingShards(), "initializing");
        this.catalog.setGauge("cluster_shards_number", res.getRelocatingShards(), "relocating");
        this.catalog.setGauge("cluster_shards_number", res.getUnassignedShards(), "unassigned");

        this.catalog.setGauge("cluster_pending_tasks_number", res.getNumberOfPendingTasks());
        this.catalog.setGauge("cluster_task_max_waiting_time_seconds", res.getTaskMaxWaitingTime().getSeconds());

        this.catalog.setGauge("cluster_is_timedout_bool", res.isTimedOut() ? 1 : 0);

        this.catalog.setGauge("cluster_inflight_fetch_number", res.getNumberOfInFlightFetch());
    }

    private void registerJVMMetrics() {
        this.catalog.registerGauge("jvm_uptime_seconds", "JVM uptime", "node");
        this.catalog.registerGauge("jvm_mem_heap_max_bytes", "Maximum used memory in heap", "node");
        this.catalog.registerGauge("jvm_mem_heap_used_bytes", "Memory used in heap", "node");
        this.catalog.registerGauge("jvm_mem_heap_used_percent", "Percentage of memory used in heap", "node");
        this.catalog.registerGauge("jvm_mem_nonheap_used_bytes", "Memory used apart from heap", "node");
        this.catalog.registerGauge("jvm_mem_heap_committed_bytes", "Committed bytes in heap", "node");
        this.catalog.registerGauge("jvm_mem_nonheap_committed_bytes", "Committed bytes apart from heap", "node");

        this.catalog.registerGauge("jvm_mem_pool_max_bytes", "Maximum usage of memory pool", "node", "pool");
        this.catalog.registerGauge("jvm_mem_pool_peak_max_bytes", "Maximum usage peak of memory pool", "node", "pool");
        this.catalog.registerGauge("jvm_mem_pool_used_bytes", "Used memory in memory pool", "node", "pool");
        this.catalog.registerGauge("jvm_mem_pool_peak_used_bytes", "Used memory peak in memory pool", "node", "pool");

        this.catalog.registerGauge("jvm_threads_number", "Number of threads", "node");
        this.catalog.registerGauge("jvm_threads_peak_number" , "Peak number of threads", "node");

        this.catalog.registerGauge("jvm_gc_collection_count", "Count of GC collections", "node", "gc");
        this.catalog.registerGauge("jvm_gc_collection_time_seconds", "Time spent for GC collections", "node", "gc");

        this.catalog.registerGauge("jvm_bufferpool_number", "Number of buffer pools", "node", "bufferpool");
        this.catalog.registerGauge("jvm_bufferpool_total_capacity_bytes", "Total capacity provided by buffer pools", "node", "bufferpool");
        this.catalog.registerGauge("jvm_bufferpool_used_bytes", "Used memory in buffer pools", "node", "bufferpool");

        this.catalog.registerGauge("jvm_classes_loaded_count", "Count of loaded classes", "node");
        this.catalog.registerGauge("jvm_classes_total_loaded_count", "Total count of loaded classes", "node");
        this.catalog.registerGauge("jvm_classes_unloaded_count", "Count of unloaded classes", "node");
    }

    private void updateJVMMetrics(String node, JvmStats jvm) {
        if (jvm != null) {
            this.catalog.setGauge("jvm_uptime_seconds", jvm.getUptime().getSeconds(), node);

            this.catalog.setGauge("jvm_mem_heap_max_bytes", jvm.getMem().getHeapMax().bytes(), node);
            this.catalog.setGauge("jvm_mem_heap_used_bytes", jvm.getMem().getHeapUsed().bytes(), node);
            this.catalog.setGauge("jvm_mem_heap_used_percent", jvm.getMem().getHeapUsedPercent(), node);
            this.catalog.setGauge("jvm_mem_nonheap_used_bytes", jvm.getMem().getNonHeapUsed().bytes(), node);
            this.catalog.setGauge("jvm_mem_heap_committed_bytes", jvm.getMem().getHeapCommitted().bytes(), node);
            this.catalog.setGauge("jvm_mem_nonheap_committed_bytes", jvm.getMem().getNonHeapCommitted().bytes(), node);

            for (JvmStats.MemoryPool mp : jvm.getMem()) {
                String name = mp.getName();
                this.catalog.setGauge("jvm_mem_pool_max_bytes", mp.getMax().bytes(), node, name);
                this.catalog.setGauge("jvm_mem_pool_peak_max_bytes", mp.getPeakMax().bytes(), node, name);
                this.catalog.setGauge("jvm_mem_pool_used_bytes", mp.getUsed().bytes(), node, name);
                this.catalog.setGauge("jvm_mem_pool_peak_used_bytes", mp.getPeakUsed().bytes(), node, name);
            }

            this.catalog.setGauge("jvm_threads_number", jvm.getThreads().getCount(), node);
            this.catalog.setGauge("jvm_threads_peak_number", jvm.getThreads().getPeakCount(), node);

            for (JvmStats.GarbageCollector gc : jvm.getGc().getCollectors()) {
                String name = gc.getName();
                this.catalog.setGauge("jvm_gc_collection_count", gc.getCollectionCount(), node, name);
                this.catalog.setGauge("jvm_gc_collection_time_seconds", gc.getCollectionTime().getSeconds(), node, name);
            }

            for (JvmStats.BufferPool bp : jvm.getBufferPools()) {
                String name = bp.getName();
                this.catalog.setGauge("jvm_bufferpool_number", bp.getCount(), node, name);
                this.catalog.setGauge("jvm_bufferpool_total_capacity_bytes", bp.getTotalCapacity().bytes(), node, name);
                this.catalog.setGauge("jvm_bufferpool_used_bytes", bp.getUsed().bytes(), node, name);
            }
            if (jvm.getClasses() != null) {
                this.catalog.setGauge("jvm_classes_loaded_count", jvm.getClasses().getLoadedClassCount(), node);
                this.catalog.setGauge("jvm_classes_total_loaded_count", jvm.getClasses().getTotalLoadedClassCount(), node);
                this.catalog.setGauge("jvm_classes_unloaded_count", jvm.getClasses().getUnloadedClassCount(), node);
            }
        }
    }

    private void registerIndicesMetrics() {
        this.catalog.registerGauge("indices_doc_count", "Total number of documents", "node");
        this.catalog.registerGauge("indices_doc_deleted_count", "Number of deleted documents", "node");

        this.catalog.registerGauge("indices_store_size_bytes", "No Help provided for the moment", "node");
        this.catalog.registerGauge("indices_store_throttle_time_seconds", "No Help provided for the moment", "node");

        this.catalog.registerGauge("indices_indexing_delete_count", "No Help provided for the moment", "node");
        this.catalog.registerGauge("indices_indexing_delete_current_number", "No Help provided for the moment", "node");
        this.catalog.registerGauge("indices_indexing_delete_time_seconds", "No Help provided for the moment", "node");
        this.catalog.registerGauge("indices_indexing_index_count", "No Help provided for the moment", "node");
        this.catalog.registerGauge("indices_indexing_index_current_number", "No Help provided for the moment", "node");
        this.catalog.registerGauge("indices_indexing_index_failed_count", "No Help provided for the moment", "node");
        this.catalog.registerGauge("indices_indexing_index_time_seconds", "No Help provided for the moment", "node");
        this.catalog.registerGauge("indices_indexing_noop_update_count", "No Help provided for the moment", "node");
        this.catalog.registerGauge("indices_indexing_is_throttled_bool", "No Help provided for the moment", "node");
        this.catalog.registerGauge("indices_indexing_throttle_time_seconds", "No Help provided for the moment", "node");

        this.catalog.registerGauge("indices_get_count", "Number of get", "node");
        this.catalog.registerGauge("indices_get_time_seconds", "No Help provided for the moment", "node");
        this.catalog.registerGauge("indices_get_exists_count", "No Help provided for the moment", "node");
        this.catalog.registerGauge("indices_get_exists_time_seconds", "No Help provided for the moment", "node");
        this.catalog.registerGauge("indices_get_missing_count", "No Help provided for the moment", "node");
        this.catalog.registerGauge("indices_get_missing_time_seconds", "No Help provided for the moment", "node");

        this.catalog.registerGauge("indices_search_open_contexts_number", "No Help provided for the moment", "node");
        this.catalog.registerGauge("indices_search_fetch_count", "No Help provided for the moment", "node");
        this.catalog.registerGauge("indices_search_fetch_current_number", "No Help provided for the moment", "node");
        this.catalog.registerGauge("indices_search_fetch_time_seconds", "No Help provided for the moment", "node");
        this.catalog.registerGauge("indices_search_query_count", "No Help provided for the moment", "node");
        this.catalog.registerGauge("indices_search_query_current_number", "No Help provided for the moment", "node");
        this.catalog.registerGauge("indices_search_query_time_seconds", "No Help provided for the moment", "node");
        this.catalog.registerGauge("indices_search_scroll_count", "No Help provided for the moment", "node");
        this.catalog.registerGauge("indices_search_scroll_current_number", "No Help provided for the moment", "node");
        this.catalog.registerGauge("indices_search_scroll_time_seconds", "No Help provided for the moment", "node");

        this.catalog.registerGauge("indices_merges_current_number", "No Help provided for the moment", "node");
        this.catalog.registerGauge("indices_merges_current_docs_number", "No Help provided for the moment", "node");
        this.catalog.registerGauge("indices_merges_current_size_bytes", "No Help provided for the moment", "node");
        this.catalog.registerGauge("indices_merges_total_number", "No Help provided for the moment", "node");
        this.catalog.registerGauge("indices_merges_total_time_seconds", "No Help provided for the moment", "node");
        this.catalog.registerGauge("indices_merges_total_docs_count", "No Help provided for the moment", "node");
        this.catalog.registerGauge("indices_merges_total_size_bytes", "No Help provided for the moment", "node");
        this.catalog.registerGauge("indices_merges_total_stopped_time_seconds", "No Help provided for the moment", "node");
        this.catalog.registerGauge("indices_merges_total_throttled_time_seconds", "No Help provided for the moment", "node");
        this.catalog.registerGauge("indices_merges_total_auto_throttle_bytes", "No Help provided for the moment", "node");

        this.catalog.registerGauge("indices_refresh_total_count", "Total number of refreshes", "node");
        this.catalog.registerGauge("indices_refresh_total_time_seconds", "Total time spent for refreshes", "node");

        this.catalog.registerGauge("indices_flush_total_count", "Total number of flushes", "node");
        this.catalog.registerGauge("indices_flush_total_time_seconds", "Total time spent for flushes", "node");

        this.catalog.registerGauge("indices_querycache_cache_count", "No Help provided for the moment", "node");
        this.catalog.registerGauge("indices_querycache_cache_size_bytes", "No Help provided for the moment", "node");
        this.catalog.registerGauge("indices_querycache_evictions_count", "No Help provided for the moment", "node");
        this.catalog.registerGauge("indices_querycache_hit_count", "No Help provided for the moment", "node");
        this.catalog.registerGauge("indices_querycache_memory_size_bytes", "No Help provided for the moment", "node");
        this.catalog.registerGauge("indices_querycache_miss_count", "No Help provided for the moment", "node");
        this.catalog.registerGauge("indices_querycache_total_count", "No Help provided for the moment", "node");

        this.catalog.registerGauge("indices_fielddata_memory_size_bytes", "No Help provided for the moment", "node");
        this.catalog.registerGauge("indices_fielddata_evictions_count", "No Help provided for the moment", "node");

        this.catalog.registerGauge("indices_percolate_count", "No Help provided for the moment", "node");
        this.catalog.registerGauge("indices_percolate_current_number", "No Help provided for the moment", "node");
        this.catalog.registerGauge("indices_percolate_memory_size_bytes", "No Help provided for the moment", "node");
        this.catalog.registerGauge("indices_percolate_queries_count", "No Help provided for the moment", "node");
        this.catalog.registerGauge("indices_percolate_time_seconds", "No Help provided for the moment", "node");

        this.catalog.registerGauge("indices_completion_size_bytes", "No Help provided for the moment", "node");

        this.catalog.registerGauge("indices_segments_count", "No Help provided for the moment", "node");
        this.catalog.registerGauge("indices_segments_memory_bytes", "No Help provided for the moment", "node","type");

        this.catalog.registerGauge("indices_suggest_current_number", "No Help provided for the moment", "node");
        this.catalog.registerGauge("indices_suggest_count", "No Help provided for the moment", "node");
        this.catalog.registerGauge("indices_suggest_time_seconds", "No Help provided for the moment", "node");

        this.catalog.registerGauge("indices_requestcache_memory_size_bytes", "Memory used for request cache", "node");
        this.catalog.registerGauge("indices_requestcache_hit_count", "Number of hits in request cache", "node");
        this.catalog.registerGauge("indices_requestcache_miss_count", "Number of misses in request cache", "node");
        this.catalog.registerGauge("indices_requestcache_evictions_count", "Number of evictions in request cache", "node");

        this.catalog.registerGauge("indices_recovery_current_number", "Current number of recoveries", "node","type");
        this.catalog.registerGauge("indices_recovery_throttle_time_seconds", "No Help provided for the moment", "node");
    }

    private void updateIndicesMetrics(String node, NodeIndicesStats idx) {
        if (idx != null) {
            this.catalog.setGauge("indices_doc_count", idx.getDocs().getCount(), node);
            this.catalog.setGauge("indices_doc_deleted_count", idx.getDocs().getDeleted(), node);

            this.catalog.setGauge("indices_store_size_bytes", idx.getStore().getSizeInBytes(), node);
            this.catalog.setGauge("indices_store_throttle_time_seconds", idx.getStore().getThrottleTime().millis()/1000.0, node);

            this.catalog.setGauge("indices_indexing_delete_count", idx.getIndexing().getTotal().getDeleteCount(), node);
            this.catalog.setGauge("indices_indexing_delete_current_number", idx.getIndexing().getTotal().getDeleteCurrent(), node);
            this.catalog.setGauge("indices_indexing_delete_time_seconds", idx.getIndexing().getTotal().getDeleteTimeInMillis()/1000.0, node);
            this.catalog.setGauge("indices_indexing_index_count", idx.getIndexing().getTotal().getIndexCount(), node);
            this.catalog.setGauge("indices_indexing_index_current_number", idx.getIndexing().getTotal().getIndexCurrent(), node);
            this.catalog.setGauge("indices_indexing_index_failed_count", idx.getIndexing().getTotal().getIndexFailedCount(), node);
            this.catalog.setGauge("indices_indexing_index_time_seconds", idx.getIndexing().getTotal().getIndexTimeInMillis()/1000.0, node);
            this.catalog.setGauge("indices_indexing_noop_update_count", idx.getIndexing().getTotal().getNoopUpdateCount(), node);
            this.catalog.setGauge("indices_indexing_is_throttled_bool", idx.getIndexing().getTotal().isThrottled() ? 1 : 0, node);
            this.catalog.setGauge("indices_indexing_throttle_time_seconds", idx.getIndexing().getTotal().getThrottleTimeInMillis()/1000.0, node);

            this.catalog.setGauge("indices_get_count", idx.getGet().getCount(), node);
            this.catalog.setGauge("indices_get_time_seconds", idx.getGet().getTimeInMillis()/1000.0, node);
            this.catalog.setGauge("indices_get_exists_count", idx.getGet().getExistsCount(), node);
            this.catalog.setGauge("indices_get_exists_time_seconds", idx.getGet().getExistsTimeInMillis()/1000.0, node);
            this.catalog.setGauge("indices_get_missing_count", idx.getGet().getMissingCount(), node);
            this.catalog.setGauge("indices_get_missing_time_seconds", idx.getGet().getMissingTimeInMillis()/1000.0, node);

            this.catalog.setGauge("indices_search_open_contexts_number", idx.getSearch().getOpenContexts(), node);
            this.catalog.setGauge("indices_search_fetch_count", idx.getSearch().getTotal().getFetchCount(), node);
            this.catalog.setGauge("indices_search_fetch_current_number", idx.getSearch().getTotal().getFetchCurrent(), node);
            this.catalog.setGauge("indices_search_fetch_time_seconds", idx.getSearch().getTotal().getFetchTimeInMillis()/1000.0, node);
            this.catalog.setGauge("indices_search_query_count", idx.getSearch().getTotal().getQueryCount(), node);
            this.catalog.setGauge("indices_search_query_current_number", idx.getSearch().getTotal().getQueryCurrent(), node);
            this.catalog.setGauge("indices_search_query_time_seconds", idx.getSearch().getTotal().getQueryTimeInMillis()/1000.0, node);
            this.catalog.setGauge("indices_search_scroll_count", idx.getSearch().getTotal().getScrollCount(), node);
            this.catalog.setGauge("indices_search_scroll_current_number", idx.getSearch().getTotal().getScrollCurrent(), node);
            this.catalog.setGauge("indices_search_scroll_time_seconds", idx.getSearch().getTotal().getScrollTimeInMillis()/1000.0, node);

            this.catalog.setGauge("indices_merges_current_number", idx.getMerge().getCurrent(), node);
            this.catalog.setGauge("indices_merges_current_docs_number", idx.getMerge().getCurrentNumDocs(), node);
            this.catalog.setGauge("indices_merges_current_size_bytes", idx.getMerge().getCurrentSizeInBytes(), node);
            this.catalog.setGauge("indices_merges_total_number", idx.getMerge().getTotal(), node);
            this.catalog.setGauge("indices_merges_total_time_seconds", idx.getMerge().getTotalTimeInMillis()/1000.0, node);
            this.catalog.setGauge("indices_merges_total_docs_count", idx.getMerge().getTotalNumDocs(), node);
            this.catalog.setGauge("indices_merges_total_size_bytes", idx.getMerge().getTotalSizeInBytes(), node);
            this.catalog.setGauge("indices_merges_total_stopped_time_seconds", idx.getMerge().getTotalStoppedTimeInMillis()/1000.0, node);
            this.catalog.setGauge("indices_merges_total_throttled_time_seconds", idx.getMerge().getTotalThrottledTimeInMillis()/1000.0, node);
            this.catalog.setGauge("indices_merges_total_auto_throttle_bytes", idx.getMerge().getTotalBytesPerSecAutoThrottle(), node);

            this.catalog.setGauge("indices_refresh_total_count", idx.getRefresh().getTotal(), node);
            this.catalog.setGauge("indices_refresh_total_time_seconds", idx.getRefresh().getTotalTimeInMillis()/1000.0, node);

            this.catalog.setGauge("indices_flush_total_count", idx.getFlush().getTotal(), node);
            this.catalog.setGauge("indices_flush_total_time_seconds", idx.getFlush().getTotalTimeInMillis()/1000.0, node);

            this.catalog.setGauge("indices_querycache_cache_count", idx.getQueryCache().getCacheCount(), node);
            this.catalog.setGauge("indices_querycache_cache_size_bytes", idx.getQueryCache().getCacheSize(), node);
            this.catalog.setGauge("indices_querycache_evictions_count", idx.getQueryCache().getEvictions(), node);
            this.catalog.setGauge("indices_querycache_hit_count", idx.getQueryCache().getHitCount(), node);
            this.catalog.setGauge("indices_querycache_memory_size_bytes", idx.getQueryCache().getMemorySizeInBytes(), node);
            this.catalog.setGauge("indices_querycache_miss_count", idx.getQueryCache().getMissCount(), node);
            this.catalog.setGauge("indices_querycache_total_count", idx.getQueryCache().getTotalCount(), node);

            this.catalog.setGauge("indices_fielddata_memory_size_bytes", idx.getFieldData().getMemorySizeInBytes(), node);
            this.catalog.setGauge("indices_fielddata_evictions_count", idx.getFieldData().getEvictions(), node);

            this.catalog.setGauge("indices_percolate_count", idx.getPercolate().getCount(), node);
            this.catalog.setGauge("indices_percolate_current_number", idx.getPercolate().getCurrent(), node);
            this.catalog.setGauge("indices_percolate_memory_size_bytes", idx.getPercolate().getMemorySizeInBytes(), node);
            this.catalog.setGauge("indices_percolate_queries_count", idx.getPercolate().getNumQueries(), node);
            this.catalog.setGauge("indices_percolate_time_seconds", idx.getPercolate().getTimeInMillis()/1000.0, node);

            this.catalog.setGauge("indices_completion_size_bytes", idx.getCompletion().getSizeInBytes(), node);

            this.catalog.setGauge("indices_segments_count", idx.getSegments().getCount(), node);
            this.catalog.setGauge("indices_segments_memory_bytes", idx.getSegments().getMemoryInBytes(), node, "all");
            this.catalog.setGauge("indices_segments_memory_bytes", idx.getSegments().getBitsetMemoryInBytes(), node,"bitset");
            this.catalog.setGauge("indices_segments_memory_bytes", idx.getSegments().getDocValuesMemoryInBytes(), node,"docvalues");
            this.catalog.setGauge("indices_segments_memory_bytes", idx.getSegments().getIndexWriterMaxMemoryInBytes(), node,"indexwriter_max");
            this.catalog.setGauge("indices_segments_memory_bytes", idx.getSegments().getIndexWriterMemoryInBytes(), node,"indexwriter");
            this.catalog.setGauge("indices_segments_memory_bytes", idx.getSegments().getNormsMemoryInBytes(), node,"norms");
            this.catalog.setGauge("indices_segments_memory_bytes", idx.getSegments().getStoredFieldsMemoryInBytes(), node,"storefields");
            this.catalog.setGauge("indices_segments_memory_bytes", idx.getSegments().getTermsMemoryInBytes(), node,"terms");
            this.catalog.setGauge("indices_segments_memory_bytes", idx.getSegments().getTermVectorsMemoryInBytes(), node,"termvectors");
            this.catalog.setGauge("indices_segments_memory_bytes", idx.getSegments().getVersionMapMemoryInBytes(), node,"versionmap");

            this.catalog.setGauge("indices_suggest_current_number", idx.getSuggest().getCurrent(), node);
            this.catalog.setGauge("indices_suggest_count", idx.getSuggest().getCount(), node);
            this.catalog.setGauge("indices_suggest_time_seconds", idx.getSuggest().getTimeInMillis()/1000.0, node);

            this.catalog.setGauge("indices_requestcache_memory_size_bytes", idx.getRequestCache().getMemorySizeInBytes(), node);
            this.catalog.setGauge("indices_requestcache_hit_count", idx.getRequestCache().getHitCount(), node);
            this.catalog.setGauge("indices_requestcache_miss_count", idx.getRequestCache().getMissCount(), node);
            this.catalog.setGauge("indices_requestcache_evictions_count", idx.getRequestCache().getEvictions(), node);

            this.catalog.setGauge("indices_recovery_current_number", idx.getRecoveryStats().currentAsSource(), node,"source");
            this.catalog.setGauge("indices_recovery_current_number", idx.getRecoveryStats().currentAsTarget(), node,"target");
            this.catalog.setGauge("indices_recovery_throttle_time_seconds", idx.getRecoveryStats().throttleTime().getSeconds(), node);
        }
    }

    private void registerTransportMetrics() {
        this.catalog.registerGauge("transport_server_open_number", "Opened server connections", "node");
        this.catalog.registerGauge("transport_rx_count", "Received packets", "node");
        this.catalog.registerGauge("transport_tx_count", "Sent packets", "node");
        this.catalog.registerGauge("transport_rx_size_bytes", "Bytes received", "node");
        this.catalog.registerGauge("transport_tx_size_bytes", "Bytes sent", "node");
    }

    private void updateTransportMetrics(String node, TransportStats ts) {
        if (ts != null) {
            this.catalog.setGauge("transport_server_open_number", ts.getServerOpen(), node);
            this.catalog.setGauge("transport_rx_count", ts.getRxCount(), node);
            this.catalog.setGauge("transport_tx_count", ts.getTxCount(), node);
            this.catalog.setGauge("transport_rx_size_bytes", ts.getRxSize().bytes(), node);
            this.catalog.setGauge("transport_tx_size_bytes", ts.getTxSize().bytes(), node);
        }
    }

    private void registerHTTPMetrics() {
        this.catalog.registerGauge("http_open_server_number", "Number of open server connections", "node");
        this.catalog.registerGauge("http_open_total_count", "Count of opened connections", "node");
    }

    private void updateHTTPMetrics(String node, HttpStats http) {
        if (http != null) {
            this.catalog.setGauge("http_open_server_number", http.getServerOpen(), node);
            this.catalog.setGauge("http_open_total_count", http.getTotalOpen(), node);
        }
    }

    private void registerScriptMetrics() {
        this.catalog.registerGauge("script_cache_evictions_count", "Number of evictions in scripts cache", "node");
        this.catalog.registerGauge("script_compilations_count", "Number of scripts compilations", "node");
    }

    private void updateScriptMetrics(String node, ScriptStats sc) {
        if (sc != null) {
            this.catalog.setGauge("script_cache_evictions_count", sc.getCacheEvictions(), node);
            this.catalog.setGauge("script_compilations_count", sc.getCompilations(), node);
        }
    }

    private void registerProcessMetrics() {
        this.catalog.registerGauge("process_cpu_percent", "CPU percentage used by ES process", "node");
        this.catalog.registerGauge("process_cpu_time_seconds", "CPU time used by ES process", "node");
        this.catalog.registerGauge("process_mem_total_virtual_bytes", "Memory used by ES process", "node");
        this.catalog.registerGauge("process_file_descriptors_open_number", "Open file descriptors", "node");
        this.catalog.registerGauge("process_file_descriptors_max_number", "Max file descriptors", "node");
    }

    private void updateProcessMetrics(String node, ProcessStats ps) {
        if (ps != null) {
            this.catalog.setGauge("process_cpu_percent", ps.getCpu().getPercent(), node);
            this.catalog.setGauge("process_cpu_time_seconds", ps.getCpu().getTotal().getSeconds(), node);
            this.catalog.setGauge("process_mem_total_virtual_bytes", ps.getMem().getTotalVirtual().bytes(), node);
            this.catalog.setGauge("process_file_descriptors_open_number", ps.getOpenFileDescriptors(), node);
            this.catalog.setGauge("process_file_descriptors_max_number", ps.getMaxFileDescriptors(), node);
        }
    }

    private void registerOsMetrics() {
        this.catalog.registerGauge("os_cpu_percent", "CPU usage in percent", "node");
        this.catalog.registerGauge("os_load_average", "CPU load", "node");
        this.catalog.registerGauge("os_mem_free_bytes", "Memory free", "node");
        this.catalog.registerGauge("os_mem_free_percent", "Memory free in percent", "node");
        this.catalog.registerGauge("os_mem_used_bytes", "Memory used", "node");
        this.catalog.registerGauge("os_mem_used_percent", "Memory used in percent", "node");
        this.catalog.registerGauge("os_mem_total_bytes", "Total memory size", "node");
        this.catalog.registerGauge("os_swap_free_bytes", "Swap free", "node");
        this.catalog.registerGauge("os_swap_used_bytes", "Swap used", "node");
        this.catalog.registerGauge("os_swap_total_bytes", "Total swap size", "node");
    }

    private void updateOsMetrics(String node, OsStats os) {
        if (os != null) {
            this.catalog.setGauge("os_cpu_percent", os.getCpuPercent(), node);
            this.catalog.setGauge("os_load_average", os.getLoadAverage(), node);
            this.catalog.setGauge("os_mem_free_bytes", os.getMem().getFree().bytes(), node);
            this.catalog.setGauge("os_mem_free_percent", os.getMem().getFreePercent(), node);
            this.catalog.setGauge("os_mem_used_bytes", os.getMem().getUsed().bytes(), node);
            this.catalog.setGauge("os_mem_used_percent", os.getMem().getUsedPercent(), node);
            this.catalog.setGauge("os_mem_total_bytes", os.getMem().getTotal().bytes(), node);
            this.catalog.setGauge("os_swap_free_bytes", os.getSwap().getFree().bytes(), node);
            this.catalog.setGauge("os_swap_used_bytes", os.getSwap().getUsed().bytes(), node);
            this.catalog.setGauge("os_swap_total_bytes", os.getSwap().getTotal().bytes(), node);
        }
    }

    private void registerCircuitBreakerMetrics() {
        this.catalog.registerGauge("circuitbreaker_estimated_bytes", "Circuit breaker estimated size", "node", "name");
        this.catalog.registerGauge("circuitbreaker_limit_bytes", "Circuit breaker size limit", "node", "name");
        this.catalog.registerGauge("circuitbreaker_overhead_ratio", "Circuit breaker overhead ratio", "node", "name");
        this.catalog.registerGauge("circuitbreaker_tripped_count", "Circuit breaker tripped count", "node", "name");
    }

    private void updateCircuitBreakersMetrics(String node, AllCircuitBreakerStats acbs) {
        if (acbs != null) {
            for (CircuitBreakerStats cbs : acbs.getAllStats()) {
                String name = cbs.getName();
                this.catalog.setGauge("circuitbreaker_estimated_bytes", cbs.getEstimated(), node, name);
                this.catalog.setGauge("circuitbreaker_limit_bytes", cbs.getLimit(), node, name);
                this.catalog.setGauge("circuitbreaker_overhead_ratio", cbs.getOverhead(), node, name);
                this.catalog.setGauge("circuitbreaker_tripped_count", cbs.getTrippedCount(), node, name);
            }
        }
    }

    private void registerThreadPoolMetrics() {
        this.catalog.registerGauge("threadpool_threads_number", "Number of threads in thread pool", "node", "name", "type");
        this.catalog.registerGauge("threadpool_threads_count", "Count of threads in thread pool", "node", "name", "type");
        this.catalog.registerGauge("threadpool_tasks_number", "Number of tasks in thread pool", "node", "name", "type");
    }

    private void updateThreadPoolMetrics(String node, ThreadPoolStats tps) {
        if (tps != null) {
            for (ThreadPoolStats.Stats st : tps) {
                String name = st.getName();
                this.catalog.setGauge("threadpool_threads_number", st.getThreads(), node, name, "threads");
                this.catalog.setGauge("threadpool_threads_number", st.getActive(), node, name, "active");
                this.catalog.setGauge("threadpool_threads_number", st.getLargest(), node, name, "largest");
                this.catalog.setGauge("threadpool_threads_count", st.getCompleted(), node, name, "completed");
                this.catalog.setGauge("threadpool_threads_count", st.getRejected(), node, name, "rejected");
                this.catalog.setGauge("threadpool_tasks_number", st.getQueue(), node, name, "queue");
            }
        }
    }

    private void registerFsMetrics() {
        this.catalog.registerGauge("fs_total_total_bytes", "Total disk space for all mount points", "node");
        this.catalog.registerGauge("fs_total_available_bytes", "Available disk space for all mount points", "node");
        this.catalog.registerGauge("fs_total_free_bytes", "Free disk space for all mountpoints", "node");
        this.catalog.registerGauge("fs_total_is_spinning_bool", "Is it a spinning disk ?", "node");

        this.catalog.registerGauge("fs_path_total_bytes", "Total disk space", "node", "path", "mount", "type");
        this.catalog.registerGauge("fs_path_available_bytes", "Available disk space", "node", "path", "mount", "type");
        this.catalog.registerGauge("fs_path_free_bytes", "Free disk space", "node", "path", "mount", "type");
        this.catalog.registerGauge("fs_path_is_spinning_bool", "Is it a spinning disk ?", "node", "path", "mount", "type");
    }

    private void updateFsMetrics(String node, FsInfo fs) {
        if (fs != null) {
            this.catalog.setGauge("fs_total_total_bytes", fs.getTotal().getTotal().bytes(), node);
            this.catalog.setGauge("fs_total_available_bytes", fs.getTotal().getAvailable().bytes(), node);
            this.catalog.setGauge("fs_total_free_bytes", fs.getTotal().getFree().bytes(), node);
            this.catalog.setGauge("fs_total_is_spinning_bool", fs.getTotal().getSpins() ? 1 : 0, node);

            for (FsInfo.Path fspath : fs) {
                String path = fspath.getPath();
                String mount = fspath.getMount();
                String type = fspath.getType();
                this.catalog.setGauge("fs_path_total_bytes", fspath.getTotal().bytes(), node, path, mount, type);
                this.catalog.setGauge("fs_path_available_bytes", fspath.getAvailable().bytes(), node, path, mount, type);
                this.catalog.setGauge("fs_path_free_bytes", fspath.getFree().bytes(), node, path, mount, type);
                this.catalog.setGauge("fs_path_is_spinning_bool", fspath.getSpins() ? 1 : 0, node, path, mount, type);
            }
        }
    }

    public void updateMetrics() {
        ClusterHealthRequest clusterHealthRequest = new ClusterHealthRequest();
        ClusterHealthResponse clusterHealthResponse = client.admin().cluster().health(clusterHealthRequest).actionGet();

        this.updateClusterMetrics(clusterHealthResponse);

        NodesStatsRequest nodesStatsRequest = new NodesStatsRequest("_local").all();
        NodesStatsResponse nodesStatsResponse = this.client.admin().cluster().nodesStats(nodesStatsRequest).actionGet();

        NodeStats nodeStats = nodesStatsResponse.getAt(0);

        String node = nodeStats.getNode().getName();
        this.updateJVMMetrics(node, nodeStats.getJvm());
        this.updateIndicesMetrics(node, nodeStats.getIndices());
        this.updateTransportMetrics(node, nodeStats.getTransport());
        this.updateHTTPMetrics(node, nodeStats.getHttp());
        this.updateScriptMetrics(node, nodeStats.getScriptStats());
        this.updateProcessMetrics(node, nodeStats.getProcess());
        this.updateOsMetrics(node, nodeStats.getOs());
        this.updateCircuitBreakersMetrics(node, nodeStats.getBreaker());
        this.updateThreadPoolMetrics(node, nodeStats.getThreadPool());
        this.updateFsMetrics(node, nodeStats.getFs());
    }

    public PrometheusMetricsCatalog getCatalog() {
        return this.catalog;
    }
}
