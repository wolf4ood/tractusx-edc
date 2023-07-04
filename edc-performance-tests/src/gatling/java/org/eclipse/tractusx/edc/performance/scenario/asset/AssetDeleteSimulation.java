package org.eclipse.tractusx.edc.performance.scenario.asset;


import org.eclipse.edc.api.model.DataAddressDto;
import org.eclipse.edc.connector.api.management.asset.model.AssetCreationRequestDto;
import org.eclipse.edc.connector.api.management.asset.model.AssetEntryDto;
import org.eclipse.tractusx.edc.performance.scenario.BaseSim;

import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

import static io.gatling.javaapi.core.CoreDsl.atOnceUsers;
import static io.gatling.javaapi.core.CoreDsl.details;
import static io.gatling.javaapi.core.CoreDsl.feed;
import static io.gatling.javaapi.core.CoreDsl.global;
import static io.gatling.javaapi.core.CoreDsl.scenario;
import static io.gatling.javaapi.http.HttpDsl.http;
import static org.eclipse.tractusx.edc.performance.scenario.asset.AssetSimUtils.assetFeeder;
import static org.eclipse.tractusx.edc.performance.scenario.asset.AssetSimUtils.createAsset;
import static org.eclipse.tractusx.edc.performance.scenario.asset.AssetSimUtils.deleteAsset;

public class AssetDeleteSimulation extends BaseSim {

    public static final String CREATE_ASSET_STEP = "Create Asset";
    public static final String DELETE_ASSET_STEP = "Delete Asset";

    public AssetDeleteSimulation() {

        Supplier<AssetEntryDto> assetSupplier = () -> AssetEntryDto.Builder.newInstance()
                .asset(AssetCreationRequestDto.Builder.newInstance()
                        .id(UUID.randomUUID().toString())
                        .properties(Map.of("type", "File"))
                        .build())
                .dataAddress(DataAddressDto.Builder.newInstance()
                        .properties(Map.of("type", "File"))
                        .build())
                .build();

        var assetIdentifier = "asset";

        setUp(scenario("Assert Delete")
                .repeat(REPEAT)
                .on(feed(assetFeeder(assetIdentifier, assetSupplier))
                        .exec(createAsset(CREATE_ASSET_STEP, (session -> session.get(assetIdentifier))).silent())
                        .exec(deleteAsset(DELETE_ASSET_STEP)))
                .injectOpen(atOnceUsers(AT_ONCE_USERS)))
                .protocols(http.baseUrl(PROVIDER_MANAGEMENT_API_URL))
                .assertions(
                        details(DELETE_ASSET_STEP).successfulRequests().count().is((long) AT_ONCE_USERS * REPEAT),
                        global().responseTime().max().lt(MAX_RESPONSE_TIME),
                        global().successfulRequests().percent().is(SUCCESS_PERCENTAGE)
                );
    }

}
