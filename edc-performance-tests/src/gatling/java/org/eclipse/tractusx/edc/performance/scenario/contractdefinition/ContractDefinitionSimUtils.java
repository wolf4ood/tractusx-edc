package org.eclipse.tractusx.edc.performance.scenario.contractdefinition;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gatling.javaapi.core.Session;
import io.gatling.javaapi.http.HttpDsl;
import io.gatling.javaapi.http.HttpRequestActionBuilder;
import org.eclipse.edc.connector.api.management.contractdefinition.model.ContractDefinitionRequestDto;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

import static io.gatling.javaapi.core.CoreDsl.StringBody;
import static io.gatling.javaapi.core.CoreDsl.jmesPath;
import static io.gatling.javaapi.http.HttpDsl.http;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static org.eclipse.tractusx.edc.performance.scenario.BaseSim.API_KEY;

public class ContractDefinitionSimUtils {


    public static final String CONTRACT_DEFINITION_ID = "contractDefinitionId";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @NotNull
    public static HttpRequestActionBuilder createContractDefinition(String stepName, Function<Session, ContractDefinitionRequestDto> extractor) {
        return http(stepName)
                .post("/contractdefinitions")
                .header(CONTENT_TYPE, "application/json")
                .header("X-Api-Key", API_KEY)
                .body(StringBody(session -> formatContractDefinition(extractor.apply(session))))
                .check(HttpDsl.status().is(200))
                .check(
                        jmesPath("id").saveAs(CONTRACT_DEFINITION_ID)
                );
    }

    private static String formatContractDefinition(ContractDefinitionRequestDto dto) {
        try {
            return MAPPER.writeValueAsString(dto);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
