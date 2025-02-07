/********************************************************************************
 * Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.tractusx.edc.iam.iatp.identity;

import org.eclipse.edc.iam.identitytrust.spi.DcpParticipantAgentServiceExtension;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredential;
import org.eclipse.edc.participant.spi.ParticipantAgentServiceExtension;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.tractusx.edc.core.utils.credentials.CredentialTypePredicate;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.eclipse.edc.participant.spi.ParticipantAgent.PARTICIPANT_IDENTITY;
import static org.eclipse.tractusx.edc.edr.spi.CoreConstants.CX_CREDENTIAL_NS;

/**
 * Implementation of {@link ParticipantAgentServiceExtension} which extracts the identity of a participant
 * from the MembershipCredential
 */
public class IatpIdentityExtractor implements DcpParticipantAgentServiceExtension {

    private static final String VC_CLAIM = "vc";
    private static final String IDENTITY_CREDENTIAL = "MembershipCredential";
    private static final String IDENTITY_PROPERTY = "holderIdentifier";

    private final CredentialTypePredicate typePredicate = new CredentialTypePredicate(CX_CREDENTIAL_NS, IDENTITY_CREDENTIAL);

    @Override
    public @NotNull Map<String, String> attributesFor(ClaimToken claimToken) {
        var credentials = getCredentialList(claimToken)
                .orElseThrow(failure -> new EdcException("Failed to fetch credentials from the claim token: %s".formatted(failure.getFailureDetail())));

        return credentials.stream()
                .filter(typePredicate)
                .findFirst()
                .flatMap(this::getIdentifier)
                .map(identity -> Map.of(PARTICIPANT_IDENTITY, identity))
                .orElseThrow(() -> new EdcException("Required credential type '%s' not present in ClaimToken, cannot extract property '%s'".formatted(IDENTITY_CREDENTIAL, IDENTITY_PROPERTY)));


    }

    private Optional<String> getIdentifier(VerifiableCredential verifiableCredential) {
        return verifiableCredential.getCredentialSubject().stream()
                .flatMap(credentialSubject -> credentialSubject.getClaims().entrySet().stream())
                .filter(entry -> entry.getKey().endsWith(IDENTITY_PROPERTY))
                .map(Map.Entry::getValue)
                .map(String.class::cast)
                .findFirst();
    }

    @SuppressWarnings("unchecked")
    private Result<List<VerifiableCredential>> getCredentialList(ClaimToken claimToken) {
        var vcListClaim = claimToken.getClaims().get(VC_CLAIM);

        if (vcListClaim == null) {
            return Result.failure("ClaimToken did not contain a '%s' claim.".formatted(VC_CLAIM));
        }
        if (!(vcListClaim instanceof List)) {
            return Result.failure("ClaimToken contains a '%s' claim, but the type is incorrect. Expected %s, got %s.".formatted(VC_CLAIM, List.class.getName(), vcListClaim.getClass().getName()));
        }
        var vcList = (List<VerifiableCredential>) vcListClaim;
        if (vcList.isEmpty()) {
            return Result.failure("ClaimToken contains a '%s' claim but it did not contain any VerifiableCredentials.".formatted(VC_CLAIM));
        }
        return Result.success(vcList);
    }
}
