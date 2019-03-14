/*
 * Copyright [2018] [Vincent VAN HOLLEBEKE]
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

import static org.elasticsearch.cluster.routing.allocation.DiskThresholdSettings.*;

import org.elasticsearch.ElasticsearchParseException;
import org.elasticsearch.action.admin.cluster.state.ClusterStateResponse;
import org.elasticsearch.cluster.metadata.MetaData;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.settings.ClusterSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.settings.SettingsException;

import java.io.IOException;


/**
 * Selected settings from Elasticsearch cluster settings.
 *
 * Disk-based shard allocation [1] settings play important role in how Elasticsearch decides where to allocate
 * new shards or if existing shards are relocated to different nodes. The tricky part about these settings is
 * that they can be expressed either in percent or bytes value (they cannot be mixed) and they can be updated on the fly.
 *
 * [1] https://www.elastic.co/guide/en/elasticsearch/reference/master/disk-allocator.html#disk-allocator
 *
 * In order to make it easy for Prometheus to consume the data we expose these settings in both formats (pct and bytes)
 * and we do our best in determining if they are currently set as pct or bytes filling appropriate variables with data
 * or null value.
 */
public class ClusterStatsData extends ActionResponse {

    private Boolean thresholdEnabled = null;

    @Nullable private Long diskLowInBytes;
    @Nullable private Long diskHighInBytes;
    @Nullable private Long floodStageInBytes;

    @Nullable private Double diskLowInPct;
    @Nullable private Double diskHighInPct;
    @Nullable private Double floodStageInPct;

    private Long[] diskLowInBytesRef = new Long[]{diskLowInBytes};
    private Long[] diskHighInBytesRef = new Long[]{diskHighInBytes};
    private Long[] floodStageInBytesRef = new Long[]{floodStageInBytes};

    private Double[] diskLowInPctRef = new Double[]{diskLowInPct};
    private Double[] diskHighInPctRef = new Double[]{diskHighInPct};
    private Double[] floodStageInPctRef = new Double[]{floodStageInPct};


    @SuppressWarnings({"checkstyle:LineLength"})
    public ClusterStatsData(ClusterStateResponse clusterStateResponse, Settings settings, ClusterSettings clusterSettings) {

        MetaData m = clusterStateResponse.getState().getMetaData();
        // There are several layers of cluster settings in Elasticsearch each having different priority.
        // We need to traverse them from the top priority down to find relevant value of each setting.
        // See https://www.elastic.co/guide/en/elasticsearch/reference/master/cluster-update-settings.html#_order_of_precedence
        for (Settings s : new Settings[]{
                // See: RestClusterGetSettingsAction#response
                // or: https://github.com/elastic/elasticsearch/pull/33247/files
                // We do not filter the settings, but we use the clusterSettings.diff()
                // In the end we expose just a few selected settings ATM.
                m.transientSettings(),
                m.persistentSettings(),
                clusterSettings.diff(m.settings(), settings)
        }) {
            thresholdEnabled = thresholdEnabled == null ?
                    s.getAsBoolean(CLUSTER_ROUTING_ALLOCATION_DISK_THRESHOLD_ENABLED_SETTING.getKey(), null) : thresholdEnabled;

            parseValue(s, CLUSTER_ROUTING_ALLOCATION_LOW_DISK_WATERMARK_SETTING.getKey(), diskLowInBytesRef, diskLowInPctRef);
            parseValue(s, CLUSTER_ROUTING_ALLOCATION_HIGH_DISK_WATERMARK_SETTING.getKey(), diskHighInBytesRef, diskHighInPctRef);
            parseValue(s, CLUSTER_ROUTING_ALLOCATION_DISK_FLOOD_STAGE_WATERMARK_SETTING.getKey(), floodStageInBytesRef, floodStageInPctRef);
        }

        diskLowInBytes = diskLowInBytesRef[0];
        diskHighInBytes = diskHighInBytesRef[0];
        floodStageInBytes = floodStageInBytesRef[0];

        diskLowInPct = diskLowInPctRef[0];
        diskHighInPct = diskHighInPctRef[0];
        floodStageInPct = floodStageInPctRef[0];
    }

    /**
     * Try to extract and parse value from settings for given key.
     * First it tries to parse it as a RatioValue (pct) then as byte size value.
     * It assigns parsed value to corresponding argument references (passed via array hack).
     * If parsing fails the method fires exception, however, this should not happen - we rely on Elasticsearch
     * to already have parsed and validated these values before. Unless we screwed something up...
     */
    private void parseValue(Settings s, String key, Long[] bytesPointer, Double[] pctPointer) {
        String value = s.get(key);
        if (value != null && pctPointer[0] == null) {
            try {
                pctPointer[0] = s.getAsRatio(key, null).getAsPercent();
            } catch (SettingsException | ElasticsearchParseException | NullPointerException e1) {
                if (bytesPointer[0] == null) {
                    try {
                        bytesPointer[0] = s.getAsBytesSize(key, null).getBytes();
                    } catch (SettingsException | ElasticsearchParseException | NullPointerException e2) {
                        // TODO(lvlcek): log.debug("This went wrong, but 'Keep Calm and Carry On'")
                        // We should avoid using logs in this class (due to perf impact), instead we should
                        // consider moving this logic to some static helper class/method going forward.
                    }
                }
            }
        }
    }

    @Override
    public void readFrom(StreamInput in) throws IOException {
        super.readFrom(in);
        thresholdEnabled = in.readOptionalBoolean();
        //
        diskLowInBytes = in.readOptionalLong();
        diskHighInBytes = in.readOptionalLong();
        floodStageInBytes = in.readOptionalLong();
        //
        diskLowInPct = in.readOptionalDouble();
        diskHighInPct = in.readOptionalDouble();
        floodStageInPct = in.readOptionalDouble();
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        out.writeOptionalBoolean(thresholdEnabled);
        //
        out.writeOptionalLong(diskLowInBytes);
        out.writeOptionalLong(diskHighInBytes);
        out.writeOptionalLong(floodStageInBytes);
        //
        out.writeOptionalDouble(diskLowInPct);
        out.writeOptionalDouble(diskHighInPct);
        out.writeOptionalDouble(floodStageInPct);
    }

    public Boolean getThresholdEnabled() {
        return thresholdEnabled;
    }

    @Nullable
    public Long getDiskLowInBytes() {
        return diskLowInBytes;
    }

    @Nullable
    public Long getDiskHighInBytes() {
        return diskHighInBytes;
    }

    @Nullable
    public Long getFloodStageInBytes() {
        return floodStageInBytes;
    }

    @Nullable
    public Double getDiskLowInPct() {
        return diskLowInPct;
    }

    @Nullable
    public Double getDiskHighInPct() {
        return diskHighInPct;
    }

    @Nullable
    public Double getFloodStageInPct() {
        return floodStageInPct;
    }
}
