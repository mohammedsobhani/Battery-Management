package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.BatteryRecord
import com.example.data.ChargingSession
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MainDashboard(viewModel: BatteryViewModel) {
    val currentTab by viewModel.currentTab.collectAsState()
    val guardianTriggered by viewModel.guardianTriggered.collectAsState()
    val thermalAlertTriggered by viewModel.thermalAlertTriggered.collectAsState()
    val isCleaning by viewModel.isCleaning.collectAsState()
    val cleanSuccess by viewModel.cleanSuccess.collectAsState()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            VoltBottomNav(
                activeTab = currentTab,
                onTabSelected = { viewModel.selectTab(it) }
            )
        },
        containerColor = DarkBackdrop
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(DarkBackdrop, Color(0xFF07090C))
                    )
                )
        ) {
            // Screen switching content block
            Column(modifier = Modifier.fillMaxSize()) {
                // Persistent Status Indicator Header
                VoltHeader(viewModel)

                // High priority safety alert notices
                AlertNoticesSection(
                    guardianTriggered = guardianTriggered,
                    thermalAlertTriggered = thermalAlertTriggered,
                    thermalTemp = viewModel.currentTemp,
                    guardianLimit = viewModel.chargeGuardianCap.collectAsState().value,
                    sosActive = viewModel.sosModeSelected.collectAsState().value && viewModel.currentLevel <= 15
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    AnimatedContent(
                        targetState = currentTab,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(180))
                        },
                        label = "tab_cross_fade"
                    ) { targetTab ->
                        when (targetTab) {
                            BatteryTab.DASHBOARD -> DashboardScreen(viewModel)
                            BatteryTab.AI_INSIGHTS -> AiInsightsScreen(viewModel)
                            BatteryTab.LEADERBOARD -> LeaderboardScreen(viewModel)
                            BatteryTab.SETTINGS -> SettingsScreen(viewModel)
                        }
                    }
                }
            }

            // RAM Sweeper overlay overlay animation
            if (isCleaning) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xE0090C0F))
                        .clickable(enabled = false) {},
                    contentAlignment = Alignment.Center
                ) {
                    CleaningAnimationWidget()
                }
            }

            // Optimization complete toast banner
            AnimatedVisibility(
                visible = cleanSuccess,
                enter = slideInVertically { -it } + fadeIn(),
                exit = slideOutVertically { -it } + fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
            ) {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = VoltGreen,
                        contentColor = Color(0xFF00381B)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                    modifier = Modifier
                        .padding(horizontal = 24.dp)
                        .testTag("clean_toast")
                ) {
                    Row(
                        modifier = Modifier.padding(symmetricPadding(16.dp, 12.dp)),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Canvas(modifier = Modifier.size(12.dp)) {
                            drawCircle(color = Color(0xFF00381B))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "VoltMax Core Optimised! RAM Deep-Cleaned.",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

// --- Dynamic Alert Notice Strips ---
@Composable
private fun AlertNoticesSection(
    guardianTriggered: Boolean,
    thermalAlertTriggered: Boolean,
    thermalTemp: Float,
    guardianLimit: Int,
    sosActive: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (thermalAlertTriggered) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0x24FF3366),
                    contentColor = ThermalRed
                ),
                border = BorderStroke(1.dp, ThermalRed.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Canvas(modifier = Modifier.size(8.dp)) {
                        drawCircle(color = ThermalRed)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "THERMAL CRITICAL: ${String.format("%.1f", thermalTemp)}°C! Battery is hot. Run Deep Clean to cool processor core.",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        if (guardianTriggered) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0x2400E5FF),
                    contentColor = CyberBlue
                ),
                border = BorderStroke(1.dp, CyberBlue.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Canvas(modifier = Modifier.size(8.dp)) {
                        drawCircle(color = CyberBlue)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "GUARDIAN ENGAGED: Current level is $guardianLimit% Cap hit. Stopped high-voltage charging to double battery health life!",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        if (sosActive) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0x30FF6D00),
                    contentColor = ThermalOrange
                ),
                border = BorderStroke(1.dp, ThermalOrange.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Canvas(modifier = Modifier.size(8.dp)) {
                        drawCircle(color = ThermalOrange)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "SOS EMERGENCY LINK: Battery dropped under 15% - Simulated GPS location transmitted safely, protecting security.",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

// Helper for symmetric paddings
private fun symmetricPadding(horizontal: androidx.compose.ui.unit.Dp, vertical: androidx.compose.ui.unit.Dp) = PaddingValues(horizontal, vertical, horizontal, vertical)

// --- Custom Top header layout ---
@Composable
private fun VoltHeader(viewModel: BatteryViewModel) {
    val simSettings by viewModel.simSettings.collectAsState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "VOLTMAX CORE",
                color = MutedSlate,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.6.sp,
                fontSize = 11.sp
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (viewModel.isCharging) "Charging Active" else "Discharging Base",
                    color = if (viewModel.isCharging) VoltGreen else PureWhite,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp
                )
                if (simSettings.enableSim) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "⚡ SIM",
                        color = CyberBlue,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        modifier = Modifier
                            .background(CyberBlue.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }
        }

        // Action Chip for quick boosted status
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
            modifier = Modifier.clickable { viewModel.runOneTapDeepClean() }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Canvas(modifier = Modifier.size(6.dp)) {
                    drawCircle(color = if (viewModel.isCharging) VoltGreen else CyberBlue)
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Boost RAM",
                    color = PureWhite,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp
                )
            }
        }
    }
}

