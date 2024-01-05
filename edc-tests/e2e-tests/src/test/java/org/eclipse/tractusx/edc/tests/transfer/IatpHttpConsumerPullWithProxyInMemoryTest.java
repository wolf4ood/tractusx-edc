/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.tractusx.edc.tests.transfer;

import jakarta.json.JsonObject;
import org.eclipse.edc.iam.did.spi.document.DidDocument;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.tractusx.edc.lifecycle.ParticipantRuntime;
import org.eclipse.tractusx.edc.lifecycle.tx.TxParticipant;
import org.eclipse.tractusx.edc.lifecycle.tx.iatp.IatpParticipantWrapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import static org.eclipse.edc.junit.testfixtures.TestUtils.getFreePort;
import static org.eclipse.tractusx.edc.helpers.IatpHelperFunctions.configureDidResolver;
import static org.eclipse.tractusx.edc.helpers.IatpHelperFunctions.fetchDid;
import static org.eclipse.tractusx.edc.helpers.IatpHelperFunctions.stsConfiguration;
import static org.eclipse.tractusx.edc.helpers.IatpHelperFunctions.toPemEncoded;
import static org.eclipse.tractusx.edc.helpers.PolicyHelperFunctions.TX_NAMESPACE;
import static org.eclipse.tractusx.edc.helpers.PolicyHelperFunctions.frameworkPolicy;

@EndToEndTest
public class IatpHttpConsumerPullWithProxyInMemoryTest extends AbstractHttpConsumerPullWithProxyTest {


    protected static final URI SOKRATES_CS_SERVICE = URI.create("http://localhost:" + getFreePort() + "/api/resolution");
    protected static final URI PLATO_CS_SERVICE = URI.create("http://localhost:" + getFreePort() + "/api/resolution");
    protected static final URI STS_URI = URI.create("http://localhost:" + getFreePort() + "/api/v1/sts");
    protected static final IatpParticipantWrapper SOKRATES_IATP = new IatpParticipantWrapper(SOKRATES, STS_URI);

    @RegisterExtension
    protected static final ParticipantRuntime SOKRATES_RUNTIME = new ParticipantRuntime(
            ":edc-tests:runtime:iatp:runtime-memory-iatp-ih",
            SOKRATES.getName(),
            SOKRATES.getBpn(),
            SOKRATES_IATP.iatpConfiguration(PLATO)
    );
    protected static final IatpParticipantWrapper PLATO_IATP = new IatpParticipantWrapper(PLATO, STS_URI);
    @RegisterExtension
    protected static final ParticipantRuntime PLATO_RUNTIME = new ParticipantRuntime(
            ":edc-tests:runtime:iatp:runtime-memory-iatp-ih",
            PLATO.getName(),
            PLATO.getBpn(),
            PLATO_IATP.iatpConfiguration(SOKRATES)
    );
    protected static final TxParticipant STS = TxParticipant.Builder.newInstance()
            .name("STS")
            .id("STS")
            .build();


    @RegisterExtension
    protected static final ParticipantRuntime STS_RUNTIME = new ParticipantRuntime(
            ":edc-tests:runtime:iatp:runtime-memory-sts",
            STS.getName(),
            STS.getBpn(),
            stsConfiguration(STS, STS_URI, SOKRATES, PLATO)
    );

    @BeforeAll
    static void prepare() {
        STS_RUNTIME.getContext().getService(Vault.class).storeSecret("private_key_alias_" + SOKRATES.getName().toLowerCase(), toPemEncoded(SOKRATES_IATP.getKeyPair().getPrivate()));
        STS_RUNTIME.getContext().getService(Vault.class).storeSecret("private_key_alias_" + PLATO.getName().toLowerCase(), toPemEncoded(PLATO_IATP.getKeyPair().getPrivate()));

        var dids = new HashMap<String, DidDocument>();
        dids.put("did:example:dataspace_issuer", fetchDid("dataspace_issuer"));
        dids.put("did:example:" + SOKRATES.getName().toLowerCase(), fetchDid(SOKRATES.getName().toLowerCase(), SOKRATES_CS_SERVICE.toString()));
        dids.put("did:example:" + PLATO.getName().toLowerCase(), fetchDid(PLATO.getName().toLowerCase(), PLATO_CS_SERVICE.toString()));

        configureDidResolver(SOKRATES_RUNTIME, dids);
        configureDidResolver(PLATO_RUNTIME, dids);
    }


    @BeforeEach
    void setup() throws IOException {
        super.setup();

    }

    @Override
    protected JsonObject createTestPolicy(String bpn) {
        return frameworkPolicy(Map.of(TX_NAMESPACE + "Membership", "active"));
    }
}
