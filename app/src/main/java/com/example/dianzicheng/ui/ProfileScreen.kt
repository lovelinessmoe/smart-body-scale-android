package com.example.dianzicheng.ui

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.health.connect.client.PermissionController
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.dianzicheng.domain.FamilyMember
import com.example.dianzicheng.domain.Sex
import com.example.dianzicheng.ui.theme.电子秤Theme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileContent(
    members: List<FamilyMember>,
    onAddMember: (String, Sex, Double, Long, Double) -> Unit,
    onDeleteMember: (FamilyMember) -> Unit,
    onResetPairing: () -> Unit,
    healthConnectEnabled: Boolean,
    onToggleHealthConnect: (Boolean) -> Unit,
    onRequestHealthConnectPermissions: () -> Unit,
    webdavUrl: String,
    webdavUsername: String,
    webdavPassword: String,
    lastBackupTime: Long,
    onSaveWebdavConfig: (String, String, String) -> Unit,
    onTestWebdavConnection: (String, String, String) -> Unit,
    onBackupData: () -> Unit,
    onRestoreData: () -> Unit,
    isOperating: Boolean,
    modifier: Modifier = Modifier
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var showWebdavDialog by remember { mutableStateOf(false) }

    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text("我的", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "添加")
                    }
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Member Section
            item {
                Text(
                    text = "成员管理",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (members.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        ),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Text(
                            "暂无成员，点击右上角图标添加",
                            modifier = Modifier.padding(24.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(members) { member ->
                    MemberCard(
                        member = member,
                        onDelete = { onDeleteMember(member) }
                    )
                }
            }

            // System Health Section
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "系统健康同步 (Health Connect)",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Favorite,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "同步至系统健康", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                            Text(
                                text = "将体重与体脂数据同步至系统 Health Connect",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = healthConnectEnabled,
                            onCheckedChange = onToggleHealthConnect
                        )
                    }

                    if (healthConnectEnabled) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                        SettingsItem(
                            title = "申请健康中心读写权限",
                            icon = Icons.Default.Security,
                            onClick = onRequestHealthConnectPermissions
                        )
                    }
                }
            }

            // WebDAV Backup Section
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "WebDAV 云端备份",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                ) {
                    SettingsItem(
                        title = if (webdavUrl.isBlank()) "配置 WebDAV 服务器" else "WebDAV 已配置 (${if (webdavUrl.length > 24) webdavUrl.take(24) + "..." else webdavUrl})",
                        icon = Icons.Default.Cloud,
                        onClick = { showWebdavDialog = true }
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                    SettingsItem(
                        title = "立即备份到 WebDAV",
                        subtitle = if (lastBackupTime > 0) "上次备份：${dateFormat.format(Date(lastBackupTime))}" else "从未备份",
                        icon = Icons.Default.CloudUpload,
                        onClick = onBackupData,
                        isLoading = isOperating
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                    SettingsItem(
                        title = "从 WebDAV 恢复数据",
                        icon = Icons.Default.CloudDownload,
                        onClick = onRestoreData,
                        isLoading = isOperating
                    )
                }
            }

            // Settings Section
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "应用设置",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                ) {
                    SettingsItem(
                        title = "重新配对设备",
                        icon = Icons.Default.Bluetooth,
                        onClick = onResetPairing
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                    SettingsItem(
                        title = "关于软件",
                        subtitle = "版本 1.2",
                        icon = Icons.Default.Info,
                        onClick = { }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }

    if (showAddDialog) {
        AddMemberDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, sex, height, birth, weight ->
                onAddMember(name, sex, height, birth, weight)
                showAddDialog = false
            }
        )
    }

    if (showWebdavDialog) {
        WebDavConfigDialog(
            initialUrl = webdavUrl,
            initialUsername = webdavUsername,
            initialPassword = webdavPassword,
            onDismiss = { showWebdavDialog = false },
            onSave = { url, user, pass ->
                onSaveWebdavConfig(url, user, pass)
                showWebdavDialog = false
            },
            onTest = { url, user, pass ->
                onTestWebdavConnection(url, user, pass)
            },
            isOperating = isOperating
        )
    }
}

@Composable
fun SettingsItem(
    title: String,
    subtitle: String? = null,
    icon: ImageVector,
    onClick: () -> Unit,
    isLoading: Boolean = false
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        color = androidx.compose.ui.graphics.Color.Transparent,
        enabled = !isLoading
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        icon,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Icon(
                Icons.Default.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
fun WebDavConfigDialog(
    initialUrl: String,
    initialUsername: String,
    initialPassword: String,
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit,
    onTest: (String, String, String) -> Unit,
    isOperating: Boolean
) {
    var url by remember { mutableStateOf(initialUrl) }
    var username by remember { mutableStateOf(initialUsername) }
    var password by remember { mutableStateOf(initialPassword) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("配置 WebDAV 云同步") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "支持坚果云、Nextcloud、OwnCloud、NAS 等任意标准 WebDAV 服务。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("服务器 URL (如 https://dav.jianguoyun.com/dav/)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                )
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("账号 / 用户名") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("密码 / 应用授权码") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(url, username, password) },
                shape = MaterialTheme.shapes.medium
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { onTest(url, username, password) },
                    enabled = !isOperating,
                    shape = MaterialTheme.shapes.medium
                ) {
                    if (isOperating) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Text("测试连接")
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("取消")
                }
            }
        }
    )
}

