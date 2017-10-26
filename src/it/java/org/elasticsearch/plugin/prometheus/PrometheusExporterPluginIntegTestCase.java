package org.elasticsearch.plugin.prometheus;

import java.io.IOException;
import java.util.Collection;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.elasticsearch.client.Requests;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.test.ESIntegTestCase;

public abstract class PrometheusExporterPluginIntegTestCase extends ESIntegTestCase {

    protected static final String PLUGINS = "plugins";
    protected static final String NODES = "nodes";
    protected static final String NAME = "name";
    protected static final String HTTP_ADDRESS = "http_address";

    @Override
    protected Collection<Class<? extends Plugin>> nodePlugins() {
        return pluginList(org.elasticsearch.plugin.prometheus.PrometheusExporterPlugin.class);
    }

    protected Settings nodeSettings(int nodeOrdinal) {
        Settings settings = Settings.builder()
                .put(super.nodeSettings(nodeOrdinal))
                .put("http.enabled", true)
                .build();
        return settings;
    }

    /**
     * Get number of nodes using cluster health request.
     * @return number of nodes
     * @throws IOException
     */
    protected int getNumberOfClusterNodes() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode = objectMapper.readTree(
                client().admin().cluster().health(
                        Requests.clusterHealthRequest()
                ).actionGet().toString()
        );
        JsonNode count = rootNode.path("number_of_nodes");
        return count.asInt();
    }
}
