package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.components.StudioCard
import com.example.ui.state.MainUiState

@Composable
fun MoreMenuScreen(
    uiState: MainUiState,
    onOpenAnalytics: () -> Unit,
    onOpenEditorInfo: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenBackupRestore: () -> Unit,
    onOpenExportData: () -> Unit,
    onOpenAboutApp: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("more_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "more_profile", contentType = "profile") {
            StudioCard(onClick = onOpenEditorInfo) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_editor_avatar),
                        contentDescription = "Rohan Editor Avatar",
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = uiState.editorProfile.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${uiState.editorProfile.brandName} · ${uiState.editorProfile.profession}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item(key = "more_analytics", contentType = "menu_item") {
            MoreMenuItemCard(
                title = "Analytics Dashboard",
                subtitle = "Earnings charts, video output & client analytics",
                icon = Icons.Default.Analytics,
                tag = "more_analytics",
                onClick = onOpenAnalytics
            )
        }
        item(key = "more_editor_info", contentType = "menu_item") {
            MoreMenuItemCard(
                title = "Editor Info",
                subtitle = "Professional profile, skills, software & experience counter",
                icon = Icons.Default.Person,
                tag = "more_editor_info",
                onClick = onOpenEditorInfo
            )
        }
        item(key = "more_settings", contentType = "menu_item") {
            MoreMenuItemCard(
                title = "App Settings",
                subtitle = "Theme mode, date format, currency & delete confirmation",
                icon = Icons.Default.Settings,
                tag = "more_settings",
                onClick = onOpenSettings
            )
        }
        item(key = "more_backup_restore", contentType = "menu_item") {
            MoreMenuItemCard(
                title = "Data Backup & Restore",
                subtitle = "Save or restore full portable JSON backup",
                icon = Icons.Default.Backup,
                tag = "more_backup_restore",
                onClick = onOpenBackupRestore
            )
        }
        item(key = "more_export_data", contentType = "menu_item") {
            MoreMenuItemCard(
                title = "Export Data",
                subtitle = "Export clients, projects, calendar & ledger to CSV",
                icon = Icons.Default.Download,
                tag = "more_export_data",
                onClick = onOpenExportData
            )
        }
        item(key = "more_about_app", contentType = "menu_item") {
            MoreMenuItemCard(
                title = "About App",
                subtitle = "Rohan Edits App info, ownership & offline architecture",
                icon = Icons.Default.Info,
                tag = "more_about_app",
                onClick = onOpenAboutApp
            )
        }
    }
}

@Composable
private fun MoreMenuItemCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    tag: String,
    onClick: () -> Unit
) {
    StudioCard(
        onClick = onClick,
        modifier = Modifier.testTag(tag)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
