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

package org.eclipse.tractusx.edc.dataplane.signaling.api.mapper;

import org.eclipse.edc.connector.dataplane.spi.DataFlowStates;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowProvisionMessage;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowStartMessage;
import org.eclipse.edc.spi.types.domain.transfer.FlowType;
import org.eclipse.edc.spi.types.domain.transfer.TransferType;
import org.eclipse.tractusx.edc.dataplane.signaling.api.model.DataFlowPrepareMessage;
import org.eclipse.tractusx.edc.dataplane.signaling.api.model.DataFlowResumeMessage;
import org.eclipse.tractusx.edc.dataplane.signaling.api.model.DataFlowStatusMessage;
import org.eclipse.tractusx.edc.dataplane.signaling.api.model.DspDataAddress;

import java.util.Map;
import java.util.Optional;

import static java.util.Optional.ofNullable;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.tractusx.edc.dataplane.signaling.api.model.DspDataAddress.DSP_DATA_ADDRESS_ENDPOINT;

/**
 * Bridges the Data Plane Signaling protocol message shapes (plain-JSON POJOs) to the legacy data-plane framework
 * types consumed by {@link org.eclipse.edc.connector.dataplane.spi.manager.DataPlaneManager}, and maps the framework
 * {@link DataFlowStates} back onto the protocol state vocabulary.
 */
public final class SignalingMapper {

    public static final String SIGNALING_COUNTER_PARTY_ID = EDC_NAMESPACE + "counterPartyId";
    public static final String SIGNALING_DATASPACE_CONTEXT = EDC_NAMESPACE + "dataspaceContext";

    private SignalingMapper() {
    }

    /**
     * Builds a legacy {@link DataFlowStartMessage} from the protocol {@code DataFlowStartMessage}. The protocol
     * {@code dataAddress} (the provider/source address resolved by the control plane) is mapped onto the framework
     * {@code sourceDataAddress}.
     */
    public static Result<DataFlowStartMessage> toFrameworkStartMessage(
            org.eclipse.tractusx.edc.dataplane.signaling.api.model.DataFlowStartMessage message) {
        return transferType(message.getTransferType())
                .map(transferType -> {
                    var builder = DataFlowStartMessage.Builder.newInstance()
                            .processId(message.getProcessId())
                            .transferType(transferType)
                            .agreementId(message.getAgreementId())
                            .assetId(message.getDatasetId())
                            .participantId(message.getParticipantId())
                            .callbackAddress(message.getCallbackAddress());

                    ofNullable(message.getMessageId()).ifPresent(builder::id);
                    ofNullable(message.getDataAddress()).map(SignalingMapper::toDataAddress).ifPresent(builder::destinationDataAddress);
                    ofNullable(message.getCounterPartyId()).ifPresent(v -> builder.property(SIGNALING_COUNTER_PARTY_ID, v));
                    ofNullable(message.getCounterPartyId()).ifPresent(v -> builder.property("https://w3id.org/tractusx/auth/audience", v));
                    ofNullable(message.getDataspaceContext()).ifPresent(v -> builder.property(SIGNALING_DATASPACE_CONTEXT, v));

                    ofNullable(message.getMetadata()).map(SignalingMapper::toDataAddress).ifPresent(builder::sourceDataAddress);

                    return builder.build();
                });
    }

    /**
     * Builds a legacy {@link DataFlowProvisionMessage} from the protocol {@code DataFlowPrepareMessage}.
     */
    public static Result<DataFlowProvisionMessage> toFrameworkProvisionMessage(DataFlowPrepareMessage message) {
        return transferType(message.getTransferType())
                .map(transferType -> {
                    var builder = DataFlowProvisionMessage.Builder.newInstance()
                            .processId(message.getProcessId())
                            .transferType(transferType)
                            .agreementId(message.getAgreementId())
                            .assetId(message.getDatasetId())
                            .participantId(message.getParticipantId())
                            .callbackAddress(message.getCallbackAddress());

                    ofNullable(message.getCounterPartyId()).ifPresent(v -> builder.property(SIGNALING_COUNTER_PARTY_ID, v));
                    ofNullable(message.getDataspaceContext()).ifPresent(v -> builder.property(SIGNALING_DATASPACE_CONTEXT, v));

                    return builder.build();
                });
    }

