package com.api_gateway.Controller;

import com.api_gateway.CircuitBreaker;
import com.api_gateway.LoadBalancer;

import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

@RestController
public class GatewayController {

    private final RestClient restClient;
    private final LoadBalancer loadBalancer;

    private final CircuitBreaker server1Breaker =
            new CircuitBreaker();

    private final CircuitBreaker server2Breaker =
            new CircuitBreaker();

    public GatewayController() {

        JdkClientHttpRequestFactory factory =
                new JdkClientHttpRequestFactory();

        factory.setReadTimeout(Duration.ofSeconds(5));

        this.restClient = RestClient.builder()
                .requestFactory(factory)
                .build();

        this.loadBalancer = new LoadBalancer();
    }

    // Gateway health check
    @GetMapping("/")
    public String gateway() {
        return "Java Execution Gateway is running";
    }

    // Execute Java code through the gateway
    @PostMapping("/execute")
    public ExecuteRequest executeCode(
            @RequestBody ExecuteRequest request) {

        Set<String> attemptedServers = new HashSet<>();

        for (int attempt = 0; attempt < 2; attempt++) {

            // Select the next backend using Round Robin
            String server = loadBalancer.getNextServer();

            // Never attempt the same backend twice in one request
            if (!attemptedServers.add(server)) {
                continue;
            }

            CircuitBreaker breaker = getBreaker(server);

            // Skip a backend if its circuit breaker is OPEN
            if (!breaker.allowRequest()) {
                continue;
            }

            try {

                // Forward Java code to the selected execution server
                ExecuteRequest response = restClient
                        .post()
                        .uri(server + "/execute")
                        .body(request)
                        .retrieve()
                        .body(ExecuteRequest.class);

                breaker.recordSuccess();

                return response;

            } catch (RestClientException exception) {

                // Record communication/HTTP failure
                breaker.recordFailure();
            }
        }

        throw new IllegalStateException(
                "No available execution server could process the request."
        );
    }

    // Select the circuit breaker belonging to the backend
    private CircuitBreaker getBreaker(String server) {

        if (server.contains(":9001")) {
            return server1Breaker;
        }

        if (server.contains(":9002")) {
            return server2Breaker;
        }

        throw new IllegalArgumentException(
                "Unknown server: " + server
        );
    }

    // Request DTO received from Postman or the frontend
    public static class ExecuteRequest {

        private String status;
        private String output;
        private String error;
        private String server;
        private long executionTimeMs;

        public ExecuteRequest() {
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getOutput() {
            return output;
        }

        public void setOutput(String output) {
            this.output = output;
        }

        public String getError() {
            return error;
        }

        public void setError(String error) {
            this.error = error;
        }

        public String getServer() {
            return server;
        }

        public void setServer(String server) {
            this.server = server;
        }

        public long getExecutionTimeMs() {
            return executionTimeMs;
        }

        public void setExecutionTimeMs(long executionTimeMs) {
            this.executionTimeMs = executionTimeMs;
        }

        // Send only the submitted code to the backend.
        // The other fields are populated by the backend response.
        private String code;

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }
    }
}