package com.saintnico.verdlyhabits.data.remote.firestore

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

data class ReferralStats(
    val fullCode: String,
    val shortCode: String,
    val qualifiedCount: Int,
    val friendsRequired: Int,
    val rewardDays: Int,
)

class ReferralRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val codesRef get() = firestore.collection("referralCodes")
    private fun signupsRef(referrerUid: String) =
        firestore.collection("referrals").document(referrerUid).collection("signups")

    suspend fun ensureCodeRegistered(fullCode: String, shortCode: String) {
        val uid = auth.currentUser?.uid ?: return
        val normalized = normalizeCode(fullCode)
        val doc = codesRef.document(normalized)
        val snap = doc.get().await()
        if (!snap.exists()) {
            doc.set(
                mapOf(
                    "ownerUid" to uid,
                    "fullCode" to normalized,
                    "shortCode" to shortCode,
                    "createdAt" to FieldValue.serverTimestamp(),
                ),
            ).await()
        } else {
            val owner = snap.getString("ownerUid")
            if (owner == uid) {
                doc.set(
                    mapOf("shortCode" to shortCode),
                    SetOptions.merge(),
                ).await()
            }
        }
        firestore.collection("users").document(uid)
            .set(mapOf("referralCode" to normalized), SetOptions.merge())
            .await()
    }

    suspend fun fetchStats(fullCode: String, friendsRequired: Int, rewardDays: Int): ReferralStats {
        val uid = auth.currentUser?.uid
        val normalized = normalizeCode(fullCode)
        val shortCode = shortCodeFrom(normalized)
        if (uid == null) {
            return ReferralStats(normalized, shortCode, 0, friendsRequired, rewardDays)
        }
        val count = signupsRef(uid).get().await().size()
        return ReferralStats(normalized, shortCode, count, friendsRequired, rewardDays)
    }

    /**
     * Records that [refereeUid] joined via [referrerCode].
     * Returns the referrer uid when attribution succeeded, else null.
     */
    suspend fun claimReferral(referrerCode: String, refereeUid: String): String? {
        var normalized = normalizeCode(referrerCode)
        if (normalized.isBlank()) return null

        var codeSnap = codesRef.document(normalized).get().await()
        if (!codeSnap.exists()) {
            val short = shortCodeFrom(normalized)
            val query = codesRef.whereEqualTo("shortCode", short).limit(1).get().await()
            codeSnap = query.documents.firstOrNull() ?: return null
            normalized = codeSnap.getString("fullCode") ?: normalized
        }

        val referrerUid = codeSnap.getString("ownerUid") ?: return null
        if (referrerUid == refereeUid) return null

        val refereeDoc = firestore.collection("users").document(refereeUid)
        val refereeSnap = refereeDoc.get().await()
        if (refereeSnap.getString("referredBy") != null) return null

        val signupRef = signupsRef(referrerUid).document(refereeUid)
        if (signupRef.get().await().exists()) return null

        val batch = firestore.batch()
        batch.set(
            signupRef,
            mapOf(
                "refereeUid" to refereeUid,
                "referrerUid" to referrerUid,
                "code" to normalized,
                "attributedAt" to FieldValue.serverTimestamp(),
                "status" to "qualified",
                "retentionBonusGranted" to false,
            ),
        )
        batch.set(
            refereeDoc,
            mapOf(
                "referredBy" to normalized,
                "referredAt" to FieldValue.serverTimestamp(),
            ),
            SetOptions.merge(),
        )
        batch.commit().await()
        return referrerUid
    }

    suspend fun resolveOwnerUid(code: String): String? {
        val normalized = normalizeCode(code)
        return codesRef.document(normalized).get().await().getString("ownerUid")
    }

    /** Referees active 7+ days → referrer earns retention bonus (once per referee). */
    suspend fun processRetentionBonuses(referrerUid: String, retentionDays: Int): Int {
        val now = System.currentTimeMillis()
        val retentionMs = retentionDays * 24L * 60 * 60 * 1000
        var granted = 0
        val docs = signupsRef(referrerUid).get().await().documents
        for (doc in docs) {
            if (doc.getBoolean("retentionBonusGranted") == true) continue
            val attributedAt = doc.getTimestamp("attributedAt")?.toDate()?.time ?: continue
            if (now - attributedAt < retentionMs) continue
            doc.reference.update("retentionBonusGranted", true).await()
            granted++
        }
        return granted
    }

    /**
     * Creator / campus vanity link — e.g. verdly.app/r/KRU.
     * Only the code owner can set it; must be unique.
     */
    suspend fun registerVanityShortCode(fullCode: String, vanity: String): Result<String> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Sign in first"))
        val normalized = normalizeCode(fullCode)
        val clean = vanity.trim().uppercase().filter { it.isLetterOrDigit() }
        if (clean.length !in 3..12) {
            return Result.failure(IllegalStateException("Use 3–12 letters or numbers"))
        }
        val taken = codesRef.whereEqualTo("shortCode", clean).limit(1).get().await()
        val existing = taken.documents.firstOrNull()
        if (existing != null && existing.getString("ownerUid") != uid) {
            return Result.failure(IllegalStateException("That link is already taken"))
        }
        val ownerDoc = codesRef.document(normalized).get().await()
        if (!ownerDoc.exists() || ownerDoc.getString("ownerUid") != uid) {
            return Result.failure(IllegalStateException("Referral code not registered"))
        }
        codesRef.document(normalized)
            .set(mapOf("shortCode" to clean, "campaignTag" to clean), SetOptions.merge())
            .await()
        return Result.success(clean)
    }

    companion object {
        fun normalizeCode(raw: String): String {
            val trimmed = raw.trim().uppercase()
            return when {
                trimmed.startsWith("VERDLY-") -> trimmed
                trimmed.length in 4..12 -> "VERDLY-$trimmed"
                else -> trimmed
            }
        }

        fun shortCodeFrom(fullCode: String): String =
            fullCode.removePrefix("VERDLY-").uppercase()
    }
}