@Composable
fun MemberCard(member: FamilyMember, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = member.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    text = "${if (member.sex == Sex.MALE) "男" else "女"} · ${member.heightCm.toInt()}cm",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(
                onClick = onDelete,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f),
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(Icons.Default.Delete, contentDescription = "删除", modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
fun AddMemberDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Sex, Double, Long, Double) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var sex by remember { mutableStateOf(Sex.MALE) }
    var height by remember { mutableStateOf("170") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加新成员") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("姓名") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                )

                Text("性别", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    FilterChip(
                        selected = sex == Sex.MALE,
                        onClick = { sex = Sex.MALE },
                        label = { Text("男生") },
                        leadingIcon = if (sex == Sex.MALE) { { Icon(Icons.Default.Check, null) } } else null
                    )
                    FilterChip(
                        selected = sex == Sex.FEMALE,
                        onClick = { sex = Sex.FEMALE },
                        label = { Text("女生") },
                        leadingIcon = if (sex == Sex.FEMALE) { { Icon(Icons.Default.Check, null) } } else null
                    )
                }

                OutlinedTextField(
                    value = height,
                    onValueChange = { height = it },
                    label = { Text("身高 (cm)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalName = if (name.isBlank()) "新成员" else name.trim()
                    onConfirm(
                        finalName,
                        sex,
                        height.toDoubleOrNull() ?: 170.0,
                        System.currentTimeMillis() - (1000L * 60 * 60 * 24 * 365 * 25),
                        0.0
                    )
                    onDismiss()
                },
                shape = MaterialTheme.shapes.medium
            ) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val members by viewModel.members.collectAsState()
    val healthConnectEnabled by viewModel.healthConnectEnabled.collectAsState()
    val webdavUrl by viewModel.webdavUrl.collectAsState()
    val webdavUsername by viewModel.webdavUsername.collectAsState()
    val webdavPassword by viewModel.webdavPassword.collectAsState()
    val lastBackupTime by viewModel.lastBackupTime.collectAsState()
    val isOperating by viewModel.isOperating.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract()
    ) { granted ->
        if (granted.isNotEmpty()) {
            Toast.makeText(context, "已获得系统健康权限！", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "未获得健康读写权限", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(statusMessage) {
        statusMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            viewModel.clearStatusMessage()
        }
    }

    ProfileContent(
        members = members,
        onAddMember = { name, sex, height, birth, weight -> viewModel.addMember(name, sex, height, birth, weight) },
        onDeleteMember = { viewModel.deleteMember(it) },
        onResetPairing = { viewModel.resetPairing() },
        healthConnectEnabled = healthConnectEnabled,
        onToggleHealthConnect = { enabled ->
            viewModel.setHealthConnectEnabled(enabled)
            if (enabled && viewModel.isHealthConnectAvailable()) {
                permissionLauncher.launch(
                    setOf(
                        androidx.health.connect.client.permission.HealthPermission.getWritePermission(androidx.health.connect.client.records.WeightRecord::class),
                        androidx.health.connect.client.permission.HealthPermission.getWritePermission(androidx.health.connect.client.records.BodyFatRecord::class),
                        androidx.health.connect.client.permission.HealthPermission.getReadPermission(androidx.health.connect.client.records.WeightRecord::class),
                        androidx.health.connect.client.permission.HealthPermission.getReadPermission(androidx.health.connect.client.records.BodyFatRecord::class)
                    )
                )
            }
        },
        onRequestHealthConnectPermissions = {
            if (viewModel.isHealthConnectAvailable()) {
                permissionLauncher.launch(
                    setOf(
                        androidx.health.connect.client.permission.HealthPermission.getWritePermission(androidx.health.connect.client.records.WeightRecord::class),
                        androidx.health.connect.client.permission.HealthPermission.getWritePermission(androidx.health.connect.client.records.BodyFatRecord::class),
                        androidx.health.connect.client.permission.HealthPermission.getReadPermission(androidx.health.connect.client.records.WeightRecord::class),
                        androidx.health.connect.client.permission.HealthPermission.getReadPermission(androidx.health.connect.client.records.BodyFatRecord::class)
                    )
                )
            } else {
                Toast.makeText(context, "当前设备不支持或未安装 Health Connect", Toast.LENGTH_SHORT).show()
            }
        },
        webdavUrl = webdavUrl,
        webdavUsername = webdavUsername,
        webdavPassword = webdavPassword,
        lastBackupTime = lastBackupTime,
        onSaveWebdavConfig = { url, user, pass -> viewModel.saveWebdavConfig(url, user, pass) },
        onTestWebdavConnection = { url, user, pass -> viewModel.testWebdavConnection(url, user, pass) },
        onBackupData = { viewModel.backupData() },
        onRestoreData = { viewModel.restoreData() },
        isOperating = isOperating,
        modifier = modifier
    )
}
