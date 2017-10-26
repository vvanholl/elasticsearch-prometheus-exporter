package org.elasticsearch.rest.action.prometheus;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.elasticsearch.action.admin.cluster.node.info.NodesInfoResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Requests;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.plugin.prometheus.PrometheusExporterPlugin;
import org.elasticsearch.plugin.prometheus.PrometheusExporterPluginIntegTestCase;
import org.elasticsearch.test.ESIntegTestCase;
import org.junit.Test;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Iterator;

import static org.elasticsearch.test.ESIntegTestCase.Scope.TEST;
import static org.elasticsearch.test.hamcrest.ElasticsearchAssertions.assertHitCount;

@ESIntegTestCase.ClusterScope(scope=TEST, numDataNodes=2, numClientNodes=1, transportClientRatio=-1)
public class ASmokeElasticsearchIntegTestCase extends PrometheusExporterPluginIntegTestCase {

    @Test
    public void clusterShouldBeEmpty() throws Exception {
        ensureGreen();
        SearchResponse searchResponse = client().prepareSearch().setQuery(QueryBuilders.matchAllQuery()).get();
        assertHitCount(searchResponse, 0);
    }

    @Test
    public void prometheusPluginShouldBeInstalledOnEachNode() throws IOException {
        ensureGreen();
        int expectedNumberOfPrometheusPlugins = getNumberOfClusterNodes();
        int numberOfPrometheusPlugins = 0;

        NodesInfoResponse response = client().admin().cluster().nodesInfo(
                Requests.nodesInfoRequest().clear().plugins(true)
        ).actionGet();

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode = objectMapper.readTree(response.toString());
        JsonNode nodes = rootNode.path(NODES);
        Iterator<String> nodeIDs = nodes.fieldNames();

        // make sure there is at least one node
        assertTrue(nodeIDs.hasNext());

        while (nodeIDs.hasNext()) {
            String nodeID = nodeIDs.next();
            Iterator<JsonNode> plugins = nodes.path(nodeID).path(PLUGINS).elements();
            // make sure each node has some plugins installed
            assertTrue(plugins.hasNext());
            while (plugins.hasNext()) {
                JsonNode plugin = plugins.next();
                // iterate util first prometheus plugin is found
                if (plugin.path(NAME).asText().equals(PrometheusExporterPlugin.NAME)) {
                    numberOfPrometheusPlugins++;
                    // prevent incorrect counting in case there would be more plugins installed on single node by error
                    break;
                }
            }
        }

        // each node is expected to have this plugin installed (no matter if it is client or data type)
        assertEquals(expectedNumberOfPrometheusPlugins, numberOfPrometheusPlugins);
    }

    @Test
    public void eachNodeCanRespondToPrometheusRESTCall() throws IOException {
        ensureGreen();
        NodesInfoResponse nodeInfo = client().admin().cluster().nodesInfo(
                Requests.nodesInfoRequest().clear()
        ).actionGet();

        logger.info("Cluster nodes info: \n{}", nodeInfo.toString());

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode = objectMapper.readTree(nodeInfo.toString());
        JsonNode nodes = rootNode.path(NODES);
        Iterator<String> nodeIDs = nodes.fieldNames();

        assertTrue(nodeIDs.hasNext());

        while (nodeIDs.hasNext()) {
            String nodeID = nodeIDs.next();
            String httpAddress = nodes.path(nodeID).path(HTTP_ADDRESS).asText();

            String nodeUrl = "http://" + httpAddress + RestPrometheusMetricsAction.PLUGIN_REST_ENDPOINT;
            logger.info("Hitting node: {}", nodeUrl);

            URL url = new java.net.URL(nodeUrl);
            HttpURLConnection http = (HttpURLConnection)url.openConnection();
            http.setRequestMethod("GET");
            http.connect();

            int responseCode = http.getResponseCode();
            String requestMethod = http.getRequestMethod();
            logger.info(" - response method: {}, code: {}", requestMethod, responseCode);

            assertEquals(HttpURLConnection.HTTP_OK, responseCode);
            assertEquals("GET", requestMethod);

            http.disconnect();
        }
    }
}
