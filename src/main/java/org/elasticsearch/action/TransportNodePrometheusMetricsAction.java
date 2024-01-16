package org.elasticsearch.action;

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthRequest;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.admin.cluster.node.stats.NodesStatsRequest;
import org.elasticsearch.action.admin.cluster.node.stats.NodesStatsResponse;
import org.elasticsearch.action.admin.indices.stats.IndicesStatsRequest;
import org.elasticsearch.action.admin.indices.stats.IndicesStatsResponse;
import org.elasticsearch.action.support.ActionFilters;
import org.elasticsearch.action.support.HandledTransportAction;
import org.elasticsearch.client.Client;
import org.elasticsearch.cluster.metadata.IndexNameExpressionResolver;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportService;

public class TransportNodePrometheusMetricsAction extends HandledTransportAction<NodePrometheusMetricsRequest, NodePrometheusMetricsResponse> {

    private final Client client;

    @Inject
    public TransportNodePrometheusMetricsAction(Settings settings, ThreadPool threadPool, Client client,
                                                TransportService transportService, ActionFilters actionFilters,
                                                IndexNameExpressionResolver indexNameExpressionResolver) {
        super(settings, NodePrometheusMetricsAction.NAME, threadPool, transportService, actionFilters, indexNameExpressionResolver, NodePrometheusMetricsRequest.class);
        this.client = client;
    }

    @Override
    protected void doExecute(NodePrometheusMetricsRequest request, ActionListener<NodePrometheusMetricsResponse> listener) {
        new AsyncAction(request, listener).start();
    }

    private class AsyncAction {

        private final ActionListener<NodePrometheusMetricsResponse> listener;
        private final ClusterHealthRequest healthRequest;
        private final NodesStatsRequest nodesStatsRequest;
        private ClusterHealthResponse clusterHealthResponse;
        private NodesStatsResponse nodesStatsResponse;

        private AsyncAction(NodePrometheusMetricsRequest request, ActionListener<NodePrometheusMetricsResponse> listener) {
            this.listener = listener;
            this.healthRequest = new ClusterHealthRequest();
            this.nodesStatsRequest = new NodesStatsRequest("_local").all();
//            this.nodesStatsRequest = new NodesStatsRequest("_local").clear(); // used for debugging only
        }

        private void start() {
            client.admin().cluster().health(healthRequest, clusterHealthResponseActionListener);
        }

        private ActionListener<IndicesStatsResponse> indicesStatsResponseActionListener =
            new ActionListener<IndicesStatsResponse>() {
                @Override
                public void onResponse(IndicesStatsResponse indicesStatsResponse) {
                    listener.onResponse(buildResponse(clusterHealthResponse, nodesStatsResponse, indicesStatsResponse));
                }
                @Override
                public void onFailure(Throwable e) {
                    listener.onFailure(new ElasticsearchException("Indices stats request failed", e));
                }
            };

        private ActionListener<NodesStatsResponse> nodesStatsResponseActionListener = new ActionListener<NodesStatsResponse>() {
            @Override
            public void onResponse(NodesStatsResponse nodeStats) {
                nodesStatsResponse = nodeStats;
                client.admin().indices().stats(new IndicesStatsRequest(), indicesStatsResponseActionListener);
            }

            @Override
            public void onFailure(Throwable throwable) {
                listener.onFailure(new ElasticsearchException("Nodes stats request failed", throwable));
            }
        };

        private ActionListener<ClusterHealthResponse> clusterHealthResponseActionListener = new ActionListener<ClusterHealthResponse>() {
            @Override
            public void onResponse(ClusterHealthResponse response) {
                clusterHealthResponse = response;
                client.admin().cluster().nodesStats(nodesStatsRequest, nodesStatsResponseActionListener);
            }

            @Override
            public void onFailure(Throwable throwable) {
                listener.onFailure(new ElasticsearchException("Cluster health request failed", throwable));
            }
        };

        protected NodePrometheusMetricsResponse buildResponse(ClusterHealthResponse clusterHealth, NodesStatsResponse nodesStats, @Nullable
            IndicesStatsResponse indicesStats) {
            NodePrometheusMetricsResponse response = new NodePrometheusMetricsResponse(clusterHealth, nodesStats.getAt(0), indicesStats);
            if (logger.isTraceEnabled()) {
                logger.trace("Return response: [{}]", response);
            }
            return response;
        }
    }
}
