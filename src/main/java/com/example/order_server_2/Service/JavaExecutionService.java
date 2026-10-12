package com.example.order_server_2.Service;

import com.example.order_server_2.DTO.ExecuteResponse;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

@Service
public class JavaExecutionService {

    private static final long COMPILE_TIMEOUT_SECONDS = 5;
    private static final long EXECUTION_TIMEOUT_SECONDS = 3;

    private static final String SERVER_NAME = "Server 2";

    public ExecuteResponse execute(String code) {

        long startTime = System.nanoTime();

        Path tempDir = null;

        try {
            // 1. Validate the submitted source code.
            if (code == null || code.isBlank()) {
                return response(
                        "ERROR",
                        "",
                        "Java code cannot be empty.",
                        startTime
                );
            }

            // Keep this first version simple:
            // submissions must define public class Main.
            if (!code.contains("public class Main")) {
                return response(
                        "ERROR",
                        "",
                        "Your code must contain public class Main.",
                        startTime
                );
            }

            // 2. Create a temporary directory for this execution.
            tempDir = Files.createTempDirectory("java-execution-");

            // 3. Write submitted code into Main.java.
            Path sourceFile = tempDir.resolve("Main.java");

            Files.writeString(
                    sourceFile,
                    code,
                    StandardCharsets.UTF_8
            );

            // 4. Compile Main.java.
            Path compileOutput = tempDir.resolve("compile-output.txt");

            ProcessBuilder compileBuilder = new ProcessBuilder(
                    "javac",
                    "-encoding",
                    "UTF-8",
                    "Main.java"
            );

            compileBuilder.directory(tempDir.toFile());

            compileBuilder.redirectErrorStream(true);
            compileBuilder.redirectOutput(compileOutput.toFile());

            Process compiler = compileBuilder.start();

            boolean compilationFinished = compiler.waitFor(
                    COMPILE_TIMEOUT_SECONDS,
                    TimeUnit.SECONDS
            );

            if (!compilationFinished) {
                compiler.destroyForcibly();
                compiler.waitFor();

                return response(
                        "TIMEOUT",
                        "",
                        "Compilation timed out.",
                        startTime
                );
            }

            String compilationMessage = readFile(compileOutput);

            if (compiler.exitValue() != 0) {
                return response(
                        "COMPILE_ERROR",
                        "",
                        compilationMessage,
                        startTime
                );
            }

            // 5. Execute the compiled Main class.
            Path executionOutput = tempDir.resolve("execution-output.txt");

            ProcessBuilder runBuilder = new ProcessBuilder(
                    "java",
                    "-cp",
                    tempDir.toAbsolutePath().toString(),
                    "Main"
            );

            runBuilder.directory(tempDir.toFile());

            runBuilder.redirectErrorStream(true);
            runBuilder.redirectOutput(executionOutput.toFile());

            Process program = runBuilder.start();

            boolean executionFinished = program.waitFor(
                    EXECUTION_TIMEOUT_SECONDS,
                    TimeUnit.SECONDS
            );

            if (!executionFinished) {
                program.destroyForcibly();
                program.waitFor();

                return response(
                        "TIMEOUT",
                        readFile(executionOutput),
                        "Program exceeded the execution time limit.",
                        startTime
                );
            }

            String output = readFile(executionOutput);

            if (program.exitValue() != 0) {
                return response(
                        "RUNTIME_ERROR",
                        output,
                        "Program exited with a non-zero exit code.",
                        startTime
                );
            }

            // 6. Return successful execution result.
            return response(
                    "SUCCESS",
                    output,
                    null,
                    startTime
            );

        } catch (InterruptedException exception) {

            Thread.currentThread().interrupt();

            return response(
                    "ERROR",
                    "",
                    "Execution was interrupted.",
                    startTime
            );

        } catch (IOException exception) {

            return response(
                    "ERROR",
                    "",
                    "Could not compile or execute Java code. "
                            + "Check that javac and java are installed "
                            + "and available on PATH.",
                    startTime
            );

        } finally {

            // 7. Remove temporary source and class files.
            if (tempDir != null) {
                deleteDirectory(tempDir);
            }
        }
    }

    private String readFile(Path path) throws IOException {

        if (!Files.exists(path)) {
            return "";
        }

        return Files.readString(path, StandardCharsets.UTF_8);
    }

    private ExecuteResponse response(
            String status,
            String output,
            String error,
            long startTime
    ) {
        long executionTimeMs = TimeUnit.NANOSECONDS.toMillis(
                System.nanoTime() - startTime
        );

        return new ExecuteResponse(
                status,
                output,
                error,
                SERVER_NAME,
                executionTimeMs
        );
    }

    private void deleteDirectory(Path directory) {

        try (var paths = Files.walk(directory)) {

            paths.sorted((a, b) -> b.compareTo(a))
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException ignored) {
                            // Cleanup failure should not hide the result.
                        }
                    });

        } catch (IOException ignored) {
            // Cleanup failure should not hide the result.
        }
    }
}