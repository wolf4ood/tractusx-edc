package org.eclipse.tractusx.edc.performance.scenario.asset;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gatling.javaapi.core.Session;
import io.gatling.javaapi.http.HttpDsl;
import io.gatling.javaapi.http.HttpRequestActionBuilder;
import org.eclipse.edc.connector.api.management.asset.model.AssetEntryDto;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static io.gatling.javaapi.core.CoreDsl.StringBody;
import static io.gatling.javaapi.core.CoreDsl.jmesPath;
import static io.gatling.javaapi.http.HttpDsl.http;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static java.lang.String.format;
import static org.eclipse.tractusx.edc.performance.scenario.BaseSim.API_KEY;

public class AssetSimUtils {
    public static final String ASSET_ID = "assetId";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @NotNull
    public static HttpRequestActionBuilder createAsset(String stepName, Function<Session, AssetEntryDto> extractor) {
        return http(stepName)
                .post("/assets")
                .header(CONTENT_TYPE, "application/json")
                .header("X-Api-Key", API_KEY)
                .body(StringBody(session -> formatAsset(extractor.apply(session))))
                .check(HttpDsl.status().is(200))
                .check(
                        jmesPath("id").saveAs(ASSET_ID)
                );
    }

    @NotNull
    public static HttpRequestActionBuilder deleteAsset(String name) {
        return http(name)
                .delete(session -> format("/assets/%s", session.getString(ASSET_ID)))
                .header(CONTENT_TYPE, "application/json")
                .header("X-Api-Key", API_KEY)
                .check(HttpDsl.status().is(204));
    }

    private static String formatAsset(AssetEntryDto dto) {
        try {
            return MAPPER.writeValueAsString(dto);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static Iterator<Map<String, Object>> assetFeeder(String identifier, Supplier<AssetEntryDto> assetSupplier) {
        return Stream.generate((Supplier<Map<String, Object>>) () -> {
            return Collections.singletonMap(identifier, assetSupplier.get());
        }).iterator();
    }
}
