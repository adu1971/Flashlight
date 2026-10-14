package com.adu.flashlight

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.adu.flashlight.ui.theme.FlashlightTheme

private val Night = Color(0xFF101319)
private val Panel = Color(0xFF1B2029)
private val Amber = Color(0xFFFFC857)
private val Muted = Color(0xFFA4ADBC)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        FlashlightController.initialize(applicationContext)
        setContent {
            FlashlightTheme(dynamicColor = false, darkTheme = true) {
                FlashlightScreen()
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun FlashlightScreen() {
    val context = LocalContext.current
    val isOn by FlashlightController.isOn.collectAsState()
    val error by FlashlightController.error.collectAsState()
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var shortcutEnabled by remember { mutableStateOf(isShortcutEnabled(context)) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasCameraPermission = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED
                shortcutEnabled = isShortcutEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (!granted) FlashlightController.setError("Camera access is needed to use the flashlight.")
        else FlashlightController.clearError()
    }

    Surface(modifier = Modifier.fillMaxSize(), color = Night) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Night)
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.FlashlightOn, contentDescription = null, tint = Amber)
                Text(
                    "FLASHLIGHT",
                    modifier = Modifier.padding(start = 10.dp),
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
            }

            Spacer(Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .size(232.dp)
                    .background(if (isOn) Color(0xFF3C3321) else Panel, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(172.dp)
                        .background(if (isOn) Amber else Color(0xFF292F39), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Lightbulb,
                        contentDescription = null,
                        modifier = Modifier.size(78.dp),
                        tint = if (isOn) Night else Muted
                    )
                }
            }
            Spacer(Modifier.height(30.dp))
            Text(
                if (isOn) "Flashlight on" else "Flashlight off",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                if (isOn) "Your light is ready." else "Tap the button or use your shortcut.",
                modifier = Modifier.padding(top = 8.dp),
                color = Muted,
                fontSize = 15.sp
            )
            Spacer(Modifier.height(28.dp))
            Button(
                onClick = {
                    if (hasCameraPermission) FlashlightController.toggle()
                    else permissionLauncher.launch(Manifest.permission.CAMERA)
                },
                modifier = Modifier.fillMaxWidth().height(58.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Amber,
                    contentColor = Night
                )
            ) {
                Text(
                    if (hasCameraPermission) {
                        if (isOn) "TURN OFF" else "TURN ON"
                    } else {
                        "ALLOW CAMERA ACCESS"
                    },
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            if (error != null) {
                Text(
                    error.orEmpty(),
                    modifier = Modifier.padding(top = 12.dp),
                    color = Color(0xFFFF8A80),
                    textAlign = TextAlign.Center,
                    fontSize = 13.sp
                )
            }

            Spacer(Modifier.weight(1f))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Panel, RoundedCornerShape(20.dp))
                    .padding(18.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Settings, contentDescription = null, tint = Amber)
                    Text(
                        "QUICK TOGGLE",
                        modifier = Modifier.padding(start = 10.dp),
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
                Text(
                    "Double-press Volume Up to switch the flashlight on or off. A single press adjusts the volume after a short delay; holding it raises the volume.",
                    modifier = Modifier.padding(top = 12.dp),
                    color = Muted,
                    fontSize = 14.sp,
                    lineHeight = 21.sp
                )
                Spacer(Modifier.height(14.dp))
                Button(
                    onClick = {
                        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF303743),
                        contentColor = Color.White
                    )
                ) {
                    Text(if (shortcutEnabled) "SHORTCUT ENABLED" else "ENABLE SHORTCUT")
                }
            }
            Text(
                "Enable “Flashlight” in Accessibility settings to use the shortcut.",
                modifier = Modifier.padding(top = 12.dp, bottom = 8.dp),
                color = Muted,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun isShortcutEnabled(context: android.content.Context): Boolean {
    val manager = context.getSystemService(AccessibilityManager::class.java)
    return manager.getEnabledAccessibilityServiceList(
        android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_ALL_MASK
    ).any { it.resolveInfo.serviceInfo.packageName == context.packageName &&
        it.resolveInfo.serviceInfo.name == FlashlightAccessibilityService::class.java.name }
}