// --- Screen 1: Dashboard Home Screen ---
@Composable
private fun DashboardScreen(viewModel: BatteryViewModel) {
    val records by viewModel.usageRecords.collectAsState()
    val level = viewModel.currentLevel
    val temp = viewModel.currentTemp
    val isCharging = viewModel.isCharging
    val currentMA = viewModel.currentCurrent
    val speedLabel = viewModel.getChargeSpeedLabel()
    val wattage = viewModel.getChargeSpeedWattageEstimate()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Core Ring Card
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Interactive Custom-drawn glowing circular meter
                BatteryVisualRing(
                    level = level,
                    temperature = temp,
                    isCharging = isCharging
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Diagnostic micro metrics inside the card block
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    MicroMetric("Temperature", String.format("%.1f°C", temp), if (temp > 37) ThermalOrange else CyberBlue)
                    MicroMetric("Voltage", "${String.format("%.2f", viewModel.currentVoltage)}V", MutedSlate)
                    MicroMetric("Live Current", "${if (currentMA > 0) "+" else ""}${currentMA}mA", if (currentMA > 0) VoltGreen else ThermalOrange)
                }
            }
        }

        // Live Charging speed metrics (if plugged)
        if (isCharging) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = speedLabel, color = CyberBlue, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text(text = "Real-time adapter quality report", color = MutedSlate, fontSize = 11.sp)
                    }
                    Box(
                        modifier = Modifier
                            .background(CyberBlue.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "~${wattage}W Speed",
                            color = CyberBlue,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        // Custom drawn historical graph!
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Historical Drain Rate",
                    color = PureWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Text(
                    text = "48 hours sinus usage database logs",
                    color = MutedSlate,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                CustomAreaChart(records = records)
            }
        }

        // Estimated Battery Age panel
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Battery Age Estimator",
                        color = PureWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "HEALTH SCORE: ${if (temp > 35) "93%" else "97%"}",
                        color = VoltGreen,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 11.sp
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = viewModel.getEstimatedBatteryAgeDescription(),
                    color = MutedSlate,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
    }
}

// MicroMetric component helper
@Composable
private fun MicroMetric(title: String, value: String, valueColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = title, color = MutedSlate, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = value, color = valueColor, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
    }
}

