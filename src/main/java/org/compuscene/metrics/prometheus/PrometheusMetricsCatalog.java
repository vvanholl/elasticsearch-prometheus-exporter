package org.compuscene.metrics.prometheus;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Gauge;
import io.prometheus.client.exporter.common.TextFormat;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;

public class PrometheusMetricsCatalog {

    private String cluster;
    private String metric_prefix;
    private HashMap metrics;
    private CollectorRegistry registry;

    public PrometheusMetricsCatalog(String cluster, String metric_prefix){
        this.cluster = cluster;
        this.metric_prefix = metric_prefix;
        this.metrics = new HashMap();
        this.registry = new CollectorRegistry();
    }

    public void registerGauge(String metric, String help, String... labels){
        String[] extended_labels = new String[labels.length +1];
        extended_labels[0]= "cluster";
        for (int i = 0; i < labels.length; i++) extended_labels[i + 1] = labels[i];

        Gauge gauge=Gauge.build().name(this.metric_prefix+metric).help(help).labelNames(extended_labels).register(this.registry);
        this.metrics.put(metric,gauge);
    }

    public void setGauge(String metric, double value, String... label_values){
        String[] extended_label_values = new String[label_values.length +1];
        extended_label_values[0]= this.cluster;
        for (int i = 0; i < label_values.length; i++) extended_label_values[i + 1] = label_values[i];

        Gauge gauge = (Gauge) this.metrics.get(metric);
        gauge.labels(extended_label_values).set(value);
    }

    public String toTextFormat() throws IOException{
        try{
            Writer writer = new StringWriter();
            TextFormat.write004(writer, this.registry.metricFamilySamples());
            return writer.toString();
        }
        catch(IOException e){
            throw e;
        }
    }
}
