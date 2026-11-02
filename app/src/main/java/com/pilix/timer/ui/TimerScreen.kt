package com.pilix.timer.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pilix.timer.TimerViewModel
import com.pilix.timer.data.AlertKind
import com.pilix.timer.data.Palette
import com.pilix.timer.data.Phase
import com.pilix.timer.data.Plan
import com.pilix.timer.data.Segment
import java.util.Locale
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** 环形显示模式 */
enum class RingMode { IDLE, COUNTDOWN, WAITING, DONE }

fun formatDuration(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) {
        String.format(Locale.US, "%d:%02d:%02d", h, m, s)
    } else {
        String.format(Locale.US, "%02d:%02d", m, s)
    }
}

@Composable
fun TimerScreen(
    vm: TimerViewModel,
    immersive: Boolean,
    onOpenPlans: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val phase = vm.phase
    val seg = vm.currentSegment
    val accent = Color(Palette.of(seg?.colorIndex ?: 0))
    val animatedAccent by animateColorAsState(accent, tween(500), label = "accent")

    // 3 秒高亮脉冲
    val pulse = remember { Animatable(0f) }
    // 静音时的 5 秒急促屏幕变化
    val flash = remember { Animatable(0f) }

    LaunchedEffect(vm.alert) {
        when (vm.alert) {
            is AlertKind.Pulse -> {
                pulse.snapTo(0f)
                repeat(6) {
                    pulse.animateTo(1f, tween(250, easing = FastOutSlowInEasing))
                    pulse.animateTo(0f, tween(250, easing = FastOutSlowInEasing))
                }
            }

            is AlertKind.ScreenFlash -> {
                flash.snapTo(1f)
                repeat(24) { i ->
                    flash.animateTo(if (i % 2 == 0) 0f else 1f, tween(200, easing = LinearEasing))
                }
                flash.animateTo(0f, tween(180))
            }

            null -> {
                pulse.animateTo(0f, tween(180))
                flash.animateTo(0f, tween(150))
            }
        }
    }

    val ringMode = when (phase) {
        Phase.IDLE -> RingMode.IDLE
        Phase.RUNNING, Phase.PAUSED -> RingMode.COUNTDOWN
        Phase.WAITING -> RingMode.WAITING
        Phase.DONE -> RingMode.DONE
    }

    val fraction = when (phase) {
        Phase.RUNNING, Phase.PAUSED -> {
            val dur = seg?.durationMs ?: 0L
            if (dur > 0) (vm.segRemainingMs.toFloat() / dur).coerceIn(0f, 1f) else 0f
        }

        else -> 1f
    }

    Box(Modifier.fillMaxSize().background(cs.background)) {
        // 背景：顶部强调色渐晕
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    listOf(animatedAccent.copy(alpha = 0.14f), Color.Transparent),
                )
            )
        )

        // 沉浸式全屏时系统栏隐藏，系统栏内边距自然为 0
        val barsPadding = if (immersive) Modifier else Modifier.systemBarsPaddingCompat()

        Column(
            Modifier
                .fillMaxSize()
                .then(barsPadding)
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // ---------- 顶栏 ----------
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PlanChip(vm.plan.name, onOpenPlans)
                Spacer(Modifier.weight(1f))
                RoundIconButton(
                    icon = Icons.Rounded.Settings,
                    description = "设置",
                    size = 40.dp,
                    onClick = onOpenSettings,
                )
            }

            Spacer(Modifier.height(14.dp))

            // ---------- 分段条 ----------
            SegmentStrip(vm.plan, vm.segIndex, phase) { Color(Palette.of(it.colorIndex)) }

            Spacer(Modifier.weight(0.9f))

            // ---------- 环形 ----------
            BoxWithConstraints(
                Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                val density = LocalDensity.current
                val ringSide = minOf(maxWidth, 330.dp)
                val timeSize = with(density) { (ringSide * 0.30f).toSp() }
                val labelSize = with(density) { (ringSide * 0.048f).toSp() }
                val subSize = with(density) { (maxWidth * 0.037f).toSp() }

                Column(
                    Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        Modifier.size(ringSide),
                        contentAlignment = Alignment.Center,
                    ) {
                        // 强调色光晕
                        Canvas(Modifier.fillMaxSize()) {
                            drawCircle(
                                brush = Brush.radialGradient(
                                    listOf(animatedAccent.copy(alpha = 0.13f), Color.Transparent),
                                    center = center,
                                    radius = size.minDimension * 0.62f,
                                ),
                                radius = size.minDimension * 0.55f,
                            )
                        }

                        TimerRing(
                            fraction = fraction,
                            accent = animatedAccent,
                            mode = ringMode,
                            pulse = pulse.value,
                            trackColor = cs.onBackground.copy(alpha = 0.08f),
                            modifier = Modifier.fillMaxSize(),
                        )

                        // 轻提醒时数字同步高亮并放大
                        val pulseScale = 1f + 0.14f * pulse.value
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.graphicsLayer {
                                scaleX = pulseScale
                                scaleY = pulseScale
                            },
                        ) {
                            Text(
                                text = mainText(vm),
                                color = when {
                                    phase == Phase.WAITING || phase == Phase.DONE ->
                                        lerp(animatedAccent, Color.White, pulse.value * 0.6f)

                                    else -> lerp(cs.onBackground, Color.White, pulse.value * 0.7f)
                                },
                                fontSize = timeSize,
                                fontWeight = FontWeight.Light,
                                letterSpacing = (-1).sp,
                                style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum"),
                                maxLines = 1,
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = mainLabel(vm),
                                color = if (phase == Phase.IDLE) {
                                    animatedAccent
                                } else {
                                    cs.onSurfaceVariant
                                },
                                fontSize = labelSize,
                                fontWeight = FontWeight.Medium,
                                letterSpacing = 2.sp,
                                maxLines = 1,
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    Text(
                        text = subLine(vm),
                        color = cs.onSurfaceVariant,
                        fontSize = subSize,
                        fontWeight = FontWeight.Normal,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            // ---------- 控制区 ----------
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RoundIconButton(
                    icon = Icons.Rounded.Refresh,
                    description = "刷新",
                    size = 52.dp,
                    onClick = { vm.resetToReady() },
                )
                Spacer(Modifier.width(26.dp))
                PrimaryButton(vm, animatedAccent)
                Spacer(Modifier.width(26.dp))
                RoundIconButton(
                    icon = if (vm.settings.soundOn) {
                        Icons.AutoMirrored.Rounded.VolumeUp
                    } else {
                        Icons.AutoMirrored.Rounded.VolumeOff
                    },
                    description = "声音",
                    size = 52.dp,
                    active = vm.settings.soundOn,
                    onClick = { vm.toggleSound() },
                )
            }

            Spacer(Modifier.height(16.dp))

            // ---------- 总用时 ----------
            Row(
                Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { vm.resetTotal() }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "总用时",
                    color = cs.onSurfaceVariant.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = formatDuration(vm.totalMs),
                    color = cs.onSurface,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum"),
                )
                Spacer(Modifier.width(8.dp))
                Icon(
                    Icons.Rounded.Refresh,
                    contentDescription = "重置总用时",
                    tint = cs.onSurfaceVariant.copy(alpha = 0.45f),
                    modifier = Modifier.size(13.dp),
                )
            }
        }

        // ---------- 静音时的屏幕变化覆盖层 ----------
        if (flash.value > 0.01f) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(animatedAccent.copy(alpha = 0.88f * flash.value))
            )
        }
    }
}

