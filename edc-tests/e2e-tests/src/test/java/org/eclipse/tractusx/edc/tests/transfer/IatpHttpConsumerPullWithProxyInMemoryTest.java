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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.JsonObject;
import org.eclipse.edc.iam.did.spi.document.DidDocument;
import org.eclipse.edc.iam.did.spi.resolution.DidResolverRegistry;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.testfixtures.TestUtils;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.tractusx.edc.did.DidExampleResolver;
import org.eclipse.tractusx.edc.lifecycle.ParticipantRuntime;
import org.eclipse.tractusx.edc.lifecycle.tx.TxParticipant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import static java.lang.String.format;
import static org.eclipse.edc.junit.testfixtures.TestUtils.getFreePort;
import static org.eclipse.tractusx.edc.helpers.PolicyHelperFunctions.TX_NAMESPACE;
import static org.eclipse.tractusx.edc.helpers.PolicyHelperFunctions.frameworkPolicy;

@EndToEndTest
public class IatpHttpConsumerPullWithProxyInMemoryTest extends AbstractHttpConsumerPullWithProxyTest {

    protected static final String PUBLIC_KEY = TestUtils.getResourceFileContentAsString("ec-p256-public.pem");
    protected static final URI SOKRATES_CS_SERVICE = URI.create("http://localhost:" + getFreePort() + "/api/resolution");
    protected static final URI PLATO_CS_SERVICE = URI.create("http://localhost:" + getFreePort() + "/api/resolution");

    protected static final TxParticipant STS = TxParticipant.Builder.newInstance()
            .name("STS")
            .id("STS")
            .build();
    private static final URI STS_URI = URI.create("http://localhost:" + getFreePort() + "/api/v1/sts");

    @RegisterExtension
    protected static final ParticipantRuntime STS_RUNTIME = new ParticipantRuntime(
            ":edc-tests:runtime:iatp:runtime-memory-sts",
            STS.getName(),
            STS.getBpn(),
            stsConfiguration(STS, SOKRATES, PLATO)
    );
    @RegisterExtension
    protected static final ParticipantRuntime SOKRATES_RUNTIME = new ParticipantRuntime(
            ":edc-tests:runtime:iatp:runtime-memory-iatp-ih",
            SOKRATES.getName(),
            SOKRATES.getBpn(),
            iatpConfiguration(SOKRATES, SOKRATES_CS_SERVICE, PLATO)
    );
    @RegisterExtension
    protected static final ParticipantRuntime PLATO_RUNTIME = new ParticipantRuntime(
            ":edc-tests:runtime:iatp:runtime-memory-iatp-ih",
            PLATO.getName(),
            PLATO.getBpn(),
            iatpConfiguration(PLATO, PLATO_CS_SERVICE, SOKRATES)
    );
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static Map<String, String> iatpConfiguration(TxParticipant participant, URI csService, TxParticipant... others) {
        var did = "did:example:" + participant.getName().toLowerCase();
        var iatpConfiguration = new HashMap<>(participant.getConfiguration()) {
            {

                put("edc.iam.sts.oauth.token.url", STS_URI + "/token");
                put("edc.iam.sts.oauth.client.id", did);
                put("edc.iam.sts.oauth.client.secret.alias", "client_secret_alias");
                put("edc.iam.issuer.id", did);
                put("edc.ih.iam.id", participant.getBpn());
                put("tx.vault.seed.secrets", "client_secret_alias:client_secret");
                put("edc.ih.iam.publickey.pem", PUBLIC_KEY);
                put("web.http.resolution.port", String.valueOf(csService.getPort()));
                put("web.http.resolution.path", csService.getPath());
                put("edc.agent.identity.key", "client_id");
                put("edc.iam.trusted-issuer.issuer.id", "did:example:dataspace_issuer");
            }
        };

        Stream.concat(Stream.of(participant), Arrays.stream(others)).forEach(p -> {
            var prefix = format("tx.iam.iatp.audiences.%s", p.getName().toLowerCase());
            iatpConfiguration.put(prefix + ".from", p.getProtocolEndpoint().getUrl().toString());
            iatpConfiguration.put(prefix + ".to", p.getBpn());
        });
        return iatpConfiguration;
    }

    public static Map<String, String> stsConfiguration(TxParticipant sts, TxParticipant... participants) {
        var stsConfiguration = new HashMap<String, String>() {
            {

                put("web.http.sts.port", String.valueOf(STS_URI.getPort()));
                put("web.http.sts.path", STS_URI.getPath());
                put("edc.dataplane.token.validation.endpoint", "");
                put("tx.vault.seed.secrets", "client_secret_alias:client_secret");
            }
        };

        Arrays.stream(participants).forEach(participant -> {
            var prefix = format("edc.iam.sts.clients.%s", participant.getName().toLowerCase());
            stsConfiguration.put(prefix + ".name", participant.getName());
            stsConfiguration.put(prefix + ".id", "did:example:" + participant.getName().toLowerCase());
            stsConfiguration.put(prefix + ".client_id", participant.getBpn());
            stsConfiguration.put(prefix + ".secret.alias", "client_secret_alias");
            stsConfiguration.put(prefix + ".private-key.alias", "private_key_alias");
        });

        var baseConfiguration = sts.getConfiguration();
        stsConfiguration.putAll(baseConfiguration);
        return stsConfiguration;
    }

    @BeforeAll
    static void prepare() {
        var privateKey = TestUtils.getResourceFileContentAsString("ec-p256-private.pem");
        STS_RUNTIME.getContext().getService(Vault.class).storeSecret("private_key_alias", privateKey);

        var dids = new HashMap<String, DidDocument>();
        dids.put("did:example:dataspace_issuer", fetchDid("dataspace_issuer"));
        dids.put("did:example:" + SOKRATES.getName().toLowerCase(), fetchDid(SOKRATES.getName().toLowerCase(), SOKRATES_CS_SERVICE.toString()));
        dids.put("did:example:" + PLATO.getName().toLowerCase(), fetchDid(PLATO.getName().toLowerCase(), PLATO_CS_SERVICE.toString()));

        configureDidResolver(SOKRATES_RUNTIME, dids);
        configureDidResolver(PLATO_RUNTIME, dids);
    }

    static void configureDidResolver(ParticipantRuntime runtime, Map<String, DidDocument> dids) {
        var typeManager = runtime.getContext().getService(TypeManager.class);
        var monitor = runtime.getContext().getService(Monitor.class);
        var didResolverRegistry = runtime.getContext().getService(DidResolverRegistry.class);
        var didResolver = new DidExampleResolver(typeManager.getMapper(), monitor);
        dids.forEach(didResolver::addCached);
        didResolverRegistry.register(didResolver);
    }

    static DidDocument fetchDid(String participant) {
        return fetchDid(participant, null);
    }

    static DidDocument fetchDid(String participant, String csServiceUrl) {
        var template = TestUtils.getResourceFileContentAsString("did_example_template.json");
        var rawDocument = template.replace("${PARTICIPANT}", participant);
        if (csServiceUrl != null) {
            rawDocument = rawDocument.replace("${PARTICIPANT_CS}", csServiceUrl);
        }
        try {
            return MAPPER.readValue(rawDocument, DidDocument.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
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