// Custom-drawn glowing circular battery meter widget
@Composable
private fun BatteryVisualRing(level: Int, temperature: Float, isCharging: Boolean) {
    val levelAnimated by animateFloatAsState(
        targetValue = level / 100f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "level_animation"
    )

    // Pulse animation for charging lightning bolts
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_lightning"
    )

    val spinAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spin"
    )

    Box(
        modifier = Modifier
            .size(190.dp)
            .testTag("battery_ring"),
        contentAlignment = Alignment.Center
    ) {
        // Custom canvas draw of circles and glows
        Canvas(modifier = Modifier.fillMaxSize()) {
            val ringWidth = 14.dp.toPx()
            val canvasSize = size.width
            val center = Offset(canvasSize / 2f, canvasSize / 2f)
            val radius = (canvasSize / 2f) - ringWidth

            // 1. Dark background ring track
            drawCircle(
                color = DarkSurfaceElevated,
                radius = radius,
                center = center,
                style = Stroke(width = ringWidth)
            )

            // 2. Flowing charging colors
            val gradientBrush = Brush.sweepGradient(
                colors = listOf(
                    VoltGreen.copy(alpha = 0.3f),
                    VoltGreen,
                    CyberBlue,
                    CyberBlue.copy(alpha = 0.3f)
                ),
                center = center
            )

            // 3. Highlighted Arc showing level
            drawArc(
                brush = if (isCharging) gradientBrush else Brush.sweepGradient(listOf(VoltGreen, VoltGreen)),
                startAngle = if (isCharging) spinAngle else -90f,
                sweepAngle = levelAnimated * 360f,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = ringWidth, cap = androidx.compose.ui.graphics.StrokeCap.Round)
            )

            // 4. Subtle center gradient radial backing
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        if (temperature > 37) ThermalOrange.copy(alpha = 0.08f) else VoltGreen.copy(alpha = 0.04f),
                        Color. someColorThatIsEmptyOrTransparent()
                    ),
                    center = center,
                    radius = radius
                ),
                radius = radius - ringWidth / 2f,
                center = center
            )
        }

        // Absolute labels mapped inside
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$level%",
                color = PureWhite,
                fontSize = 42.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-1).sp
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isCharging) {
                    Text(
                        text = "⚡ QUICK CHARGE",
                        color = CyberBlue,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.drawBehind {
                            drawCircle(color = CyberBlue, radius = 4f, alpha = pulseAlpha)
                        }
                    )
                } else {
                    Text(
                        text = "HEALTH OPTIMUM",
                        color = VoltGreen,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 0.8.sp
                    )
                }
            }
        }
    }
}

// Fallback transparent color helper
private fun Color.Companion.someColorThatIsEmptyOrTransparent() = Color.Transparent

