package org.totschnig.myexpenses.provider

// TABLES

const val TABLE_TRANSACTIONS: String = "transactions"
const val TABLE_ACCOUNTS: String = "accounts"
const val TABLE_SYNC_STATE: String = "_sync_state"
const val TABLE_CATEGORIES: String = "categories"
const val TABLE_METHODS: String = "paymentmethods"
const val TABLE_ACCOUNTTYES_METHODS: String = "accounttype_paymentmethod"
const val TABLE_TEMPLATES: String = "templates"
const val TABLE_PAYEES: String = "payee"
const val TABLE_CURRENCIES: String = "currency"
const val TABLE_PLAN_INSTANCE_STATUS: String = "planinstance_transaction"
const val TABLE_CHANGES: String = "changes"
const val TABLE_SETTINGS: String = "settings"
const val TABLE_ACCOUNT_EXCHANGE_RATES: String = "account_exchangerates"
const val TABLE_TAGS: String = "tags"
const val TABLE_TRANSACTIONS_TAGS: String = "transactions_tags"
const val TABLE_ACCOUNTS_TAGS: String = "accounts_tags"
const val TABLE_TEMPLATES_TAGS: String = "templates_tags"

/**
 * used on backup and restore
 */
const val TABLE_EVENT_CACHE: String = "event_cache"
const val TABLE_BUDGETS: String = "budgets"
const val TABLE_BUDGET_ALLOCATIONS: String = "budget_allocations"
const val TABLE_DEBTS: String = "debts"
const val TABLE_BANKS: String = "banks"
const val TABLE_ATTRIBUTES: String = "attributes"
const val TABLE_ATTACHMENTS: String = "attachments"
const val TABLE_TRANSACTION_ATTACHMENTS: String = "transaction_attachments"
const val TABLE_TRANSACTION_ATTRIBUTES: String = "transaction_attributes"
const val TABLE_ACCOUNT_ATTRIBUTES: String = "account_attributes"
const val TABLE_EQUIVALENT_AMOUNTS: String = "equivalent_amounts"
const val TABLE_PRICES: String = "prices"
const val TABLE_ACCOUNT_TYPES: String = "account_types"
const val TABLE_ACCOUNT_FLAGS: String = "account_flags"



// VIEWS
const val VIEW_COMMITTED: String = "transactions_committed"
const val VIEW_WITH_ACCOUNT: String = "transactions_with_account"
const val VIEW_UNCOMMITTED: String = "transactions_uncommitted"
const val VIEW_ALL: String = "transactions_all"
const val VIEW_TEMPLATES_ALL: String = "templates_all"
const val VIEW_TEMPLATES_UNCOMMITTED: String = "templates_uncommitted"
const val VIEW_EXTENDED: String = "transactions_extended"
const val VIEW_CHANGES_EXTENDED: String = "changes_extended"
const val VIEW_TEMPLATES_EXTENDED: String = "templates_extended"
const val VIEW_PRIORITIZED_PRICES: String = "prioritized_prices"

// Database columns
const val KEY_IS_ASSET: String = "isAsset"
const val KEY_TYPE_SORT_KEY: String = "type_sort_key"
const val KEY_SUPPORTS_RECONCILIATION: String = "supportsReconciliation"
const val KEY_DATE: String = "date"
const val KEY_VALUE_DATE: String = "value_date"
const val KEY_AMOUNT: String = "amount"

/**
 * alias that we need in order to have a common column name both for
 * 1) home aggregate,
 * 2) all other accounts
 */
