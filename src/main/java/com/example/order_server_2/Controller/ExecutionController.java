package com.example.order_server_2.Controller;

import com.example.order_server_2.DTO.ExecuteRequest;
import com.example.order_server_2.DTO.ExecuteResponse;
import com.example.order_server_2.Service.JavaExecutionService;
import org.springframework.web.bind.annotation.*;

@RestController
public class ExecutionController {

    private final JavaExecutionService executionService;

    public ExecutionController(JavaExecutionService executionService) {
        this.executionService = executionService;
    }

    @PostMapping("/execute")
    public ExecuteResponse execute(@RequestBody ExecuteRequest request) {
        return executionService.execute(request.getCode());
    }

    @GetMapping("/health")
    public String health() {
        return "UP";
    }
}
