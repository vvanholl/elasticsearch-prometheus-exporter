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

import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.io.stream.Writeable;
import org.elasticsearch.common.metrics.CounterMetric;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PrometheusSnapshotLifecycleStats implements Writeable {
    private final CounterMetric retentionRunCount = new CounterMetric();
    private final CounterMetric retentionFailedCount = new CounterMetric();
    private final CounterMetric retentionTimedOut = new CounterMetric();
    private final CounterMetric retentionTimeMs = new CounterMetric();
    private final Map<String, PrometheusSnapshotPolicyStats> policyStats;

    public PrometheusSnapshotLifecycleStats(StreamInput in) throws IOException {
        this.policyStats = new ConcurrentHashMap<>(in.readMap(StreamInput::readString, PrometheusSnapshotPolicyStats::new));
        this.retentionRunCount.inc(in.readVLong());
        this.retentionFailedCount.inc(in.readVLong());
        this.retentionTimedOut.inc(in.readVLong());
        this.retentionTimeMs.inc(in.readVLong());
    }

    public CounterMetric getRetentionRunCount() {
        return retentionRunCount;
    }

    public CounterMetric getRetentionFailedCount() {
        return retentionFailedCount;
    }

    public CounterMetric getRetentionTimedOut() {
        return retentionTimedOut;
    }

    public CounterMetric getRetentionTimeMs() {
        return retentionTimeMs;
    }

    public Map<String, PrometheusSnapshotPolicyStats> getPolicyStats() {
        return policyStats;
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        out.writeMap(policyStats, StreamOutput::writeString, (v, o) -> o.writeTo(v));
        out.writeVLong(retentionRunCount.count());
        out.writeVLong(retentionFailedCount.count());
        out.writeVLong(retentionTimedOut.count());
        out.writeVLong(retentionTimeMs.count());
    }

    public static class PrometheusSnapshotPolicyStats implements Writeable {
        private final String policyId;
        private final CounterMetric snapshotsTaken = new CounterMetric();
        private final CounterMetric snapshotsFailed = new CounterMetric();
        private final CounterMetric snapshotsDeleted = new CounterMetric();
        private final CounterMetric snapshotDeleteFailures = new CounterMetric();

        public PrometheusSnapshotPolicyStats(StreamInput in) throws IOException {
            this.policyId = in.readString();
            this.snapshotsTaken.inc(in.readVLong());
            this.snapshotsFailed.inc(in.readVLong());
            this.snapshotsDeleted.inc(in.readVLong());
            this.snapshotDeleteFailures.inc(in.readVLong());
        }

        public String getPolicyId() {
            return policyId;
        }

        public CounterMetric getSnapshotsTaken() {
            return snapshotsTaken;
        }

        public CounterMetric getSnapshotsFailed() {
            return snapshotsFailed;
        }

        public CounterMetric getSnapshotsDeleted() {
            return snapshotsDeleted;
        }

        public CounterMetric getSnapshotDeleteFailures() {
            return snapshotDeleteFailures;
        }

        @Override
        public void writeTo(StreamOutput out) throws IOException {
            out.writeString(policyId);
            out.writeVLong(snapshotsTaken.count());
            out.writeVLong(snapshotsFailed.count());
            out.writeVLong(snapshotsDeleted.count());
            out.writeVLong(snapshotDeleteFailures.count());
        }
    }
}
