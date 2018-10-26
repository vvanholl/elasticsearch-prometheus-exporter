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

package org.elasticsearch.action;

import static org.compuscene.metrics.prometheus.PrometheusMetricsCollector.PROMETHEUS_INDICES;

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
import org.elasticsearch.client.Requests;
import org.elasticsearch.cluster.metadata.IndexNameExpressionResolver;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportService;

/**
 * Transport action class for Prometheus Exporter plugin.
 */
public class TransportNodePrometheusMetricsAction extends HandledTransportAction<NodePrometheusMetricsRequest,
        NodePrometheusMetricsResponse> {
    private final Client client;

    @Inject
    public TransportNodePrometheusMetricsAction(Settings settings, ThreadPool threadPool, Client client,
                                                TransportService transportService, ActionFilters actionFilters,
                                                IndexNameExpressionResolver indexNameExpressionResolver) {
        super(settings, NodePrometheusMetricsAction.NAME, threadPool, transportService, actionFilters,
                indexNameExpressionResolver, NodePrometheusMetricsRequest::new);
        this.client = client;
    }

    @Override
    protected void doExecute(NodePrometheusMetricsRequest request, ActionListener<NodePrometheusMetricsResponse> listener) {
        new AsyncAction(listener).start();
    }

    private class AsyncAction {
        private final ActionListener<NodePrometheusMetricsResponse> listener;
        private final ClusterHealthRequest healthRequest;
        private final NodesStatsRequest nodesStatsRequest;
        private final IndicesStatsRequest indicesStatsRequest;
        private ClusterHealthResponse clusterHealthResponse;
        private NodesStatsResponse nodesStatsResponse;

        private AsyncAction(ActionListener<NodePrometheusMetricsResponse> listener) {
            this.listener = listener;

            // Note: when using ClusterHealthRequest in Java, it pulls data at the shards level, according to ES source
            // code comment this is "so it is backward compatible with the transport client behaviour".
            // hence we are explicit about ClusterHealthRequest level and do not rely on defaults.
            // https://www.elastic.co/guide/en/elasticsearch/reference/6.4/cluster-health.html#request-params
            this.healthRequest = Requests.clusterHealthRequest().local(true);
            // There does not seem to be such api in ES 5.6.14, hence commenting it out. It should be used by default.
            // this.healthRequest.level(ClusterHealthRequest.Level.SHARDS);

            this.nodesStatsRequest = Requests.nodesStatsRequest("_local").clear().all();
            // Note: this request is not "node-specific", it does not support any "_local" notion
            // it is broad-casted to all cluster nodes.
            this.indicesStatsRequest = new IndicesStatsRequest();
        }

        private ActionListener<IndicesStatsResponse> indicesStatsResponseActionListener =
                new ActionListener<IndicesStatsResponse>() {
            @Override
            public void onResponse(IndicesStatsResponse indicesStatsResponse) {
                listener.onResponse(buildResponse(clusterHealthResponse, nodesStatsResponse, indicesStatsResponse));
            }

            @Override
            public void onFailure(Exception e) {
                listener.onFailure(new ElasticsearchException("Indices stats request failed", e));
            }
        };

        private ActionListener<NodesStatsResponse> nodesStatsResponseActionListener = new ActionListener<NodesStatsResponse>() {
            @Override
            public void onResponse(NodesStatsResponse nodeStats) {
                if (PROMETHEUS_INDICES.get(settings)) {
                    nodesStatsResponse = nodeStats;
                    client.admin().indices().stats(indicesStatsRequest, indicesStatsResponseActionListener);
                } else {
                    listener.onResponse(buildResponse(clusterHealthResponse, nodeStats, null));
                }
            }

            @Override
            public void onFailure(Exception e) {
                listener.onFailure(new ElasticsearchException("Nodes stats request failed", e));
            }
        };

        private ActionListener<ClusterHealthResponse> clusterHealthResponseActionListener =
                new ActionListener<ClusterHealthResponse>() {
            @Override
            public void onResponse(ClusterHealthResponse response) {
                clusterHealthResponse = response;
                client.admin().cluster().nodesStats(nodesStatsRequest, nodesStatsResponseActionListener);
            }

            @Override
            public void onFailure(Exception e) {
                listener.onFailure(new ElasticsearchException("Cluster health request failed", e));
            }
        };

        private void start() {
            client.admin().cluster().health(healthRequest, clusterHealthResponseActionListener);
        }

        protected NodePrometheusMetricsResponse buildResponse(ClusterHealthResponse clusterHealth,
                                                              NodesStatsResponse nodesStats,
                                                              @Nullable IndicesStatsResponse indicesStats) {
            NodePrometheusMetricsResponse response = new NodePrometheusMetricsResponse(clusterHealth,
                    nodesStats.getNodes().get(0), indicesStats);
            if (logger.isTraceEnabled()) {
                logger.trace("Return response: [{}]", response);
            }
            return response;
        }
    }
}
