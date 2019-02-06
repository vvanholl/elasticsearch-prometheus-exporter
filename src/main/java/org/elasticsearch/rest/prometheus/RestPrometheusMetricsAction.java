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

package org.elasticsearch.rest.prometheus;

import static org.elasticsearch.action.NodePrometheusMetricsAction.INSTANCE;
import static org.elasticsearch.rest.RestRequest.Method.GET;

import org.compuscene.metrics.prometheus.PrometheusMetricsCatalog;
import org.compuscene.metrics.prometheus.PrometheusMetricsCollector;
import org.compuscene.metrics.prometheus.PrometheusSettings;
import org.elasticsearch.action.NodePrometheusMetricsRequest;
import org.elasticsearch.action.NodePrometheusMetricsResponse;
import org.elasticsearch.client.node.NodeClient;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.ClusterSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.rest.*;
import org.elasticsearch.rest.action.RestResponseListener;

import java.util.Locale;

/**
 * REST action class for Prometheus Exporter plugin.
 */
public class RestPrometheusMetricsAction extends BaseRestHandler {

    private final PrometheusSettings prometheusSettings;

    @Inject
    public RestPrometheusMetricsAction(Settings settings, ClusterSettings clusterSettings, RestController controller) {
        super(settings);
        this.prometheusSettings = new PrometheusSettings(settings, clusterSettings);
        controller.registerHandler(GET, "/_prometheus/metrics", this);
    }

    @Override
    public String getName() {
        return "prometheus_metrics_action";
    }

     // This method does not throw any IOException because there are no request parameters to be parsed
     // and processed. This may change in the future.
    @Override
    protected RestChannelConsumer prepareRequest(RestRequest request, NodeClient client) {
        if (logger.isTraceEnabled()) {
            logger.trace(String.format(Locale.ENGLISH, "Received request for Prometheus metrics from %s",
                    request.getRemoteAddress().toString()));
        }

        NodePrometheusMetricsRequest metricsRequest = new NodePrometheusMetricsRequest();

        return channel -> client.execute(INSTANCE, metricsRequest,
                new RestResponseListener<NodePrometheusMetricsResponse>(channel) {

                    @Override
                    public RestResponse buildResponse(NodePrometheusMetricsResponse response) throws Exception {
                String clusterName = response.getClusterHealth().getClusterName();
                String nodeName = response.getNodeStats().getNode().getName();
                String nodeId = response.getNodeStats().getNode().getId();
                if (logger.isTraceEnabled()) {
                    logger.trace("Prepare new Prometheus metric collector for: [{}], [{}], [{}]", clusterName, nodeId,
                            nodeName);
                }
                PrometheusMetricsCatalog catalog = new PrometheusMetricsCatalog(clusterName, nodeName, nodeId, "es_");
                PrometheusMetricsCollector collector = new PrometheusMetricsCollector(
                        catalog,
                        prometheusSettings.getPrometheusIndices(),
                        prometheusSettings.getPrometheusClusterSettings());
                collector.registerMetrics();
                collector.updateMetrics(response.getClusterHealth(), response.getNodeStats(), response.getIndicesStats(),
                        response.getClusterStatsData());
                return new BytesRestResponse(RestStatus.OK, collector.getCatalog().toTextFormat());
            }
        });
    }
}
