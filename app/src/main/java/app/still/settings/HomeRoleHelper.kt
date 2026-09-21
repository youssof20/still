package app.still.settings

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings

object HomeRoleHelper {
    fun isDefaultHome(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(RoleManager::class.java) ?: return false
            if (roleManager.isRoleAvailable(RoleManager.ROLE_HOME)) {
                return roleManager.isRoleHeld(RoleManager.ROLE_HOME)
            }
        }
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val resolve = intent.resolveActivity(context.packageManager) ?: return false
        return resolve.packageName == context.packageName
    }

    /**
     * Returns an intent that opens the supported OS flow for choosing the default Home app.
     * Callers must tolerate cancellation; the app remains usable without the role.
     */
    fun createHomeRoleRequestIntent(context: Context): Intent {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(RoleManager::class.java)
            if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_HOME)) {
                return roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME)
            }
        }
        return Intent(Settings.ACTION_HOME_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    fun createHomeSettingsIntent(): Intent =
        Intent(Settings.ACTION_HOME_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}
