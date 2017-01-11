package com.referral.logic

import com.referral.model.CustomerSize
import com.referral.model.DealCriteria

/**
 * Created by jrust on 12/13/2016.
 */
public class CommissionEvaluator {
    companion object {
        fun determineCompensation(dealCriteria : DealCriteria?, customerSize: CustomerSize?) : String {
            val smallDealCommissions : Array<String> = arrayOf("250", "300", "350", "400")
            val mediumDealCommissions : Array<String> = arrayOf("1000", "1250", "1500", "1750")
            val largeDealCommissions : Array<String> = arrayOf("2000", "2500", "3000", "3500")
            val commissionMatrix : Array<Array<String>> = arrayOf(smallDealCommissions, mediumDealCommissions, largeDealCommissions)
            var dealIndex : Int? = null
            var customerIndex : Int? = null

            if(DealCriteria.SMALL.equals(dealCriteria)) {
                dealIndex = 0
            } else if(DealCriteria.MID.equals(dealCriteria)) {
                dealIndex = 1
            } else if(DealCriteria.LARGE.equals(dealCriteria)) {
                dealIndex = 2
            }

            if(CustomerSize.MICRO.equals(customerSize)) {
                customerIndex = 0
            } else if(CustomerSize.SMALL.equals(customerSize)) {
                customerIndex = 1
            } else if(CustomerSize.MID.equals(customerSize)) {
                customerIndex = 2
            } else if(CustomerSize.LARGE.equals(customerSize)) {
                customerIndex = 3
            }

            if (customerIndex != null && dealIndex != null) {
                return commissionMatrix[dealIndex][customerIndex]
            }

            return ""
        }
    }
}