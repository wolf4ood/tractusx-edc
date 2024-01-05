package org.eclipse.tractusx.edc.lifecycle.tx.iatp;

import com.nimbusds.jose.jwk.Curve;
import org.eclipse.tractusx.edc.lifecycle.tx.TxParticipant;

import java.net.URI;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import static java.lang.String.format;
import static org.eclipse.edc.junit.testfixtures.TestUtils.getFreePort;
import static org.eclipse.tractusx.edc.helpers.IatpHelperFunctions.toPemEncoded;

public class IatpParticipantWrapper {

    protected final URI csService = URI.create("http://localhost:" + getFreePort() + "/api/resolution");
    private final TxParticipant participant;
    private final URI stsUri;
    private final KeyPair keyPair;


    public IatpParticipantWrapper(TxParticipant participant, URI stsUri) {
        this.participant = participant;
        this.stsUri = stsUri;
        this.keyPair = generateKeyPair();
    }


    public KeyPair getKeyPair() {
        return keyPair;
    }

    public Map<String, String> iatpConfiguration(TxParticipant... others) {
        var did = "did:example:" + participant.getName().toLowerCase();
        var iatpConfiguration = new HashMap<>(participant.getConfiguration()) {
            {

                put("edc.iam.sts.oauth.token.url", stsUri + "/token");
                put("edc.iam.sts.oauth.client.id", did);
                put("edc.iam.sts.oauth.client.secret.alias", "client_secret_alias");
                put("edc.iam.issuer.id", did);
                put("edc.ih.iam.id", participant.getBpn());
                put("tx.vault.seed.secrets", "client_secret_alias:client_secret");
                put("edc.ih.iam.publickey.pem", toPemEncoded(keyPair.getPublic()));
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

    private KeyPair generateKeyPair() {
        try {
            KeyPairGenerator gen = KeyPairGenerator.getInstance("EC");
            gen.initialize(Curve.P_256.toECParameterSpec());
            return gen.generateKeyPair();
        } catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException e) {
            throw new RuntimeException(e);
        }
    }
}
