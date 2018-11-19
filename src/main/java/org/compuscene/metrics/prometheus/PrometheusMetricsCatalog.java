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

package org.compuscene.metrics.prometheus;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.rest.prometheus.RestPrometheusMetricsAction;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Locale;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Gauge;
import io.prometheus.client.Summary;
import io.prometheus.client.exporter.common.TextFormat;

/**
 * A class that describes a Prometheus metrics catalog.
 */
public class PrometheusMetricsCatalog {
    private static final Logger logger = LogManager.getLogger(RestPrometheusMetricsAction.class);

    private String clusterName;
    private String nodeName;
    private String nodeId;

    private String metricPrefix;

    private HashMap<String, Object> metrics;
    private CollectorRegistry registry;

    public PrometheusMetricsCatalog(String clusterName, String nodeName, String nodeId, String metricPrefix) {
        this.clusterName = clusterName;
        this.nodeName = nodeName;
        this.nodeId = nodeId;

        this.metricPrefix = metricPrefix;

        metrics = new HashMap<>();
        registry = new CollectorRegistry();
    }

    private String[] getExtendedClusterLabelNames(String... labelNames) {
        String[] extended = new String[labelNames.length + 1];
        extended[0] = "cluster";

        System.arraycopy(labelNames, 0, extended, 1, labelNames.length);

        return extended;
    }

    private String[] getExtendedClusterLabelValues(String... labelValues) {
        String[] extended = new String[labelValues.length + 1];
        extended[0] = clusterName;

        System.arraycopy(labelValues, 0, extended, 1, labelValues.length);

        return extended;
    }

    private String[] getExtendedNodeLabelNames(String... labelNames) {
        String[] extended = new String[labelNames.length + 3];
        extended[0] = "cluster";
        extended[1] = "node";
        extended[2] = "nodeid";

        System.arraycopy(labelNames, 0, extended, 3, labelNames.length);

        return extended;
    }

    private String[] getExtendedNodeLabelValues(String... labelValues) {
        String[] extended = new String[labelValues.length + 3];
        extended[0] = clusterName;
        extended[1] = nodeName;
        extended[2] = nodeId;

        System.arraycopy(labelValues, 0, extended, 3, labelValues.length);

        return extended;
    }

    public void registerClusterGauge(String metric, String help, String... labels) {
        Gauge gauge = Gauge.build().
                name(metricPrefix + metric).
                help(help).
                labelNames(getExtendedClusterLabelNames(labels)).
                register(registry);

        metrics.put(metric, gauge);

        logger.debug(String.format(Locale.ENGLISH, "Registered new cluster gauge %s", metric));
    }

    public void setClusterGauge(String metric, double value, String... labelValues) {
        Gauge gauge = (Gauge) metrics.get(metric);
        gauge.labels(getExtendedClusterLabelValues(labelValues)).set(value);
    }

    public void registerNodeGauge(String metric, String help, String... labels) {
        Gauge gauge = Gauge.build().
                name(metricPrefix + metric).
                help(help).
                labelNames(getExtendedNodeLabelNames(labels)).
                register(registry);

        metrics.put(metric, gauge);

        logger.debug(String.format(Locale.ENGLISH, "Registered new node gauge %s", metric));
    }

    public void setNodeGauge(String metric, double value, String... labelValues) {
        Gauge gauge = (Gauge) metrics.get(metric);
        gauge.labels(getExtendedNodeLabelValues(labelValues)).set(value);
    }

    public void registerSummaryTimer(String metric, String help, String... labels) {
        Summary summary = Summary.build().
                name(metricPrefix + metric).
                help(help).
                labelNames(getExtendedNodeLabelNames(labels)).
                register(registry);

        metrics.put(metric, summary);

        logger.debug(String.format(Locale.ENGLISH, "Registered new summary %s", metric));
    }

    public Summary.Timer startSummaryTimer(String metric, String... labelValues) {
        Summary summary = (Summary) metrics.get(metric);
        return summary.labels(getExtendedNodeLabelValues(labelValues)).startTimer();
    }

    public String toTextFormat() throws IOException {
        Writer writer = new StringWriter();
        TextFormat.write004(writer, registry.metricFamilySamples());
        return writer.toString();
    }
}
