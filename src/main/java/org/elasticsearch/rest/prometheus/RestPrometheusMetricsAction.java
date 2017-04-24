package org.elasticsearch.rest.prometheus;

import org.apache.logging.log4j.Logger;
import org.compuscene.metrics.prometheus.PrometheusMetricsCollector;
import org.elasticsearch.client.node.NodeClient;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.logging.ESLoggerFactory;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.rest.BaseRestHandler;
import org.elasticsearch.rest.BytesRestResponse;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestRequest;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import static org.elasticsearch.rest.RestRequest.Method.GET;
import static org.elasticsearch.rest.RestStatus.OK;

public class RestPrometheusMetricsAction extends BaseRestHandler {

    private final static Logger logger = ESLoggerFactory.getLogger(RestPrometheusMetricsAction.class.getSimpleName());

    private AtomicReference<PrometheusMetricsCollector> collector = new AtomicReference<>();

    @Inject
    public RestPrometheusMetricsAction(Settings settings, RestController controller) {

        super(settings);

        controller.registerHandler(GET, "/_prometheus/metrics", this);
    }

    @Override
    protected RestChannelConsumer prepareRequest(RestRequest request, NodeClient client) throws IOException {
        logger.trace(String.format("Received request for Prometheus metrics from %s", request.getRemoteAddress().toString()));

        collector.compareAndSet(null, new PrometheusMetricsCollector(settings, client));

        collector.get().updateMetrics();

        return channel -> channel.sendResponse(new BytesRestResponse(OK, collector.get().getCatalog().toTextFormat()));

    }
}
