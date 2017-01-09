package org.elasticsearch.plugin.prometheus;

import org.apache.logging.log4j.Logger;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.index.IndexModule;
import org.elasticsearch.plugins.ActionPlugin;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.rest.RestHandler;
import org.elasticsearch.rest.prometheus.RestPrometheusMetricsAction;

import java.util.List;

import static java.util.Collections.singletonList;

public class PrometheusExporterPlugin extends Plugin implements ActionPlugin {

    private final Logger logger = Loggers.getLogger(PrometheusExporterPlugin.class);

    @Inject
    public PrometheusExporterPlugin(Settings settings) {
        logger.info("starting Prometheus exporter plugin...");
    }

    @Override
    public void onIndexModule(IndexModule indexModule) {
        super.onIndexModule(indexModule);
    }

    @Override
    public List<Class<? extends RestHandler>> getRestHandlers() {
        return singletonList(RestPrometheusMetricsAction.class);
    }

}