const val KEY_DISPLAY_AMOUNT: String = "display_amount"
const val KEY_COMMENT: String = "comment"
const val KEY_ROWID: String = "_id"
const val KEY_CATID: String = "cat_id"
const val KEY_ACCOUNTID: String = "account_id"
const val KEY_PAYEEID: String = "payee_id"
const val KEY_TRANSFER_PEER: String = "transfer_peer"
const val KEY_METHODID: String = "method_id"
const val KEY_TITLE: String = "title"
const val KEY_LABEL: String = "label"
const val KEY_PATH: String = "path"
const val KEY_MATCHES_FILTER: String = "matches"
const val KEY_LEVEL: String = "level"
const val KEY_COLOR: String = "color"
const val KEY_TYPE: String = "type"
const val KEY_FLAG: String = "flag"
const val KEY_CURRENCY: String = "currency"
const val KEY_DESCRIPTION: String = "description"
const val KEY_OPENING_BALANCE: String = "opening_balance"
const val KEY_EQUIVALENT_OPENING_BALANCE: String = "equivalent_opening_balance"
const val KEY_USAGES: String = "usages"
const val KEY_PARENTID: String = "parent_id"
const val KEY_TRANSFER_ACCOUNT: String = "transfer_account"
const val KEY_TRANSFER_ACCOUNT_LABEL: String = "transfer_account_label"
const val KEY_TRANSFER_ACCOUNT_CURRENCY: String = "transfer_account_currency"
const val KEY_STATUS: String = "status"
const val KEY_PAYEE_NAME: String = "name"
const val KEY_SHORT_NAME: String = "short_name"
const val KEY_METHOD_LABEL: String = "method_label"
const val KEY_METHOD_ICON: String = "method_icon"
const val KEY_PAYEE_NAME_NORMALIZED: String = "name_normalized"
const val KEY_TRANSACTIONID: String = "transaction_id"
const val KEY_GROUPING: String = "grouping"
const val KEY_CR_STATUS: String = "cr_status"
const val KEY_REFERENCE_NUMBER: String = "number"
const val KEY_IS_NUMBERED: String = "is_numbered"
const val KEY_PLANID: String = "plan_id"
const val KEY_PLAN_EXECUTION: String = "plan_execution"
const val KEY_PLAN_EXECUTION_ADVANCE: String = "plan_execution_advance"
const val KEY_DEFAULT_ACTION: String = "default_action"
const val KEY_IS_DEFAULT: String = "is_default"
const val KEY_TEMPLATEID: String = "template_id"
const val KEY_INSTANCEID: String = "instance_id"
const val KEY_CODE: String = "code"
const val KEY_WEEK_START: String = "week_start"
const val KEY_GROUP_START: String = "group_start"
const val KEY_DAY: String = "day"
const val KEY_WEEK: String = "week"
const val KEY_MONTH: String = "month"
const val KEY_YEAR: String = "year"
const val KEY_YEAR_OF_WEEK_START: String = "year_of_week_start"
const val KEY_YEAR_OF_MONTH_START: String = "year_of_month_start"
const val KEY_THIS_DAY: String = "this_day"
const val KEY_THIS_WEEK: String = "this_week"
const val KEY_THIS_MONTH: String = "this_month"
const val KEY_THIS_YEAR: String = "this_year"
const val KEY_THIS_YEAR_OF_WEEK_START: String = "this_year_of_week_start"
const val KEY_THIS_YEAR_OF_MONTH_START: String = "this_year_of_month_start"
const val KEY_MAX_VALUE: String = "max_value"
const val KEY_CURRENT_BALANCE: String = "current_balance"
const val KEY_EQUIVALENT_CURRENT_BALANCE: String = "equivalent_current_balance"
const val KEY_TOTAL: String = "total"
const val KEY_EQUIVALENT_TOTAL: String = "equivalent_total"
const val KEY_CURRENT: String = "current"
const val KEY_CLEARED_TOTAL: String = "cleared_total"
const val KEY_RECONCILED_TOTAL: String = "reconciled_total"
const val KEY_SUM_EXPENSES: String = "sum_expenses"
const val KEY_SUM_INCOME: String = "sum_income"
const val KEY_SUM_TRANSFERS: String = "sum_transfers"
const val KEY_EQUIVALENT_EXPENSES: String = "equivalent_expenses"
const val KEY_EQUIVALENT_INCOME: String = "equivalent_income"
const val KEY_EQUIVALENT_TRANSFERS: String = "equivalent_transfers"
const val KEY_MAPPED_CATEGORIES: String = "mapped_categories"
const val KEY_MAPPED_PAYEES: String = "mapped_payees"
const val KEY_MAPPED_METHODS: String = "mapped_methods"
const val KEY_MAPPED_TEMPLATES: String = "mapped_templates"
const val KEY_MAPPED_TRANSACTIONS: String = "mapped_transactions"
const val KEY_MAPPED_BUDGETS: String = "mapped_budgets"
const val KEY_HAS_CLEARED: String = "has_cleared"
const val KEY_IS_AGGREGATE: String = "is_aggregate"
const val KEY_HAS_FUTURE: String =
    "has_future" //has the accounts transactions stored for future dates
