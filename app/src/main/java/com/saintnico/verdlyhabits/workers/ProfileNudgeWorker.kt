package com.saintnico.verdlyhabits.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.saintnico.verdlyhabits.MainActivity
import com.saintnico.verdlyhabits.engine.ProfileCompletionEngine
import com.saintnico.verdlyhabits.preferences.ThemePreference
import kotlinx.coroutines.flow.first

class ProfileNudgeWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val prefs = ThemePreference(applicationContext)
        val name = prefs.userName.first()
        val username = prefs.userUsername.first()
        val photo = prefs.userPhotoUri.first()
        val motto = prefs.userMotto.first()
        val bio = prefs.userBio.first()
        val flair = prefs.userFavoritePlant.first()

        if (ProfileCompletionEngine.isComplete(name, username, photo, motto, bio, flair)) {
            return Result.success()
        }

        val next = ProfileCompletionEngine.incompleteSteps(name, username, photo, motto, bio, flair)
            .firstOrNull()
        if (next == null) return Result.success()

        val (title, body) = when (next.id) {
            "photo" -> "Add your photo" to "A face builds trust — rivals connect faster when they see you."
            "headline" -> "Write your headline" to "One line people remember. Finish your profile story."
            "bio" -> "Tell your story" to "What are you building? A sharp bio gets challenge invites."
            "flair" -> "Pick a flair tag" to "Show your vibe — morning grinder, night owl, or consistency king."
            "username" -> "Claim your @name" to "You're still anonymous on leaderboards. Pick a handle."
            else -> "Finish your profile" to "You're ${(ProfileCompletionEngine.completionFraction(name, username, photo, motto, bio, flair) * 100).toInt()}% done — complete your card."
        }

        showNotification(title, body)
        return Result.success()
    }

    private fun showNotification(title: String, body: String) {
        val channelId = "profile_nudge"
        val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(
                NotificationChannel(channelId, "Profile Setup", NotificationManager.IMPORTANCE_DEFAULT),
            )
        }
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("open_edit_profile", true)
        }
        val pi = PendingIntent.getActivity(
            applicationContext,
            1003,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()
        nm.notify(1003, notification)
    }
}
