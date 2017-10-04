package org.elasticsearch.action;

import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.admin.cluster.node.stats.NodeStats;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;

import java.io.IOException;

public class NodePrometheusMetricsResponse extends ActionResponse {

    private ClusterHealthResponse clusterHealth;
    private NodeStats nodeStats;

    public NodePrometheusMetricsResponse() {
    }

    public NodePrometheusMetricsResponse(ClusterHealthResponse clusterHealth, NodeStats nodesStats) {
        this.clusterHealth = clusterHealth;
        this.nodeStats = nodesStats;
    }

    public ClusterHealthResponse getClusterHealth() {
        return this.clusterHealth;
    }

    public NodeStats getNodeStats() {
        return this.nodeStats;
    }

    public static NodePrometheusMetricsResponse readNodePrometheusMetrics(StreamInput in) throws IOException {
        NodePrometheusMetricsResponse metrics = new NodePrometheusMetricsResponse();
        metrics.readFrom(in);
        return metrics;
    }

    @Override
    public void readFrom(StreamInput in) throws IOException {
        super.readFrom(in);
        clusterHealth = ClusterHealthResponse.readResponseFrom(in);
        nodeStats = NodeStats.readNodeStats(in);
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        clusterHealth.writeTo(out);
        nodeStats.writeTo(out);
    }
}
