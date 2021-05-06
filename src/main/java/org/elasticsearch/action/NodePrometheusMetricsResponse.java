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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.compuscene.metrics.prometheus.PrometheusSettings;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.admin.cluster.node.stats.NodeStats;
import org.elasticsearch.action.admin.cluster.state.ClusterStateResponse;
import org.elasticsearch.action.admin.indices.stats.IndicesStatsResponse;
import org.elasticsearch.action.admin.indices.stats.PackageAccessHelper;
import org.elasticsearch.cluster.metadata.Metadata;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.io.stream.*;
import org.elasticsearch.common.settings.ClusterSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.rest.prometheus.RestPrometheusMetricsAction;
import org.elasticsearch.xpack.core.slm.SnapshotLifecycleMetadata;
import org.elasticsearch.xpack.core.slm.SnapshotLifecycleStats;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Action response class for Prometheus Exporter plugin.
 */
public class NodePrometheusMetricsResponse extends ActionResponse {
    private static final Logger logger = LogManager.getLogger(RestPrometheusMetricsAction.class);
    private ClusterHealthResponse clusterHealth;
    private NodeStats nodeStats;
    @Nullable private IndicesStatsResponse indicesStats;
    private ClusterStatsData clusterStatsData = null;
    private PrometheusSnapshotLifecycleStats slmStats = null;

    public NodePrometheusMetricsResponse(StreamInput in) throws IOException {
        super(in);
        clusterHealth = new ClusterHealthResponse(in);
        nodeStats = new NodeStats(in);
        indicesStats = PackageAccessHelper.createIndicesStatsResponse(in);
        clusterStatsData = new ClusterStatsData(in);
        slmStats = new PrometheusSnapshotLifecycleStats(in);
    }

    private InputStreamStreamInput getInputStreamFromWriteable(Writeable w) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        OutputStreamStreamOutput osso = new OutputStreamStreamOutput(baos);
        ByteArrayInputStream bais = null;
        InputStreamStreamInput issi;
        try {
            w.writeTo(osso);
            bais = new ByteArrayInputStream(baos.toByteArray());
            issi = new InputStreamStreamInput(bais);
        } catch (IOException e) {
            throw e;
        } finally {
            osso.close();
            baos.close();
            if (bais != null) {
                bais.close();
            }
        }
        return issi;
    }

    private PrometheusSnapshotLifecycleStats getPrometheusSnapshotLifecycleStats(ClusterStateResponse clusterStateResponse) {
        Metadata m = clusterStateResponse.getState().getMetadata();
        InputStreamStreamInput issiSnapshotLifecycleMetadata = null;
        InputStreamStreamInput issiSlmStats = null;
        PrometheusSnapshotLifecycleStats prometheusSnapshotLifecycleStats = null;
        try {
            Metadata.Custom metdataCustom = m.getCustoms().get(SnapshotLifecycleMetadata.TYPE);
            if (metdataCustom != null) {
                issiSnapshotLifecycleMetadata = getInputStreamFromWriteable(metdataCustom);
                SnapshotLifecycleStats slmStats = new SnapshotLifecycleMetadata(issiSnapshotLifecycleMetadata).getStats();
                issiSlmStats = getInputStreamFromWriteable(slmStats);
                prometheusSnapshotLifecycleStats = new PrometheusSnapshotLifecycleStats(issiSlmStats);
            }
        } catch (IOException e) {
            logger.error("Failed to get SLM stats", e);
        } finally {
            if (issiSnapshotLifecycleMetadata != null) {
                try {
                    issiSnapshotLifecycleMetadata.close();
                } catch (IOException e) {
                    logger.error("Failed to close issiSnapshotLifecycleMetadata", e);
                }
            }
            if (issiSlmStats != null) {
                try {
                    issiSlmStats.close();
                } catch (IOException e) {
                    logger.error("Failed to close issiSlmStats", e);
                }
            }
        }
        return prometheusSnapshotLifecycleStats;
    }

    public NodePrometheusMetricsResponse(ClusterHealthResponse clusterHealth, NodeStats nodesStats,
                                         @Nullable IndicesStatsResponse indicesStats,
                                         @Nullable ClusterStateResponse clusterStateResponse,
                                         Settings settings,
                                         ClusterSettings clusterSettings,
                                         PrometheusSettings prometheusSettings) {
        this.clusterHealth = clusterHealth;
        this.nodeStats = nodesStats;
        this.indicesStats = indicesStats;
        if (clusterStateResponse != null) {
            if (prometheusSettings.getPrometheusClusterSettings()) {
                this.clusterStatsData = new ClusterStatsData(clusterStateResponse, settings, clusterSettings);
            }
            if (prometheusSettings.getPrometheusSlm()) {
                this.slmStats = getPrometheusSnapshotLifecycleStats(clusterStateResponse);
            }
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

    @Nullable
    public PrometheusSnapshotLifecycleStats getSlmStats() {
        return this.slmStats;
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        clusterHealth.writeTo(out);
        nodeStats.writeTo(out);
        out.writeOptionalWriteable(indicesStats);
        clusterStatsData.writeTo(out);
        slmStats.writeTo(out);
    }
}
