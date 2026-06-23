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
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import java.util.ArrayList;
import java.util.List;

import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;

/**
 * Representation of a {@code DataAddress} as defined by the Data Plane Signaling protocol (DSP 2025 transfer
 * data-address shape). Serialized as plain JSON, mirroring
 * {@code org.eclipse.edc.signaling.domain.DspDataAddress} from the EDC reference implementation.
 */
@JsonDeserialize(builder = DspDataAddress.Builder.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public final class DspDataAddress {

    public static final String DSP_DATA_ADDRESS_ENDPOINT = EDC_NAMESPACE + "endpoint";

    @JsonProperty(TYPE)
    private final String type = "DataAddress";
    private String endpointType;
    private String endpoint;
    private List<EndpointProperty> endpointProperties = new ArrayList<>();

    private DspDataAddress() {
    }

    public String getType() {
        return type;
    }

    public String getEndpointType() {
        return endpointType;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public List<EndpointProperty> getEndpointProperties() {
        return endpointProperties;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class EndpointProperty {
        @JsonProperty(TYPE)
        private final String type = "EndpointProperty";
        private final String name;
        private final String value;

        public EndpointProperty(@JsonProperty("name") String name, @JsonProperty("value") String value) {
            this.name = name;
            this.value = value;
        }

        public String getType() {
            return type;
        }

        public String getName() {
            return name;
        }

        public String getValue() {
            return value;
        }
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {

        private final DspDataAddress instance = new DspDataAddress();

        private Builder() {
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public DspDataAddress build() {
            return instance;
        }

        public Builder endpointType(String endpointType) {
            instance.endpointType = endpointType;
            return this;
        }

        public Builder endpoint(String endpoint) {
            instance.endpoint = endpoint;
            return this;
        }

        public Builder endpointProperties(List<EndpointProperty> endpointProperties) {
            instance.endpointProperties = endpointProperties;
            return this;
        }

        public Builder property(String name, String value) {
            instance.endpointProperties.add(new EndpointProperty(name, value));
            return this;
        }
    }
}
