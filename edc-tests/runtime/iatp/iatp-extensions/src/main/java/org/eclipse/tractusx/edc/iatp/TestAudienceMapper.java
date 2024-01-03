/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.tractusx.edc.iatp;

import org.eclipse.edc.identitytrust.AudienceResolver;

import java.util.Map;
import java.util.Optional;

public class TestAudienceMapper implements AudienceResolver {

    private final Map<String, String> audienceMapping;

    public TestAudienceMapper(Map<String, String> audienceMapping) {
        this.audienceMapping = audienceMapping;
    }

    @Override
    public String resolve(String s) {
        return Optional.ofNullable(audienceMapping.get(s)).orElse(s);
    }
    
}