// ---------------------------------------------------------------- 文案

@Composable
private fun mainText(vm: TimerViewModel): String = when (vm.phase) {
    Phase.WAITING -> formatDuration(vm.overtimeMs)
    Phase.DONE -> formatDuration(vm.activeMs)
    else -> formatDuration(vm.segRemainingMs)
}

@Composable
private fun mainLabel(vm: TimerViewModel): String = when (vm.phase) {
    Phase.IDLE -> vm.currentSegment?.name ?: "准备"
    Phase.RUNNING -> vm.currentSegment?.name ?: ""
    Phase.PAUSED -> "已暂停"
    Phase.WAITING -> "超出时间"
    Phase.DONE -> "本轮用时"
}

@Composable
private fun subLine(vm: TimerViewModel): String = when (vm.phase) {
    Phase.IDLE -> "准备开始"
    Phase.RUNNING -> {
        val dur = vm.currentSegment?.durationMs ?: 0L
        "已用 ${formatDuration(vm.activeMs)} / ${formatDuration(dur)}"
    }

    Phase.PAUSED -> "已用 ${formatDuration(vm.activeMs)}"
    Phase.WAITING -> {
        val next = vm.plan.segments.getOrNull(vm.segIndex)
        if (next != null) "下一步 · ${next.name} ${next.minutes} 分钟" else ""
    }

    Phase.DONE -> "点击刷新开始下一位"
}

// ---------------------------------------------------------------- 环形

