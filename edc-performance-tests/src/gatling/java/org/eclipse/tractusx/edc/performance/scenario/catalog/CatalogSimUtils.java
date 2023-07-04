package org.eclipse.tractusx.edc.performance.scenario.catalog;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gatling.javaapi.core.Session;
import io.gatling.javaapi.http.HttpDsl;
import io.gatling.javaapi.http.HttpRequestActionBuilder;
import org.eclipse.edc.connector.api.management.catalog.model.CatalogRequestDto;
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
import static org.eclipse.tractusx.edc.performance.scenario.BaseSim.API_KEY;

public class CatalogSimUtils {
    public static final String ASSET_ID = "assetId";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @NotNull
    public static HttpRequestActionBuilder getCatalog(String stepName, Function<Session, CatalogRequestDto> extractor) {
        return http(stepName)
                .post("/catalog/request")
                .header(CONTENT_TYPE, "application/json")
                .header("X-Api-Key", API_KEY)
                .body(StringBody(session -> formatCatalog(extractor.apply(session))))
                .check(HttpDsl.status().is(200))
                .check(
                        jmesPath("id").saveAs(ASSET_ID)
                );
    }


    private static String formatCatalog(CatalogRequestDto dto) {
        try {
            return MAPPER.writeValueAsString(dto);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static Iterator<Map<String, Object>> catalogFeeder(String identifier, Supplier<CatalogRequestDto> assetSupplier) {
        return Stream.generate((Supplier<Map<String, Object>>) () -> Collections.singletonMap(identifier, assetSupplier.get())).iterator();
    }
}