const val KEY_SUM: String = "sum"
const val KEY_SORT_KEY: String = "sort_key"
const val KEY_EXCLUDE_FROM_TOTALS: String = "exclude_from_totals"
const val KEY_PREDEFINED_METHOD_NAME: String = "predefined"
const val KEY_UUID: String = "uuid"
const val KEY_URI: String = "uri"
const val KEY_URI_LIST: String = "uri_list"
const val KEY_ATTACHMENT_COUNT: String = "attachment_count"
const val KEY_SYNC_ACCOUNT_NAME: String = "sync_account_name"
const val KEY_TRANSFER_AMOUNT: String = "transfer_amount"
const val KEY_LABEL_NORMALIZED: String = "label_normalized"
const val KEY_LAST_USED: String = "last_used"
const val KEY_HAS_TRANSFERS: String = "has_transfers"
const val KEY_MAPPED_TAGS: String = "mapped_tags"
const val KEY_PLAN_INFO: String = "plan_info"
const val KEY_PARENT_UUID: String = "parent_uuid"
const val KEY_SYNC_SEQUENCE_LOCAL: String = "sync_sequence_local"
const val KEY_ACCOUNT_LABEL: String = "account_label"
const val KEY_ACCOUNT_TYPE: String = "account_type"
const val KEY_ACCOUNT_UUID: String = "account_uuid"
const val KEY_IS_SAME_CURRENCY: String = "is_same_currency"
const val KEY_TIMESTAMP: String = "timestamp"
const val KEY_KEY: String = "key"
const val KEY_VALUE: String = "value"
const val KEY_SORT_DIRECTION: String = "sort_direction"
const val KEY_SORT_BY: String = "sort_by"
const val KEY_CURRENCY_SELF: String = "currency_self"
const val KEY_CURRENCY_OTHER: String = "currency_other"
const val KEY_EXCHANGE_RATE: String = "exchange_rate"
const val KEY_ORIGINAL_AMOUNT: String = "original_amount"
const val KEY_ORIGINAL_CURRENCY: String = "original_currency"
const val KEY_EQUIVALENT_AMOUNT: String = "equivalent_amount"

/*
true if the transfer peer is either part of a split transaction or part of an archive
*/
const val KEY_TRANSFER_PEER_IS_PART: String = "transfer_peer_is_part"

/*
true if the transfer peer is archived, i.e. part of an archive
*/
const val KEY_TRANSFER_PEER_IS_ARCHIVED: String = "transfer_peer_is_archived"
const val KEY_BUDGETID: String = "budget_id"
const val KEY_START: String = "start"
const val KEY_END: String = "end"
const val KEY_TAGID: String = "tag_id"
const val KEY_TRANSFER_CURRENCY: String = "transfer_currency"
const val KEY_COUNT: String = "count"
const val KEY_TAGLIST: String = "tag_list"
const val KEY_DEBT_ID: String = "debt_id"
const val KEY_MAPPED_DEBTS: String = "mapped_debts"

/**
 * If this field is part of a projection for a query to the Methods URI, only payment methods
 * mapped to account types will be returned
 */
const val KEY_ACCOUNT_TYPE_LIST: String = "account_type_list"

/**
 * Used for both saving goal and credit limit on accounts
 */
const val KEY_CRITERION: String = "criterion"

/**
 * column alias for the second group (month or week)
 */
const val KEY_SECOND_GROUP: String = "second"

