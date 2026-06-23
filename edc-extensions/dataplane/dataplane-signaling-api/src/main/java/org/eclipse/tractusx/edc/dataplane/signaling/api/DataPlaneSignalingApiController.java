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

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import org.eclipse.edc.connector.dataplane.spi.manager.DataPlaneManager;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowResponseMessage;
import org.eclipse.edc.web.spi.exception.InvalidRequestException;
import org.eclipse.tractusx.edc.dataplane.signaling.api.mapper.SignalingMapper;
import org.eclipse.tractusx.edc.dataplane.signaling.api.model.DataFlowPrepareMessage;
import org.eclipse.tractusx.edc.dataplane.signaling.api.model.DataFlowResumeMessage;
import org.eclipse.tractusx.edc.dataplane.signaling.api.model.DataFlowStartMessage;
import org.eclipse.tractusx.edc.dataplane.signaling.api.model.DataFlowStartedNotificationMessage;
import org.eclipse.tractusx.edc.dataplane.signaling.api.model.DataFlowStatusMessage;
import org.eclipse.tractusx.edc.dataplane.signaling.api.model.DataFlowStatusResponseMessage;
import org.eclipse.tractusx.edc.dataplane.signaling.api.model.DataFlowSuspendMessage;
import org.eclipse.tractusx.edc.dataplane.signaling.api.model.DataFlowTerminateMessage;

import java.util.Optional;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

/**
 * Implements the provider data-plane server side of the Data Plane Signaling protocol on top of the legacy
 * {@link DataPlaneManager} framework.
 */
@Consumes({APPLICATION_JSON})
@Produces({APPLICATION_JSON})
@Path("/dataflows")
public class DataPlaneSignalingApiController implements DataPlaneSignalingApi {

    private final DataPlaneManager dataPlaneManager;
    private final Monitor monitor;

    public DataPlaneSignalingApiController(DataPlaneManager dataPlaneManager, Monitor monitor) {
        this.dataPlaneManager = dataPlaneManager;
        this.monitor = monitor;
    }

    @POST
    @Path("/prepare")
    @Override
    public DataFlowStatusMessage prepare(DataFlowPrepareMessage message) {
        var provisionMessage = SignalingMapper.toFrameworkProvisionMessage(message)
                .orElseThrow(f -> new InvalidRequestException(f.getFailureDetail()));

        if (provisionMessage.getDestination() != null) {
            var response = dataPlaneManager.provision(provisionMessage)
                    .orElseThrow(f -> new InvalidRequestException(f.getFailureDetail()));

            return statusMessage(message.getProcessId(), response);
        } else {
            return statusMessage(message.getProcessId(), null);
        }


    }

    @POST
    @Path("/start")
    @Override
    public DataFlowStatusMessage start(DataFlowStartMessage message) {
        var startMessage = SignalingMapper.toFrameworkStartMessage(message)
                .orElseThrow(f -> new InvalidRequestException(f.getFailureDetail()));

        return startFlow(message.getProcessId(), startMessage);
    }

    @POST
    @Path("/{id}/suspend")
    @Override
    public Response suspend(@PathParam("id") String id, DataFlowSuspendMessage message) {
        dataPlaneManager.suspend(id)
                .orElseThrow(f -> new InvalidRequestException(f.getFailureDetail()));
        return Response.ok().build();
    }

    @POST
    @Path("/{id}/resume")
    @Override
    public DataFlowStatusMessage resume(@PathParam("id") String id, DataFlowResumeMessage message) {
        // the legacy framework has no native resume: map it onto start() for the existing flow
        var startMessage = SignalingMapper.toFrameworkResumeMessage(id, message);
        return startFlow(id, startMessage);
    }

    @POST
    @Path("/{id}/terminate")
    @Override
    public Response terminate(@PathParam("id") String id, DataFlowTerminateMessage message) {
        var reason = Optional.ofNullable(message).map(DataFlowTerminateMessage::getReason).orElse(null);
        dataPlaneManager.terminate(id, reason)
                .orElseThrow(f -> new InvalidRequestException(f.getFailureDetail()));
        return Response.ok().build();
    }
    
    @GET
    @Path("/{id}/status")
    @Override
    public DataFlowStatusResponseMessage getStatus(@PathParam("id") String id) {
        var state = dataPlaneManager.getTransferState(id);
        return DataFlowStatusResponseMessage.Builder.newInstance()
                .dataFlowId(id)
                .state(SignalingMapper.toProtocolState(state))
                .build();
    }

    @POST
    @Path("/{id}/started")
    @Override
    public Response started(@PathParam("id") String id, DataFlowStartedNotificationMessage message) {
        // consumer-side notification: not acted upon by the legacy framework, acknowledged as a no-op stub
        monitor.debug("Received 'started' notification for data flow %s".formatted(id));
        return Response.ok().build();
    }

    @POST
    @Path("/{id}/completed")
    @Override
    public Response completed(@PathParam("id") String id) {
        // consumer-side notification: not acted upon by the legacy framework, acknowledged as a no-op stub
        monitor.debug("Received 'completed' notification for data flow %s".formatted(id));
        return Response.ok().build();
    }

    private DataFlowStatusMessage startFlow(String flowId, org.eclipse.edc.spi.types.domain.transfer.DataFlowStartMessage startMessage) {
        dataPlaneManager.validate(startMessage)
                .onFailure(f -> monitor.warning("Failed to validate data flow request: %s".formatted(f.getFailureDetail())))
                .orElseThrow(f -> f.getMessages().isEmpty()
                        ? new InvalidRequestException("Failed to validate data flow request: %s".formatted(startMessage.getId()))
                        : new InvalidRequestException(f.getMessages()));

        var response = dataPlaneManager.start(startMessage)
                .orElseThrow(f -> new InvalidRequestException(f.getFailureDetail()));

        return statusMessage(flowId, response);
    }

    private DataFlowStatusMessage statusMessage(String flowId, DataFlowResponseMessage response) {
        var dataAddress = Optional.ofNullable(response).map(DataFlowResponseMessage::getDataAddress).orElse(null);
        return SignalingMapper.statusMessage(flowId, dataPlaneManager.getTransferState(flowId), dataAddress);
    }
}
