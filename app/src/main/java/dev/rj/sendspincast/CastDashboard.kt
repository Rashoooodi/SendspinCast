package dev.rj.sendspincast

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.chrisbanes.haze.HazeProgressive
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.sin

private val Ink = Color(0xFF0C0C12)
private val Lilac = Color(0xFFD6C2FF)
private val Pink = Color(0xFFFFB7D3)
private val Muted = Color(0xFFADACBA)
private val White = Color(0xFFF6F3FC)

@Composable
fun CastDashboard(onToggle: () -> Unit, onCopy: () -> Unit, onShare: () -> Unit) {
    val context = LocalContext.current
    val scheme = if (Build.VERSION.SDK_INT >= 31) dynamicDarkColorScheme(context) else darkColorScheme(
        primary = Lilac,
        secondary = Pink,
        background = Ink,
        surface = Color(0xFF21202C)
    )
    var isCasting by remember { mutableStateOf(CastService.isRunning) }
    var error by remember { mutableStateOf(CastService.lastError) }
    LaunchedEffect(Unit) {
        while (true) {
            isCasting = CastService.isRunning
            error = CastService.lastError
            delay(600)
        }
    }
    val accent = scheme.primary
    val ip = CastConfig.localIp() ?: "No local IP"
    val haze = remember { HazeState() }

    MaterialTheme(colorScheme = scheme, typography = Typography()) {
        Box(Modifier.fillMaxSize().background(Ink)) {
            AuroraBackground(isCasting, haze, accent)
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 20.dp, end = 20.dp, top = 28.dp, bottom = 38.dp
                ),
                verticalArrangement = Arrangement.spacedBy(19.dp)
            ) {
                item { TopLine(accent, isCasting) }
                item {
                    Column(modifier = Modifier.padding(top = 12.dp, bottom = 2.dp)) {
                        Text("Your phone.\nEvery speaker.",
                            fontSize = 37.sp,
                            fontWeight = FontWeight.SemiBold,
                            lineHeight = 41.sp,
                            color = White,
                            letterSpacing = (-1.3).sp)
                        Spacer(Modifier.height(10.dp))
                        Text("Cast the sound, not your screen.",
                            color = Muted, fontSize = 15.sp)
                    }
                }
                item {
                    GlassPanel(haze) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("LIVE BROADCAST", color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                                StatusChip(isCasting, accent)
                            }
                            WaveOrb(isCasting, accent)
                            Text(if (isCasting) "You're on air" else "Ready when you are",
                                fontSize = 23.sp, fontWeight = FontWeight.SemiBold, color = White)
                            Spacer(Modifier.height(6.dp))
                            Text(if (isCasting) "Audio is streaming to your local network" else "Capture audio from your apps",
                                fontSize = 13.sp, color = Muted, textAlign = TextAlign.Center)
                            Spacer(Modifier.height(23.dp))
                            Button(
                                onClick = onToggle,
                                modifier = Modifier.fillMaxWidth().height(60.dp),
                                shape = RoundedCornerShape(21.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isCasting) Color(0xFF44394F) else accent,
                                    contentColor = if (isCasting) White else Color(0xFF1B1226)
                                )
                            ) {
                                Icon(if (isCasting) Icons.Rounded.Stop else Icons.Rounded.PlayArrow, null,
                                    modifier = Modifier.size(24.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(if (isCasting) "Stop casting" else "Start casting",
                                    fontWeight = FontWeight.Bold, fontSize = 17.sp)
                            }
                        }
                    }
                }
                item {
                    GlassPanel(haze) {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Link, null, tint = accent, modifier = Modifier.size(22.dp))
                                Spacer(Modifier.width(10.dp))
                                Text("Stream link", color = White, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                            }
                            Box(
                                Modifier.fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0xA40D1019))
                                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                                    .padding(15.dp)
                            ) {
                                Column {
                                    Text("LOCAL ADDRESS", fontSize = 10.sp, color = Muted, letterSpacing = 1.6.sp)
                                    Spacer(Modifier.height(6.dp))
                                    Text("http://$ip:8765/live.aac",
                                        color = White, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    Text("Private access key hidden", color = Muted, fontSize = 12.sp,
                                        modifier = Modifier.padding(top = 5.dp))
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(
                                    onClick = onCopy,
                                    modifier = Modifier.weight(1f).height(48.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = accent.copy(alpha = 0.18f),
                                        contentColor = accent
                                    )
                                ) {
                                    Icon(Icons.Rounded.ContentCopy, null, modifier = Modifier.size(17.dp))
                                    Spacer(Modifier.width(7.dp))
                                    Text("Copy link", fontWeight = FontWeight.SemiBold)
                                }
                                Button(
                                    onClick = onShare,
                                    modifier = Modifier.weight(1f).height(48.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.White.copy(alpha = 0.10f),
                                        contentColor = White
                                    )
                                ) {
                                    Icon(Icons.Rounded.Share, null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(7.dp))
                                    Text("Share", fontWeight = FontWeight.SemiBold)
                                }
                            }
                            Text("Keep this link private. Anyone with access to it on your network can listen while you're casting.",
                                color = Muted, fontSize = 12.sp, lineHeight = 17.sp)
                        }
                    }
                }
                item {
                    GlassPanel(haze) {
                        Column(verticalArrangement = Arrangement.spacedBy(15.dp)) {
                            Text("Audio details", fontWeight = FontWeight.SemiBold, color = White, fontSize = 18.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                                DetailPill("AAC-LC", modifier = Modifier.weight(1f))
                                DetailPill("48 kHz", modifier = Modifier.weight(1f))
                                DetailPill("Stereo", modifier = Modifier.weight(1f))
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Wifi, null, modifier = Modifier.size(16.dp), tint = accent)
                                Spacer(Modifier.width(9.dp))
                                Text("Network: $ip", color = Muted, fontSize = 12.sp)
                            }
                        }
                    }
                }
                item {
                    GlassPanel(haze) {
                        Column(verticalArrangement = Arrangement.spacedBy(13.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Headphones, null, tint = accent, modifier = Modifier.size(21.dp))
                                Spacer(Modifier.width(9.dp))
                                Text("Play on Music Assistant", color = White, fontSize = 18.sp,
                                    fontWeight = FontWeight.SemiBold)
                            }
                            InstructionLine("01", "Start casting and approve Android's audio capture request.")
                            InstructionLine("02", "In Music Assistant, add a custom radio station from your copied URL.")
                            InstructionLine("03", "Play the station on a Sendspin speaker or group.")
                        }
                    }
                }
                item {
                    AnimatedVisibility(
                        visible = error != null && !isCasting,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Text("Last broadcast error: ${error.orEmpty()}", color = Pink, fontSize = 12.sp)
                    }
                }
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Info, null, modifier = Modifier.size(16.dp), tint = Muted)
                        Spacer(Modifier.width(8.dp))
                        Text("v0.2 alpha  ·  Local only  ·  No root required",
                            fontSize = 11.sp, color = Muted)
                    }
                }
            }
        }
    }
}

