package com.referral.contract

import com.referral.model.ReferralStatus
import net.corda.core.contracts.*
import net.corda.core.contracts.clauses.*
import net.corda.core.crypto.SecureHash
import net.corda.core.random63BitValue

open class ReferralContract : Contract {
    override fun verify(tx: TransactionForContract) =
            verifyClause(tx, AllComposition(Clauses.Timestamp(), Clauses.Group()), tx.commands.select<Commands>())

    /**
     * Currently this contract only implements one command.
     * If you wish to add further commands to perhaps Amend() or Cancel() a purchase order, you would add them here. You
     * would then need to add associated clauses to handle transaction verification for the new commands.
     */
    interface Commands : CommandData {
        data class WriteReferral(override val nonce: Long = random63BitValue()) : IssueCommand, Commands
        //        // Additional commands defined below.
//        data class Amend(): TypeOnlyCommandData, Commands
    }

    /** This is a reference to the underlying legal contract template and associated parameters. */
    override val legalContractReference: SecureHash = SecureHash.sha256("referral contract template and params")

    /** This is where we implement our clauses. */
    interface Clauses {
        /** Checks for the existence of a timestamp. */
        class Timestamp : Clause<ContractState, Commands, Unit>() {
            override fun verify(tx: TransactionForContract,
                                inputs: List<ContractState>,
                                outputs: List<ContractState>,
                                commands: List<AuthenticatedObject<Commands>>,
                                groupingKey: Unit?): Set<Commands> {
                require(tx.timestamp?.midpoint != null) { "must be timestamped" }
                // We return an empty set because we don't process any commands
                return emptySet()
            }
        }

        // If you add additional clauses. Make sure to reference them within the 'Anycomposition()' clause.
        class Group : GroupClauseVerifier<ReferralState, Commands, UniqueIdentifier>(AnyComposition(CreateReferral(), InitiallyAcceptReferral(), DeclineReferral(), ApproveReferral())) {
            override fun groupStates(tx: TransactionForContract): List<TransactionForContract.InOutGroup<ReferralState, UniqueIdentifier>>
                    // Group by purchase order linearId for in / out states
                    = tx.groupStates(ReferralState::linearId)
        }

        class CreateReferral : Clause<ReferralState, Commands, UniqueIdentifier>() {
            override fun verify(tx: TransactionForContract,
                                inputs: List<ReferralState>,
                                outputs: List<ReferralState>,
                                commands: List<AuthenticatedObject<Commands>>,
                                groupingKey: UniqueIdentifier?): Set<Commands> {
                val command = tx.commands.requireSingleCommand<Commands.WriteReferral>()
                val out = outputs.single()
                if(!out.referral.status.equals(ReferralStatus.ACTIVE)) {
                    return emptySet()
                }

                requireThat {
                    // Generic constraints around generation of the issue referral transaction.
                    "No inputs should be consumed when issuing a referral." by (inputs.isEmpty())
                    "Only one output state should be created for each group." by (outputs.size == 1)
                    "The buyer and the seller cannot be the same entity." by (out.buyer != out.seller)
                    "The 'participants' and 'parties' must be the same." by (out.parties.map { it.owningKey }.containsAll(out.participants))
                    "The buyer and the seller are the parties." by (out.parties.containsAll(listOf(out.buyer, out.seller)))

                    // Purchase order specific constraints.
                    "The referral id is required" by out.referral.referralId.isNotBlank()
                    "The customer name is required" by out.referral.customerName.isNotBlank()
                    "The branch id is required" by out.referral.branchId.isNotBlank()
                    "The contact number is required" by (out.referral.contactNumber != Long.MIN_VALUE)
                    "The create date is null" by out.referral.createDate.isNotBlank()
                    "The customer size is required" by (out.referral.customerSize != null)
                    "The deal criteria is not provided" by (out.referral.dealCriteria == null)
                    "The status is provided as active" by out.referral.status.equals(ReferralStatus.ACTIVE)
                    "The partner id is provided" by out.referral.partnerName.isNotBlank()
                }

                return setOf(command.value)
            }
        }

