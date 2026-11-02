package com.pilix.timer.ui

import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pilix.timer.TimerViewModel

@Composable
fun SettingsScreen(
    vm: TimerViewModel,
    onBack: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val context = LocalContext.current
    val s = vm.settings

    Column(
        Modifier
            .fillMaxSize()
            .background(cs.background)
            .systemBarsPadding()
            .verticalScroll(rememberScrollState()),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, "返回", tint = cs.onSurface)
            }
            Text(
                "设置",
                color = cs.onSurface,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }

        Spacer(Modifier.height(8.dp))

        SettingSwitch(
            title = "声音提醒",
            subtitle = "关闭后分段结束改用 5 秒屏幕变化提醒",
            checked = s.soundOn,
            onChange = { vm.updateSettings(s.copy(soundOn = it)) },
        )
        SettingSwitch(
            title = "震动反馈",
            subtitle = "分段切换时轻微震动",
            checked = s.vibrateOn,
            onChange = { vm.updateSettings(s.copy(vibrateOn = it)) },
        )
        SettingSwitch(
            title = "屏幕常亮",
            subtitle = "计时期间屏幕不熄灭",
            checked = s.keepScreenOn,
            onChange = { vm.updateSettings(s.copy(keepScreenOn = it)) },
        )
        SettingSwitch(
            title = "沉浸式全屏",
            subtitle = "隐藏状态栏与导航栏",
            checked = s.immersive,
            onChange = { vm.updateSettings(s.copy(immersive = it)) },
        )

        Spacer(Modifier.height(6.dp))

        // 主题
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Text("主题", color = cs.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(0 to "跟随系统", 1 to "深色", 2 to "浅色").forEach { (mode, label) ->
                    val selected = s.themeMode == mode
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(50))
                            .background(
                                if (selected) cs.primary.copy(alpha = 0.18f)
                                else cs.surfaceVariant.copy(alpha = 0.7f)
                            )
                            .border(
                                1.dp,
                                if (selected) cs.primary.copy(alpha = 0.5f) else cs.outline.copy(alpha = 0.5f),
                                RoundedCornerShape(50),
                            )
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                            .clickable { vm.updateSettings(s.copy(themeMode = mode)) },
                    ) {
                        Text(
                            label,
                            color = if (selected) cs.primary else cs.onSurfaceVariant,
                            fontSize = 13.sp,
                        )
                    }
                }
            }
        }

        // 勿扰模式说明
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(cs.surface.copy(alpha = 0.7f))
                .border(1.dp, cs.outline.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
                .padding(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Rounded.VolumeUp, null, tint = cs.primary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "提醒不被静音",
                    color = cs.onSurface,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "提示音走系统「闹钟」音轨、震动按「闹钟」用途登记，" +
                    "因此手机处于勿扰模式时仍会正常提醒。" +
                    "如果听不到声音，请确认系统闹钟音量不为 0。",
                color = cs.onSurfaceVariant,
                fontSize = 13.sp,
                lineHeight = 19.sp,
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = {
                    runCatching {
                        context.startActivity(
                            Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }
                }) { Text("勿扰访问权限") }
                OutlinedButton(onClick = {
                    runCatching {
                        context.startActivity(
                            Intent(Settings.ACTION_SOUND_SETTINGS)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }
                }) { Text("系统音量") }
            }
        }

        Spacer(Modifier.height(8.dp))

        Column(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Text("关于", color = cs.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(6.dp))
            Text(
                "PiliX Timer · v1.0.0\n为 EO Forum Update 环节设计的分段计时器",
                color = cs.onSurfaceVariant,
                fontSize = 13.sp,
                lineHeight = 19.sp,
            )
        }

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun SettingSwitch(
    title: String,
    subtitle: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = cs.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, color = cs.onSurfaceVariant, fontSize = 12.sp)
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = androidx.compose.ui.graphics.Color.White,
                checkedTrackColor = cs.primary,
            ),
        )
    }
}
