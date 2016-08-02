package org.compuscene.metrics.prometheus;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;
import io.prometheus.client.exporter.common.TextFormat;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.ESLoggerFactory;
import org.elasticsearch.rest.prometheus.RestPrometheusMetricsAction;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;

public class PrometheusMetricsCatalog {
    private final static ESLogger logger = ESLoggerFactory.getLogger(RestPrometheusMetricsAction.class.getSimpleName());

    private String cluster;
    private String metric_prefix;
    private HashMap metrics;
    private CollectorRegistry registry;

    public PrometheusMetricsCatalog(String cluster, String metric_prefix) {
        this.cluster = cluster;
        this.metric_prefix = metric_prefix;
        this.metrics = new HashMap();
        this.registry = new CollectorRegistry();
    }

    public void registerGauge(String metric, String help, String... labels) {
        String[] extended_labels = new String[labels.length + 1];
        extended_labels[0] = "cluster";
        for (int i = 0; i < labels.length; i++) extended_labels[i + 1] = labels[i];

        Gauge gauge = Gauge.build().name(this.metric_prefix + metric).help(help).labelNames(extended_labels).register(this.registry);
        this.metrics.put(metric, gauge);

        logger.debug(String.format("Registered new gauge %s", metric));
    }

    public void setGauge(String metric, double value, String... label_values) {
        String[] extended_label_values = new String[label_values.length + 1];
        extended_label_values[0] = this.cluster;
        for (int i = 0; i < label_values.length; i++) extended_label_values[i + 1] = label_values[i];

        Gauge gauge = (Gauge) this.metrics.get(metric);
        gauge.labels(extended_label_values).set(value);
    }

    public void registerCounter(String metric, String help, String... labels) {
        String[] extended_labels = new String[labels.length + 1];
        extended_labels[0] = "cluster";
        for (int i = 0; i < labels.length; i++) extended_labels[i + 1] = labels[i];

        Counter counter = Counter.build().name(this.metric_prefix + metric).help(help).labelNames(extended_labels).register(this.registry);
        this.metrics.put(metric, counter);

        logger.debug(String.format("Registered new counter %s", metric));
    }

    public void setCounter(String metric, double value, String... label_values) {
        String[] extended_label_values = new String[label_values.length + 1];
        extended_label_values[0] = this.cluster;
        for (int i = 0; i < label_values.length; i++) extended_label_values[i + 1] = label_values[i];

        Counter counter = (Counter) this.metrics.get(metric);

        double increment = value - counter.labels(extended_label_values).get();

        if (increment >= 0) {
            counter.labels(extended_label_values).inc(increment);
        } else {
            logger.error(String.format("Can not increment metric %s with value %f", metric, increment));
        }
    }

    public String toTextFormat() throws IOException {
        try {
            Writer writer = new StringWriter();
            TextFormat.write004(writer, this.registry.metricFamilySamples());
            return writer.toString();
        } catch (IOException e) {
            throw e;
        }
    }
}