// --- Screen 2: Gemini AI Diagnostics ---
@Composable
private fun AiInsightsScreen(viewModel: BatteryViewModel) {
    val loading by viewModel.aiLoading.collectAsState()
    val insights by viewModel.aiInsightsText.collectAsState()
    val currentLevel = viewModel.currentLevel
    val temp = viewModel.currentTemp

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // High visibility intro banner calling out Gemini
        Card(
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, VoltGreen.copy(alpha = 0.25f)),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillParentWidth()
                    .padding(20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "AI DIAGNOSTIC MATRIX",
                        color = VoltGreen,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        letterSpacing = 1.2.sp
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "Gemini Powered",
                        color = MutedSlate,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 10.sp,
                        modifier = Modifier
                            .background(MutedSlate.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Tap below to run our revolutionary AI diagnostic engine. Translates real-time cell parameters, thermal spikes, and charger types into actionable reports.",
                    color = MutedSlate,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { viewModel.requestDiagnosticInsights() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("ai_insight_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VoltGreen,
                        contentColor = Color(0xFF00381B)
                    )
                ) {
                    if (loading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color(0xFF00381B))
                    } else {
                        Text(
                            text = "COMPUTE GLOBAL DIAGNOSTIC",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.sp,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
        }

        // Insights displaying panel
        if (loading) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(color = VoltGreen, modifier = Modifier.size(40.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Querying VoltMax Neural Net...",
                        color = PureWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Correlating charging cycles & thermal records",
                        color = MutedSlate,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        } else if (insights != null) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ai_insights_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "V-MAX HEALTH DIAGNOSTICS",
                            color = CyberBlue,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = "COMPLETED",
                            color = VoltGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = insights!!,
                        color = PureWhite,
                        fontSize = 14.sp,
                        lineHeight = 22.sp,
                        fontFamily = FontFamily.SansSerif
                    )
                }
            }
        } else {
            // Friendly Empty State banner
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🔬 Diagnostics Pending",
                        color = MutedSlate,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "Press 'Compute Global Diagnostic' to analyze logs.",
                        color = MutedSlate.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

private fun Modifier.fillParentWidth() = this.fillMaxWidth()

// --- Screen 3: Gamified App Drain Leaderboard ---
@Composable
private fun LeaderboardScreen(viewModel: BatteryViewModel) {
    val drainList by viewModel.appDrainList.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // High visibility summary podium card
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "APP DRAIN LEADERBOARD",
                    color = CyberBlue,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 1.2.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "🏆 ${drainList.firstOrNull()?.appName ?: "Scanning"} is your top battery killer!",
                    color = PureWhite,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp
                )
                Text(
                    text = "Background processes isolated. Press 'Deep Clean' to kill lingering wake-locks.",
                    color = MutedSlate,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp),
                    lineHeight = 16.sp
                )
            }
        }

        // Leaderboard rank list
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Active Drain Contributions",
                    color = PureWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    drainList.forEachIndexed { index, stat ->
                        AppDrainRow(index = index, stat = stat)
                    }
                }
            }
        }
    }
}

