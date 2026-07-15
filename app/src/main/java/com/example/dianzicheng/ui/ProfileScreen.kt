package com.example.dianzicheng.ui

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.dianzicheng.domain.FamilyMember
import com.example.dianzicheng.domain.Sex
import com.example.dianzicheng.ui.theme.电子秤Theme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileContent(
    members: List<FamilyMember>,
    onAddMember: (String, Sex, Double, Long, Double) -> Unit,
    onDeleteMember: (FamilyMember) -> Unit,
    onResetPairing: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddDialog by remember { mutableStateOf(false) }

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
                        icon = Icons.Default.Info,
                        onClick = { /* Show about dialog */ }
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
}

@Composable
fun SettingsItem(title: String, icon: ImageVector, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        color = androidx.compose.ui.graphics.Color.Transparent
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
                Icon(
                    icon, 
                    contentDescription = null, 
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(text = title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.weight(1f))
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
                    onConfirm(
                        name,
                        sex,
                        height.toDoubleOrNull() ?: 170.0,
                        System.currentTimeMillis() - (1000L * 60 * 60 * 24 * 365 * 25),
                        0.0
                    )
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
    val members by viewModel.members.collectAsState()
    ProfileContent(
        members = members,
        onAddMember = { name, sex, height, birth, weight -> viewModel.addMember(name, sex, height, birth, weight) },
        onDeleteMember = { viewModel.deleteMember(it) },
        onResetPairing = { viewModel.resetPairing() },
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun ProfilePreview() {
    电子秤Theme(dynamicColor = true) {
        ProfileContent(
            members = listOf(
                FamilyMember("1", "我的名字", Sex.MALE, 175.0, 0, 70.0),
                FamilyMember("2", "家庭成员", Sex.FEMALE, 165.0, 0, 50.0)
            ),
            onAddMember = { _, _, _, _, _ -> },
            onDeleteMember = {},
            onResetPairing = {}
        )
    }
}
