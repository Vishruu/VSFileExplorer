package com.vishruu.vsfileexplorer

import android.content.Context
import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AppLockScreen(context: Context, onUnlocked: () -> Unit) {
    var isSetup by remember { mutableStateOf(AppLockManager.isPasswordSetup(context)) }

    if (!isSetup) {
        AppLockSetupScreen(
            context = context,
            onSetup = { pwd ->
                if (AppLockManager.setupPassword(context, pwd)) {
                    AppLockManager.setMethod(context, AppLockManager.Method.BOTH)
                    onUnlocked()
                } else Toast.makeText(context, "Setup failed", Toast.LENGTH_SHORT).show()
            }
        )
    } else {
        AppLockUnlockScreen(
            context = context,
            onUnlocked = onUnlocked
        )
    }
}

@Composable
fun AppLockSetupScreen(context: Context, onSetup: (String) -> Unit) {
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var showPwd by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(80.dp))
        Box(Modifier.size(96.dp).clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.Lock, null, tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(52.dp))
        }
        Spacer(Modifier.height(24.dp))
        Text("Set App Lock", fontSize = 20.sp, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(8.dp))
        Text("Create a password to protect the app.\nYou can enable fingerprint later in Settings.",
            fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f))
        Spacer(Modifier.height(30.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it; error = null },
            label = { Text("Password (min 4 chars)") },
            singleLine = true,
            visualTransformation = if (showPwd) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { showPwd = !showPwd }) {
                    Icon(if (showPwd) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, "Toggle")
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = confirm,
            onValueChange = { confirm = it; error = null },
            label = { Text("Confirm password") },
            singleLine = true,
            visualTransformation = if (showPwd) VisualTransformation.None else PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        if (error != null) {
            Spacer(Modifier.height(10.dp))
            Text(error!!, fontSize = 13.sp, color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = {
                when {
                    password.length < 4 -> error = "At least 4 characters"
                    password != confirm -> error = "Passwords do not match"
                    else -> onSetup(password)
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary)
        ) { Text("Set Password", fontSize = 15.sp, fontWeight = FontWeight.Bold) }
    }
}

@Composable
fun AppLockUnlockScreen(context: Context, onUnlocked: () -> Unit) {
    var password by remember { mutableStateOf("") }
    var showPwd by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var attempts by remember { mutableIntStateOf(0) }
    var lockUntil by remember { mutableStateOf(0L) }
    var lockRemaining by remember { mutableIntStateOf(0) }
    var showPasswordField by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val method = remember { AppLockManager.getMethod(context) }
    val biometricAvailable = remember {
        val bm = BiometricManager.from(context)
        bm.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.BIOMETRIC_WEAK
        ) == BiometricManager.BIOMETRIC_SUCCESS
    }

    val useFingerprint = biometricAvailable && method != AppLockManager.Method.PASSWORD_ONLY
    val usePassword = method != AppLockManager.Method.FINGERPRINT_ONLY || !biometricAvailable

    // Try fingerprint on first show (if method allows)
    LaunchedEffect(Unit) {
        if (useFingerprint) {
            delay(300)
            showFingerprintPrompt(context as FragmentActivity, onUnlocked, onError = { })
        }
    }

    LaunchedEffect(lockUntil) {
        if (lockUntil > System.currentTimeMillis()) {
            while (System.currentTimeMillis() < lockUntil) {
                lockRemaining = ((lockUntil - System.currentTimeMillis()) / 1000).toInt() + 1
                delay(500)
            }
            lockRemaining = 0; lockUntil = 0L; attempts = 0; error = null
        }
    }

    val locked = lockRemaining > 0

    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(80.dp))
        Box(Modifier.size(96.dp).clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.Lock, null, tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(52.dp))
        }
        Spacer(Modifier.height(24.dp))
        Text("App Locked", fontSize = 20.sp, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(6.dp))
        Text(
            text = when {
                useFingerprint && usePassword -> "Use fingerprint or password"
                useFingerprint -> "Touch sensor to unlock"
                else -> "Enter password to unlock"
            },
            fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f)
        )
        Spacer(Modifier.height(30.dp))

        if (locked) {
            Text("Too many wrong attempts", fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(6.dp))
            Text("Wait: ${lockRemaining}s", fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onBackground)
        } else {
            // Fingerprint button
            if (useFingerprint) {
                IconButton(
                    onClick = {
                        showFingerprintPrompt(context as FragmentActivity, onUnlocked, onError = { msg ->
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        })
                    },
                    modifier = Modifier.size(80.dp)
                ) {
                    Icon(Icons.Filled.Fingerprint, "Fingerprint",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(64.dp))
                }
                Spacer(Modifier.height(16.dp))
            }

            // Password field
            if (usePassword && (showPasswordField || !useFingerprint)) {
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; error = null },
                    label = { Text("Password") },
                    singleLine = true,
                    visualTransformation = if (showPwd) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showPwd = !showPwd }) {
                            Icon(if (showPwd) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, "Toggle")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                if (error != null) {
                    Spacer(Modifier.height(10.dp))
                    Text(error!!, fontSize = 13.sp, color = MaterialTheme.colorScheme.error)
                }
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = {
                        if (password.isEmpty()) { error = "Enter password"; return@Button }
                        scope.launch {
                            if (AppLockManager.verifyPassword(context, password)) {
                                onUnlocked()
                            } else {
                                attempts++
                                password = ""
                                if (attempts >= 5) {
                                    lockUntil = System.currentTimeMillis() + 30_000L
                                    error = null
                                } else error = "Wrong password"
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary)
                ) { Text("Unlock", fontSize = 15.sp, fontWeight = FontWeight.Bold) }
            } else if (useFingerprint && usePassword) {
                TextButton(onClick = { showPasswordField = true }) {
                    Text("Use password instead", fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

private fun showFingerprintPrompt(
    activity: FragmentActivity,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    val executor = ContextCompat.getMainExecutor(activity)
    val prompt = BiometricPrompt(activity, executor,
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onSuccess()
            }
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                if (errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON &&
                    errorCode != BiometricPrompt.ERROR_USER_CANCELED &&
                    errorCode != BiometricPrompt.ERROR_CANCELED) {
                    onError(errString.toString())
                }
            }
            override fun onAuthenticationFailed() { }
        })
    val info = BiometricPrompt.PromptInfo.Builder()
        .setTitle("Unlock VS File Explorer")
        .setSubtitle("Use fingerprint to continue")
        .setNegativeButtonText("Use password")
        .setAllowedAuthenticators(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.BIOMETRIC_WEAK
        )
        .build()
    prompt.authenticate(info)
}