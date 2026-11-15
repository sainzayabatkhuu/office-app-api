package com.sol.office.report.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

@ConfigurationProperties(prefix = "office.report")
public record ReportProperties(@DefaultValue Storage storage, @DefaultValue Execution execution) {

    public record Storage(@DefaultValue("./data/report-storage") String baseDir) {
    }

    public record Execution(
            /* Max concurrent report fills per instance; Jasper is CPU and memory heavy. */
            @DefaultValue("4") int maxConcurrent,
            /* How long a SYNC request waits for a free generation slot before it is rejected as busy. */
            @DefaultValue("120s") Duration syncTimeout,
            /* Jasper governor: abort fills running longer than this. */
            @DefaultValue("10m") Duration fillTimeout,
            /* Jasper governor: abort reports with more pages than this. */
            @DefaultValue("5000") int maxPages,
            @DefaultValue("30") int outputRetentionDays,
            /* PENDING/PROCESSING executions older than this are considered dead (instance crashed). */
            @DefaultValue("2h") Duration staleAfter) {
    }
}
