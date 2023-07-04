package org.eclipse.tractusx.edc.performance.scenario.asset;

import io.gatling.javaapi.core.CoreDsl;
import org.eclipse.edc.api.model.DataAddressDto;
import org.eclipse.edc.connector.api.management.asset.model.AssetCreationRequestDto;
import org.eclipse.edc.connector.api.management.asset.model.AssetEntryDto;
import org.eclipse.tractusx.edc.performance.scenario.BaseSim;

import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

import static io.gatling.javaapi.core.CoreDsl.atOnceUsers;
import static io.gatling.javaapi.core.CoreDsl.details;
import static io.gatling.javaapi.core.CoreDsl.global;
import static io.gatling.javaapi.core.CoreDsl.scenario;
import static io.gatling.javaapi.http.HttpDsl.http;

public class AssetInsertSimulation extends BaseSim {

    public static final String CREATE_ASSET_STEP = "Create Asset";

    public AssetInsertSimulation() {

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
        setUp(scenario("Assert Insert")

                .repeat(REPEAT)
                .on(CoreDsl.feed(AssetSimUtils.assetFeeder(assetIdentifier, assetSupplier))
                        .exec(AssetSimUtils.createAsset(CREATE_ASSET_STEP, (session -> session.get(assetIdentifier)))))
                .injectOpen(atOnceUsers(AT_ONCE_USERS)))
                .protocols(http.baseUrl(PROVIDER_MANAGEMENT_API_URL))
                .assertions(
                        details(CREATE_ASSET_STEP).successfulRequests().count().is((long) AT_ONCE_USERS * REPEAT),
                        global().responseTime().max().lt(MAX_RESPONSE_TIME),
                        global().successfulRequests().percent().is(SUCCESS_PERCENTAGE)
                );
    }

}
