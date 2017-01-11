package com.referral.api

import com.referral.contract.ReferralContract
import com.referral.contract.ReferralState
import com.referral.model.Referral
import com.referral.flow.ReferralFlow
import com.referral.flow.ReferralFlowResult
import com.referral.logic.CommissionEvaluator
import com.referral.model.DealCriteria
import com.referral.model.ReferralStatus
import net.corda.core.contracts.StateAndRef
import net.corda.core.node.ServiceHub
import net.corda.core.node.services.linearHeadsOfType
import javax.ws.rs.*
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response


// This API is accessible from /api/example. All paths specified below are relative to it.
@Path("referral")
class ReferralApi(val services: ServiceHub) {
    val me: String = services.myInfo.legalIdentity.name

    /**
     * Returns the party name of the node providing this end-point.
     */
    @GET
    @Path("me")
    @Produces(MediaType.APPLICATION_JSON)
    fun whoami() = mapOf("me" to me)

    /**
     * Returns all parties registered with the [NetworkMapService], the names can be used to look-up identities
     * by using the [IdentityService].
     */
    @GET
    @Path("peers")
    @Produces(MediaType.APPLICATION_JSON)
    fun getPeers() = mapOf("peers" to services.networkMapCache.partyNodes
            .map { it.legalIdentity.name }
            .filter { it != me && it != "Controller" })

    /**
     * Displays all purchase order states that exist in the vault.
     */
    @GET
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    fun getReferrals(@QueryParam("status") status : String?): List<Referral> {
        if(status == null) {
            return services.vaultService.linearHeadsOfType<ReferralState>().values.map { it -> it.state.data.referral }.toList()
        }

        return services.vaultService.linearHeadsOfType<ReferralState>().values.filter { it.state.data.referral.status.name.equals(status) }.map { it -> it.state.data.referral }.toList()
    }

    /**
     * Displays all purchase order states that exist in the vault.
     */
    @GET
    @Path("{referralId}")
    @Produces(MediaType.APPLICATION_JSON)
    fun getReferral(@PathParam("referralId") referralId : String): Referral? {
        val matchingReferral : StateAndRef<ReferralState>?  = services.vaultService.linearHeadsOfType<ReferralState>().values.find { it.state.data.referral.referralId.equals(referralId) }
        if(matchingReferral != null) {
            return matchingReferral.state.data.referral
        }

        return null
    }

    /**
     * This should only be called from the 'buyer' node. It initiates a flow to agree a referral with a
     * partner. Once the flow finishes it will have written the referral to ledger. Both the buyer and the
     * seller will be able to see it when calling /api/referral/referrals on their respective nodes.
     *
     * This end-point takes a Party name parameter as part of the path. If the serving node can't find the other party
     * in its network map cache, it will return an HTTP bad request.
     *
     * The flow is invoked asynchronously. It returns a future when the flow's call() method returns.
     */
    @POST
    @Path("{party}")
    fun createReferral(referral: Referral, @PathParam("party") partyName: String): Response {
        val otherParty = services.identityService.partyFromName(partyName)
        if (otherParty != null) {
            val state = ReferralState(referral, services.myInfo.legalIdentity, otherParty, ReferralContract())
            // The line below blocks and waits for the future to resolve.
            val result: ReferralFlowResult = services.invokeFlowAsync(ReferralFlow.Initiator::class.java, state, otherParty).resultFuture.get()
            when (result) {
                is ReferralFlowResult.Success ->
                    return Response
                            .status(Response.Status.CREATED)
                            .entity(result.message)
                            .build()
                is ReferralFlowResult.Failure ->
                    return Response
                            .status(Response.Status.BAD_REQUEST)
                            .entity(result.message)
                            .build()
            }
        } else {
            return Response.status(Response.Status.BAD_REQUEST).build()
        }
    }

