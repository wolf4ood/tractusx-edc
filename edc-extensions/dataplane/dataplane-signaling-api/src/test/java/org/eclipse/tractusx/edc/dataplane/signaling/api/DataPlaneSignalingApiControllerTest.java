/********************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 ********************************************************************************/

package org.eclipse.tractusx.edc.dataplane.signaling.api;

import io.restassured.specification.RequestSpecification;
import org.eclipse.edc.connector.dataplane.spi.DataFlowStates;
import org.eclipse.edc.connector.dataplane.spi.manager.DataPlaneManager;
import org.eclipse.edc.junit.annotations.ApiTest;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowResponseMessage;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowStartMessage;
import org.eclipse.edc.web.jersey.testfixtures.RestControllerTestBase;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.eclipse.edc.spi.response.ResponseStatus.FATAL_ERROR;
import static org.eclipse.tractusx.edc.dataplane.signaling.api.model.DspDataAddress.DSP_DATA_ADDRESS_ENDPOINT;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ApiTest
class DataPlaneSignalingApiControllerTest extends RestControllerTestBase {

    private static final String FLOW_ID = "flow-1";
    private static final String START_BODY = """
            {
              "messageId": "msg-1",
              "participantId": "did:web:provider",
              "counterPartyId": "did:web:consumer",
              "dataspaceContext": "ctx",
              "processId": "flow-1",
              "agreementId": "agreement-1",
              "datasetId": "asset-1",
              "callbackAddress": "http://control-plane/callback",
              "transferType": "HttpData-PULL",
              "claims": {}
            }
            """;

    private final DataPlaneManager dataPlaneManager = mock();

    @Override
    protected Object controller() {
        return new DataPlaneSignalingApiController(dataPlaneManager, monitor);
    }

    private RequestSpecification baseRequest() {
        return given().baseUri("http://localhost:" + port).basePath("/dataflows");
    }

    @Test
    void start_shouldStartFlow_andReturnStatus() {
        when(dataPlaneManager.validate(any())).thenReturn(Result.success());
        when(dataPlaneManager.start(any())).thenReturn(StatusResult.success(DataFlowResponseMessage.Builder.newInstance().build()));
        when(dataPlaneManager.getTransferState(FLOW_ID)).thenReturn(DataFlowStates.STARTED);

        baseRequest()
                .contentType(JSON)
                .body(START_BODY)
                .post("/start")
                .then()
                .statusCode(200)
                .body("dataFlowId", org.hamcrest.Matchers.equalTo(FLOW_ID))
                .body("state", org.hamcrest.Matchers.equalTo("STARTED"));

        verify(dataPlaneManager).start(any(DataFlowStartMessage.class));
    }

    @Test
    void start_shouldReturnDataAddress_whenProvided() {
        var edr = DataAddress.Builder.newInstance().type("HttpData").property(DSP_DATA_ADDRESS_ENDPOINT, "http://edr").build();
        when(dataPlaneManager.validate(any())).thenReturn(Result.success());
        when(dataPlaneManager.start(any())).thenReturn(StatusResult.success(DataFlowResponseMessage.Builder.newInstance().dataAddress(edr).build()));
        when(dataPlaneManager.getTransferState(FLOW_ID)).thenReturn(DataFlowStates.STARTED);

        baseRequest()
                .contentType(JSON)
                .body(START_BODY)
                .post("/start")
                .then()
                .statusCode(200)
                .body("dataAddress.endpointType", org.hamcrest.Matchers.equalTo("HttpData"))
                .body("dataAddress.endpoint", org.hamcrest.Matchers.equalTo("http://edr"));
    }

    @Test
    void start_shouldReturnBadRequest_whenValidationFails() {
        when(dataPlaneManager.validate(any())).thenReturn(Result.failure("invalid"));

        baseRequest()
                .contentType(JSON)
                .body(START_BODY)
                .post("/start")
                .then()
                .statusCode(400);
    }

