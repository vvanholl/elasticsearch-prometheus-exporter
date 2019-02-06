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

import org.compuscene.metrics.prometheus.PrometheusSettings;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthRequest;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.admin.cluster.node.stats.NodesStatsRequest;
import org.elasticsearch.action.admin.cluster.node.stats.NodesStatsResponse;
import org.elasticsearch.action.admin.cluster.state.ClusterStateRequest;
import org.elasticsearch.action.admin.cluster.state.ClusterStateResponse;
import org.elasticsearch.action.admin.indices.stats.IndicesStatsRequest;
import org.elasticsearch.action.admin.indices.stats.IndicesStatsResponse;
import org.elasticsearch.action.support.ActionFilters;
import org.elasticsearch.action.support.HandledTransportAction;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.Requests;
import org.elasticsearch.cluster.metadata.IndexNameExpressionResolver;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.ClusterSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportService;

/**
 * Transport action class for Prometheus Exporter plugin.
 *
 * It performs several requests within the cluster to gather "cluster health", "nodes stats", "indices stats"
 * and "cluster state" (i.e. cluster settings) info. Some of those requests are optional depending on plugin
 * settings.
 */
public class TransportNodePrometheusMetricsAction extends HandledTransportAction<NodePrometheusMetricsRequest,
        NodePrometheusMetricsResponse> {
    private final Client client;
    private final Settings settings;
    private final ClusterSettings clusterSettings;
    private final PrometheusSettings prometheusSettings;

    @Inject
    public TransportNodePrometheusMetricsAction(Settings settings, ThreadPool threadPool, Client client,
                                                TransportService transportService, ActionFilters actionFilters,
                                                IndexNameExpressionResolver indexNameExpressionResolver,
                                                ClusterSettings clusterSettings) {
        super(settings, NodePrometheusMetricsAction.NAME, threadPool, transportService, actionFilters,
                indexNameExpressionResolver, NodePrometheusMetricsRequest::new);
        this.client = client;
        this.settings = settings;
        this.clusterSettings = clusterSettings;
        this.prometheusSettings = new PrometheusSettings(settings, clusterSettings);
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
        private final ClusterStateRequest clusterStateRequest;

        private ClusterHealthResponse clusterHealthResponse = null;
        private NodesStatsResponse nodesStatsResponse = null;
        private IndicesStatsResponse indicesStatsResponse = null;
        private ClusterStateResponse clusterStateResponse = null;

        // read the state of prometheus dynamic settings only once at the beginning of the async request
        private boolean isPrometheusIndices = prometheusSettings.getPrometheusIndices();
        private boolean isPrometheusClusterSettings = prometheusSettings.getPrometheusClusterSettings();

        // All the requests are executed in sequential non-blocking order.
        // It is implemented by wrapping each individual request with ActionListener
        // and chaining all of them into a sequence. The last member of the chain call method that gathers
        // all the responses from previous requests and pass them to outer listener (i.e. calling client).
        // Optional requests are skipped.
        //
        // In the future we might consider executing all the requests in parallel if needed (CountDownLatch?),
        // however, some of the requests can impact cluster performance (especially if the cluster is already overloaded)
        // and in this situation it is better to run all requests in predictable order so that collected metrics
        // stay consistent.
        private AsyncAction(ActionListener<NodePrometheusMetricsResponse> listener) {
            this.listener = listener;

            // Note: when using ClusterHealthRequest in Java, it pulls data at the shards level, according to ES source
            // code comment this is "so it is backward compatible with the transport client behaviour".
            // hence we are explicit about ClusterHealthRequest level and do not rely on defaults.
            // https://www.elastic.co/guide/en/elasticsearch/reference/6.4/cluster-health.html#request-params
            this.healthRequest = Requests.clusterHealthRequest().local(true);
            this.healthRequest.level(ClusterHealthRequest.Level.SHARDS);

            this.nodesStatsRequest = Requests.nodesStatsRequest("_local").clear().all();

            // Indices stats request is not "node-specific", it does not support any "_local" notion
            // it is broad-casted to all cluster nodes.
            this.indicesStatsRequest = isPrometheusIndices ? new IndicesStatsRequest() : null;

            // Cluster settings are get via ClusterStateRequest (see elasticsearch RestClusterGetSettingsAction for details)
            // We prefer to send it to master node (hence local=false; it should be set by default but we want to be sure).
            this.clusterStateRequest = isPrometheusClusterSettings ? Requests.clusterStateRequest()
                    .clear().metaData(true).local(false) : null;
        }

        private void gatherRequests() {
            listener.onResponse(buildResponse(clusterHealthResponse, nodesStatsResponse, indicesStatsResponse,
                    clusterStateResponse));
        }

        private ActionListener<ClusterStateResponse> clusterStateResponseActionListener =
            new ActionListener<ClusterStateResponse>() {
                @Override
                public void onResponse(ClusterStateResponse response) {
                    clusterStateResponse = response;
                    gatherRequests();
                }

                @Override
                public void onFailure(Exception e) {
                    listener.onFailure(new ElasticsearchException("Cluster state request failed", e));
                }
            };

        private ActionListener<IndicesStatsResponse> indicesStatsResponseActionListener =
            new ActionListener<IndicesStatsResponse>() {
                @Override
                public void onResponse(IndicesStatsResponse response) {
                    indicesStatsResponse = response;
                    if (isPrometheusClusterSettings) {
                        client.admin().cluster().state(clusterStateRequest, clusterStateResponseActionListener);
                    } else {
                        gatherRequests();
                    }
                }

                @Override
                public void onFailure(Exception e) {
                    listener.onFailure(new ElasticsearchException("Indices stats request failed", e));
                }
            };

        private ActionListener<NodesStatsResponse> nodesStatsResponseActionListener =
            new ActionListener<NodesStatsResponse>() {
                @Override
                public void onResponse(NodesStatsResponse nodeStats) {
                    nodesStatsResponse = nodeStats;
                    if (isPrometheusIndices) {
                        client.admin().indices().stats(indicesStatsRequest, indicesStatsResponseActionListener);
                    } else {
                        indicesStatsResponseActionListener.onResponse(null);
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
                                                              @Nullable IndicesStatsResponse indicesStats,
                                                              @Nullable ClusterStateResponse clusterStateResponse) {
            NodePrometheusMetricsResponse response = new NodePrometheusMetricsResponse(clusterHealth,
                    nodesStats.getNodes().get(0), indicesStats, clusterStateResponse,
                    settings, clusterSettings);
            if (logger.isTraceEnabled()) {
                logger.trace("Return response: [{}]", response);
            }
            return response;
        }
    }
}
