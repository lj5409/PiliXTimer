package com.pilix.timer.ui

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pilix.timer.TimerViewModel
import com.pilix.timer.data.EndAlert
import com.pilix.timer.data.Palette
import com.pilix.timer.data.Plan
import com.pilix.timer.data.Segment
import com.pilix.timer.data.StartMode
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanEditorScreen(
    vm: TimerViewModel,
    onBack: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    var draft by remember { mutableStateOf(vm.plan) }
    var showSaveAs by remember { mutableStateOf(false) }
    var saveAsName by remember { mutableStateOf("${vm.plan.name} 副本") }

    Column(
        Modifier
            .fillMaxSize()
            .background(cs.background)
            .systemBarsPadding(),
    ) {
        // 顶栏
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, "返回", tint = cs.onSurface)
            }
            Text(
                "方案编辑",
                color = cs.onSurface,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.weight(1f))
            TextButton(onClick = {
                vm.savePlan(draft)
                onBack()
            }) {
                Text("保存", color = cs.primary, fontWeight = FontWeight.SemiBold)
            }
        }

        // 已保存方案
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            vm.plans.forEach { p ->
                val selected = p.id == draft.id
                Box(
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .background(
                            if (selected) cs.primary.copy(alpha = 0.18f)
                            else cs.surfaceVariant.copy(alpha = 0.7f)
                        )
                        .clickable {
                            // 切换方案同时立即应用，编辑后点「保存」写回
                            draft = p
                            vm.selectPlan(p)
                        }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                ) {
                    Text(
                        p.name,
                        color = if (selected) cs.primary else cs.onSurfaceVariant,
                        fontSize = 13.sp,
                        maxLines = 1,
                    )
                }
            }
            Box(
                Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(cs.surfaceVariant.copy(alpha = 0.7f))
                    .clickable {
                        draft = vm.plan.copy(
                            id = "plan-" + UUID.randomUUID(),
                            name = "新方案",
                            builtin = false,
                            segments = vm.plan.segments.map { it.copy(id = UUID.randomUUID().toString()) },
                        )
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Add, "新建方案", tint = cs.onSurfaceVariant, modifier = Modifier.size(18.dp))
            }
        }

        LazyColumn(
            Modifier.fillMaxWidth().weight(1f),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                OutlinedTextField(
                    value = draft.name,
                    onValueChange = { draft = draft.copy(name = it) },
                    label = { Text("方案名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            items(draft.segments, key = { it.id }) { seg ->
                SegmentCard(
                    segment = seg,
                    canDelete = draft.segments.size > 1,
                    onChange = { updated ->
                        draft = draft.copy(
                            segments = draft.segments.map { if (it.id == seg.id) updated else it }
                        )
                    },
                    onDelete = {
                        draft = draft.copy(segments = draft.segments.filterNot { it.id == seg.id })
                    },
                )
            }

            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OutlinedButton(
                        onClick = {
                            draft = draft.copy(
                                segments = draft.segments + Segment(
                                    id = UUID.randomUUID().toString(),
                                    name = "新分段",
                                    minutes = 1,
                                    colorIndex = draft.segments.size,
                                    endAlert = EndAlert.SOUND,
                                    startMode = StartMode.AUTO,
                                )
                            )
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Rounded.Add, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("添加分段")
                    }
                    OutlinedButton(
                        onClick = { showSaveAs = true },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Rounded.ContentCopy, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("另存为")
                    }
                }
            }

            item {
                if (!draft.builtin) {
                    TextButton(
                        onClick = {
                            vm.deletePlan(draft.id)
                            draft = vm.plans.firstOrNull { it.builtin } ?: vm.plans.first()
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Rounded.Delete, null, modifier = Modifier.size(18.dp), tint = cs.error)
                        Spacer(Modifier.width(6.dp))
                        Text("删除此方案", color = cs.error)
                    }
                }
            }
        }
    }

    if (showSaveAs) {
        AlertDialog(
            onDismissRequest = { showSaveAs = false },
            title = { Text("另存为新方案") },
            text = {
                OutlinedTextField(
                    value = saveAsName,
                    onValueChange = { saveAsName = it },
                    label = { Text("方案名称") },
                    singleLine = true,
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val copy = draft.copy(
                            id = "plan-" + UUID.randomUUID(),
                            name = saveAsName.ifBlank { "新方案" },
                            builtin = false,
                            segments = draft.segments.map { it.copy(id = UUID.randomUUID().toString()) },
                        )
                        vm.savePlan(copy)
                        draft = copy
                        showSaveAs = false
                    },
                ) { Text("保存") }
            },
            dismissButton = {
                TextButton(onClick = { showSaveAs = false }) { Text("取消") }
            },
        )
    }
}