    @PUT
    @Path("{referralId}/{dealCriteria}/{party}")
    fun referralCustomerApproves(@PathParam("referralId") referralId: String, @PathParam("dealCriteria") dealCriteria: DealCriteria, @PathParam("party") partyName: String): Response {
        val otherParty = services.identityService.partyFromName(partyName)

        val statesAndReferences : Collection<StateAndRef<ReferralState>> = services.vaultService.linearHeadsOfType<ReferralState>().values
        val stateAndRef = statesAndReferences.find { it.state.data.ref.equals(referralId) }

        var referralState : ReferralState? = stateAndRef?.state?.data

        referralState?.referral?.status = ReferralStatus.CLOSED
        referralState?.referral?.dealCriteria = dealCriteria

        referralState?.referral?.compensation = CommissionEvaluator.determineCompensation(referralState?.referral?.dealCriteria, referralState?.referral?.customerSize)

        if(otherParty != null) {
            // The line below blocks and waits for the future to resolve.
            val result: ReferralFlowResult = services.invokeFlowAsync(ReferralFlow.Initiator::class.java, referralState, otherParty).resultFuture.get()
            when (result) {
                is ReferralFlowResult.Success ->
                    return Response
                            .status(Response.Status.CREATED)
                            .entity(result.message)
                            .build()
                is ReferralFlowResult.Failure ->
                    return Response
                            .status(Response.Status.BAD_REQUEST)
                            .entity(result.message)
                            .build()
            }
        } else {
            return Response.status(Response.Status.BAD_REQUEST).build()
        }
    }

    @DELETE
    @Path("{referralId}/{party}")
    fun referralCustomerDeclined(@PathParam("referralId") referralId: String, @PathParam("party") partyName: String): Response {
        val otherParty = services.identityService.partyFromName(partyName)

        val statesAndReferences : Collection<StateAndRef<ReferralState>> = services.vaultService.linearHeadsOfType<ReferralState>().values
        val stateAndRef = statesAndReferences.find { it.state.data.ref.equals(referralId) }

        var referralState : ReferralState? = stateAndRef?.state?.data

        referralState?.referral?.status = ReferralStatus.DECLINED

        if(otherParty != null) {
            // The line below blocks and waits for the future to resolve.
            val result: ReferralFlowResult = services.invokeFlowAsync(ReferralFlow.Initiator::class.java, referralState, otherParty).resultFuture.get()
            when (result) {
                is ReferralFlowResult.Success ->
                    return Response
                            .status(Response.Status.CREATED)
                            .entity(result.message)
                            .build()
                is ReferralFlowResult.Failure ->
                    return Response
                            .status(Response.Status.BAD_REQUEST)
                            .entity(result.message)
                            .build()
            }
        } else {
            return Response.status(Response.Status.BAD_REQUEST).build()
        }
    }

    @PUT
    @Path("{referralId}/{party}")
    fun referralCustomerAccepted(@PathParam("referralId") referralId: String, @PathParam("party") partyName: String): Response {
        val otherParty = services.identityService.partyFromName(partyName)

        val statesAndReferences : Collection<StateAndRef<ReferralState>> = services.vaultService.linearHeadsOfType<ReferralState>().values
        val stateAndRef = statesAndReferences.find { it.state.data.ref.equals(referralId) }

        var referralState : ReferralState? = stateAndRef?.state?.data

        referralState?.referral?.status = ReferralStatus.PENDING

        if(otherParty != null) {
            // The line below blocks and waits for the future to resolve.
            val result: ReferralFlowResult = services.invokeFlowAsync(ReferralFlow.Initiator::class.java, referralState, otherParty).resultFuture.get()
            when (result) {
                is ReferralFlowResult.Success ->
                    return Response
                            .status(Response.Status.CREATED)
                            .entity(result.message)
                            .build()
                is ReferralFlowResult.Failure ->
                    return Response
                            .status(Response.Status.BAD_REQUEST)
                            .entity(result.message)
                            .build()
            }
        } else {
            return Response.status(Response.Status.BAD_REQUEST).build()
        }
    }
}
