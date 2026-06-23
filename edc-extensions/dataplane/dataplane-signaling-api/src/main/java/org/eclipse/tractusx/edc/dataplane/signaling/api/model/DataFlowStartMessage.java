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

package org.eclipse.tractusx.edc.dataplane.signaling.api.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * {@code DataFlowStartMessage} as defined by the Data Plane Signaling protocol, sent by a control plane to start a
 * data flow on the provider data plane. Serialized as plain JSON.
 */
@JsonDeserialize(builder = DataFlowStartMessage.Builder.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public final class DataFlowStartMessage {

    private String messageId;
    private String participantId;
    private String counterPartyId;
    private String dataspaceContext;
    private String processId;
    private String agreementId;
    private String datasetId;
    private URI callbackAddress;
    private String transferType;
    private DspDataAddress dataAddress;
    private List<String> labels;
    private Map<String, Object> metadata;
    private Map<String, Object> claims;

    private DataFlowStartMessage() {
    }

    public String getMessageId() {
        return messageId;
    }

    public String getParticipantId() {
        return participantId;
    }

    public String getCounterPartyId() {
        return counterPartyId;
    }

    public String getDataspaceContext() {
        return dataspaceContext;
    }

    public String getProcessId() {
        return processId;
    }

    public String getAgreementId() {
        return agreementId;
    }

    public String getDatasetId() {
        return datasetId;
    }

    public URI getCallbackAddress() {
        return callbackAddress;
    }

    public String getTransferType() {
        return transferType;
    }

    public DspDataAddress getDataAddress() {
        return dataAddress;
    }

    public List<String> getLabels() {
        return labels;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public Map<String, Object> getClaims() {
        return claims;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {

        private final DataFlowStartMessage instance = new DataFlowStartMessage();

        private Builder() {
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public DataFlowStartMessage build() {
            return instance;
        }

        public Builder messageId(String messageId) {
            instance.messageId = messageId;
            return this;
        }

        public Builder participantId(String participantId) {
            instance.participantId = participantId;
            return this;
        }

        public Builder counterPartyId(String counterPartyId) {
            instance.counterPartyId = counterPartyId;
            return this;
        }

        public Builder dataspaceContext(String dataspaceContext) {
            instance.dataspaceContext = dataspaceContext;
            return this;
        }

        public Builder processId(String processId) {
            instance.processId = processId;
            return this;
        }

        public Builder agreementId(String agreementId) {
            instance.agreementId = agreementId;
            return this;
        }

        public Builder datasetId(String datasetId) {
            instance.datasetId = datasetId;
            return this;
        }

        public Builder callbackAddress(URI callbackAddress) {
            instance.callbackAddress = callbackAddress;
            return this;
        }

        public Builder transferType(String transferType) {
            instance.transferType = transferType;
            return this;
        }

        public Builder dataAddress(DspDataAddress dataAddress) {
            instance.dataAddress = dataAddress;
            return this;
        }

        public Builder labels(List<String> labels) {
            instance.labels = labels;
            return this;
        }

        public Builder metadata(Map<String, Object> metadata) {
            instance.metadata = metadata;
            return this;
        }

        public Builder claims(Map<String, Object> claims) {
            instance.claims = claims;
            return this;
        }
    }
}
