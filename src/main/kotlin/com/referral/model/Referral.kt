package com.referral.model

import net.corda.node.api.StatesQuery
import java.util.*

/**
 * This file contains the data structures which the parties using this CorDapp will reach an agreement over. States can
 * support arbitrary complex object graphs. For a more complicated one, see
 *
 * samples/irs-demo/src/kotlin/net/corda/irs/contract/IRS.kt
 *
 * in the main Corda repo (http://github.com/corda/corda).
 *
 * These structures could be embedded within the ContractState, however for clarity we have moved them in to a separate
 * file.
 */

public class Referral {
    var referralId: String = ""
    var customerName: String = ""
    var branchId: String = ""
    var contactNumber: Long = 0
    var createDate: String = ""
    var status: ReferralStatus = ReferralStatus.ACTIVE
    var dealCriteria: DealCriteria? = null
    var customerSize: CustomerSize? = null
    var partnerName: String = ""
    var compensation: String? = null
}

enum class ReferralStatus {
    DECLINED, ACTIVE, PENDING, CLOSED
}

enum class DealCriteria {
    SMALL, MID, LARGE
}

enum class CustomerSize {
    MICRO, SMALL, MID, LARGE
}