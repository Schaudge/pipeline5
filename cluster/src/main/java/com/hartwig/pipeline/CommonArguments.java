package com.hartwig.pipeline;

import org.apache.commons.cli.CommandLine;

import java.util.Optional;

public interface CommonArguments {

    String PROJECT = "project";
    String REGION = "region";
    String LOCAL_SSDS = "local_ssds";
    String POLL_INTERVAL = "poll_interval";
    String PREEMPTIBLE_VMS = "preemptible_vms";
    String STORAGE_KEY_PATH = "storage_key_path";
    String SERVICE_ACCOUNT_EMAIL = "service_account_email";
    String CLOUD_SDK = "cloud_sdk";
    String PRIVATE_KEY_PATH = "private_key_path";

    String project();

    Optional<String> privateKeyPath();

    String cloudSdkPath();

    String region();

    boolean usePreemptibleVms();

    boolean useLocalSsds();

    Integer pollInterval();

    Optional<String> privateNetwork();

    String serviceAccountEmail();

    Optional<String> cmek();

    Optional<String> runId();

    Optional<Integer> sbpApiRunId();

    static Optional<String> privateKey(CommandLine commandLine) {
        if (commandLine.hasOption(PRIVATE_KEY_PATH)) {
            return Optional.of(commandLine.getOptionValue(PRIVATE_KEY_PATH));
        }
        return Optional.empty();
    }
}