@Composable
private fun TopLine(accent: Color, isCasting: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Box(
            Modifier.size(47.dp).clip(RoundedCornerShape(16.dp))
                .background(Brush.linearGradient(listOf(accent, Pink))),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.GraphicEq, null, tint = Color(0xFF1B1226), modifier = Modifier.size(27.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("Sendspin Cast", color = White, fontWeight = FontWeight.Bold, fontSize = 19.sp,
                letterSpacing = (-0.3).sp)
            Text("AUDIO EVERYWHERE", color = Muted, fontSize = 10.sp, letterSpacing = 1.5.sp)
        }
        StatusChip(isCasting, accent)
    }
}

@Composable
private fun StatusChip(active: Boolean, accent: Color) {
    val dot by animateFloatAsState(if (active) 1f else 0.50f, label = "status")
    Row(
        modifier = Modifier.clip(CircleShape)
            .background(if (active) accent.copy(alpha = 0.17f) else Color.White.copy(alpha = 0.07f))
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(7.dp).clip(CircleShape)
            .background(if (active) accent.copy(alpha = dot) else Muted))
        Spacer(Modifier.width(7.dp))
        Text(if (active) "LIVE" else "IDLE",
            color = if (active) accent else Muted,
            fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.1.sp)
    }
}

@Composable
private fun AuroraBackground(active: Boolean, haze: HazeState, accent: Color) {
    val motion = rememberInfiniteTransition(label = "aurora")
    val drift by motion.animateFloat(
        initialValue = 0f, targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(11000), RepeatMode.Restart), label = "drift"
    )
    val intensity by animateFloatAsState(
        targetValue = if (active) 1.0f else 0.55f,
        animationSpec = spring(stiffness = 55f), label = "glow"
    )
    Canvas(Modifier.fillMaxSize().hazeSource(haze)) {
        drawRect(Ink)
        val w = size.width
        val h = size.height
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(accent.copy(alpha = 0.33f * intensity), Color.Transparent),
                center = Offset(w * (0.80f + 0.13f * sin(drift)), h * 0.13f),
                radius = w * 0.85f
            ),
            radius = w * 0.85f,
            center = Offset(w * (0.80f + 0.13f * sin(drift)), h * 0.13f)
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Pink.copy(alpha = 0.20f * intensity), Color.Transparent),
                center = Offset(w * (0.12f + 0.13f * sin(drift + 1.5f)), h * 0.58f),
                radius = w * 0.80f
            ),
            radius = w * 0.80f,
            center = Offset(w * (0.12f + 0.13f * sin(drift + 1.5f)), h * 0.58f)
        )
    }
}

