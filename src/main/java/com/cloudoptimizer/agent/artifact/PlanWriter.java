package com.cloudoptimizer.agent.artifact;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Serializes an {@link OptimizationPlan} to the {@code artifacts/} directory in
 * both JSON and YAML formats.
 *
 * <p>This is the only class responsible for plan I/O.  Callers hand in a plan
 * and a target directory; the writer handles the rest.
 *
 * <p>Output files:
 * <ul>
 *   <li>{@code <dir>/optimization_plan.json}</li>
 *   <li>{@code <dir>/optimization_plan.yaml}</li>
 * </ul>
 */
@Service
public class PlanWriter {

    private static final Logger log = LoggerFactory.getLogger(PlanWriter.class);

    static final String JSON_FILENAME = "optimization_plan.json";
    static final String YAML_FILENAME = "optimization_plan.yaml";

    private final ObjectMapper jsonMapper;
    private final ObjectMapper yamlMapper;

    public PlanWriter() {
        this.jsonMapper = buildJsonMapper();
        this.yamlMapper = buildYamlMapper();
    }

    /**
     * Writes {@code plan} to {@code targetDir} as both JSON and YAML.
     * Creates {@code targetDir} if it does not exist.
     *
     * @param plan      the plan to persist
     * @param targetDir directory in which to write the output files
     * @throws IOException if any file I/O fails
     */
    public void write(OptimizationPlan plan, Path targetDir) throws IOException {
        ensureDir(targetDir);
        writeJson(plan, targetDir.resolve(JSON_FILENAME));
        writeYaml(plan, targetDir.resolve(YAML_FILENAME));
        log.info("OptimizationPlan {} written to {}",
                plan.getMetadata().getPlanId(), targetDir.toAbsolutePath());
    }

    /**
     * Writes only the JSON representation.
     * Useful in tests that don't want to deal with YAML.
     */
    public void writeJson(OptimizationPlan plan, Path targetFile) throws IOException {
        jsonMapper.writeValue(targetFile.toFile(), plan);
        log.debug("Written JSON plan: {}", targetFile);
    }

    /**
     * Writes only the YAML representation.
     */
    public void writeYaml(OptimizationPlan plan, Path targetFile) throws IOException {
        yamlMapper.writeValue(targetFile.toFile(), plan);
        log.debug("Written YAML plan: {}", targetFile);
    }

    /**
     * Reads a plan back from a JSON file.  Used in tests and future tooling.
     */
    public OptimizationPlan readJson(Path jsonFile) throws IOException {
        return jsonMapper.readValue(jsonFile.toFile(), OptimizationPlan.class);
    }

    /**
     * Reads a plan back from a YAML file.  Used in tests and future tooling.
     */
    public OptimizationPlan readYaml(Path yamlFile) throws IOException {
        return yamlMapper.readValue(yamlFile.toFile(), OptimizationPlan.class);
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    private static ObjectMapper buildJsonMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .enable(SerializationFeature.INDENT_OUTPUT);
    }

    private static ObjectMapper buildYamlMapper() {
        YAMLFactory factory = YAMLFactory.builder()
                .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
                .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES)
                .build();
        return new ObjectMapper(factory)
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    private static void ensureDir(Path dir) throws IOException {
        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
        }
    }
}