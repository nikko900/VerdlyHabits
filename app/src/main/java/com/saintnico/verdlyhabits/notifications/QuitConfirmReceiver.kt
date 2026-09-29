package com.saintnico.verdlyhabits.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.saintnico.verdlyhabits.data.goals.QuitConfirmationWriter
import com.saintnico.verdlyhabits.data.local.goals.ConfirmationSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class QuitConfirmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        val goalId = intent.getStringExtra(EXTRA_GOAL_ID)?.takeIf { it.isNotBlank() } ?: return
        val app = context.applicationContext
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (action) {
                    ACTION_STILL_CLEAR -> QuitConfirmationWriter.confirmStillClear(
                        context = app,
                        goalId = goalId,
                        source = ConfirmationSource.NOTIFICATION_ACTION,
                    )
                    ACTION_SLIP -> QuitConfirmationWriter.logSlip(
                        context = app,
                        goalId = goalId,
                        source = ConfirmationSource.NOTIFICATION_ACTION,
                    )
                }
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_STILL_CLEAR = "com.saintnico.verdlyhabits.QUIT_STILL_CLEAR"
        const val ACTION_SLIP = "com.saintnico.verdlyhabits.QUIT_LOG_SLIP"
        const val EXTRA_GOAL_ID = "goal_id"
    }
}
