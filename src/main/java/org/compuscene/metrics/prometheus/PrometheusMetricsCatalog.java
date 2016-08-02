package org.compuscene.metrics.prometheus;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;
import io.prometheus.client.Summary;
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
        metrics = new HashMap();
        registry = new CollectorRegistry();
    }

    private String[] getExtendedLabelNames(String... label_names) {
        String[] extended = new String[label_names.length + 1];
        extended[0] = "cluster";

        System.arraycopy(label_names, 0, extended, 1, label_names.length);

        return extended;
    }

    private String[] getExtendedLabelValues(String... label_values) {
        String[] extended = new String[label_values.length + 1];
        extended[0] = cluster;

        System.arraycopy(label_values, 0, extended, 1, label_values.length);

        return extended;
    }

    public void registerGauge(String metric, String help, String... labels) {
        Gauge gauge = Gauge.build().
                name(metric_prefix + metric).
                help(help).
                labelNames(getExtendedLabelNames(labels)).
                register(registry);

        metrics.put(metric, gauge);

        logger.debug(String.format("Registered new gauge %s", metric));
    }

    public void setGauge(String metric, double value, String... label_values) {
        Gauge gauge = (Gauge) metrics.get(metric);
        gauge.labels(getExtendedLabelValues(label_values)).set(value);
    }

    public void registerCounter(String metric, String help, String... labels) {
        Counter counter = Counter.build().
                name(metric_prefix + metric).
                help(help).
                labelNames(getExtendedLabelNames(labels)).
                register(registry);

        metrics.put(metric, counter);

        logger.debug(String.format("Registered new counter %s", metric));
    }

    public void setCounter(String metric, double value, String... label_values) {
        String[] extended_label_values = getExtendedLabelValues(label_values);
        Counter counter = (Counter) metrics.get(metric);

        double increment = value - counter.labels(extended_label_values).get();

        if (increment >= 0) {
            counter.labels(extended_label_values).inc(increment);
        } else {
            logger.error(String.format("Can not increment metric %s with value %f, skipping", metric, increment));
        }
    }

    public void registerSummaryTimer(String metric, String help, String... labels) {
        Summary summary = Summary.build().
                name(metric_prefix + metric).
                help(help).
                labelNames(getExtendedLabelNames(labels)).
                register(registry);

        metrics.put(metric, summary);

        logger.debug(String.format("Registered new summary %s", metric));
    }

    public Summary.Timer startSummaryTimer(String metric, String... label_values) {
        Summary summary = (Summary) metrics.get(metric);
        return summary.labels(getExtendedLabelValues(label_values)).startTimer();
    }

    public String toTextFormat() throws IOException {
        try {
            Writer writer = new StringWriter();
            TextFormat.write004(writer, registry.metricFamilySamples());
            return writer.toString();
        } catch (IOException e) {
            throw e;
        }
    }
}
