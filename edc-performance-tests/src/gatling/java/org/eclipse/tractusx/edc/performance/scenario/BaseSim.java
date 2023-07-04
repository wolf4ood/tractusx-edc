package org.eclipse.tractusx.edc.performance.scenario;

import io.gatling.javaapi.core.Simulation;

import static org.eclipse.edc.util.configuration.ConfigurationFunctions.propOrEnv;

public abstract class BaseSim extends Simulation {


    public static final String API_KEY = propOrEnv("simulation.apiKey", "password");
    protected static final int MAX_RESPONSE_TIME = Integer.parseInt(propOrEnv("simulation.maxResponseTime", "10000"));
    protected static final double SUCCESS_PERCENTAGE = Double.parseDouble(propOrEnv("simulation.successPercentage", "100.0"));
    protected static final int REPEAT = Integer.parseInt(propOrEnv("simulation.repeat", "1"));
    protected static final int AT_ONCE_USERS = Integer.parseInt(propOrEnv("simulation.atOnceUsers", "1"));
    protected static final String CONSUMER_MANAGEMENT_API_URL = propOrEnv("simulation.consumer.managementApiUrl", "http://localhost:9192/api/management/v2");
    protected static final String PROVIDER_IDS_API_URL = propOrEnv("simulation.consumer.idsUrl", "http://localhost:8282/api/v1/ids/data");
    protected static final String PROVIDER_MANAGEMENT_API_URL = propOrEnv("simulation.provider.managementApiUrl", "http://localhost:8182/api/management/v2");

    public BaseSim() {

    }
}