// Single App Drain Row with customized color categories
@Composable
private fun AppDrainRow(index: Int, stat: AppDrainStat) {
    // Unique color badge mapping
    val categoryColor = when (stat.iconCategory) {
        "social" -> CyberBlue
        "video" -> Color(0xFFD0BCFF)
        "chat" -> VoltGreen
        "game" -> ThermalOrange
        else -> MutedSlate
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Ranking spot number badge
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(
                    if (index == 0) ThermalOrange.copy(alpha = 0.2f) else DarkSurfaceElevated,
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "${index + 1}",
                color = if (index == 0) ThermalOrange else PureWhite,
                fontWeight = FontWeight.Black,
                fontSize = 11.sp
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Visual icon pill
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(categoryColor.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            val iconChar = stat.appName.first().toString()
            Text(text = iconChar, color = categoryColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Name and details block
        Column(modifier = Modifier.weight(1f)) {
            Text(text = stat.appName, color = PureWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(text = stat.packageName, color = MutedSlate, fontSize = 10.sp)
        }

        // Percentage indicator and customized bar
        Column(
            horizontalAlignment = Alignment.End,
            modifier = Modifier.width(80.dp)
        ) {
            Text(
                text = "${String.format("%.1f", stat.drainPercent)}%",
                color = if (index == 0) ThermalOrange else PureWhite,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { stat.drainPercent / 100f },
                color = if (index == 0) ThermalOrange else categoryColor,
                trackColor = DarkSurfaceElevated,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
            )
        }
    }
}

// --- Screen 4: Settings & Interactive Live Simulation Deck ---
@Composable
private fun SettingsScreen(viewModel: BatteryViewModel) {
    val context = LocalContext.current
    val guardianLimit by viewModel.chargeGuardianCap.collectAsState()
    val autoKill by viewModel.autoKillScheduler.collectAsState()
    val sosActive by viewModel.sosModeSelected.collectAsState()
    val phoneNum by viewModel.sosContactNumber.collectAsState()
    val sosMsg by viewModel.sosMessage.collectAsState()
    val simSettings by viewModel.simSettings.collectAsState()

    var showSimEditDeck by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- 1. Charge Guardian Section ---
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "CHARGE GUARDIAN™ ALARMS",
                    color = CyberBlue,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 1.2.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Longevity Stop: $guardianLimit%",
                    color = PureWhite,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp
                )
                Text(
                    text = "Auto-Cap limit prevents heat and high electrical cell stress past $guardianLimit%, extending life spans by up to 2 years.",
                    color = MutedSlate,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp),
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                Slider(
                    value = guardianLimit.toFloat(),
                    onValueChange = { viewModel.updateGuardianCap(it.toInt()) },
                    valueRange = 70f..100f,
                    colors = SliderDefaults.colors(
                        thumbColor = VoltGreen,
                        activeTrackColor = VoltGreen,
                        inactiveTrackColor = DarkSurfaceElevated
                    ),
                    modifier = Modifier.testTag("guardian_slider")
                )
            }
        }

        // --- 2. Auto-kill scheduler & SOS Safety configs ---
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(text = "Security & Scheduler Links", color = PureWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)

                // Auto sweep toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Auto-Kill Scheduled Sweep", color = PureWhite, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text(text = "Runs optimization automatically hourly", color = MutedSlate, fontSize = 11.sp)
                    }
                    Switch(
                        checked = autoKill,
                        onCheckedChange = { viewModel.toggleAutoKill(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = VoltGreen)
                    )
                }

                Divider(color = DarkSurfaceElevated, thickness = 1.dp)

                // SOS Trigger alert toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Low Battery SOS Dispatch", color = PureWhite, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text(text = "Transmits location immediately at 10% battery", color = MutedSlate, fontSize = 11.sp)
                    }
                    Switch(
                        checked = sosActive,
                        onCheckedChange = { viewModel.toggleSosMode(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = VoltGreen),
                        modifier = Modifier.testTag("sos_switch")
                    )
                }

                // If SOS is active, show customizable fields
                if (sosActive) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = phoneNum,
                            onValueChange = { viewModel.updateSosNumber(it) },
                            label = { Text("SOS Safety Contact") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = VoltGreen,
                                unfocusedBorderColor = DarkSurfaceElevated,
                                focusedTextColor = PureWhite,
                                unfocusedTextColor = PureWhite
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = sosMsg,
                            onValueChange = { viewModel.updateSosMessage(it) },
                            label = { Text("SOS Message Template") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = VoltGreen,
                                unfocusedBorderColor = DarkSurfaceElevated,
                                focusedTextColor = PureWhite,
                                unfocusedTextColor = PureWhite
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        // --- 3. TEST & SIMULATION DECK (Genuinely Innovative Browser Preview Tool) ---
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
            border = BorderStroke(1.dp, CyberBlue.copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "LIVE DEVELOPER SIMULATOR", color = CyberBlue, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        Text(text = "Interactive preview diagnostics", color = MutedSlate, fontSize = 10.sp)
                    }
                    Button(
                        onClick = { showSimEditDeck = !showSimEditDeck },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DarkSurface)
                    ) {
                        Text(text = if (showSimEditDeck) "Hide Deck" else "Open Deck", color = PureWhite, fontSize = 10.sp)
                    }
                }

                if (showSimEditDeck) {
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Enable Emulator Simulator Controls", color = PureWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Switch(
                            checked = simSettings.enableSim,
                            onCheckedChange = { active ->
                                viewModel.updateSimulation(
                                    enable = active,
                                    level = simSettings.simLevel,
                                    temp = simSettings.simTemp,
                                    current = simSettings.simCurrent,
                                    charging = simSettings.simIsCharging
                                )
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = CyberBlue)
                        )
                    }

                    if (simSettings.enableSim) {
                        Spacer(modifier = Modifier.height(14.dp))

                        // Sim battery level slider
                        Text(text = "Simulated Battery Level: ${simSettings.simLevel}%", color = PureWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Slider(
                            value = simSettings.simLevel.toFloat(),
                            onValueChange = {
                                viewModel.updateSimulation(
                                    enable = true,
                                    level = it.toInt(),
                                    temp = simSettings.simTemp,
                                    current = simSettings.simCurrent,
                                    charging = simSettings.simIsCharging
                                )
                            },
                            valueRange = 1f..100f
                        )

                        // Sim Temperature slider
                        Text(text = "Simulated Temperature: ${String.format("%.1f", simSettings.simTemp)}°C", color = PureWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Slider(
                            value = simSettings.simTemp,
                            onValueChange = {
                                viewModel.updateSimulation(
                                    enable = true,
                                    level = simSettings.simLevel,
                                    temp = it,
                                    current = simSettings.simCurrent,
                                    charging = simSettings.simIsCharging
                                )
                            },
                            valueRange = 15f..50f
                        )

                        // Sim Current slider
                        Text(text = "Simulated Current: ${simSettings.simCurrent}mA", color = PureWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Slider(
                            value = simSettings.simCurrent.toFloat(),
                            onValueChange = {
                                viewModel.updateSimulation(
                                    enable = true,
                                    level = simSettings.simLevel,
                                    temp = simSettings.simTemp,
                                    current = it.toInt(),
                                    charging = simSettings.simIsCharging
                                )
                            },
                            valueRange = -500f..4500f
                        )

                        // Sim Charging Switch state
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "Simulate Plugged charging active", color = PureWhite, fontSize = 11.sp)
                            Switch(
                                checked = simSettings.simIsCharging,
                                onCheckedChange = { charging ->
                                    viewModel.updateSimulation(
                                        enable = true,
                                        level = simSettings.simLevel,
                                        temp = simSettings.simTemp,
                                        current = if (charging) 3200 else -180,
                                        charging = charging
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
    }
}

// --- Custom-drawn Clean Canvas Historical Trend Line Chart ---
@Composable
private fun CustomAreaChart(records: List<BatteryRecord>) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .testTag("history_chart")
    ) {
        val width = size.width
        val height = size.height

        if (records.isEmpty()) {
            drawCircle(color = VoltGreen, radius = 4f, center = Offset(width / 2f, height / 2f))
            return@Canvas
        }

        // Standard scaling calculations
        val maxPoints = records.size
        val levelPoints = records.map { it.level }
        val maxVal = 100f
        val minVal = 0f

        val stepX = width / (maxPoints - 1).coerceAtLeast(1)

        val fillPath = Path()
        val linePath = Path()

        records.forEachIndexed { idx, record ->
            val x = idx * stepX
            val normalY = (record.level - minVal) / (maxVal - minVal)
            val y = height - (normalY * (height - 30f)) - 10f // leaves margin

            if (idx == 0) {
                linePath.moveTo(x, y)
                fillPath.moveTo(x, height)
                fillPath.lineTo(x, y)
            } else {
                linePath.lineTo(x, y)
                fillPath.lineTo(x, y)
            }

            if (idx == records.size - 1) {
                fillPath.lineTo(x, height)
                fillPath.close()
            }
        }

        // Draw nice grid dividers
        val gridLines = 4
        for (i in 1..gridLines) {
            val gridY = (height / (gridLines + 1)) * i
            drawLine(
                color = GridCyan,
                start = Offset(0f, gridY),
                end = Offset(width, gridY),
                strokeWidth = 1f
            )
        }

        // Draw the smooth background area gradient under the curve
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    VoltGreen.copy(alpha = 0.22f),
                    VoltGreen.copy(alpha = 0.01f)
                )
            )
        )

        // Draw the solid trend border line
        drawPath(
            path = linePath,
            color = VoltGreen,
            style = Stroke(width = 4f)
        )
    }
}

