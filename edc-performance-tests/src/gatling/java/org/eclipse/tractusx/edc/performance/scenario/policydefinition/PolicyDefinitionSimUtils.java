/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.tractusx.edc.performance.scenario.policydefinition;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gatling.javaapi.core.Session;
import io.gatling.javaapi.http.HttpDsl;
import io.gatling.javaapi.http.HttpRequestActionBuilder;
import org.eclipse.edc.connector.api.management.policy.model.PolicyDefinitionRequestDto;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

import static io.gatling.javaapi.core.CoreDsl.StringBody;
import static io.gatling.javaapi.core.CoreDsl.jmesPath;
import static io.gatling.javaapi.http.HttpDsl.http;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static org.eclipse.tractusx.edc.performance.scenario.BaseSim.API_KEY;

public class PolicyDefinitionSimUtils {


    public static final String POLICY_DEFINITION_ID = "contractDefinitionId";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @NotNull
    public static HttpRequestActionBuilder createPolicyDefinition(String stepName, Function<Session, PolicyDefinitionRequestDto> extractor) {
        return http(stepName)
                .post("/policydefinitions")
                .header(CONTENT_TYPE, "application/json")
                .header("X-Api-Key", API_KEY)
                .body(StringBody(session -> formatPolicyDefinition(extractor.apply(session))))
                .check(HttpDsl.status().is(200))
                .check(
                        jmesPath("id").saveAs(POLICY_DEFINITION_ID)
                );
    }

    private static String formatPolicyDefinition(PolicyDefinitionRequestDto dto) {
        try {
            return MAPPER.writeValueAsString(dto);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
