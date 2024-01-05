package org.eclipse.tractusx.edc.helpers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.eclipse.edc.iam.did.spi.document.DidDocument;
import org.eclipse.edc.iam.did.spi.resolution.DidResolverRegistry;
import org.eclipse.edc.junit.testfixtures.TestUtils;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.tractusx.edc.did.DidExampleResolver;
import org.eclipse.tractusx.edc.lifecycle.ParticipantRuntime;
import org.eclipse.tractusx.edc.lifecycle.tx.TxParticipant;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.security.Key;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import static java.lang.String.format;

public class IatpHelperFunctions {

    protected static final String PUBLIC_KEY = TestUtils.getResourceFileContentAsString("ec-p256-public.pem");

    private static final ObjectMapper MAPPER = new ObjectMapper();


    public static void configureDidResolver(ParticipantRuntime runtime, Map<String, DidDocument> dids) {
        var typeManager = runtime.getContext().getService(TypeManager.class);
        var monitor = runtime.getContext().getService(Monitor.class);
        var didResolverRegistry = runtime.getContext().getService(DidResolverRegistry.class);
        var didResolver = new DidExampleResolver(typeManager.getMapper(), monitor);
        dids.forEach(didResolver::addCached);
        didResolverRegistry.register(didResolver);

    }

    public static String toPemEncoded(Key key) {
        var writer = new StringWriter();
        try (var jcaPEMWriter = new JcaPEMWriter(writer)) {
            jcaPEMWriter.writeObject(key);
        } catch (IOException e) {
            throw new EdcException("Unable to convert private in PEM format ", e);
        }
        return writer.toString();
    }

    public static DidDocument fetchDid(String participant) {
        return fetchDid(participant, null);
    }

    public static DidDocument fetchDid(String participant, String csServiceUrl) {
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

    public static Map<String, String> iatpConfiguration(TxParticipant participant, URI stsUri, URI csService, TxParticipant... others) {

        try {
            KeyPairGenerator kg = KeyPairGenerator.getInstance("EC");
            var pair = kg.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

        var did = "did:example:" + participant.getName().toLowerCase();
        var iatpConfiguration = new HashMap<>(participant.getConfiguration()) {
            {

                put("edc.iam.sts.oauth.token.url", stsUri + "/token");
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

    public static Map<String, String> stsConfiguration(TxParticipant sts, URI stsUri, TxParticipant... participants) {
        var stsConfiguration = new HashMap<String, String>() {
            {

                put("web.http.sts.port", String.valueOf(stsUri.getPort()));
                put("web.http.sts.path", stsUri.getPath());
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
            stsConfiguration.put(prefix + ".private-key.alias", "private_key_alias_" + participant.getName().toLowerCase());
        });

        var baseConfiguration = sts.getConfiguration();
        stsConfiguration.putAll(baseConfiguration);
        return stsConfiguration;
    }

}
