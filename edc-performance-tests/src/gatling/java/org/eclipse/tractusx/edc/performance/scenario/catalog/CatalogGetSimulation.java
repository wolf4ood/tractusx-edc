package org.eclipse.tractusx.edc.performance.scenario.catalog;

import org.eclipse.edc.connector.api.management.catalog.model.CatalogRequestDto;
import org.eclipse.tractusx.edc.performance.scenario.BaseSim;

import java.util.function.Supplier;

import static io.gatling.javaapi.core.CoreDsl.atOnceUsers;
import static io.gatling.javaapi.core.CoreDsl.details;
import static io.gatling.javaapi.core.CoreDsl.feed;
import static io.gatling.javaapi.core.CoreDsl.global;
import static io.gatling.javaapi.core.CoreDsl.scenario;
import static io.gatling.javaapi.http.HttpDsl.http;
import static org.eclipse.tractusx.edc.performance.scenario.catalog.CatalogSimUtils.catalogFeeder;
import static org.eclipse.tractusx.edc.performance.scenario.catalog.CatalogSimUtils.getCatalog;

public class CatalogGetSimulation extends BaseSim {

    public static final String GET_CATALOG_STEP = "Get Catalog";

    public CatalogGetSimulation() {
        
        Supplier<CatalogRequestDto> assetSupplier = () -> CatalogRequestDto.Builder.newInstance().providerUrl(PROVIDER_IDS_API_URL).build();
        var catalogIdentifier = "catalog";
        setUp(scenario("Catalog GET")
                .repeat(REPEAT)
                .on(feed(catalogFeeder(catalogIdentifier, assetSupplier))
                        .exec(getCatalog(GET_CATALOG_STEP, (session -> session.get(catalogIdentifier)))))
                .injectOpen(atOnceUsers(AT_ONCE_USERS)))
                .protocols(http.baseUrl(CONSUMER_MANAGEMENT_API_URL))
                .assertions(
                        details(GET_CATALOG_STEP).successfulRequests().count().is((long) AT_ONCE_USERS * REPEAT),
                        global().responseTime().max().lt(MAX_RESPONSE_TIME),
                        global().successfulRequests().percent().is(SUCCESS_PERCENTAGE)
                );
    }

}
