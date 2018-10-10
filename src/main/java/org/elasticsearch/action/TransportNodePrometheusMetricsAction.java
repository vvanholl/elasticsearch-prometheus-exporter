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

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthRequest;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.admin.cluster.node.stats.NodesStatsRequest;
import org.elasticsearch.action.admin.cluster.node.stats.NodesStatsResponse;
import org.elasticsearch.action.support.ActionFilters;
import org.elasticsearch.action.support.HandledTransportAction;
import org.elasticsearch.client.Client;
import org.elasticsearch.cluster.metadata.IndexNameExpressionResolver;
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
        private ClusterHealthResponse clusterHealthResponse;

        private ActionListener<NodesStatsResponse> nodesStatsResponseActionListener = new ActionListener<NodesStatsResponse>() {
            @Override
            public void onResponse(NodesStatsResponse nodeStats) {
                listener.onResponse(buildResponse(clusterHealthResponse, nodeStats));
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

        private AsyncAction(ActionListener<NodePrometheusMetricsResponse> listener) {
            this.listener = listener;
            this.healthRequest = new ClusterHealthRequest();
            this.nodesStatsRequest = new NodesStatsRequest("_local").all();
        }

        private void start() {
            client.admin().cluster().health(healthRequest, clusterHealthResponseActionListener);
        }

        protected NodePrometheusMetricsResponse buildResponse(ClusterHealthResponse clusterHealth,
                                                              NodesStatsResponse nodesStats) {
            NodePrometheusMetricsResponse response = new NodePrometheusMetricsResponse(clusterHealth,
                    nodesStats.getNodes().get(0));
            if (logger.isTraceEnabled()) {
                logger.trace("Return response: [{}]", response);
            }
            return response;
        }
    }
}
