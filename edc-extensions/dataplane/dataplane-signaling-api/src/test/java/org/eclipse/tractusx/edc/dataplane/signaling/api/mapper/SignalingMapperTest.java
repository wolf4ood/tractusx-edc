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
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.transfer.FlowType;
import org.eclipse.tractusx.edc.dataplane.signaling.api.model.DataFlowStartMessage;
import org.eclipse.tractusx.edc.dataplane.signaling.api.model.DspDataAddress;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.tractusx.edc.dataplane.signaling.api.mapper.SignalingMapper.SIGNALING_COUNTER_PARTY_ID;
import static org.eclipse.tractusx.edc.dataplane.signaling.api.model.DspDataAddress.DSP_DATA_ADDRESS_ENDPOINT;

class SignalingMapperTest {

    @Test
    void transferType_shouldParseDestinationAndFlowType() {
        var result = SignalingMapper.transferType("HttpData-PULL");

        assertThat(result.succeeded()).isTrue();
        assertThat(result.getContent().destinationType()).isEqualTo("HttpData");
        assertThat(result.getContent().flowType()).isEqualTo(FlowType.PULL);
        assertThat(result.getContent().responseChannelType()).isNull();
    }

    @Test
    void transferType_shouldParseResponseChannel() {
        var result = SignalingMapper.transferType("HttpData-PULL-HttpData");

        assertThat(result.succeeded()).isTrue();
        assertThat(result.getContent().responseChannelType()).isEqualTo("HttpData");
    }

    @Test
    void transferType_shouldFail_whenInvalid() {
        assertThat(SignalingMapper.transferType("HttpData").failed()).isTrue();
        assertThat(SignalingMapper.transferType("HttpData-FOO").failed()).isTrue();
        assertThat(SignalingMapper.transferType("").failed()).isTrue();
    }

    @Test
    void toFrameworkStartMessage_shouldMapAllFields() {
        var message = DataFlowStartMessage.Builder.newInstance()
                .messageId("msg-1")
                .processId("flow-1")
                .agreementId("agreement-1")
                .datasetId("asset-1")
                .participantId("provider")
                .counterPartyId("consumer")
                .callbackAddress(URI.create("http://callback"))
                .transferType("HttpData-PULL")
                .dataAddress(DspDataAddress.Builder.newInstance().endpointType("HttpData").endpoint("http://source").build())
                .claims(Map.of("foo", "bar"))
                .build();

        var result = SignalingMapper.toFrameworkStartMessage(message);

        assertThat(result.succeeded()).isTrue();
        var framework = result.getContent();
        assertThat(framework.getId()).isEqualTo("msg-1");
        assertThat(framework.getProcessId()).isEqualTo("flow-1");
        assertThat(framework.getAgreementId()).isEqualTo("agreement-1");
        assertThat(framework.getAssetId()).isEqualTo("asset-1");
        assertThat(framework.getParticipantId()).isEqualTo("provider");
        assertThat(framework.getTransferType().flowType()).isEqualTo(FlowType.PULL);
        assertThat(framework.getSourceDataAddress().getType()).isEqualTo("HttpData");
        assertThat(framework.getProperties()).containsEntry(SIGNALING_COUNTER_PARTY_ID, "consumer");
    }

    @Test
    void dataAddressRoundTrip() {
        var dsp = DspDataAddress.Builder.newInstance()
                .endpointType("HttpData")
                .endpoint("http://endpoint")
                .property("authKey", "Authorization")
                .build();

        var dataAddress = SignalingMapper.toDataAddress(dsp);
        assertThat(dataAddress.getType()).isEqualTo("HttpData");
        assertThat(dataAddress.getStringProperty("authKey")).isEqualTo("Authorization");
        assertThat(dataAddress.getStringProperty(DSP_DATA_ADDRESS_ENDPOINT)).isEqualTo("http://endpoint");

        var roundTrip = SignalingMapper.toDspDataAddress(dataAddress);
        assertThat(roundTrip.getEndpointType()).isEqualTo("HttpData");
        assertThat(roundTrip.getEndpoint()).isEqualTo("http://endpoint");
    }

    @Test
    void toProtocolState_shouldMapStates() {
        assertThat(SignalingMapper.toProtocolState(DataFlowStates.PROVISIONING)).isEqualTo("PREPARING");
        assertThat(SignalingMapper.toProtocolState(DataFlowStates.PROVISIONED)).isEqualTo("PREPARED");
        assertThat(SignalingMapper.toProtocolState(DataFlowStates.STARTED)).isEqualTo("STARTED");
        assertThat(SignalingMapper.toProtocolState(DataFlowStates.SUSPENDED)).isEqualTo("SUSPENDED");
        assertThat(SignalingMapper.toProtocolState(DataFlowStates.COMPLETED)).isEqualTo("COMPLETED");
        assertThat(SignalingMapper.toProtocolState(DataFlowStates.TERMINATED)).isEqualTo("TERMINATED");
        assertThat(SignalingMapper.toProtocolState(DataFlowStates.FAILED)).isEqualTo("TERMINATED");
        assertThat(SignalingMapper.toProtocolState(null)).isEqualTo("INITIALIZED");
    }

    @Test
    void toDataAddress_shouldBeAssignableToDataAddress() {
        DataAddress dataAddress = SignalingMapper.toDataAddress(
                DspDataAddress.Builder.newInstance().endpointType("HttpData").endpoint("http://x").build());
        assertThat(dataAddress).isNotNull();
    }
}