@Composable
fun TimerRing(
    fraction: Float,
    accent: Color,
    mode: RingMode,
    pulse: Float,
    trackColor: Color,
    modifier: Modifier = Modifier,
) {
    val infinite = rememberInfiniteTransition(label = "ring")
    val breathe by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "breathe",
    )

    Canvas(modifier) {
        val stroke = size.minDimension * 0.058f
        val inset = stroke / 2f + 3.dp.toPx()
        val d = min(size.width, size.height) - inset * 2f
        val topLeft = Offset((size.width - d) / 2f, (size.height - d) / 2f)
        val arcSize = Size(d, d)
        val startAngle = -90f
        val sweep = 360f * fraction.coerceIn(0f, 1f)
        val breathing = if (mode == RingMode.WAITING) breathe else 0f

        // 轨道
        drawArc(
            color = trackColor,
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = stroke, cap = StrokeCap.Round),
        )

        if (mode == RingMode.IDLE) return@Canvas

        val alpha = when (mode) {
            RingMode.DONE -> 0.35f
            RingMode.WAITING -> 0.55f + 0.35f * breathing
            else -> 1f
        }

        // 外发光：轻提醒时明显增强（加宽 + 提亮）
        drawArc(
            color = accent.copy(alpha = ((0.12f + 0.50f * pulse + 0.10f * breathing) * alpha).coerceAtMost(1f)),
            startAngle = startAngle,
            sweepAngle = sweep,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = stroke * (2.1f + 0.8f * pulse), cap = StrokeCap.Round),
        )

        // 进度：轻提醒时同步提亮
        drawArc(
            color = accent.copy(alpha = (alpha + 0.35f * pulse).coerceAtMost(1f)),
            startAngle = startAngle,
            sweepAngle = sweep,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = stroke * (1f + 0.12f * pulse), cap = StrokeCap.Round),
        )

        // 圆头高亮
        if (mode == RingMode.COUNTDOWN && fraction > 0.0005f && fraction < 0.9995f) {
            val angle = Math.toRadians((startAngle + sweep).toDouble())
            val r = d / 2f
            val p = Offset(
                size.width / 2f + (r * cos(angle)).toFloat(),
                size.height / 2f + (r * sin(angle)).toFloat(),
            )
            drawCircle(accent.copy(alpha = 0.28f), radius = stroke * 0.95f, center = p)
            drawCircle(Color.White.copy(alpha = 0.92f), radius = stroke * 0.26f, center = p)
        }
    }
}

// ---------------------------------------------------------------- 分段条

@Composable
private fun SegmentStrip(
    plan: Plan,
    activeIndex: Int,
    phase: Phase,
    accentOf: (Segment) -> Color,
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        plan.segments.forEachIndexed { index, seg ->
            val color = accentOf(seg)
            val alpha: Float
            val height: androidx.compose.ui.unit.Dp
            when {
                phase == Phase.IDLE -> {
                    alpha = 0.20f
                    height = 4.dp
                }

                index < activeIndex -> {
                    alpha = 0.38f
                    height = 4.dp
                }

                index == activeIndex -> {
                    alpha = 1f
                    height = 6.dp
                }

                else -> {
                    alpha = 0.10f
                    height = 4.dp
                }
            }
            Box(
                Modifier
                    .weight(seg.minutes.toFloat().coerceAtLeast(0.6f))
                    .height(height)
                    .clip(RoundedCornerShape(50))
                    .background(color.copy(alpha = alpha))
            )
        }
    }
}

// ---------------------------------------------------------------- 控件

@Composable
private fun PlanChip(name: String, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(cs.surfaceVariant.copy(alpha = 0.75f))
            .clickable(onClick = onClick)
            .padding(start = 12.dp, end = 14.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Rounded.Tune,
            contentDescription = null,
            tint = cs.onSurfaceVariant,
            modifier = Modifier.size(15.dp),
        )
        Spacer(Modifier.width(7.dp))
        Text(
            text = name,
            color = cs.onSurface,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
        )
    }
}

@Composable
private fun RoundIconButton(
    icon: ImageVector,
    description: String,
    size: androidx.compose.ui.unit.Dp,
    active: Boolean = true,
    onClick: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.9f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "btnScale",
    )
    val tint = if (active) cs.onSurface else cs.onSurfaceVariant.copy(alpha = 0.45f)

    Box(
        Modifier
            .size(size)
            .scale(scale)
            .clip(CircleShape)
            .background(cs.surfaceVariant.copy(alpha = if (active) 0.55f else 0.3f))
            .border(1.dp, cs.outline.copy(alpha = 0.6f), CircleShape)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = tint,
            modifier = Modifier.size(size * 0.45f),
        )
    }
}

@Composable
private fun PrimaryButton(vm: TimerViewModel, accent: Color) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "primaryScale",
    )
    val icon = when (vm.phase) {
        Phase.RUNNING -> Icons.Rounded.Pause
        Phase.DONE -> Icons.Rounded.Check
        else -> Icons.Rounded.PlayArrow
    }

    Box(
        Modifier
            .size(74.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(
                Brush.verticalGradient(
                    listOf(accent.copy(alpha = 0.95f), accent.copy(alpha = 0.75f))
                )
            )
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = { vm.onPrimary() },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = when (vm.phase) {
                Phase.RUNNING -> "暂停"
                Phase.DONE -> "完成"
                else -> "开始"
            },
            tint = Color.White,
            modifier = Modifier.size(34.dp),
        )
    }
}

/**
 * 系统栏内边距。
 * 沉浸式全屏下系统栏隐藏，insets 自然为 0；非沉浸式时又能正确避开状态栏与导航栏。
 */
@Composable
private fun Modifier.systemBarsPaddingCompat(): Modifier = this.systemBarsPadding()
