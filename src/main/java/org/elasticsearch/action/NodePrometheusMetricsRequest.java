package org.elasticsearch.action;

import org.elasticsearch.action.support.master.MasterNodeReadRequest;

public class NodePrometheusMetricsRequest extends MasterNodeReadRequest<NodePrometheusMetricsRequest> {

    @Override
    public ActionRequestValidationException validate() {
        return null;
    }
}
