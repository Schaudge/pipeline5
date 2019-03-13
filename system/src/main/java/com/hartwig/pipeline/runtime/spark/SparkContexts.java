package com.hartwig.pipeline.runtime.spark;

import java.util.Map;

import com.hartwig.pipeline.runtime.configuration.Configuration;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaSparkContext;
import org.bdgenomics.adam.serialization.ADAMKryoRegistrator;

public class SparkContexts {

    public static JavaSparkContext create(String appName, Configuration configuration) {
        return create(appName, configuration.spark().get("master"), configuration.spark());
    }

    public static JavaSparkContext create(String appName, String master, Map<String, String> sparkProperties) {
        SparkConf conf = new SparkConf().set("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
                .set("spark.ui.showConsoleProgress", "false")
                .set("spark.kryoserializer.buffer.max", "2046m")
                .set("spark.kryo.referenceTracking", "false")
                .set("spark.kryo.registrator", ADAMKryoRegistrator.class.getName())
                .set("spark.kryo.registrationRequired", "true")
                .set("spark.ui.showConsoleProgress", "false")
                .set("spark.driver.maxResultSize", "0")
                .setAppName(appName);
        if (master != null) {
            conf.setMaster(master);
        }
        sparkProperties.forEach(conf::set);
        return new JavaSparkContext(conf);
    }
}