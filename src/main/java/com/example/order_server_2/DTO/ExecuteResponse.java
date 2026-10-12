package com.example.order_server_2.DTO;


public class ExecuteResponse {

    private String status;
    private String output;
    private String error;
    private String server;
    private long executionTimeMs;

    public ExecuteResponse() {
    }

    public ExecuteResponse(
            String status,
            String output,
            String error,
            String server,
            long executionTimeMs
    ) {
        this.status = status;
        this.output = output;
        this.error = error;
        this.server = server;
        this.executionTimeMs = executionTimeMs;
    }

    public String getStatus() {
        return status;
    }

    public String getOutput() {
        return output;
    }

    public String getError() {
        return error;
    }

    public String getServer() {
        return server;
    }

    public long getExecutionTimeMs() {
        return executionTimeMs;
    }
}