    @Test
    void start_shouldReturnBadRequest_whenTransferTypeMissing() {
        baseRequest()
                .contentType(JSON)
                .body("""
                        { "processId": "flow-1" }
                        """)
                .post("/start")
                .then()
                .statusCode(400);
    }

    @Test
    void prepare_shouldProvisionFlow_andReturnStatus() {
        when(dataPlaneManager.provision(any())).thenReturn(StatusResult.success(DataFlowResponseMessage.Builder.newInstance().provisioning(true).build()));
        when(dataPlaneManager.getTransferState(FLOW_ID)).thenReturn(DataFlowStates.PROVISIONING);

        baseRequest()
                .contentType(JSON)
                .body(START_BODY)
                .post("/prepare")
                .then()
                .statusCode(200)
                .body("state", org.hamcrest.Matchers.equalTo("PREPARING"));
    }

    @Test
    void suspend_shouldSuspendFlow() {
        when(dataPlaneManager.suspend(FLOW_ID)).thenReturn(StatusResult.success());

        baseRequest()
                .contentType(JSON)
                .body("{ \"messageId\": \"msg-1\", \"reason\": \"because\" }")
                .post("/{id}/suspend", FLOW_ID)
                .then()
                .statusCode(200);

        verify(dataPlaneManager).suspend(FLOW_ID);
    }

    @Test
    void terminate_shouldTerminateFlow_withReason() {
        when(dataPlaneManager.terminate(eq(FLOW_ID), anyString())).thenReturn(StatusResult.success());

        baseRequest()
                .contentType(JSON)
                .body("{ \"messageId\": \"msg-1\", \"reason\": \"done\" }")
                .post("/{id}/terminate", FLOW_ID)
                .then()
                .statusCode(200);

        verify(dataPlaneManager).terminate(FLOW_ID, "done");
    }

    @Test
    void terminate_shouldReturnBadRequest_whenManagerFails() {
        when(dataPlaneManager.terminate(eq(FLOW_ID), any())).thenReturn(StatusResult.failure(FATAL_ERROR, "boom"));

        baseRequest()
                .contentType(JSON)
                .body("{ \"messageId\": \"msg-1\" }")
                .post("/{id}/terminate", FLOW_ID)
                .then()
                .statusCode(400);
    }

    @Test
    void resume_shouldRestartFlow() {
        when(dataPlaneManager.validate(any())).thenReturn(Result.success());
        when(dataPlaneManager.start(any())).thenReturn(StatusResult.success(DataFlowResponseMessage.Builder.newInstance().build()));
        when(dataPlaneManager.getTransferState(FLOW_ID)).thenReturn(DataFlowStates.STARTED);

        baseRequest()
                .contentType(JSON)
                .body("{ \"messageId\": \"msg-1\" }")
                .post("/{id}/resume", FLOW_ID)
                .then()
                .statusCode(200)
                .body("state", org.hamcrest.Matchers.equalTo("STARTED"));
    }

    @Test
    void started_shouldAcknowledge_asStub() {
        baseRequest()
                .contentType(JSON)
                .body("{ \"messageId\": \"msg-1\" }")
                .post("/{id}/started", FLOW_ID)
                .then()
                .statusCode(200);

        verifyNoInteractions(dataPlaneManager);
    }

    @Test
    void completed_shouldAcknowledge_asStub() {
        baseRequest()
                .contentType(JSON)
                .post("/{id}/completed", FLOW_ID)
                .then()
                .statusCode(200);

        verifyNoInteractions(dataPlaneManager);
    }

    @Test
    void getStatus_shouldReturnState() {
        when(dataPlaneManager.getTransferState(FLOW_ID)).thenReturn(DataFlowStates.SUSPENDED);

        baseRequest()
                .get("/{id}/status", FLOW_ID)
                .then()
                .statusCode(200)
                .body("dataFlowId", org.hamcrest.Matchers.equalTo(FLOW_ID))
                .body("state", org.hamcrest.Matchers.equalTo("SUSPENDED"));
    }
}
