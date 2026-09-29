package com.saintnico.verdlyhabits.billing

import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode

/**
 * Soft, LinkedIn-style billing copy. Never expose Play Console, license testing,
 * HTTPS/SSL, or other developer diagnostics to the user.
 */
object BillingErrors {

    const val STORE_UNAVAILABLE =
        "Couldn't load plans right now. Check your connection and try again in a moment."

    fun userMessage(error: PurchasesError): String =
        userMessage(error.code, error.message)

    fun userMessage(code: PurchasesErrorCode?, raw: String?): String {
        val text = raw.orEmpty()
        return when (code) {
            PurchasesErrorCode.ConfigurationError,
            PurchasesErrorCode.StoreProblemError,
            -> STORE_UNAVAILABLE
            PurchasesErrorCode.ProductNotAvailableForPurchaseError ->
                "That item isn't available right now. Try again later."
            PurchasesErrorCode.PurchaseNotAllowedError ->
                "This purchase can't be completed right now."
            PurchasesErrorCode.NetworkError ->
                "Couldn't reach the store. Check your internet and try again."
            PurchasesErrorCode.PurchaseInvalidError ->
                "That purchase didn't go through. Try again in a moment."
            PurchasesErrorCode.PaymentPendingError ->
                "Your payment is still confirming. We'll update this shortly."
            else -> when {
                text.contains("configuration", ignoreCase = true) ||
                    text.contains("underlying error", ignoreCase = true) ||
                    text.contains("Billing is not available", ignoreCase = true) ||
                    text.contains("Play Store", ignoreCase = true) ||
                    text.contains("Google Play", ignoreCase = true) ||
                    text.contains("BillingUnavailable", ignoreCase = true) ->
                    STORE_UNAVAILABLE
                text.isBlank() -> "Couldn't complete that. Try again in a moment."
                looksTechnical(text) -> STORE_UNAVAILABLE
                else -> text
            }
        }
    }

    private fun looksTechnical(text: String): Boolean {
        val t = text.lowercase()
        return t.contains("https") ||
            t.contains("ssl") ||
            t.contains("certificate") ||
            t.contains("exception") ||
            t.contains("billingclient") ||
            t.contains("sku") ||
            t.contains("revenuecat") ||
            t.contains("play console") ||
            t.contains("license test") ||
            Regex("""\b[A-Z][a-zA-Z]+Error\b""").containsMatchIn(text)
    }
}
