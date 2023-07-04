package org.eclipse.tractusx.edc.performance.scenario.contractnegotiation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gatling.javaapi.core.ChainBuilder;
import io.gatling.javaapi.core.Session;
import io.gatling.javaapi.http.HttpDsl;
import io.gatling.javaapi.http.HttpRequestActionBuilder;
import org.eclipse.edc.connector.api.management.contractnegotiation.model.NegotiationInitiateRequestDto;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static io.gatling.javaapi.core.CoreDsl.StringBody;
import static io.gatling.javaapi.core.CoreDsl.doWhileDuring;
import static io.gatling.javaapi.core.CoreDsl.exec;
import static io.gatling.javaapi.core.CoreDsl.jmesPath;
import static io.gatling.javaapi.http.HttpDsl.http;
import static io.gatling.javaapi.http.HttpDsl.status;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static java.lang.String.format;
import static org.eclipse.tractusx.edc.performance.scenario.BaseSim.API_KEY;

public class ContractNegotiationSimUtils {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static Iterator<Map<String, Object>> contractNegotiationFeeder(String identifier, Supplier<NegotiationInitiateRequestDto> assetSupplier) {
        return Stream.generate((Supplier<Map<String, Object>>) () -> Collections.singletonMap(identifier, assetSupplier.get())).iterator();
    }
    
    @NotNull
    public static HttpRequestActionBuilder negotiateContract(String stepName, String negotiationSelector, Function<Session, NegotiationInitiateRequestDto> extractor) {
        return http(stepName)
                .post("/contractnegotiations")
                .header(CONTENT_TYPE, "application/json")
                .header("X-Api-Key", API_KEY)
                .body(StringBody(session -> formatAsset(extractor.apply(session))))
                .check(HttpDsl.status().is(200))
                .check(
                        jmesPath("id").saveAs(negotiationSelector)
                );
    }

    public static ChainBuilder waitForContractAgreement(String sessionIdContractAgreementSelector, Function<Session, String> extractor) {
        return exec(session -> session.set("status", -1))
                .group("Wait for agreement")
                .on(doWhileDuring(session -> contractAgreementNotCompleted(session, sessionIdContractAgreementSelector), Duration.ofSeconds(30))
                        .on(exec(getContractStatus(extractor)).pace(Duration.ofSeconds(1)))
                )
                .exitHereIf(session -> contractAgreementNotCompleted(session, sessionIdContractAgreementSelector));
    }

    private static boolean contractAgreementNotCompleted(Session session, String contractAgreementId) {
        return session.getString(contractAgreementId) == null;
    }

    @NotNull
    private static HttpRequestActionBuilder getContractStatus(Function<Session, String> extractor) {
        return http("Get status")
                .get(session -> format("/contractnegotiations/%s", extractor.apply(session)))
                .header(CONTENT_TYPE, "application/json")
                .header("X-Api-Key", API_KEY)
                .check(status().is(200))
                .check(
                        jmesPath("id").is(extractor),
                        jmesPath("state").saveAs("status")
                )
                .checkIf(
                        session -> {
                            return "CONFIRMED".equals(session.getString("status"));
                        }
                ).then(
                        jmesPath("contractAgreementId").notNull().saveAs("contractAgreementId")
                );
    }

    private static String formatAsset(NegotiationInitiateRequestDto dto) {
        try {
            return MAPPER.writeValueAsString(dto);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
