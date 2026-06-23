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

/**
 * {@code DataFlowStartedNotificationMessage} as defined by the Data Plane Signaling protocol, sent by a consumer data
 * plane to notify the provider that a transfer has started. Serialized as plain JSON.
 */
@JsonDeserialize(builder = DataFlowStartedNotificationMessage.Builder.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public final class DataFlowStartedNotificationMessage {

    private String messageId;
    private DspDataAddress dataAddress;

    private DataFlowStartedNotificationMessage() {
    }

    public String getMessageId() {
        return messageId;
    }

    public DspDataAddress getDataAddress() {
        return dataAddress;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {

        private final DataFlowStartedNotificationMessage instance = new DataFlowStartedNotificationMessage();

        private Builder() {
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public DataFlowStartedNotificationMessage build() {
            return instance;
        }

        public Builder messageId(String messageId) {
            instance.messageId = messageId;
            return this;
        }

        public Builder dataAddress(DspDataAddress dataAddress) {
            instance.dataAddress = dataAddress;
            return this;
        }
    }
}
