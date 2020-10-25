package com.hartwig.pipeline.tools;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface Versions {

    String BWA = "0.7.17";
    String SAMBAMBA = "0.6.8";
    String GATK = "3.8.0";
    String BCF_TOOLS = "1.9";
    String SAGE = "2.4";
    String SNPEFF = "4.3s";
    String TABIX = "0.2.6";
    String AMBER = "3.4";
    String COBALT = "1.10";
    String HEALTH_CHECKER = "3.1";
    String PURPLE = "2.50";
    String CIRCOS = "0.69.6";
    String GRIDSS = "2.9.3";
    String GRIPSS = "1.8";
    String LINX = "1.11";
    String BACHELOR = "1.12";
    String CHORD = "2.00_1.14";
    String SAMTOOLS = "1.10";
    String BAMCOMP = "1.3";

    static void printAll() {
        Logger logger = LoggerFactory.getLogger(Versions.class);
        logger.info("Version of pipeline5 is [{}] ", pipelineVersion());
        logger.info("Versions of tools used are [");
        Stream.of(Versions.class.getDeclaredFields())
                .filter(field -> Modifier.isStatic(field.getModifiers()))
                .map(Versions::format)
                .forEach(logger::info);
        logger.info("]");
    }

    static String pipelineVersion() {
        String version = Versions.class.getPackage().getImplementationVersion();
        return version != null ? version : "local-SNAPSHOT";
    }

    static String pipelineMajorMinorVersion() {
        return majorMinorVersion(pipelineVersion());
    }

    static String majorMinorVersion(final String version) {
        if (version != null) {
            String[] parts = version.split("\\.");
            if (parts.length == 3) {
                return String.format("%s.%s", parts[0], parts[1]);
            }
        }
        return version;
    }

    @NotNull
    static String format(final Field field) {
        try {
            return field.getName() + ": " + field.get(null);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    static String imageVersion() {
        return "5-16";
    }
}
