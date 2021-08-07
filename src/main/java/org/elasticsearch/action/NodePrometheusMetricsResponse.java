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
import org.elasticsearch.action.admin.cluster.state.ClusterStateResponse;
import org.elasticsearch.action.admin.indices.stats.IndicesStatsResponse;
import org.elasticsearch.action.admin.indices.stats.PackageAccessHelper;
import org.elasticsearch.core.Nullable;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.settings.ClusterSettings;
import org.elasticsearch.common.settings.Settings;

import java.io.IOException;

/**
 * Action response class for Prometheus Exporter plugin.
 */
public class NodePrometheusMetricsResponse extends ActionResponse {
    private ClusterHealthResponse clusterHealth;
    private NodeStats nodeStats;
    @Nullable private IndicesStatsResponse indicesStats;
    private ClusterStatsData clusterStatsData = null;

    public NodePrometheusMetricsResponse(StreamInput in) throws IOException {
        super(in);
        clusterHealth = new ClusterHealthResponse(in);
        nodeStats = new NodeStats(in);
        indicesStats = PackageAccessHelper.createIndicesStatsResponse(in);
        clusterStatsData = new ClusterStatsData(in);
    }

    public NodePrometheusMetricsResponse(ClusterHealthResponse clusterHealth, NodeStats nodesStats,
                                         @Nullable IndicesStatsResponse indicesStats,
                                         @Nullable ClusterStateResponse clusterStateResponse,
                                         Settings settings,
                                         ClusterSettings clusterSettings) {
        this.clusterHealth = clusterHealth;
        this.nodeStats = nodesStats;
        this.indicesStats = indicesStats;
        if (clusterStateResponse != null) {
            this.clusterStatsData = new ClusterStatsData(clusterStateResponse, settings, clusterSettings);
        }
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

    @Nullable
    public ClusterStatsData getClusterStatsData() {
        return this.clusterStatsData;
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        clusterHealth.writeTo(out);
        nodeStats.writeTo(out);
        out.writeOptionalWriteable(indicesStats);
        clusterStatsData.writeTo(out);
    }
}
