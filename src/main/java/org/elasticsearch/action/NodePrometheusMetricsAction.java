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

import org.elasticsearch.common.io.stream.Writeable;

/**
 * Action class for Prometheus Exporter plugin.
 */
public class NodePrometheusMetricsAction extends ActionType<NodePrometheusMetricsResponse> {
    // TODO(lukas-vlcek): There are ongoing changes for ActionType class. This code needs additional review.
    // - https://github.com/elastic/elasticsearch/pull/43778
    // - https://github.com/elastic/elasticsearch/commit/b33ffc1ae06035e934277f17c4b5d9851f607056#diff-80df90ca727aadbbe854902f81bda313
    // - https://github.com/elastic/elasticsearch/commit/5a9f81791a1be7fe6dd97728384ebafb189ab211#diff-80df90ca727aadbbe854902f81bda313
    public static final NodePrometheusMetricsAction INSTANCE = new NodePrometheusMetricsAction();
    public static final String NAME = "cluster:monitor/prometheus/metrics";

    private NodePrometheusMetricsAction() {
        super(NAME, null);
    }

    @Override
    public Writeable.Reader<NodePrometheusMetricsResponse> getResponseReader() {
        return NodePrometheusMetricsResponse::new;
    }
}
