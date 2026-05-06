package com.mobilerun.portal.update

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import android.util.Log
import com.mobilerun.portal.api.ApiHandler
import com.mobilerun.portal.service.AutoAcceptGate

class UpdateInstallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
        val message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE).orEmpty()
        Log.d(TAG, "Install status=$status message=$message")

        when (status) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                val confirmIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(Intent.EXTRA_INTENT)
                }
                if (confirmIntent != null) {
                    confirmIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    AutoAcceptGate.armInstall()
                    context.startActivity(confirmIntent)
                } else {
                    broadcastResult(context, success = false, message = "Install confirmation missing")
                }
            }
            PackageInstaller.STATUS_SUCCESS -> {
                UpdateChecker.pendingInstallResult =
                    InstallResult.Done(true, "Update installed successfully")
                broadcastResult(context, success = true, message = "Update installed successfully")
            }
            PackageInstaller.STATUS_FAILURE_CONFLICT -> {
                handleSignatureConflict(context)
            }
            else -> {
                if (message.contains("INSTALL_FAILED_UPDATE_INCOMPATIBLE", ignoreCase = true)) {
                    handleSignatureConflict(context)
                } else {
                    val resultMessage = "Update failed: $message"
                    UpdateChecker.pendingInstallResult =
                        InstallResult.Done(false, resultMessage)
                    broadcastResult(context, success = false, message = resultMessage)
                }
            }
        }
    }

    private fun handleSignatureConflict(context: Context) {
        UpdateChecker.saveCachedApkToDownloads(context)
        UpdateChecker.pendingInstallResult = InstallResult.SignatureConflict
        context.sendBroadcast(
            Intent(ACTION_SIGNATURE_CONFLICT)
                .setPackage(context.packageName),
        )
    }

    private fun broadcastResult(context: Context, success: Boolean, message: String) {
        context.sendBroadcast(
            Intent(ApiHandler.ACTION_INSTALL_RESULT)
                .setPackage(context.packageName)
                .putExtra(ApiHandler.EXTRA_INSTALL_SUCCESS, success)
                .putExtra(ApiHandler.EXTRA_INSTALL_MESSAGE, message),
        )
    }

    companion object {
        const val ACTION_INSTALL_STATUS = "com.mobilerun.portal.action.UPDATE_INSTALL_STATUS"
        const val ACTION_SIGNATURE_CONFLICT = "com.mobilerun.portal.action.UPDATE_SIGNATURE_CONFLICT"
        private const val TAG = "UpdateInstallReceiver"
    }
}
