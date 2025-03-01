package com.example.scrollstop

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {

    companion object {
        private const val OVERLAY_PERMISSION_REQUEST_CODE = 1001
        private const val ACCESSIBILITY_PERMISSION_REQUEST_CODE = 1002
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ScrollStopScreen(
                        onOverlayPermissionClick = { requestOverlayPermission() },
                        onAccessibilityPermissionClick = { requestAccessibilityPermission() },
                        onStartServiceClick = {
                            if (checkPermissions()) {
                                Toast.makeText(this, "Service started. You can now switch to other apps.", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(this, "Please enable all required permissions first.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }
        }
    }

    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivityForResult(intent, OVERLAY_PERMISSION_REQUEST_CODE)
        } else {
            Toast.makeText(this, "Overlay permission already granted", Toast.LENGTH_SHORT).show()
        }
    }

    private fun requestAccessibilityPermission() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        startActivityForResult(intent, ACCESSIBILITY_PERMISSION_REQUEST_CODE)
        Toast.makeText(
            this,
            "Please enable 'Scroll Stop' in the Accessibility settings",
            Toast.LENGTH_LONG
        ).show()
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == OVERLAY_PERMISSION_REQUEST_CODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "Overlay permission granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Overlay permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkPermissions(): Boolean {
        var overlayPermissionGranted = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            overlayPermissionGranted = Settings.canDrawOverlays(this)
        }

        val accessibilityPermissionGranted = isAccessibilityServiceEnabled()

        return overlayPermissionGranted && accessibilityPermissionGranted
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val accessibilityManager = getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = accessibilityManager.getEnabledAccessibilityServiceList(
            AccessibilityServiceInfo.FEEDBACK_ALL_MASK
        )

        for (service in enabledServices) {
            if (service.resolveInfo.serviceInfo.packageName == packageName) {
                return true
            }
        }
        return false
    }
}

@Composable
fun ScrollStopScreen(
    onOverlayPermissionClick: () -> Unit,
    onAccessibilityPermissionClick: () -> Unit,
    onStartServiceClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Scroll Stop App",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Text(
            text = "This app will block scroll events in other applications and exit after 3 scroll attempts.",
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Button(
            onClick = onOverlayPermissionClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Grant Overlay Permission")
        }

        Button(
            onClick = onAccessibilityPermissionClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Grant Accessibility Permission")
        }

        Button(
            onClick = onStartServiceClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Start Scroll Blocker")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Instructions:\n1. Grant all permissions\n2. Start the service\n3. Switch to any app\n4. The app will block scrolling and exit after 3 attempts",
            fontSize = 16.sp,
            textAlign = TextAlign.Start,
            modifier = Modifier.padding(top = 16.dp)
        )
    }
}