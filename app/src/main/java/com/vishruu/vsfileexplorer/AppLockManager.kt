package com.vishruu.vsfileexplorer

import android.content.Context
import android.util.Base64
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object AppLockManager {
    private const val PREFS = "vs_applock_prefs"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_METHOD = "method"
    private const val KEY_HASH = "hash"
    private const val KEY_SALT = "salt"
    private const val SALT_SIZE = 16
    private const val ITERATIONS = 100_000
    private const val KEY_BITS = 256

    enum class Method(val label: String) {
        BOTH("Fingerprint + Password"),
        FINGERPRINT_ONLY("Fingerprint only"),
        PASSWORD_ONLY("Password only")
    }

    private fun hashPassword(password: String, salt: ByteArray): String {
        val spec = PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_BITS)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val hash = factory.generateSecret(spec).encoded
        return Base64.encodeToString(hash, Base64.NO_WRAP)
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun isEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_ENABLED, false)

    fun setEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    fun getMethod(context: Context): Method {
        val v = prefs(context).getString(KEY_METHOD, Method.BOTH.name) ?: Method.BOTH.name
        return try { Method.valueOf(v) } catch (e: Exception) { Method.BOTH }
    }

    fun setMethod(context: Context, method: Method) {
        prefs(context).edit().putString(KEY_METHOD, method.name).apply()
    }

    fun isPasswordSetup(context: Context): Boolean {
        val p = prefs(context)
        return p.contains(KEY_HASH) && p.contains(KEY_SALT)
    }

    fun setupPassword(context: Context, password: String): Boolean {
        return try {
            val salt = ByteArray(SALT_SIZE).also { SecureRandom().nextBytes(it) }
            val hash = hashPassword(password, salt)
            prefs(context).edit()
                .putString(KEY_HASH, hash)
                .putString(KEY_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
                .putBoolean(KEY_ENABLED, true)
                .apply()
            true
        } catch (e: Exception) { false }
    }

    fun verifyPassword(context: Context, password: String): Boolean {
        return try {
            val p = prefs(context)
            val storedHash = p.getString(KEY_HASH, null) ?: return false
            val saltB64 = p.getString(KEY_SALT, null) ?: return false
            val salt = Base64.decode(saltB64, Base64.NO_WRAP)
            hashPassword(password, salt) == storedHash
        } catch (e: Exception) { false }
    }

    fun changePassword(context: Context, oldPwd: String, newPwd: String): Boolean {
        if (!verifyPassword(context, oldPwd)) return false
        return setupPassword(context, newPwd)
    }

    fun disableLock(context: Context, password: String): Boolean {
        if (!verifyPassword(context, password)) return false
        setEnabled(context, false)
        return true
    }
}