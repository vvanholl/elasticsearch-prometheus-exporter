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

package org.elasticsearch.plugin.prometheus;

import org.apache.logging.log4j.Logger;
import org.compuscene.metrics.prometheus.PrometheusMetricsCollector;
import org.elasticsearch.action.ActionRequest;
import org.elasticsearch.action.ActionResponse;
import org.elasticsearch.action.NodePrometheusMetricsAction;
import org.elasticsearch.action.TransportNodePrometheusMetricsAction;
import org.elasticsearch.cluster.metadata.IndexNameExpressionResolver;
import org.elasticsearch.cluster.node.DiscoveryNodes;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.settings.*;
import org.elasticsearch.plugins.ActionPlugin;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestHandler;
import org.elasticsearch.rest.prometheus.RestPrometheusMetricsAction;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

/**
 * Prometheus Exporter plugin main class.
 */
public class PrometheusExporterPlugin extends Plugin implements ActionPlugin {
    private static final Logger logger = Loggers.getLogger(PrometheusExporterPlugin.class);

    public PrometheusExporterPlugin() {
        logger.info("starting Prometheus exporter plugin");
    }

    @Override
    public List<ActionHandler<? extends ActionRequest, ? extends ActionResponse>> getActions() {
        return Arrays.asList(
                new ActionHandler<>(NodePrometheusMetricsAction.INSTANCE, TransportNodePrometheusMetricsAction.class)
        );
    }

    @Override
    public List<RestHandler> getRestHandlers(Settings settings, RestController restController, ClusterSettings clusterSettings,
                                             IndexScopedSettings indexScopedSettings, SettingsFilter settingsFilter,
                                             IndexNameExpressionResolver indexNameExpressionResolver,
                                             Supplier<DiscoveryNodes> nodesInCluster) {
        return Arrays.asList(
                new RestPrometheusMetricsAction(settings, restController)
        );
    }

    @Override
    public List<Setting<?>> getSettings() {
        return Arrays.asList(
                PrometheusMetricsCollector.PROMETHEUS_INDICES
        );
    }
}
