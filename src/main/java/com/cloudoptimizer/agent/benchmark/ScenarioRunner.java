package com.cloudoptimizer.agent.benchmark;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Orchestrates all five benchmark scenarios and produces a structured suite report.
 *
 * <p>This is intentionally <b>Spring-free</b> so it can be invoked from:
 * <ul>
 *   <li>Unit tests (no application context needed)</li>
 *   <li>A main method / CLI entry point</li>
 *   <li>A future REST endpoint ({@code GET /api/benchmark/run})</li>
 * </ul>
 *
 * <p>Output is written to {@code artifacts/benchmark/} as both individual
 * scenario JSON files and an aggregated {@code suite-report.json}.
 */
public class ScenarioRunner {

    private static final Logger log = LoggerFactory.getLogger(ScenarioRunner.class);

    private static final List<Scenario> ALL_SCENARIOS = List.of(
            new RetryStormScenario(),
            new ThreadSaturationScenario(),
            new HeapOverprovisionedScenario(),
            new CpuThrottlingScenario(),
            new BurstTrafficScenario()
    );

    private final List<Scenario>   scenarios;
    private final ScenarioConfig   config;
    private final ObjectMapper     mapper;

    /** Runs all five scenarios with default config. */
    public ScenarioRunner() {
        this(ALL_SCENARIOS, ScenarioConfig.defaults());
    }

    /** Runs all five scenarios with the given config (useful for parameterised tests). */
    public ScenarioRunner(ScenarioConfig config) {
        this(ALL_SCENARIOS, config);
    }

    /** Full control — inject specific scenarios and config (used in unit tests). */
    public ScenarioRunner(List<Scenario> scenarios, ScenarioConfig config) {
        this.scenarios = List.copyOf(scenarios);
        this.config    = config;
        this.mapper    = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .enable(SerializationFeature.INDENT_OUTPUT);
    }

    // ── Run API ───────────────────────────────────────────────────────────────

    /**
     * Runs all configured scenarios and returns a suite report.
     * Never throws — individual scenario failures are captured in the report.
     */
    public SuiteReport runAll() {
        log.info("Starting benchmark suite: {} scenarios", scenarios.size());
        Instant start = Instant.now();

        List<ScenarioResult> results = scenarios.stream()
                .map(this::runSafe)
                .collect(Collectors.toList());

        SuiteReport report = SuiteReport.from(results, start);
        log.info("Benchmark suite complete: {}/{} passed, {}/{} exit-criteria met",
                report.getPassed(), results.size(),
                report.getExitCriteriaMet(), results.size());
        return report;
    }

    /**
     * Runs all scenarios and writes individual + aggregate JSON to
     * {@code artifacts/benchmark/}.
     */
    public SuiteReport runAllAndWrite(String artifactsBase) throws IOException {
        SuiteReport report = runAll();
        Path dir = Paths.get(artifactsBase, "benchmark");
        Files.createDirectories(dir);

        for (ScenarioResult r : report.getResults()) {
            Path out = dir.resolve(r.getScenarioName() + ".json");
            mapper.writeValue(out.toFile(), r);
            log.info("Written: {}", out);
        }

        Path suite = dir.resolve("suite-report.json");
        mapper.writeValue(suite.toFile(), report);
        log.info("Suite report written: {}", suite);
        return report;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private ScenarioResult runSafe(Scenario scenario) {
        try {
            log.info("Running scenario: {}", scenario.name());
            ScenarioResult result = scenario.run(config);
            log.info("Scenario {} — exit criteria met: {}", scenario.name(), result.isExitCriteriaMet());
            return result;
        } catch (Exception e) {
            log.error("Scenario {} failed: {}", scenario.name(), e.getMessage(), e);
            // Return a minimal failed result so the suite still completes
            return ScenarioResult.builder()
                    .scenarioName(scenario.name())
                    .scenarioDescription(scenario.description())
                    .ranAt(Instant.now())
                    .config(config)
                    .findings(List.of("ERROR: " + e.getMessage()))
                    .exitCriteriaMet(false)
                    .build();
        }
    }

    // ── Suite report ──────────────────────────────────────────────────────────

    /**
     * Aggregated result across all scenarios in a run.
     */
    public static class SuiteReport {

        @JsonProperty("ran_at")
        private final Instant ranAt;

        @JsonProperty("scenario_count")
        private final int scenarioCount;

        @JsonProperty("passed")
        private final long passed;

        @JsonProperty("exit_criteria_met")
        private final long exitCriteriaMet;

        @JsonProperty("all_exit_criteria_met")
        private final boolean allExitCriteriaMet;

        @JsonProperty("results")
        private final List<ScenarioResult> results;

        @JsonProperty("summary")
        private final Map<String, Boolean> summary;

        private SuiteReport(Instant ranAt, List<ScenarioResult> results) {
            this.ranAt              = ranAt;
            this.results            = List.copyOf(results);
            this.scenarioCount      = results.size();
            this.passed             = results.stream().filter(r -> r.getAmplification() != null).count();
            this.exitCriteriaMet    = results.stream().filter(ScenarioResult::isExitCriteriaMet).count();
            this.allExitCriteriaMet = exitCriteriaMet == scenarioCount;
            this.summary            = results.stream().collect(
                    Collectors.toMap(ScenarioResult::getScenarioName,
                                     ScenarioResult::isExitCriteriaMet));
        }

        static SuiteReport from(List<ScenarioResult> results, Instant start) {
            return new SuiteReport(start, results);
        }

        public Instant getRanAt()              { return ranAt; }
        public int     getScenarioCount()      { return scenarioCount; }
        public long    getPassed()             { return passed; }
        public long    getExitCriteriaMet()    { return exitCriteriaMet; }
        public boolean isAllExitCriteriaMet()  { return allExitCriteriaMet; }
        public List<ScenarioResult> getResults() { return results; }
        public Map<String, Boolean> getSummary() { return summary; }
    }
}