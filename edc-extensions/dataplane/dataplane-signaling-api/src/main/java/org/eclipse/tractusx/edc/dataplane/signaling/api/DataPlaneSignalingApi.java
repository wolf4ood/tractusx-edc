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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.core.Response;
import org.eclipse.tractusx.edc.dataplane.signaling.api.model.DataFlowPrepareMessage;
import org.eclipse.tractusx.edc.dataplane.signaling.api.model.DataFlowResumeMessage;
import org.eclipse.tractusx.edc.dataplane.signaling.api.model.DataFlowStartMessage;
import org.eclipse.tractusx.edc.dataplane.signaling.api.model.DataFlowStartedNotificationMessage;
import org.eclipse.tractusx.edc.dataplane.signaling.api.model.DataFlowStatusMessage;
import org.eclipse.tractusx.edc.dataplane.signaling.api.model.DataFlowStatusResponseMessage;
import org.eclipse.tractusx.edc.dataplane.signaling.api.model.DataFlowSuspendMessage;
import org.eclipse.tractusx.edc.dataplane.signaling.api.model.DataFlowTerminateMessage;

/**
 * Provider data-plane server endpoints of the
 * <a href="https://eclipse-dataplane-signaling.github.io/dataplane-signaling/HEAD/">Data Plane Signaling protocol</a>,
 * backed by the legacy EDC data-plane framework.
 */
@Tag(name = "Data Plane Signaling API")
public interface DataPlaneSignalingApi {

    @Operation(description = "Prepares (provisions) a data flow on the provider data plane.",
            responses = @ApiResponse(responseCode = "200", description = "Data flow prepared"))
    DataFlowStatusMessage prepare(DataFlowPrepareMessage message);

    @Operation(description = "Starts a data flow on the provider data plane.",
            responses = @ApiResponse(responseCode = "200", description = "Data flow started"))
    DataFlowStatusMessage start(DataFlowStartMessage message);

    @Operation(description = "Suspends a running data flow.",
            responses = @ApiResponse(responseCode = "200", description = "Data flow suspended"))
    Response suspend(String id, DataFlowSuspendMessage message);

    @Operation(description = "Resumes a suspended data flow.",
            responses = @ApiResponse(responseCode = "200", description = "Data flow resumed"))
    DataFlowStatusMessage resume(String id, DataFlowResumeMessage message);

    @Operation(description = "Terminates a data flow.",
            responses = @ApiResponse(responseCode = "200", description = "Data flow terminated"))
    Response terminate(String id, DataFlowTerminateMessage message);

    @Operation(description = "Returns the current status of a data flow.",
            responses = @ApiResponse(responseCode = "200", description = "Data flow status"))
    DataFlowStatusResponseMessage getStatus(String id);

    @Operation(description = "Notifies the provider data plane that the consumer data plane has started the transfer.",
            responses = @ApiResponse(responseCode = "200", description = "Notification accepted"))
    Response started(String id, DataFlowStartedNotificationMessage message);

    @Operation(description = "Notifies the provider data plane that the transfer has completed.",
            responses = @ApiResponse(responseCode = "200", description = "Notification accepted"))
    Response completed(String id);
}
