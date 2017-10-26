package org.elasticsearch.plugin.prometheus;

import org.elasticsearch.action.ActionModule;
import org.elasticsearch.action.NodePrometheusMetricsAction;
import org.elasticsearch.action.TransportNodePrometheusMetricsAction;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.rest.RestModule;
import org.elasticsearch.rest.action.prometheus.RestPrometheusMetricsAction;

public class PrometheusExporterPlugin extends Plugin {

    private final ESLogger logger = Loggers.getLogger(PrometheusExporterPlugin.class);

    public static final String NAME = "prometheus-exporter";

    @Inject
    public PrometheusExporterPlugin(Settings settings) {
        logger.info("starting Prometheus exporter plugin...");
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public String description() {
        return "Prometheus Exporter Plugin";
    }

    public void onModule(ActionModule actionModule) {
        actionModule.registerAction(NodePrometheusMetricsAction.INSTANCE, TransportNodePrometheusMetricsAction.class);
    }

    public void onModule(RestModule restModule) {
        restModule.addRestAction(RestPrometheusMetricsAction.class);
    }

}
