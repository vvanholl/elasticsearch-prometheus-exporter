package org.elasticsearch.rest.action.prometheus;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.compuscene.metrics.prometheus.PrometheusResponse;
import org.elasticsearch.action.admin.cluster.node.info.NodesInfoResponse;
import org.elasticsearch.client.Requests;
import org.elasticsearch.plugin.prometheus.PrometheusExporterPluginIntegTestCase;
import org.elasticsearch.test.ESIntegTestCase;
import org.junit.Test;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import static org.elasticsearch.test.ESIntegTestCase.Scope.TEST;

@ESIntegTestCase.ClusterScope(scope=TEST, numDataNodes=2, numClientNodes=1, transportClientRatio=-1)
public class PrometheusResponseIntegTestCase extends PrometheusExporterPluginIntegTestCase {

    @Test
    public void eachNodeShouldReturnLocalNodeStats() throws IOException {
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

            StringBuilder sb = new StringBuilder();
            try (java.util.Scanner s = new java.util.Scanner(new java.net.URL(nodeUrl).openStream())) {
                sb.append(s.useDelimiter("\\A").next());
            }

            PrometheusResponse pr = new PrometheusResponse(sb.toString());

            // See the @ClusterScope annotation, the must match
            assertEquals("3.0", pr.getMetric("es_cluster_nodes_number").get(0).value);
            assertEquals("2.0", pr.getMetric("es_cluster_datanodes_number").get(0).value);

            // Now take some metric that is node specific
            assertEquals("0.0", pr.getMetric("es_indices_doc_number").get(0).value);
            List<PrometheusResponse.Label> labels = pr.getMetric("es_indices_doc_number").get(0).labels;
            assertEquals(3, labels.size());
            for (int i : new Integer[]{0,1,2}) {
                String key = labels.get(i).key;
                String value = labels.get(i).value;
                switch (i) {
                    case 0: assertEquals("cluster", key);
                            //assertEquals("_generated_", value);
                            break;
                    case 1: assertEquals("node", key);
                            //assertEquals("_generated_", value);
                            break;
                    case 2: assertEquals("nodeId", key);
                            // Here we verify the metrics was taken from the node which received the http request
                            assertEquals("\""+nodeID+"\"", value);
                            break;
                }
            }
        }
    }
}