    /**
     * Builds a legacy {@link DataFlowStartMessage} for a resume request. The legacy framework has no native resume,
     * so a resume is mapped onto {@code start()} for the already-existing flow identified by {@code flowId}. Note the
     * {@code transferType} cannot be recovered from a resume message; callers relying on it must re-send it.
     */
    public static DataFlowStartMessage toFrameworkResumeMessage(String flowId, DataFlowResumeMessage message) {
        var builder = DataFlowStartMessage.Builder.newInstance()
                .processId(flowId);

        ofNullable(message.getMessageId()).ifPresent(builder::id);
        ofNullable(message.getDataAddress()).map(SignalingMapper::toDataAddress).ifPresent(builder::sourceDataAddress);

        return builder.build();
    }

    /**
     * Converts a protocol {@link DspDataAddress} to a legacy {@link DataAddress}.
     */
    public static DataAddress toDataAddress(DspDataAddress dataAddress) {
        var builder = DataAddress.Builder.newInstance()
                .type(dataAddress.getEndpointType());

        dataAddress.getEndpointProperties().forEach(property -> builder.property(property.getName(), property.getValue()));

        return builder
                .property(DSP_DATA_ADDRESS_ENDPOINT, dataAddress.getEndpoint())
                .build();
    }


    /**
     * Converts a protocol {@link DspDataAddress} to a legacy {@link DataAddress}.
     */
    public static DataAddress toDataAddress(Map<String, Object> metadata) {
        var builder = DataAddress.Builder.newInstance()
                .type(metadata.get("type").toString());

        metadata.forEach((key, value) -> builder.property(key, value.toString()));

        return builder.build();
    }

    /**
     * Converts a legacy {@link DataAddress} to a protocol {@link DspDataAddress}.
     */
    public static DspDataAddress toDspDataAddress(DataAddress dataAddress) {
        var builder = DspDataAddress.Builder.newInstance()
                .endpointType(dataAddress.getType())
                .endpoint(dataAddress.getStringProperty(DSP_DATA_ADDRESS_ENDPOINT));

        dataAddress.getProperties().forEach((key, value) -> builder.property(key, String.valueOf(value)));

        return builder.build();
    }

    /**
     * Maps a framework {@link DataFlowStates} onto the Data Plane Signaling protocol state vocabulary.
     */
    public static String toProtocolState(DataFlowStates state) {
        if (state == null) {
            return "INITIALIZED";
        }
        return switch (state) {
            case PROVISIONING, PROVISION_REQUESTED, PROVISION_NOTIFYING -> "PREPARING";
            case PROVISIONED -> "PREPARED";
            case RECEIVED -> "STARTING";
            case STARTED -> "STARTED";
            case SUSPENDED -> "SUSPENDED";
            case COMPLETED, NOTIFIED -> "COMPLETED";
            case TERMINATED, FAILED, DEPROVISIONING, DEPROVISION_REQUESTED, DEPROVISIONED, DEPROVISION_FAILED ->
                    "TERMINATED";
        };
    }

    /**
     * Parses the combined Data Plane Signaling {@code transferType} string (e.g. {@code "HttpData-PULL"} or
     * {@code "HttpData-PULL-HttpData"}) into a framework {@link TransferType}.
     */
    public static Result<TransferType> transferType(String transferType) {
        if (transferType == null || transferType.isBlank()) {
            return Result.failure("transferType must not be empty");
        }
        var tokens = transferType.split("-");
        if (tokens.length < 2) {
            return Result.failure("Invalid transferType '%s': expected '<destinationType>-<PULL|PUSH>[-<responseChannel>]'".formatted(transferType));
        }
        FlowType flowType;
        try {
            flowType = FlowType.valueOf(tokens[1]);
        } catch (IllegalArgumentException e) {
            return Result.failure("Invalid flow type in transferType '%s': %s".formatted(transferType, tokens[1]));
        }
        var responseChannel = tokens.length > 2 ? tokens[2] : null;
        return Result.success(new TransferType(tokens[0], flowType, responseChannel));
    }

    /**
     * Builds a protocol {@link DataFlowStatusMessage} for the given flow, state and optional resolved data address.
     */
    public static DataFlowStatusMessage statusMessage(String dataFlowId, DataFlowStates state, DataAddress dataAddress) {
        return DataFlowStatusMessage.Builder.newInstance()
                .dataFlowId(dataFlowId)
                .state(toProtocolState(state))
                .dataAddress(Optional.ofNullable(dataAddress).map(SignalingMapper::toDspDataAddress).orElse(null))
                .build();
    }
}