/**
 * Budget set for the grouping type that is active on an account
 */
const val KEY_BUDGET: String = "budget"

/**
 * For each budget allocation, we also store a potential rollover from the previous period
 */
const val KEY_BUDGET_ROLLOVER_PREVIOUS: String = "rollOverPrevious"

/**
 * Rollover amounts are stored redundantly, both for the period with the leftover, and for the next
 * period where it rolls to. Thus we can display this information for both periods, without
 * needing to calculate or lookup
 */
const val KEY_BUDGET_ROLLOVER_NEXT: String = "rollOverNext"
const val KEY_VISIBLE: String = "visible"
const val KEY_FLAG_LABEL: String = "flag_label"
const val KEY_FLAG_SORT_KEY: String = "flag_sort_key"
const val KEY_FLAG_ICON: String = "flag_icon"
const val METHOD_FLAG_SORT: String = "flagSort"
const val KEY_SORTED_IDS: String = "sortedIds"

/**
 * boolean flag for accounts: A sealed account can no longer be edited
 */
const val KEY_SEALED: String = "sealed"
const val KEY_HAS_SEALED_DEBT: String = "hasSealedDebt"
const val KEY_HAS_SEALED_ACCOUNT: String = "hasSealedAccount"
const val KEY_HAS_SEALED_ACCOUNT_WITH_TRANSFER: String = "hasSealedAccountWithTransfer"
const val KEY_AMOUNT_HOME_EQUIVALENT: String = "amountHomeEquivalent"

/**
 * if of a drawable resource representing a category
 */
const val KEY_ICON: String = "icon"
const val KEY_HAS_DESCENDANTS: String = "hasDescendants"

/**
 * flag for budget amounts that only apply to one period
 */
const val KEY_ONE_TIME: String = "oneTime"
const val KEY_EQUIVALENT_SUM: String = "equivalentSum"

/**
 * Bankleitzahl
 */
const val KEY_BLZ: String = "blz"

/**
 * Business Identifier Code
 */
const val KEY_BIC: String = "bic"
const val KEY_BANK_NAME: String = "name"
const val KEY_USER_ID: String = "user_id"
const val KEY_BANK_ID: String = "bank_id"
const val KEY_IBAN: String = "iban"
const val KEY_ATTRIBUTE_NAME: String = "attribute_name"
const val KEY_CONTEXT: String = "context"
const val KEY_ATTRIBUTE_ID: String = "attribute_id"
const val KEY_ATTACHMENT_ID: String = "attachment_id"
const val KEY_VERSION: String = "version"

// Prices
const val KEY_COMMODITY: String = "commodity"
const val KEY_SOURCE: String = "source"

/**
 * flag for accounts with dynamic exchange rates
 */
const val KEY_DYNAMIC: String = "dynamic"
const val KEY_LATEST_EXCHANGE_RATE: String = "latest_exchange_rate"
const val KEY_LATEST_EXCHANGE_RATE_DATE: String = "latest_exchange_rate_date"
const val KEY_ACCOUNT_TYPE_LABEL: String = "account_type_label"

//Status constants
/**
 * No special status
 */
const val STATUS_NONE: Int = 0

/**
 * transaction that already has been exported
 */
const val STATUS_EXPORTED: Int = 1

/**
 * previously split transaction (and its parts) that are currently edited, now unused
 */
const val STATUS_UNCOMMITTED: Int = 2

/**
 * a transaction that has been created as a result of an export
 * with EXPORT_HANDLE_DELETED_CREATE_HELPER
 */
const val STATUS_HELPER: Int = 3

/**
 * Status for the parent archive transaction
 */
const val STATUS_ARCHIVE: Int = 4

/**
 * Status for the transactions contained in the archive
 */
const val STATUS_ARCHIVED: Int = 5

//Magic numbers
const val SPLIT_CATID: Long = 0L
const val NULL_ROW_ID: Long = 0L
const val NULL_CHANGE_INDICATOR: String = "__NULL__"

//METHOD names
const val METHOD_TYPE_SORT: String = "typeSort"