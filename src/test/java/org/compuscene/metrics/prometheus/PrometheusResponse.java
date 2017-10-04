package org.compuscene.metrics.prometheus;

import java.util.*;

/**
 * This class can parse Prometheus format for the needs of integration tests.
 * Unfortunately, as of writing there is no official parser in Java.
 */
public class PrometheusResponse {

    public class Label {
        public final String key;
        public final String value;
        public Label (String key, String value) {
            this.key = key;
            this.value = value;
        }
    }

    public class Content {
        public final List<Label> labels;
        public final String value;
        public Content(String value, List<Label> labels) {
            this.value = value;
            this.labels = labels;
        }
    }

    private final String source;

    private Map<String, List<Content>> metrics = new HashMap<>();

    public PrometheusResponse(String source) {
        this.source = source;

        Scanner scanner = new Scanner(this.source);
        while (scanner.hasNextLine()) {

            String line = scanner.nextLine();
            if (line.startsWith("#"))
                continue;

            int first = line.indexOf("{");
            String metric = line.substring(0, first);
            String rest = line.substring(first);

            int last = rest.lastIndexOf("}");
            String value = rest.substring(last+1).trim();

            String labelsToParse = rest.substring(1, last);
            List<Label> labels = new ArrayList<>();
            for (String item: labelsToParse.split(",")) {
                item = item.trim();
                if (item.length() > 0 && item.indexOf("=") > 0) {
                    String key = item.substring(0, item.indexOf("="));
                    String val = item.substring(item.indexOf("=")+1);
                    labels.add(new Label(key, val));
                }
            }

            Content content = new Content(value, labels);
            if (metrics.keySet().contains(metric)) {
                metrics.get(metric).add(content);
            } else {
                List<Content> values = new ArrayList<>();
                metrics.put(metric, values);
                values.add(content);
            }

        }

        scanner.close();
    }

    public String getSource() {
        return source;
    }

    public Map<String, List<Content>> getMetrics() {
        return metrics;
    }

    public List<Content> getMetric(String metric) {
        return getMetrics().get(metric);
    }
}
