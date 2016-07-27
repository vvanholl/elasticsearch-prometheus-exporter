package org.elasticsearch.plugin.prometheus;

import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.rest.RestModule;
import org.elasticsearch.rest.prometheus.RestPrometheusMetricsAction;

public class PrometheusExporterPlugin extends Plugin {

    private Settings settings;
    private final ESLogger logger = Loggers.getLogger(PrometheusExporterPlugin.class);

    @Inject
    public PrometheusExporterPlugin(Settings settings) {
        this.settings = settings;
        logger.info("starting Prometheus exporter plugin...");
    }

    @Override
    public String name() {
        return "prometheus-exporter";
    }

    @Override
    public String description() {
        return "Prometheus Exporter Plugin";
    }

    public void onModule(RestModule module) {
        module.addRestAction(RestPrometheusMetricsAction.class);
    }
}
