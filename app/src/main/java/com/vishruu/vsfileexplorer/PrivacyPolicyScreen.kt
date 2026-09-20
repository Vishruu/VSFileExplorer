package com.vishruu.vsfileexplorer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PrivacyPolicyScreen(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back",
                    tint = MaterialTheme.colorScheme.primary)
            }
            Text("Privacy Policy", fontSize = 20.sp, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary)
        }

        Spacer(Modifier.height(20.dp))

        PolicySection("Data Collection",
            "VS File Explorer does not collect any personal data.\n\n" +
                    "We do not track your activity, files, or location. " +
                    "No analytics are sent to external servers.")

        PolicySection("File Access",
            "This app requires storage permission to manage your files. " +
                    "All file operations happen locally on your device. " +
                    "Your files never leave your phone.")

        PolicySection("Encryption",
            "Files encrypted in the Vault are protected with AES-256-GCM. " +
                    "Passwords are stored as salted hashes and cannot be recovered. " +
                    "We do not have access to your encrypted files.")

        PolicySection("Network Features",
            "WiFi File Transfer runs a local server on your device. " +
                    "It only works on your local WiFi network and does not " +
                    "send data to any external server.\n\n" +
                    "FTP/SFTP connections are direct between your device and " +
                    "the server you configure. No data passes through us.")

        PolicySection("Permissions",
            "The app requests:\n" +
                    "• Storage — to manage files\n" +
                    "• Camera — for Intruder Selfie (optional)\n" +
                    "• Internet — for WiFi Transfer & FTP/SFTP\n\n" +
                    "All permissions can be revoked in system settings.")

        PolicySection("Third Parties",
            "We do not share your data with any third party. " +
                    "No ads. No trackers. No external analytics.")

        PolicySection("Contact",
            "For questions, contact: vishalsalve699@gmail.com")  //change mail id before publish

        Spacer(Modifier.height(24.dp))

        Text("Last updated: 2026", fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun PolicySection(title: String, body: String) {
    Spacer(Modifier.height(16.dp))
    Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary)
    Spacer(Modifier.height(6.dp))
    Text(body, fontSize = 13.sp,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
        lineHeight = 20.sp)
    Spacer(Modifier.height(8.dp))
}