        class InitiallyAcceptReferral : Clause<ReferralState, Commands, UniqueIdentifier>() {
            override fun verify(tx: TransactionForContract,
                                inputs: List<ReferralState>,
                                outputs: List<ReferralState>,
                                commands: List<AuthenticatedObject<Commands>>,
                                groupingKey: UniqueIdentifier?): Set<Commands> {
                val command = tx.commands.requireSingleCommand<Commands.WriteReferral>()
                val out = outputs.single()

                if(!out.referral.status.equals(ReferralStatus.PENDING)) {
                    return emptySet()
                }

                requireThat {
                    // Generic constraints around generation of the issue referral transaction.
                    "No inputs should be consumed when issuing a referral." by (inputs.isEmpty())
                    "Only one output state should be created for each group." by (outputs.size == 1)
                    "The buyer and the seller cannot be the same entity." by (out.buyer != out.seller)
                    "The 'participants' and 'parties' must be the same." by (out.parties.map { it.owningKey }.containsAll(out.participants))
                    "The buyer and the seller are the parties." by (out.parties.containsAll(listOf(out.buyer, out.seller)))

                    // Purchase order specific constraints.
                    "The referral id is required" by out.referral.referralId.isNotBlank()
                    "The customer name is required" by out.referral.customerName.isNotBlank()
                    "The branch id is required" by out.referral.branchId.isNotBlank()
                    "The contact number is required" by (out.referral.contactNumber != Long.MIN_VALUE)
                    "The create date is null" by out.referral.createDate.isNotBlank()
                    "The customer size is required" by (out.referral.customerSize != null)
                    "The deal criteria is not provided" by (out.referral.dealCriteria == null)
                    "The status is provided as pending" by out.referral.status.equals(ReferralStatus.PENDING)
                    "The partner id is provided" by out.referral.partnerName.isNotBlank()
                }

                return setOf(command.value)
            }
        }

        class DeclineReferral : Clause<ReferralState, Commands, UniqueIdentifier>() {
            override fun verify(tx: TransactionForContract,
                                inputs: List<ReferralState>,
                                outputs: List<ReferralState>,
                                commands: List<AuthenticatedObject<Commands>>,
                                groupingKey: UniqueIdentifier?): Set<Commands> {
                val command = tx.commands.requireSingleCommand<Commands.WriteReferral>()

                val out = outputs.single()
                if(!out.referral.status.equals(ReferralStatus.DECLINED)) {
                    return emptySet()
                }

                requireThat {
                    // Generic constraints around generation of the issue referral transaction.
                    "No inputs should be consumed when issuing a referral." by (inputs.isEmpty())
                    "Only one output state should be created for each group." by (outputs.size == 1)
                    "The buyer and the seller cannot be the same entity." by (out.buyer != out.seller)
                    "The 'participants' and 'parties' must be the same." by (out.parties.map { it.owningKey }.containsAll(out.participants))
                    "The buyer and the seller are the parties." by (out.parties.containsAll(listOf(out.buyer, out.seller)))

                    // Purchase order specific constraints.
                    "The referral id is required" by out.referral.referralId.isNotBlank()
                    "The customer name is required" by out.referral.customerName.isNotBlank()
                    "The branch id is required" by out.referral.branchId.isNotBlank()
                    "The contact number is required" by (out.referral.contactNumber != Long.MIN_VALUE)
                    "The create date is null" by out.referral.createDate.isNotBlank()
                    "The customer size is required" by (out.referral.customerSize != null)
                    "The status is provided as declined" by out.referral.status.equals(ReferralStatus.DECLINED)
                    "The partner id is provided" by out.referral.partnerName.isNotBlank()
                }

                return setOf(command.value)
            }
        }

        class ApproveReferral : Clause<ReferralState, Commands, UniqueIdentifier>() {
            override fun verify(tx: TransactionForContract,
                                inputs: List<ReferralState>,
                                outputs: List<ReferralState>,
                                commands: List<AuthenticatedObject<Commands>>,
                                groupingKey: UniqueIdentifier?): Set<Commands> {
                val command = tx.commands.requireSingleCommand<Commands.WriteReferral>()

                val out = outputs.single()
                if(!out.referral.status.equals(ReferralStatus.CLOSED)) {
                    return emptySet()
                }

                requireThat {
                    // Generic constraints around generation of the issue referral transaction.
                    "No inputs should be consumed when issuing a referral." by (inputs.isEmpty())
                    "Only one output state should be created for each group." by (outputs.size == 1)
                    "The buyer and the seller cannot be the same entity." by (out.buyer != out.seller)
                    "The 'participants' and 'parties' must be the same." by (out.parties.map { it.owningKey }.containsAll(out.participants))
                    "The buyer and the seller are the parties." by (out.parties.containsAll(listOf(out.buyer, out.seller)))

                    // Purchase order specific constraints.
                    "The referral id is required" by out.referral.referralId.isNotBlank()
                    "The customer name is required" by out.referral.customerName.isNotBlank()
                    "The branch id is required" by out.referral.branchId.isNotBlank()
                    "The contact number is required" by (out.referral.contactNumber != Long.MIN_VALUE)
                    "The create date is null" by out.referral.createDate.isNotBlank()
                    "The customer size is required" by (out.referral.customerSize != null)
                    "The status is provided as approved" by out.referral.status.equals(ReferralStatus.CLOSED)
                    "The partner id is provided" by out.referral.partnerName.isNotBlank()
                }

                return setOf(command.value)
            }
        }
    }
}