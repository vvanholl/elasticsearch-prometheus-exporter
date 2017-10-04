package org.compuscene.metrics.prometheus;

import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ParsePrometheusResponseTestCase {

    @Test
    public void parseResponse() throws FileNotFoundException {

        PrometheusResponse pr;
        try (Scanner scanner = new Scanner(
                new File(getClass().getClassLoader().getResource("prometheus_response_example.txt").getFile())
        )) {

            StringBuilder sb = new StringBuilder();
            while (scanner.hasNextLine()) {
                sb.append(scanner.nextLine()).append(System.getProperty("line.separator"));
            }
            pr = new PrometheusResponse(sb.toString());
        }

        assertEquals(68047,pr.getSource().length());

        Set<String> metrics = pr.getMetrics().keySet();
        assertFalse(metrics.isEmpty());

        String metricName;
        List<PrometheusResponse.Content> content;

        // es_cluster_nodes_number{cluster="SUITE-CHILD_VM=[0]-CLUSTER_SEED=[3225147025735939808]-HASH=[47716121841A0]-cluster",} 1.0
        metricName = "es_cluster_nodes_number";
        assertTrue(metrics.contains(metricName));
        assertEquals(1, pr.getMetric(metricName).size());

        content = pr.getMetric(metricName);
        assertEquals("1.0", content.get(0).value);
        assertEquals(1, content.get(0).labels.size());
        assertEquals("cluster", content.get(0).labels.get(0).key);
        assertEquals("\"SUITE-CHILD_VM=[0]-CLUSTER_SEED=[3225147025735939808]-HASH=[47716121841A0]-cluster\"", content.get(0).labels.get(0).value);

        // es_jvm_mem_pool_peak_used_bytes{cluster="SUITE-CHILD_VM=[0]-CLUSTER_SEED=[3225147025735939808]-HASH=[47716121841A0]-cluster",node="node_s0",nodeId="RYH2vYAvQr-L7NqqsCWaFA",pool="old",} 1.4223184E7
        // es_jvm_mem_pool_peak_used_bytes{cluster="SUITE-CHILD_VM=[0]-CLUSTER_SEED=[3225147025735939808]-HASH=[47716121841A0]-cluster",node="node_s0",nodeId="RYH2vYAvQr-L7NqqsCWaFA",pool="survivor",} 1.099272E7
        // es_jvm_mem_pool_peak_used_bytes{cluster="SUITE-CHILD_VM=[0]-CLUSTER_SEED=[3225147025735939808]-HASH=[47716121841A0]-cluster",node="node_s0",nodeId="RYH2vYAvQr-L7NqqsCWaFA",pool="young",} 8.4557808E7
        metricName = "es_jvm_mem_pool_peak_used_bytes";
        assertTrue(metrics.contains(metricName));
        assertEquals(3, pr.getMetric(metricName).size());

        content = pr.getMetric(metricName);
        assertEquals("1.4223184E7", content.get(0).value);
        assertEquals("1.099272E7", content.get(1).value);
        assertEquals("8.4557808E7", content.get(2).value);

        assertEquals(4, content.get(0).labels.size());
        assertEquals(4, content.get(1).labels.size());
        assertEquals(4, content.get(2).labels.size());

        for (int i : new Integer[]{0,1,2}) {
            PrometheusResponse.Content c = content.get(i);
            assertEquals("cluster", c.labels.get(0).key);
            assertEquals("\"SUITE-CHILD_VM=[0]-CLUSTER_SEED=[3225147025735939808]-HASH=[47716121841A0]-cluster\"", c.labels.get(0).value);
            assertEquals("node", c.labels.get(1).key);
            assertEquals("\"node_s0\"", c.labels.get(1).value);
            assertEquals("nodeId", c.labels.get(2).key);
            assertEquals("\"RYH2vYAvQr-L7NqqsCWaFA\"", c.labels.get(2).value);
            assertEquals("pool", c.labels.get(3).key);
            assertEquals(i == 0 ? "\"old\"" : i == 1 ? "\"survivor\"" : "\"young\"", c.labels.get(3).value);
        }
    }
}