@Composable
private fun SegmentCard(
    segment: Segment,
    canDelete: Boolean,
    onChange: (Segment) -> Unit,
    onDelete: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val accent = Color(Palette.of(segment.colorIndex))

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(cs.surface.copy(alpha = 0.7f))
            .border(1.dp, cs.outline.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // 颜色选择
            Box(
                Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(accent)
                    .clickable { onChange(segment.copy(colorIndex = segment.colorIndex + 1)) },
            )
            Spacer(Modifier.width(12.dp))
            OutlinedTextField(
                value = segment.name,
                onValueChange = { onChange(segment.copy(name = it)) },
                singleLine = true,
                modifier = Modifier.weight(1f),
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 15.sp),
            )
            if (canDelete) {
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Rounded.Delete,
                        "删除分段",
                        tint = cs.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // 时长
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "时长",
                color = cs.onSurfaceVariant,
                fontSize = 13.sp,
                modifier = Modifier.width(38.dp),
            )
            StepperButton(Icons.Rounded.Remove, "减少") {
                onChange(segment.copy(minutes = (segment.minutes - 1).coerceAtLeast(1)))
            }
            Text(
                "${segment.minutes} 分钟",
                color = cs.onSurface,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.width(84.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            StepperButton(Icons.Rounded.Add, "增加") {
                onChange(segment.copy(minutes = (segment.minutes + 1).coerceAtMost(120)))
            }
        }

        Spacer(Modifier.height(8.dp))

        // 结束提醒
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "提醒",
                color = cs.onSurfaceVariant,
                fontSize = 13.sp,
                modifier = Modifier.width(38.dp),
            )
            ChoiceChipRow(
                options = listOf(
                    EndAlert.PULSE to "轻提醒",
                    EndAlert.SOUND to "声音",
                    EndAlert.NONE to "无",
                ),
                selected = segment.endAlert,
                onSelect = { onChange(segment.copy(endAlert = it)) },
            )
        }

        Spacer(Modifier.height(6.dp))

        // 开始方式
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "开始",
                color = cs.onSurfaceVariant,
                fontSize = 13.sp,
                modifier = Modifier.width(38.dp),
            )
            ChoiceChipRow(
                options = listOf(
                    StartMode.AUTO to "自动接续",
                    StartMode.TAP to "手动开始",
                ),
                selected = segment.startMode,
                onSelect = { onChange(segment.copy(startMode = it)) },
            )
        }
    }
}

@Composable
private fun <T> ChoiceChipRow(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (value, label) ->
            FilterChip(
                selected = value == selected,
                onClick = { onSelect(value) },
                label = { Text(label, fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.20f),
                    selectedLabelColor = MaterialTheme.colorScheme.primary,
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = value == selected,
                    borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                    selectedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                ),
            )
        }
    }
}

@Composable
private fun StepperButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    Box(
        Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(cs.surfaceVariant.copy(alpha = 0.8f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            description,
            tint = cs.onSurface,
            modifier = Modifier.size(18.dp),
        )
    }
}
