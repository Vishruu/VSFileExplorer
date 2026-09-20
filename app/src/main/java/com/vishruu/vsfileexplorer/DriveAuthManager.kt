package com.vishruu.vsfileexplorer

import android.accounts.Account
import android.content.Context
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope

object DriveAuthManager {
    const val DRIVE_SCOPE = "https://www.googleapis.com/auth/drive"
    private const val PREFS = "vs_drive_prefs"
    private const val KEY_EMAIL = "email"

    fun getClient(context: Context): GoogleSignInClient {
        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DRIVE_SCOPE))
            .build()
        return GoogleSignIn.getClient(context, options)
    }

    fun getLastAccount(context: Context): GoogleSignInAccount? =
        GoogleSignIn.getLastSignedInAccount(context)

    fun isSignedIn(context: Context): Boolean {
        val acc = getLastAccount(context)
        return acc != null && GoogleSignIn.hasPermissions(acc, Scope(DRIVE_SCOPE))
    }

    fun saveEmail(context: Context, email: String?) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_EMAIL, email ?: "").apply()
    }

    fun getEmail(context: Context): String? {
        val e = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_EMAIL, "")
        return if (e.isNullOrEmpty()) null else e
    }

    fun signOut(context: Context) {
        getClient(context).signOut()
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
    }

    /** Access token milega (auto-refresh ke saath) */
    suspend fun getAccessToken(context: Context): String? {
        return try {
            val acc = getLastAccount(context) ?: return null
            val account: Account = acc.account ?: return null
            GoogleAuthUtil.getToken(context, account, "oauth2:$DRIVE_SCOPE")
        } catch (e: Exception) {
            null
        }
    }
}