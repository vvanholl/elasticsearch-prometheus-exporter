package org.elasticsearch.action;

import org.elasticsearch.client.ElasticsearchClient;

public class NodePrometheusMetricsAction extends Action<NodePrometheusMetricsRequest, NodePrometheusMetricsResponse, NodePrometheusRequestBuilder> {

    public static final NodePrometheusMetricsAction INSTANCE = new NodePrometheusMetricsAction();
    public static final String NAME = "cluster:monitor/prometheus/metrics";

    private NodePrometheusMetricsAction() {
        super(NAME);
    }

    @Override
    public NodePrometheusMetricsResponse newResponse() {
        return new NodePrometheusMetricsResponse();
    }

    @Override
    public NodePrometheusRequestBuilder newRequestBuilder(ElasticsearchClient client) {
        return new NodePrometheusRequestBuilder(client, this);
    }
}
