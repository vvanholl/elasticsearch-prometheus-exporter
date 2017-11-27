package org.elasticsearch.action;

import org.elasticsearch.action.support.master.MasterNodeReadRequest;

/**
 * Action request class for Prometheus Exporter plugin.
 */
public class NodePrometheusMetricsRequest extends MasterNodeReadRequest<NodePrometheusMetricsRequest> {
    @Override
    public ActionRequestValidationException validate() {
        return null;
    }
}
