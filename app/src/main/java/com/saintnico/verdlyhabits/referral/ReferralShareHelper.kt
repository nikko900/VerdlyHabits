package com.saintnico.verdlyhabits.referral

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import androidx.core.app.ShareCompat
import com.saintnico.verdlyhabits.ui.viewmodel.ReferralUiState

object ReferralShareHelper {

    fun buildInviteMessage(context: Context, state: ReferralUiState): String {
        val code = state.fullCode.ifBlank { ReferralManager.getReferralCode(context) }
        val short = state.shortCode.ifBlank { ReferralManager.shortCode(code) }
        val link = state.shareLink.ifBlank { ReferralManager.shareLink(code) }
        val deepLink = state.deepLink.ifBlank { ReferralManager.deepLink(code) }
        val playStore = "https://play.google.com/store/apps/details?id=${context.packageName}"
        val remaining = (state.friendsRequired - state.qualifiedCount).coerceAtLeast(0)

        return buildString {
            appendLine("Join me on Verdly — build habits that actually stick.")
            appendLine()
            appendLine("1) Install Verdly")
            appendLine("2) Sign up with my invite code: $code")
            appendLine("3) We both unlock Pro perks")
            appendLine()
            appendLine("Invite link: $link")
            appendLine("App link: $deepLink")
            appendLine("Play Store: $playStore")
            appendLine()
            append("Your friend gets ${ReferralManager.REFEREE_BONUS_DAYS} days of Pro. ")
            if (remaining > 0) {
                append("I'm $remaining friend${if (remaining == 1) "" else "s"} away from ")
            }
            append("${ReferralManager.REFERRER_REWARD_DAYS} days of Pro free.")
        }
    }

    fun copyInvite(context: Context, state: ReferralUiState) {
        val message = buildInviteMessage(context, state)
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Verdly invite", message))
    }

    fun shareInvite(context: Context, state: ReferralUiState): Boolean {
        val message = buildInviteMessage(context, state)
        val activity = context.findActivity()
        if (activity != null) {
            val sharedImage = runCatching {
                com.saintnico.verdlyhabits.ui.components.share.shareReferralInviteCard(
                    context = context,
                    fullCode = state.fullCode,
                    shortCode = state.shortCode,
                    shareLink = state.shareLink,
                    shareText = message,
                )
            }.getOrDefault(false)
            if (sharedImage) return true
            shareText(activity, message)
            return true
        }
        shareTextFromContext(context, message)
        return true
    }

    private fun shareText(activity: Activity, message: String) {
        ShareCompat.IntentBuilder(activity)
            .setType("text/plain")
            .setText(message)
            .setChooserTitle("Invite friends to Verdly")
            .startChooser()
    }

    private fun shareTextFromContext(context: Context, message: String) {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, message)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val chooser = Intent.createChooser(send, "Invite friends to Verdly").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    private fun Context.findActivity(): Activity? {
        var ctx: Context? = this
        while (ctx is ContextWrapper) {
            if (ctx is Activity) return ctx
            ctx = ctx.baseContext
        }
        return null
    }
}