@Composable
private fun WaveOrb(active: Boolean, accent: Color) {
    val infinite = rememberInfiniteTransition(label = "orb")
    val phase by infinite.animateFloat(
        0f, (2f * PI).toFloat(),
        infiniteRepeatable(tween(2400), RepeatMode.Restart), label = "wave"
    )
    val strength by animateFloatAsState(
        if (active) 1f else 0.28f,
        animationSpec = spring(stiffness = 40f), label = "strength"
    )
    Canvas(Modifier.fillMaxWidth().height(202.dp)) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.height * 0.36f
        for (ring in 3 downTo 1) {
            drawCircle(
                color = accent.copy(alpha = (0.05f + ring * 0.025f) * strength),
                radius = radius + ring * size.height * 0.065f,
                center = center
            )
        }
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(accent.copy(alpha = .42f), accent.copy(alpha = 0.12f), Color.Transparent),
                center = center,
                radius = radius * 1.7f
            ),
            radius = radius * 1.7f, center = center
        )
        drawCircle(color = Color(0xCC17131F), center = center, radius = radius)
        drawCircle(color = accent.copy(alpha = .33f), center = center, radius = radius,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx()))
        val bars = 17
        val spacing = radius * 1.46f / bars
        for (i in 0 until bars) {
            val x = center.x + (i - bars / 2) * spacing
            val envelope = 1f - kotlin.math.abs(i - bars / 2) / (bars / 2f + 1f)
            val flicker = (0.48f + 0.52f * kotlin.math.abs(sin(phase * 2f + i * 0.72f)))
            val height = (8.dp.toPx() + 44.dp.toPx() * envelope * flicker * strength)
            drawLine(
                color = if (i % 3 == 0) Pink else accent,
                start = Offset(x, center.y - height / 2f),
                end = Offset(x, center.y + height / 2f),
                strokeWidth = 4.dp.toPx(), cap = StrokeCap.Round
            )
        }
    }
}

@Composable
private fun GlassPanel(haze: HazeState, content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(29.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .hazeEffect(state = haze) {
                blurRadius = 24.dp
                progressive = HazeProgressive.verticalGradient(startIntensity = .72f, endIntensity = 1f)
                noiseFactor = 0.06f
            }
            .background(Color(0xAA25212D))
            .border(1.dp, Color.White.copy(alpha = 0.11f), shape)
            .padding(19.dp)
    ) { content() }
}

@Composable
private fun DetailPill(value: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(value, fontSize = 12.sp, color = White, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun InstructionLine(number: String, message: String) {
    Row(verticalAlignment = Alignment.Top) {
        Text(number, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Lilac,
            modifier = Modifier.width(32.dp).padding(top = 2.dp))
        Text(message, fontSize = 13.sp, color = Muted, lineHeight = 19.sp)
    }
}
