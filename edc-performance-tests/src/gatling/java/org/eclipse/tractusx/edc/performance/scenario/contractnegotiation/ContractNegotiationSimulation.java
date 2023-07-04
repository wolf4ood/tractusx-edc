package org.eclipse.tractusx.edc.performance.scenario.contractnegotiation;


import org.eclipse.edc.connector.api.management.contractnegotiation.model.ContractOfferDescription;
import org.eclipse.edc.connector.api.management.contractnegotiation.model.NegotiationInitiateRequestDto;
import org.eclipse.edc.policy.model.Action;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.tractusx.edc.performance.scenario.BaseSim;

import java.util.List;
import java.util.function.Supplier;

import static io.gatling.javaapi.core.CoreDsl.atOnceUsers;
import static io.gatling.javaapi.core.CoreDsl.details;
import static io.gatling.javaapi.core.CoreDsl.feed;
import static io.gatling.javaapi.core.CoreDsl.global;
import static io.gatling.javaapi.core.CoreDsl.scenario;
import static io.gatling.javaapi.http.HttpDsl.http;
import static org.eclipse.tractusx.edc.performance.scenario.contractnegotiation.ContractNegotiationSimUtils.contractNegotiationFeeder;
import static org.eclipse.tractusx.edc.performance.scenario.contractnegotiation.ContractNegotiationSimUtils.negotiateContract;
import static org.eclipse.tractusx.edc.performance.scenario.contractnegotiation.ContractNegotiationSimUtils.waitForContractAgreement;


public class ContractNegotiationSimulation extends BaseSim {

    public static final String NEGOTIATE_CONTRACT_STEP = "Negotiate Contract";

    public static final String SESSION_CONTRACT_AGREEMENT_SELECTOR = "sessionContractAgreementSelector";

    public static final String SESSION_CONTRACT_NEGOTIATION_REQUEST_SELECTOR = "contractNegotiationRequestSelector";


    public ContractNegotiationSimulation() {

        Supplier<NegotiationInitiateRequestDto> negotiationSupplier = () -> NegotiationInitiateRequestDto.Builder.newInstance()
                .connectorId("id")
                .connectorAddress(PROVIDER_IDS_API_URL)
                .protocol("ids-multipart")
                .offer(ContractOfferDescription.Builder.newInstance()
                        .offerId("1:3a75736e-001d-4364-8bd4-9888490edb58")
                        .assetId("test-document")
                        .policy(Policy.Builder.newInstance()
                                .permissions(List.of(Permission.Builder.newInstance()
                                        .action(Action.Builder.newInstance().type("USE").build())
                                        .target("test-document")
                                        .build()))
                                .build())
                        .build())
                .build();

        var contractIdentifier = "contractNegotiation";
        setUp(scenario("Contract Negotiation")
                .repeat(REPEAT)
                .on(feed(contractNegotiationFeeder(contractIdentifier, negotiationSupplier))
                        .exec(negotiateContract(NEGOTIATE_CONTRACT_STEP, SESSION_CONTRACT_NEGOTIATION_REQUEST_SELECTOR, (session -> session.get(contractIdentifier))))
                        .exec(waitForContractAgreement(SESSION_CONTRACT_AGREEMENT_SELECTOR, (session -> session.getString(SESSION_CONTRACT_NEGOTIATION_REQUEST_SELECTOR)))))
                .injectOpen(atOnceUsers(AT_ONCE_USERS)))
                .protocols(http.baseUrl(CONSUMER_MANAGEMENT_API_URL))
                .assertions(
                        details(NEGOTIATE_CONTRACT_STEP).successfulRequests().count().is((long) AT_ONCE_USERS * REPEAT),
                        global().responseTime().max().lt(MAX_RESPONSE_TIME),
                        global().successfulRequests().percent().is(SUCCESS_PERCENTAGE)
                );
    }

}
