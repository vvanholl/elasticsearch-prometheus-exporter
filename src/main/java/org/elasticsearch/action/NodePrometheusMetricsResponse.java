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

import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.admin.cluster.node.stats.NodeStats;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import java.io.IOException;

/**
 * Action response class for Prometheus Exporter plugin.
 */
public class NodePrometheusMetricsResponse extends ActionResponse {
    private ClusterHealthResponse clusterHealth;
    private NodeStats nodeStats;

    public NodePrometheusMetricsResponse() {
    }

    public NodePrometheusMetricsResponse(ClusterHealthResponse clusterHealth, NodeStats nodesStats) {
        this.clusterHealth = clusterHealth;
        this.nodeStats = nodesStats;
    }

    public static NodePrometheusMetricsResponse readNodePrometheusMetrics(StreamInput in) throws IOException {
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
