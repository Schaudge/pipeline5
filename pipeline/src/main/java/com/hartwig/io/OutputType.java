package com.hartwig.io;

public enum OutputType {

    UNMAPPED("bam"),
    ALIGNED("bam"),
    INDEL_REALIGNED("bam"),
    DUPLICATE_MARKED("bam"),
    GERMLINE_VARIANTS("vcf");

    private final String extension;

    OutputType(final String extension) {
        this.extension = extension;
    }

    public String getExtension() {
        return extension;
    }
}
