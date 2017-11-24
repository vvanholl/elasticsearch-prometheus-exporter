package org.elasticsearch.rest.prometheus;

import static org.elasticsearch.action.NodePrometheusMetricsAction.INSTANCE;
import static org.elasticsearch.rest.RestRequest.Method.GET;
import org.apache.logging.log4j.Logger;
import org.compuscene.metrics.prometheus.PrometheusMetricsCollector;
import org.elasticsearch.action.NodePrometheusMetricsRequest;
import org.elasticsearch.action.NodePrometheusMetricsResponse;
import org.elasticsearch.client.node.NodeClient;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.logging.ESLoggerFactory;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.rest.*;
import org.elasticsearch.rest.action.RestResponseListener;
import java.io.IOException;

public class RestPrometheusMetricsAction extends BaseRestHandler {
    private final static Logger logger = ESLoggerFactory.getLogger(RestPrometheusMetricsAction.class.getSimpleName());

    @Inject
    public RestPrometheusMetricsAction(Settings settings, RestController controller) {
        super(settings);
        controller.registerHandler(GET, "/_prometheus/metrics", this);
    }

    @Override
    public String getName() {
        return "prometheus_metrics_action";
    }

    @Override
    protected RestChannelConsumer prepareRequest(RestRequest request, NodeClient client) throws IOException {
        if (logger.isTraceEnabled()) {
            logger.trace(String.format("Received request for Prometheus metrics from %s", request.getRemoteAddress().toString()));
        }

        NodePrometheusMetricsRequest metricsRequest = new NodePrometheusMetricsRequest();

        return channel -> client.execute(INSTANCE, metricsRequest, new RestResponseListener<NodePrometheusMetricsResponse>(channel) {
            @Override
            public RestResponse buildResponse(NodePrometheusMetricsResponse response) throws Exception {
                String clusterName = response.getClusterHealth().getClusterName();
                String nodeName = response.getNodeStats().getNode().getName();
                String nodeId = response.getNodeStats().getNode().getId();
                if (logger.isTraceEnabled()) {
                    logger.trace("Prepare new Prometheus metric collector for: [{}], [{}], [{}]", clusterName, nodeId, nodeName);
                }
                PrometheusMetricsCollector collector = new PrometheusMetricsCollector(settings, clusterName, nodeName, nodeId);
                collector.updateMetrics(response.getClusterHealth(), response.getNodeStats());
                return new BytesRestResponse(RestStatus.OK, collector.getCatalog().toTextFormat());
            }
        });
    }
}
