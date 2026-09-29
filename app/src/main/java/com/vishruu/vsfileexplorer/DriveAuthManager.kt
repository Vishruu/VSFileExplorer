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

    /**
     * Sign-in check — GoogleSignIn pehle, fallback saved email.
     */
    fun isSignedIn(context: Context): Boolean {
        val acc = GoogleSignIn.getLastSignedInAccount(context)
        if (acc != null) return true
        // Fallback: saved email
        val email = getEmail(context)
        return !email.isNullOrEmpty()
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

    /**
     * Access token — saved email se account nikalo agar GoogleSignIn null ho.
     */
    suspend fun getAccessToken(context: Context): String? {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                // 1. GoogleSignIn account try karo
                val acc = GoogleSignIn.getLastSignedInAccount(context)
                if (acc?.account != null) {
                    return@withContext GoogleAuthUtil.getToken(
                        context,
                        acc.account!!,
                        "oauth2:$DRIVE_SCOPE"
                    )
                }

                // 2. Fallback: saved email se account banao
                val savedEmail = getEmail(context) ?: return@withContext null
                if (savedEmail.isEmpty()) return@withContext null

                val account = Account(savedEmail, "com.google")
                GoogleAuthUtil.getToken(context, account, "oauth2:$DRIVE_SCOPE")
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
}