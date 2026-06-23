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
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

/**
 * {@code DataFlowStatusMessage} as defined by the Data Plane Signaling protocol, returned by the provider data plane
 * in response to {@code prepare}, {@code start} and {@code resume}. Serialized as plain JSON.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonDeserialize(builder = DataFlowStatusMessage.Builder.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public final class DataFlowStatusMessage {

    private String messageId;
    private String dataFlowId;
    private String state;
    private DspDataAddress dataAddress;
    private String error;

    private DataFlowStatusMessage() {
    }

    public String getMessageId() {
        return messageId;
    }

    public String getDataFlowId() {
        return dataFlowId;
    }

    public String getState() {
        return state;
    }

    public DspDataAddress getDataAddress() {
        return dataAddress;
    }

    public String getError() {
        return error;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {

        private final DataFlowStatusMessage instance = new DataFlowStatusMessage();

        private Builder() {
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public DataFlowStatusMessage build() {
            return instance;
        }

        public Builder messageId(String messageId) {
            instance.messageId = messageId;
            return this;
        }

        public Builder dataFlowId(String dataFlowId) {
            instance.dataFlowId = dataFlowId;
            return this;
        }

        public Builder state(String state) {
            instance.state = state;
            return this;
        }

        public Builder dataAddress(DspDataAddress dataAddress) {
            instance.dataAddress = dataAddress;
            return this;
        }

        public Builder error(String error) {
            instance.error = error;
            return this;
        }
    }
}