// --- Standard Tab Navigation with active pills ---
@Composable
private fun VoltBottomNav(
    activeTab: BatteryTab,
    onTabSelected: (BatteryTab) -> Unit
) {
    NavigationBar(
        containerColor = DarkSurface,
        windowInsets = WindowInsets.navigationBars
    ) {
        NavigationBarItem(
            selected = activeTab == BatteryTab.DASHBOARD,
            onClick = { onTabSelected(BatteryTab.DASHBOARD) },
            icon = { Icon(Icons.Default.Home, contentDescription = "Home Core") },
            label = { Text("Core", fontSize = 10.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = VoltGreen,
                selectedTextColor = VoltGreen,
                indicatorColor = DarkSurfaceElevated,
                unselectedIconColor = MutedSlate,
                unselectedTextColor = MutedSlate
            )
        )

        NavigationBarItem(
            selected = activeTab == BatteryTab.AI_INSIGHTS,
            onClick = { onTabSelected(BatteryTab.AI_INSIGHTS) },
            icon = { Icon(Icons.Default.Build, contentDescription = "AI Diagnostic Panel") },
            label = { Text("Diagnostic", fontSize = 10.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = VoltGreen,
                selectedTextColor = VoltGreen,
                indicatorColor = DarkSurfaceElevated,
                unselectedIconColor = MutedSlate,
                unselectedTextColor = MutedSlate
            )
        )

        NavigationBarItem(
            selected = activeTab == BatteryTab.LEADERBOARD,
            onClick = { onTabSelected(BatteryTab.LEADERBOARD) },
            icon = { Icon(Icons.Default.List, contentDescription = "Leaderboard app list") },
            label = { Text("Leader", fontSize = 10.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = VoltGreen,
                selectedTextColor = VoltGreen,
                indicatorColor = DarkSurfaceElevated,
                unselectedIconColor = MutedSlate,
                unselectedTextColor = MutedSlate
            )
        )

        NavigationBarItem(
            selected = activeTab == BatteryTab.SETTINGS,
            onClick = { onTabSelected(BatteryTab.SETTINGS) },
            icon = { Icon(Icons.Default.Settings, contentDescription = "Settings configuration") },
            label = { Text("Configs", fontSize = 10.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = VoltGreen,
                selectedTextColor = VoltGreen,
                indicatorColor = DarkSurfaceElevated,
                unselectedIconColor = MutedSlate,
                unselectedTextColor = MutedSlate
            )
        )
    }
}

// Custom-designed orbit animation for Deep RAM Clean actions
@Composable
private fun CleaningAnimationWidget() {
    val infiniteTransition = rememberInfiniteTransition(label = "sweeping_loop")
    val orbitAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbit_motion"
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier.size(100.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val coreRadius = 24.dp.toPx()
                val sweepRadius = 40.dp.toPx()

                // Static inner core glowing reactor
                drawCircle(color = VoltGreen.copy(alpha = 0.2f), radius = coreRadius + 10f)
                drawCircle(color = VoltGreen, radius = coreRadius)

                // Outer rotating sweeping orbital probe
                val angleRad = Math.toRadians(orbitAngle.toDouble())
                val orbitX = (center.x + sweepRadius * Math.cos(angleRad)).toFloat()
                val orbitY = (center.y + sweepRadius * Math.sin(angleRad)).toFloat()

                // Orbital ring track
                drawCircle(
                    color = CyberBlue.copy(alpha = 0.25f),
                    radius = sweepRadius,
                    style = Stroke(width = 2f)
                )

                // Flying orbital satellite tip
                drawCircle(
                    color = CyberBlue,
                    radius = 8.dp.toPx(),
                    center = Offset(orbitX, orbitY)
                )
            }
        }
        Spacer(modifier = Modifier.height(18.dp))
        Text(
            text = "DEEP CLEANING RAM SPACE...",
            color = VoltGreen,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 13.sp,
            letterSpacing = 1.6.sp
        )
        Text(
            text = "Terminating orphan threads & cooling core",
            color = MutedSlate,
            fontSize = 11.sp,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
