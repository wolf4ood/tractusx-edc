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

package org.eclipse.tractusx.edc.iatp.ih;

import org.eclipse.edc.identityhub.spi.generator.PresentationCreatorRegistry;
import org.eclipse.edc.identityhub.spi.store.CredentialStore;
import org.eclipse.edc.identityhub.spi.store.model.VerifiableCredentialResource;
import org.eclipse.edc.identitytrust.model.CredentialFormat;
import org.eclipse.edc.identitytrust.model.CredentialSubject;
import org.eclipse.edc.identitytrust.model.Issuer;
import org.eclipse.edc.identitytrust.model.VerifiableCredential;
import org.eclipse.edc.identitytrust.model.VerifiableCredentialContainer;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;

@Extension("Identity Hub extension for testing")
public class IdentityHubExtension implements ServiceExtension {

    private static final String PRIVATE_KEY = """
            -----BEGIN EC PRIVATE KEY-----
            MHcCAQEEIARDUGJgKy1yzxkueIJ1k3MPUWQ/tbQWQNqW6TjyHpdcoAoGCCqGSM49
            AwEHoUQDQgAE1l0Lof0a1yBc8KXhesAnoBvxZw5roYnkAXuqCYfNK3ex+hMWFuiX
            GUxHlzShAehR6wvwzV23bbC0tcFcVgW//A==
            -----END EC PRIVATE KEY-----
            """;
    @Inject
    private Vault vault;
    @Inject
    private PresentationCreatorRegistry registry;
    @Inject
    private CredentialStore credentialStore;

    @Override
    public void initialize(ServiceExtensionContext context) {

        var issuerDid = "did:example:dataspace_issuer";

        var did = context.getConfig().getString("edc.iam.issuer.id");

        var credential = VerifiableCredential.Builder.newInstance()
                .type("MembershipCredential")
                .credentialSubject(CredentialSubject.Builder.newInstance()
                        .claim("holderIdentifier", context.getParticipantId())
                        .build())
                .issuer(new Issuer(issuerDid, Map.of()))
                .issuanceDate(Instant.now())
                .build();

        var credentialResource = VerifiableCredentialResource.Builder.newInstance()
                .issuerId(issuerDid)
                .holderId(did)
                .credential(new VerifiableCredentialContainer(getCredential(context.getParticipantId()), CredentialFormat.JSON_LD, credential))
                .build();

        credentialStore.create(credentialResource);

        var privateKeyAlias = did + "#key-1";
        vault.storeSecret(privateKeyAlias, PRIVATE_KEY);
        registry.addKeyId(privateKeyAlias, CredentialFormat.JSON_LD);
        registry.addKeyId(privateKeyAlias, CredentialFormat.JWT);
    }


    private String getCredential(String participantId) {
        try {
            var content = getClass().getClassLoader().getResourceAsStream("credentials/" + participantId.toLowerCase() + "-membership.json").readAllBytes();
            return new String(content);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
