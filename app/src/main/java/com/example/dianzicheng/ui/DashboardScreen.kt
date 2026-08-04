package com.example.dianzicheng.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dianzicheng.data.ble.BleScaleClient
import com.example.dianzicheng.domain.BodyMeasurement
import com.example.dianzicheng.domain.FamilyMember
import com.example.dianzicheng.domain.Sex
import com.example.dianzicheng.ui.theme.电子秤Theme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardContent(
    uiState: ScaleUiState,
    onStartScan: () -> Unit,
    onDismissAlert: () -> Unit,
    onSelectMember: (FamilyMember?) -> Unit,
    onBindMember: (FamilyMember) -> Unit,
    modifier: Modifier = Modifier
) {
    var showMemberSelectDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("体重秤", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Member Selector Chip
            Surface(
                onClick = { showMemberSelectDialog = true },
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = uiState.selectedMember?.let {
                            "绑定成员: ${it.name} (${if (it.sex == Sex.MALE) "男" else "女"} ${it.heightCm.toInt()}cm)"
                        } ?: "全员智能自动匹配",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Connection Status Chip
            ConnectionStatusChip(uiState.connection)

            Spacer(modifier = Modifier.weight(1f))

            // Weight Display with modern circle
            WeightDisplay(uiState.liveWeightKg, uiState.isStable)

            Spacer(modifier = Modifier.weight(1f))

            // Metrics Card
            AnimatedVisibility(
                visible = uiState.currentMeasurement != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                uiState.currentMeasurement?.let {
                    MeasurementResultCard(
                        measurement = it,
                        availableMembers = uiState.availableMembers,
                        onBindMember = onBindMember
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action Button
            if (uiState.connection == BleScaleClient.ConnectionState.IDLE) {
                FilledTonalButton(
                    onClick = onStartScan,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = MaterialTheme.shapes.large
                ) {
                    Text("开始称重", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            } else if (uiState.connection == BleScaleClient.ConnectionState.SCANNING) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(CircleShape)
                )
                Text(
                    "正在寻找设备...",
                    modifier = Modifier.padding(top = 8.dp),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (uiState.showNewMemberAlert) {
        AlertDialog(
            onDismissRequest = onDismissAlert,
            title = { Text("发现新成员？") },
            text = { Text("测量体重与已有成员差距较大，您可以稍后在“我的”页面添加新成员。") },
            confirmButton = {
                TextButton(onClick = onDismissAlert) {
                    Text("知道了")
                }
            }
        )
    }

    if (showMemberSelectDialog) {
        SelectMemberDialog(
            members = uiState.availableMembers,
            currentMemberId = uiState.selectedMember?.id,
            onDismiss = { showMemberSelectDialog = false },
            onSelect = { member ->
                onSelectMember(member)
                showMemberSelectDialog = false
            }
        )
    }
}

@Composable
fun ConnectionStatusChip(state: BleScaleClient.ConnectionState) {
    val color = when (state) {
        BleScaleClient.ConnectionState.IDLE -> MaterialTheme.colorScheme.outline
        BleScaleClient.ConnectionState.SCANNING -> MaterialTheme.colorScheme.primary
        BleScaleClient.ConnectionState.CONNECTED, BleScaleClient.ConnectionState.MEASURING -> Color(0xFF4CAF50)
        else -> MaterialTheme.colorScheme.error
    }

    Surface(
        color = color.copy(alpha = 0.1f),
        shape = CircleShape,
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(color, CircleShape)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = when (state) {
                    BleScaleClient.ConnectionState.IDLE -> "未连接"
                    BleScaleClient.ConnectionState.SCANNING -> "搜索中"
                    BleScaleClient.ConnectionState.CONNECTING -> "连接中"
                    BleScaleClient.ConnectionState.CONNECTED -> "已连接"
                    BleScaleClient.ConnectionState.MEASURING -> "测量中"
                },
                style = MaterialTheme.typography.labelLarge,
                color = color
            )
        }
    }
}

@Composable
fun WeightDisplay(weight: Double, isStable: Boolean) {
    val infiniteTransition = rememberInfiniteTransition()
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (!isStable && weight > 0) 0.5f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(
        modifier = Modifier
            .size(260.dp)
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                        Color.Transparent
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = String.format("%.2f", weight),
                fontSize = 76.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = if (weight > 0) alpha else 0.3f)
            )
            Text(
                text = "kg",
                fontSize = 24.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun MeasurementResultCard(
    measurement: BodyMeasurement,
    availableMembers: List<FamilyMember>,
    onBindMember: (FamilyMember) -> Unit
) {
    var showBindDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Member Binding Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.AccountCircle,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "数据所属: ${measurement.memberNameSnapshot ?: "智能匹配中"}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                if (availableMembers.isNotEmpty()) {
                    OutlinedButton(
                        onClick = { showBindDialog = true },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        modifier = Modifier.height(32.dp),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text("重选/绑定成员", fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                MetricItem(
                    label = "BMI",
                    value = String.format("%.1f", measurement.bmi),
                    status = getBmiStatus(measurement.bmi),
                    modifier = Modifier.weight(1f)
                )
                MetricItem(
                    label = "体脂率",
                    value = String.format("%.1f%%", measurement.bodyFatPct),
                    status = getFatStatus(measurement.bodyFatPct),
                    modifier = Modifier.weight(1f)
                )
                MetricItem(
                    label = "水分",
                    value = String.format("%.1f%%", measurement.waterPct),
                    status = if (measurement.waterPct in 50.0..65.0) "标准" else "注意",
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                MetricItem("肌肉量", String.format("%.1fkg", measurement.muscleKg), modifier = Modifier.weight(1f))
                MetricItem("蛋白质", String.format("%.1f%%", measurement.proteinPct), modifier = Modifier.weight(1f))
                MetricItem("骨量", String.format("%.1fkg", measurement.boneMassKg), modifier = Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                MetricItem("阻抗", "${measurement.impedanceOhm.toInt()}Ω", modifier = Modifier.weight(1f))
            }
        }
    }

    if (showBindDialog) {
        SelectMemberDialog(
            members = availableMembers,
            currentMemberId = measurement.memberId,
            onDismiss = { showBindDialog = false },
            onSelect = { member ->
                if (member != null) {
                    onBindMember(member)
                }
                showBindDialog = false
            }
        )
    }
}

@Composable
fun SelectMemberDialog(
    members: List<FamilyMember>,
    currentMemberId: String?,
    onDismiss: () -> Unit,
    onSelect: (FamilyMember?) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择绑定成员") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "选择成员后，系统将使用该成员的身高、年龄与性别重新计算准确的体脂率等指标并保存绑定。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                
                // Auto Match Option
                Surface(
                    onClick = { onSelect(null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = if (currentMemberId == null) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else Color.Transparent
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("全员智能自动匹配", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                if (members.isEmpty()) {
                    Text(
                        "暂无成员，请在“我的”页面添加家庭成员",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 240.dp)) {
                        items(members) { member ->
                            val isSelected = member.id == currentMemberId
                            Surface(
                                onClick = { onSelect(member) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else Color.Transparent
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Person,
                                        contentDescription = null,
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = member.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                                        Text(
                                            text = "${if (member.sex == Sex.MALE) "男" else "女"} · ${member.heightCm.toInt()}cm",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (isSelected) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}

private fun getBmiStatus(bmi: Double): String = when {
    bmi < 18.5 -> "偏瘦"
    bmi < 24.0 -> "标准"
    bmi < 28.0 -> "超重"
    else -> "肥胖"
}

private fun getFatStatus(fat: Double): String = when {
    fat < 10.0 -> "偏低"
    fat < 20.0 -> "标准"
    fat < 25.0 -> "偏高"
    else -> "肥胖"
}

@Composable
fun MetricItem(label: String, value: String, status: String? = null, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
        Text(text = value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
        if (status != null) {
            val color = if (status == "标准") Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
            Surface(
                color = color.copy(alpha = 0.1f),
                shape = CircleShape,
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Text(
                    text = status,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = color,
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
fun DashboardScreen(
    viewModel: ScaleViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        if (uiState.connection == BleScaleClient.ConnectionState.IDLE) {
            viewModel.startScanning()
        }
    }

    DashboardContent(
        uiState = uiState,
        onStartScan = { viewModel.startScanning() },
        onDismissAlert = { viewModel.dismissAlert() },
        onSelectMember = { viewModel.selectMember(it) },
        onBindMember = { viewModel.bindCurrentMeasurementToMember(it) },
        modifier = modifier
    )
}
