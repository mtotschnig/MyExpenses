package org.totschnig.myexpenses.next.data.contracts

import androidx.annotation.StringRes
import org.totschnig.myexpenses.next.core.common.R

@get:StringRes
val BuiltInArea.labelRes: Int
    get() = when (this) {
        BuiltInArea.INSURANCE -> R.string.next_contract_area_insurance
        BuiltInArea.HOUSING -> R.string.next_contract_area_housing
        BuiltInArea.TELECOM -> R.string.next_contract_area_telecom
        BuiltInArea.STREAMING -> R.string.next_contract_area_streaming
        BuiltInArea.FITNESS -> R.string.next_contract_area_fitness
        BuiltInArea.MOBILITY -> R.string.next_contract_area_mobility
        BuiltInArea.FINANCE -> R.string.next_contract_area_finance
        BuiltInArea.SOFTWARE -> R.string.next_contract_area_software
        BuiltInArea.PRESS -> R.string.next_contract_area_press
        BuiltInArea.MEMBERSHIP -> R.string.next_contract_area_membership
        BuiltInArea.DONATIONS -> R.string.next_contract_area_donations
        BuiltInArea.EDUCATION -> R.string.next_contract_area_education
    }

@get:StringRes
val ContractInterval.labelRes: Int
    get() = when (this) {
        ContractInterval.WEEKLY -> R.string.next_interval_weekly
        ContractInterval.BIWEEKLY -> R.string.next_interval_biweekly
        ContractInterval.MONTHLY -> R.string.next_interval_monthly
        ContractInterval.BIMONTHLY -> R.string.next_interval_bimonthly
        ContractInterval.QUARTERLY -> R.string.next_interval_quarterly
        ContractInterval.HALF_YEARLY -> R.string.next_interval_half_yearly
        ContractInterval.YEARLY -> R.string.next_interval_yearly
    }
