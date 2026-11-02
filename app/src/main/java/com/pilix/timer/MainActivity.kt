package com.pilix.timer

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.pilix.timer.data.Palette
import com.pilix.timer.ui.PlanEditorScreen
import com.pilix.timer.ui.PiliXTheme
import com.pilix.timer.ui.SettingsScreen
import com.pilix.timer.ui.TimerScreen

class MainActivity : ComponentActivity() {

    private val vm: TimerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settings = vm.settings
            PiliXTheme(themeMode = settings.themeMode) {
                LaunchedEffect(settings.immersive, settings.keepScreenOn) {
                    applyWindowFlags(settings.immersive, settings.keepScreenOn)
                }
                AppRoot(vm)
            }
        }
    }

    private fun applyWindowFlags(immersive: Boolean, keepScreenOn: Boolean) {
        if (keepScreenOn) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        if (immersive) {
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
    }
}

private enum class Screen { TIMER, PLANS, SETTINGS }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppRoot(vm: TimerViewModel) {
    var screen by rememberSaveable { mutableStateOf(Screen.TIMER) }
    var showPlanPicker by remember { mutableStateOf(false) }

    BackHandler(enabled = screen != Screen.TIMER) {
        screen = Screen.TIMER
    }

    AnimatedContent(
        targetState = screen,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "screen",
    ) { target ->
        when (target) {
            Screen.TIMER -> TimerScreen(
                vm = vm,
                immersive = vm.settings.immersive,
                onOpenPlans = { showPlanPicker = true },
                onOpenSettings = { screen = Screen.SETTINGS },
            )

            Screen.PLANS -> PlanEditorScreen(vm = vm, onBack = { screen = Screen.TIMER })
            Screen.SETTINGS -> SettingsScreen(vm = vm, onBack = { screen = Screen.TIMER })
        }
    }

    if (showPlanPicker) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { showPlanPicker = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
                Text(
                    "选择方案",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
                vm.plans.forEach { p ->
                    val active = p.id == vm.plan.id
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(14.dp))
                            .clickable {
                                vm.selectPlan(p)
                                showPlanPicker = false
                            }
                            .padding(vertical = 12.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(Color(Palette.of(p.segments.firstOrNull()?.colorIndex ?: 0)))
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                p.name,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 15.sp,
                                fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                            )
                            Text(
                                p.segments.joinToString(" · ") {
                                    "${it.name} ${it.minutes}′"
                                },
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp,
                            )
                        }
                        if (active) {
                            Icon(
                                Icons.Rounded.Check,
                                "当前方案",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(14.dp))
                        .clickable {
                            showPlanPicker = false
                            screen = Screen.PLANS
                        }
                        .padding(vertical = 12.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Rounded.Edit,
                        null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "编辑方案",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }

                Spacer(Modifier.height(28.dp))
            }
        }
    }
}
