package org.elasticsearch.action;

import org.elasticsearch.action.support.master.MasterNodeReadOperationRequestBuilder;
import org.elasticsearch.client.ElasticsearchClient;

/**
 * Request builder class for Prometheus Exporter plugin.
 */
public class NodePrometheusRequestBuilder extends MasterNodeReadOperationRequestBuilder<NodePrometheusMetricsRequest,
        NodePrometheusMetricsResponse, NodePrometheusRequestBuilder> {
    public NodePrometheusRequestBuilder(ElasticsearchClient client, NodePrometheusMetricsAction action) {
        super(client, action, new NodePrometheusMetricsRequest().local(true));
    }
}
