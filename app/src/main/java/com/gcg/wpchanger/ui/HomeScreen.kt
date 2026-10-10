package com.gcg.wpchanger.ui

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.BatteryManager
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.gcg.wpchanger.data.StackNames
import com.gcg.wpchanger.data.TimerInterval
import com.gcg.wpchanger.data.WallpaperItem
import com.gcg.wpchanger.data.WallpaperStack
import com.gcg.wpchanger.data.WallpaperTarget
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    viewModel: WallpaperViewModel,
    onPickPhotosClick: () -> Unit,
    onPickFolderClick: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var previewItem by remember { mutableStateOf<WallpaperItem?>(null) }
    var stackDialog by remember { mutableStateOf<StackDialog?>(null) }

    val context = LocalContext.current
    var isIgnoringBatteryOptimizations by remember {
        mutableStateOf(isIgnoringBatteryOptimizations(context))
    }
    var hasNotificationPermission by remember {
        mutableStateOf(hasNotificationPermission(context))
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                // Re-check after returning from the system battery/notification settings screen,
                // or after responding to the notification permission prompt.
                isIgnoringBatteryOptimizations = isIgnoringBatteryOptimizations(context)
                hasNotificationPermission = hasNotificationPermission(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Battery Saver / low-battery state can change while the app is open (unlike the checks
    // above), so this listens live instead of only re-checking on resume.
    var isBatterySaverOn by remember { mutableStateOf(isBatterySaverOn(context)) }
    var isBatteryLow by remember { mutableStateOf(isBatteryLow(context)) }
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(receiverContext: Context, intent: Intent) {
                isBatterySaverOn = isBatterySaverOn(context)
                isBatteryLow = isBatteryLow(context)
            }
        }
        val filter = IntentFilter().apply {
            addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
            addAction(Intent.ACTION_BATTERY_CHANGED)
        }
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        onDispose { context.unregisterReceiver(receiver) }
    }

    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Wallpaper,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = "Wallpaper Changer",
                            fontWeight = FontWeight.Bold,
                        )
                    }
                },
                actions = {
                    StatusPill(isActive = uiState.isActive)
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { innerPadding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Hero Auto-Changer Control Card
            item(span = { GridItemSpan(maxLineSpan) }) {
                HeroControlCard(
                    uiState = uiState,
                    onToggleActive = { viewModel.toggleActive(it) },
                    onChangeNow = { viewModel.changeWallpaperNow() },
                    isBatterySaverOn = isBatterySaverOn,
                    isBatteryLow = isBatteryLow,
                )
            }

            // Battery optimization nudge (only relevant once rotation is actually turned on)
            if (uiState.isActive && !isIgnoringBatteryOptimizations) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    BatteryOptimizationBanner(
                        onRequestClick = {
                            // Opens App info rather than the direct exemption prompt: Play policy only
                            // allows REQUEST_IGNORE_BATTERY_OPTIMIZATIONS for apps like VoIP or navigation.
                            val intent = Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.parse("package:${context.packageName}"),
                            )
                            context.startActivity(intent)
                        },
                    )
                }
            }

            // Timer Interval Options
            item(span = { GridItemSpan(maxLineSpan) }) {
                TimerIntervalCard(
                    selectedInterval = uiState.interval,
                    onIntervalSelected = { viewModel.setInterval(it) },
                    notifyOnChange = uiState.notifyOnChange,
                    onNotifyOnChangeToggled = { checked ->
                        viewModel.setNotifyOnChange(checked)
                        if (checked && !hasNotificationPermission) {
                            onRequestNotificationPermission()
                        }
                    },
                    showNotificationPermissionWarning = uiState.notifyOnChange && !hasNotificationPermission,
                )
            }

            // Target Screen Selection
            item(span = { GridItemSpan(maxLineSpan) }) {
                TargetScreenCard(
                    selectedTarget = uiState.target,
                    onTargetSelected = { viewModel.setTarget(it) },
                )
            }

            // Stack switcher, sitting right above the photos of the selected stack
            item(span = { GridItemSpan(maxLineSpan) }) {
                StacksCard(
                    stacks = uiState.stacks,
                    activeStack = uiState.activeStack,
                    onStackSelected = { viewModel.selectStack(it) },
                    onNewStackClick = { stackDialog = StackDialog.NEW },
                    onRenameClick = { stackDialog = StackDialog.RENAME },
                    onDeleteClick = { stackDialog = StackDialog.DELETE },
                )
            }

            // Wallpaper Pool Header & Action Buttons
            item(span = { GridItemSpan(maxLineSpan) }) {
                WallpaperPoolHeader(
                    title = uiState.activeStack.ifEmpty { "Wallpaper Pool" },
                    count = uiState.wallpapers.size,
                    totalSizeBytes = uiState.wallpapers.sumOf { it.sizeBytes },
                    onPickPhotosClick = onPickPhotosClick,
                    onPickFolderClick = onPickFolderClick,
                    onClearAllClick = { showClearConfirmDialog = true },
                )
            }

            // Wallpapers Grid or Empty State
            if (uiState.wallpapers.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    EmptyWallpaperPool(
                        onPickPhotosClick = onPickPhotosClick,
                        onPickFolderClick = onPickFolderClick,
                    )
                }
            } else {
                items(
                    items = uiState.wallpapers,
                    key = { it.id },
                ) { item ->
                    WallpaperCard(
                        item = item,
                        isLastApplied = item.id == uiState.lastWallpaperId,
                        onDeleteClick = { viewModel.deleteWallpaper(item.id) },
                        onItemClick = { previewItem = item },
                    )
                }
            }
        }
    }

    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = { Text("Clear All Wallpapers?") },
            text = { Text("This will remove all wallpapers from \"${uiState.activeStack}\". You will need to select new photos.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAllWallpapers()
                        showClearConfirmDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }

    val stackNames = uiState.stacks.map { it.name }
    when (stackDialog) {
        StackDialog.NEW -> StackNameDialog(
            title = "New Stack",
            initialName = "",
            existingNames = stackNames,
            confirmLabel = "Create",
            onConfirm = { viewModel.createStack(it) },
            onDismiss = { stackDialog = null },
        )
        StackDialog.RENAME -> StackNameDialog(
            title = "Rename Stack",
            initialName = uiState.activeStack,
            existingNames = stackNames - uiState.activeStack,
            confirmLabel = "Rename",
            onConfirm = { viewModel.renameActiveStack(it) },
            onDismiss = { stackDialog = null },
        )
        StackDialog.DELETE -> AlertDialog(
            onDismissRequest = { stackDialog = null },
            title = { Text("Delete \"${uiState.activeStack}\"?") },
            text = {
                Text(
                    "This removes the stack and its ${uiState.wallpapers.size} photo(s) from the app. " +
                        "The originals in your gallery aren't affected.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteActiveStack()
                        stackDialog = null
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { stackDialog = null }) {
                    Text("Cancel")
                }
            },
        )
        null -> Unit
    }

    // Full screen image preview dialog
    previewItem?.let { item ->
        Dialog(onDismissRequest = { previewItem = null }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .aspectRatio(9f / 16f),
                shape = RoundedCornerShape(24.dp),
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(item.file)
                            .crossfade(true)
                            .build(),
                        contentDescription = item.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )

                    // Top Bar with Close button
                    IconButton(
                        onClick = { previewItem = null },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close preview",
                            tint = Color.White,
                        )
                    }

                    // Bottom info bar
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)),
                                ),
                            )
                            .padding(16.dp),
                    ) {
                        Column {
                            Text(
                                text = item.name,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = "${formatFileSize(item.sizeBytes)} • Added ${formatDate(item.addedTimestamp)}",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 12.sp,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusPill(isActive: Boolean) {
    Surface(
        shape = CircleShape,
        color = if (isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.padding(end = 12.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(
                        color = if (isActive) Color(0xFF10B981) else MaterialTheme.colorScheme.outline,
                        shape = CircleShape,
                    ),
            )
            Text(
                text = if (isActive) "Active" else "Paused",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun HeroControlCard(
    uiState: WallpaperUiState,
    onToggleActive: (Boolean) -> Unit,
    onChangeNow: () -> Unit,
    isBatterySaverOn: Boolean,
    isBatteryLow: Boolean,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Auto-Change Wallpaper",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = if (uiState.isActive) {
                            "Rotates every ${uiState.interval.label.lowercase()} • ${uiState.target.label} • ${uiState.activeStack}"
                        } else {
                            "Paused. Toggle switch to start rotation."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Switch(
                    checked = uiState.isActive,
                    onCheckedChange = onToggleActive,
                    thumbContent = if (uiState.isActive) {
                        {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(SwitchDefaults.IconSize),
                            )
                        }
                    } else {
                        null
                    },
                )
            }

            if (uiState.lastChangedTimestamp > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "Last changed: ${formatDate(uiState.lastChangedTimestamp)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (uiState.isActive && (isBatterySaverOn || isBatteryLow)) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.BatteryAlert,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.tertiary,
                    )
                    Text(
                        text = if (isBatterySaverOn) {
                            "Paused while Battery Saver is on"
                        } else {
                            "Paused while battery is low"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }
            }

            // Switching to (or creating) an empty stack doesn't pause rotation, so say why nothing changes.
            if (uiState.isActive && uiState.wallpapers.isEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoLibrary,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.error,
                    )
                    Text(
                        text = "\"${uiState.activeStack}\" is empty. Add photos or pick another stack.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            FilledTonalButton(
                onClick = onChangeNow,
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading && uiState.wallpapers.isNotEmpty(),
                shape = RoundedCornerShape(12.dp),
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Applying Wallpaper...")
                } else {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Change Wallpaper Now")
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TimerIntervalCard(
    selectedInterval: TimerInterval,
    onIntervalSelected: (TimerInterval) -> Unit,
    notifyOnChange: Boolean,
    onNotifyOnChangeToggled: (Boolean) -> Unit,
    showNotificationPermissionWarning: Boolean,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Text(
                text = "Timer Interval",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Select how often wallpaper randomly updates",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(10.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                TimerInterval.entries.forEach { interval ->
                    val isSelected = interval == selectedInterval
                    FilterChip(
                        selected = isSelected,
                        onClick = { onIntervalSelected(interval) },
                        label = { Text(interval.label) },
                        leadingIcon = if (isSelected) {
                            {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(FilterChipDefaults.IconSize),
                                )
                            }
                        } else {
                            null
                        },
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNotifyOnChangeToggled(!notifyOnChange) },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(checked = notifyOnChange, onCheckedChange = onNotifyOnChangeToggled)
                Text(
                    text = "Notify me when the wallpaper changes",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            if (showNotificationPermissionWarning) {
                Text(
                    text = "Notifications are blocked in system settings, so you won't see these.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(start = 12.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TargetScreenCard(
    selectedTarget: WallpaperTarget,
    onTargetSelected: (WallpaperTarget) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Text(
                text = "Apply Target",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Choose which screen(s) to update",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(10.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                WallpaperTarget.entries.forEach { target ->
                    val isSelected = target == selectedTarget
                    FilterChip(
                        selected = isSelected,
                        onClick = { onTargetSelected(target) },
                        label = { Text(target.label) },
                        leadingIcon = if (isSelected) {
                            {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(FilterChipDefaults.IconSize),
                                )
                            }
                        } else {
                            null
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun BatteryOptimizationBanner(onRequestClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = Icons.Default.BatteryAlert,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Improve rotation reliability",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
                Text(
                    text = "Battery optimization can delay or skip scheduled wallpaper changes. For on-time rotation, set this app's battery usage to Unrestricted in App info.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
            TextButton(onClick = onRequestClick) {
                Text("Open")
            }
        }
    }
}

private fun isIgnoringBatteryOptimizations(context: Context): Boolean {
    val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return true
    return powerManager.isIgnoringBatteryOptimizations(context.packageName)
}

private fun hasNotificationPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED
}

private fun isBatterySaverOn(context: Context): Boolean {
    val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return false
    return powerManager.isPowerSaveMode
}

private fun isBatteryLow(context: Context): Boolean {
    val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager ?: return false
    val level = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    return level in 0..LOW_BATTERY_THRESHOLD_PERCENT && !batteryManager.isCharging
}

private const val LOW_BATTERY_THRESHOLD_PERCENT = 15

private enum class StackDialog { NEW, RENAME, DELETE }

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StacksCard(
    stacks: List<WallpaperStack>,
    activeStack: String,
    onStackSelected: (String) -> Unit,
    onNewStackClick: () -> Unit,
    onRenameClick: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Stacks",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Rotation uses photos from the selected stack",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onRenameClick) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = "Rename stack")
                }
                IconButton(onClick = onDeleteClick, enabled = stacks.size > 1) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete stack")
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                stacks.forEach { stack ->
                    val isSelected = stack.name == activeStack
                    FilterChip(
                        selected = isSelected,
                        onClick = { onStackSelected(stack.name) },
                        label = { Text("${stack.name} · ${stack.count}") },
                        leadingIcon = if (isSelected) {
                            {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(FilterChipDefaults.IconSize),
                                )
                            }
                        } else {
                            null
                        },
                    )
                }
                AssistChip(
                    onClick = onNewStackClick,
                    label = { Text("New") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(FilterChipDefaults.IconSize),
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun StackNameDialog(
    title: String,
    initialName: String,
    existingNames: List<String>,
    confirmLabel: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    val error = StackNames.validate(name, existingNames)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                singleLine = true,
                isError = name.isNotEmpty() && error != null,
                supportingText = if (name.isNotEmpty() && error != null) {
                    { Text(error) }
                } else {
                    null
                },
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(name)
                    onDismiss()
                },
                enabled = error == null,
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@Composable
private fun WallpaperPoolHeader(
    title: String,
    count: Int,
    totalSizeBytes: Long,
    onPickPhotosClick: () -> Unit,
    onPickFolderClick: () -> Unit,
    onClearAllClick: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Text(
                        text = "$count",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }

            if (count > 0) {
                TextButton(
                    onClick = onClearAllClick,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Clear All")
                }
            }
        }

        if (count > 0) {
            Spacer(modifier = Modifier.height(2.dp))
            val isOverThreshold = totalSizeBytes > STORAGE_WARNING_THRESHOLD_BYTES
            Text(
                text = "${formatFileSize(totalSizeBytes)} used" +
                    if (isOverThreshold) " • consider clearing photos you no longer need" else "",
                style = MaterialTheme.typography.bodySmall,
                color = if (isOverThreshold) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                onClick = onPickPhotosClick,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add Photos")
            }

            OutlinedButton(
                onClick = onPickFolderClick,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.FolderOpen,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add Folder")
            }
        }
    }
}

@Composable
private fun EmptyWallpaperPool(
    onPickPhotosClick: () -> Unit,
    onPickFolderClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = Icons.Default.PhotoLibrary,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "This Stack Is Empty",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Select pictures using the system photo picker or choose a folder to automatically rotate them.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onPickPhotosClick) {
                    Text("Select Photos")
                }
                OutlinedButton(onClick = onPickFolderClick) {
                    Text("Select Folder")
                }
            }
        }
    }
}

@Composable
private fun WallpaperCard(
    item: WallpaperItem,
    isLastApplied: Boolean,
    onDeleteClick: () -> Unit,
    onItemClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(9f / 16f)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onItemClick)
            .then(
                if (isLastApplied) {
                    Modifier.border(
                        2.dp,
                        MaterialTheme.colorScheme.primary,
                        RoundedCornerShape(16.dp),
                    )
                } else {
                    Modifier
                },
            ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(item.file)
                    .crossfade(true)
                    .build(),
                contentDescription = item.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )

            // Top gradient overlay for delete button visibility
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Black.copy(alpha = 0.5f), Color.Transparent),
                        ),
                    ),
            )

            // Delete button
            IconButton(
                onClick = onDeleteClick,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(28.dp)
                    .background(Color.Black.copy(alpha = 0.6f), CircleShape),
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Delete",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp),
                )
            }

            // Bottom gradient overlay for active badge and label
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)),
                        ),
                    )
                    .padding(horizontal = 6.dp, vertical = 6.dp),
            ) {
                if (isLastApplied) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.align(Alignment.BottomStart),
                    ) {
                        Text(
                            text = "ACTIVE",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                } else {
                    Text(
                        text = item.name,
                        color = Color.White,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

private const val STORAGE_WARNING_THRESHOLD_BYTES = 500L * 1024 * 1024

private fun formatDate(timestamp: Long): String {
    if (timestamp == 0L) return "Never"
    val sdf = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

private fun formatFileSize(size: Long): String {
    if (size <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB")
    val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
    val formatted = String.format(Locale.getDefault(), "%.1f", size / Math.pow(1024.0, digitGroups.toDouble()))
    return "$formatted ${units[digitGroups]}"
}
