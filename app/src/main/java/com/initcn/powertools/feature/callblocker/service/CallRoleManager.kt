package com.initcn.powertools.feature.callblocker.service

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent

object CallRoleManager {

    // Checks if the app currently holds the Call Screening Role.
    fun hasCallScreeningRole(context: Context): Boolean {
        val roleManager = context.getSystemService(Context.ROLE_SERVICE) as RoleManager
        return roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)
    }

    /**
     * Returns the Intent required to prompt the user to grant the Call Screening Role.
     * This intent must be launched using an ActivityResultLauncher in Compose.
     */
    fun createRoleRequestIntent(context: Context): Intent? {
        val roleManager = context.getSystemService(Context.ROLE_SERVICE) as RoleManager
        if (roleManager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING)) {
            return roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
        }
        return null
    }
}