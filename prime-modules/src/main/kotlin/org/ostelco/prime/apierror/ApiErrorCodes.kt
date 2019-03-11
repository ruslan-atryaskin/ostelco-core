package org.ostelco.prime.apierror

enum class ApiErrorCode {
    FAILED_TO_CREATE_PAYMENT_PROFILE,
    FAILED_TO_FETCH_PAYMENT_PROFILE,
    FAILED_TO_STORE_APPLICATION_TOKEN,
    FAILED_TO_FETCH_BUNDLES,
    FAILED_TO_FETCH_PSEUDONYM_FOR_SUBSCRIBER,
    FAILED_TO_FETCH_PAYMENT_HISTORY,
    FAILED_TO_FETCH_PRODUCT_LIST,
    FAILED_TO_PURCHASE_PRODUCT,
    FAILED_TO_FETCH_REFERRALS,
    FAILED_TO_FETCH_REFERRED_BY_LIST,
    FAILED_TO_FETCH_PRODUCT_INFORMATION,
    FAILED_TO_STORE_PAYMENT_SOURCE,
    FAILED_TO_SET_DEFAULT_PAYMENT_SOURCE,
    FAILED_TO_FETCH_PAYMENT_SOURCES_LIST,
    FAILED_TO_REMOVE_PAYMENT_SOURCE,
    FAILED_TO_FETCH_CUSTOMER_ID,
    FAILED_TO_FETCH_PROFILE,
    FAILED_TO_CREATE_PROFILE,
    FAILED_TO_UPDATE_PROFILE,
    FAILED_TO_FETCH_CONSENT,
    FAILED_TO_IMPORT_OFFER,
    FAILED_TO_REFUND_PURCHASE,
    FAILED_TO_GENERATE_STRIPE_EPHEMERAL_KEY,
    FAILED_TO_FETCH_PLAN,
    FAILED_TO_FETCH_PLANS_FOR_SUBSCRIBER,
    FAILED_TO_STORE_PLAN,
    FAILED_TO_REMOVE_PLAN,
    FAILED_TO_SUBSCRIBE_TO_PLAN,
    FAILED_TO_RECORD_PLAN_INVOICE,
    FAILED_TO_FETCH_SUBSCRIPTIONS,
    FAILED_TO_CREATE_SUBSCRIPTION,
    FAILED_TO_STORE_SUBSCRIPTION,
    FAILED_TO_REMOVE_SUBSCRIPTION,
    FAILED_TO_CREATE_SCANID,
    FAILED_TO_FETCH_SCAN_INFORMATION,
    FAILED_TO_UPDATE_SCAN_RESULTS,
    FAILED_TO_FETCH_SUBSCRIBER_STATE,
    FAILED_TO_FETCH_SIM_PROFILE,
    FAILED_TO_ACTIVATE_SIM_PROFILE_WITH_HLR,
    FAILED_TO_DEACTIVATE_SIM_PROFILE_WITH_HLR,
    FAILED_TO_ACTIVATE_SIM_PROFILE,
    FAILED_TO_RESERVE_ACTIVATED_SIM_PROFILE,
    FAILED_TO_IMPORT_BATCH,
    FAILED_TO_FETCH_REGIONS
}
