package org.elasticsearch.action;

import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.admin.cluster.node.stats.NodeStats;
import org.elasticsearch.action.admin.indices.stats.IndicesStatsResponse;
import org.elasticsearch.action.admin.indices.stats.PackageAccessHelper;
import org.elasticsearch.action.admin.indices.stats.ShardStats;
import org.elasticsearch.action.support.broadcast.BroadcastResponse;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;

import java.io.IOException;
import java.util.Arrays;

public class NodePrometheusMetricsResponse extends ActionResponse {

    private ClusterHealthResponse clusterHealth;
    private NodeStats nodeStats;
    @Nullable
    private IndicesStatsResponse indicesStats;

    public NodePrometheusMetricsResponse() {
    }

    public NodePrometheusMetricsResponse(ClusterHealthResponse clusterHealth, NodeStats nodesStats,
        @Nullable IndicesStatsResponse indicesStats) {
        this.clusterHealth = clusterHealth;
        this.nodeStats = nodesStats;
        this.indicesStats = indicesStats;
    }

    public  NodePrometheusMetricsResponse readNodePrometheusMetrics(StreamInput in) throws IOException {
        NodePrometheusMetricsResponse metrics = new NodePrometheusMetricsResponse();
        metrics.readFrom(in);
        return metrics;
    }
    public ClusterHealthResponse getClusterHealth() {
        return this.clusterHealth;
    }

    public NodeStats getNodeStats() {
        return this.nodeStats;
    }

    @Nullable
    public IndicesStatsResponse getIndicesStats() {
        return this.indicesStats;
    }

    @Override
    public void readFrom(StreamInput in) throws IOException {
        super.readFrom(in);
        clusterHealth = ClusterHealthResponse.readResponseFrom(in);
        nodeStats = NodeStats.readNodeStats(in);
        BroadcastResponse br = new BroadcastResponse();
        br.readFrom(in);
        int size = in.readInt();
        ShardStats[] ss = new ShardStats[size];
        for (int i = 0; i < size; i ++) {
            ss[i] = ShardStats.readShardStats(in);
        }
        indicesStats = PackageAccessHelper.createIndicesStatsResponse(
            ss, br.getTotalShards(), br.getSuccessfulShards(), br.getFailedShards(),
            Arrays.asList(br.getShardFailures())
        );
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        clusterHealth.writeTo(out);
        nodeStats.writeTo(out);
        if (indicesStats != null) {
            //indicesStats.writeTo(out);
            ((BroadcastResponse) indicesStats).writeTo(out);
            out.writeGenericValue(indicesStats.getShards());
        }
    }
}
