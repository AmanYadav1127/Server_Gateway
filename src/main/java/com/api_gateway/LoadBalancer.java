package com.api_gateway;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
public class LoadBalancer {

    private final List<String> servers = List.of(
            "http://localhost:9001",
            "http://localhost:9002"
    );

    private int currentServer = 0;

    private final RestClient restClient;

    public LoadBalancer() {
        this.restClient = RestClient.create();
    }

    public synchronized String getNextServer() {

        int checkedServers = 0;

        while (checkedServers < servers.size()) {

            String server = servers.get(currentServer);

            currentServer = (currentServer + 1) % servers.size();

            checkedServers++;

            if (isHealthy(server)) {
                System.out.println("Selected server: " + server);
                return server;
            }
        }

        throw new RuntimeException("No healthy servers available");
    }

    private boolean isHealthy(String server) {

        try {

            String response = restClient
                    .get()
                    .uri(server + "/health")
                    .retrieve()
                    .body(String.class);

            boolean healthy = response != null
                    && response.trim().equalsIgnoreCase("UP");

            System.out.println(
                    "Health check: " + server
                            + " -> " + response
                            + " (healthy=" + healthy + ")"
            );

            return healthy;

        } catch (Exception e) {

            System.err.println(
                    "Health check failed for " + server
                            + ": " + e.getMessage()
            );

            return false;
        }
    }
}