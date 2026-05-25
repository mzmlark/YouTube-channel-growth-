package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import java.text.DecimalFormat
import java.util.Locale

class MainActivity : ComponentActivity() {
    private val viewModel = AnalyticsViewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainScreen(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainScreen(viewModel: AnalyticsViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.state.collectAsState()
    var showChannelSelector by remember { mutableStateOf(false) }
    var showNotificationDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("main_screen_scaffold"),
        containerColor = BentoBg,
        bottomBar = {
            BentoBottomNavigation(
                currentTab = state.bottomTab,
                onTabSelect = { viewModel.setBottomTab(it) }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Main Header
                BentoAppHeader(
                    selectedChannel = state.selectedChannel,
                    isSimulating = state.isLiveSimulating,
                    onToggleSimulate = { viewModel.toggleSimulation() },
                    onAvatarClick = { showChannelSelector = true },
                    onNotificationClick = { showNotificationDialog = true }
                )

                // Tab Routing Content
                Box(
                    modifier = Modifier
                        .fillTransient()
                        .weight(1f)
                ) {
                    AnimatedContent(
                        targetState = state.bottomTab,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(220)) togetherWith
                                    fadeOut(animationSpec = tween(220))
                        },
                        label = "tab_navigation_content"
                    ) { currentTab ->
                        when (currentTab) {
                            "home" -> BentoHomeTab(state = state)
                            "content" -> BentoContentTab(state = state)
                            "earn" -> BentoEarnTab(state = state)
                            "analytics" -> BentoAnalyticsTab(state = state)
                        }
                    }
                }
            }

            // Custom Channel Selector Sheet Overlay
            if (showChannelSelector) {
                ChannelSelectorOverlay(
                    selectedChannel = state.selectedChannel,
                    channels = viewModel.getChannels(),
                    onChannelSelect = { channel ->
                        viewModel.selectChannel(channel)
                        showChannelSelector = false
                    },
                    onDismiss = { showChannelSelector = false }
                )
            }

            // Notifications Mock Overlay
            if (showNotificationDialog) {
                NotificationOverlay(
                    activities = state.liveActivities.take(5),
                    onDismiss = { showNotificationDialog = false }
                )
            }
        }
    }
}

// Utility extension to avoid compiler warning
fun Modifier.fillTransient() = this.fillMaxWidth()

@Composable
fun BentoAppHeader(
    selectedChannel: ChannelInfo,
    isSimulating: Boolean,
    onToggleSimulate: () -> Unit,
    onAvatarClick: () -> Unit,
    onNotificationClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left Side: Channel Avatar & Live Details
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Interactive Channel Avatar
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(BentoBluePrimary, BentoBluePrimary.copy(alpha = 0.8f)),
                            radius = 120f
                        )
                    )
                    .clickable(onClick = onAvatarClick)
                    .testTag("channel_avatar_button"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = selectedChannel.name.take(2).uppercase(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }

            // Channel Title & Status Text
            Column(
                modifier = Modifier.clickable(onClick = onAvatarClick)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = selectedChannel.name,
                        color = BentoTextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Switch Channel",
                        tint = BentoTextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Subtitle: Pulse Realtime Signal
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val infiniteTransition = rememberInfiniteTransition(label = "live_dot")
                    val pulseOpacity by infiniteTransition.animateFloat(
                        initialValue = 0.3f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1000, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "dot_alpha"
                    )

                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(
                                color = if (isSimulating) GreenAlert.copy(alpha = pulseOpacity) else BentoTextSecondary.copy(
                                    alpha = 0.6f
                                )
                            )
                    )
                    Text(
                        text = if (isSimulating) "Live Tracker Active" else "Stream Paused",
                        color = BentoTextSecondary.copy(alpha = 0.8f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Right Side: Quick Sim Toggle & Alarm Button
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Live Simulation Control Button
            IconButton(
                onClick = onToggleSimulate,
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = if (isSimulating) BentoBlueContainer else BentoGreyContainer,
                        shape = CircleShape
                    )
                    .testTag("simulator_toggle_button")
            ) {
                Icon(
                    imageVector = if (isSimulating) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = "Toggle Simulation",
                    tint = if (isSimulating) BentoBlueDarkText else BentoTextPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Notification Circular Button
            IconButton(
                onClick = onNotificationClick,
                modifier = Modifier
                    .size(40.dp)
                    .background(color = BentoGreyContainer, shape = CircleShape)
                    .testTag("notification_bell_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = "Notifications log",
                    tint = BentoTextPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun BentoHomeTab(state: AnalyticsState) {
    ScrollableColumnContainer {
        // Top Large Primary Cell: Subscriber Counter & Moving Trend Chart
        BentoSubscriberCard(state = state)

        Spacer(modifier = Modifier.height(12.dp))

        // Row containing Views (Light Purple) & Revenue (Light Grey/Bordered)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                BentoViewsCard(state = state)
            }
            Box(modifier = Modifier.weight(1f)) {
                BentoRevenueCard(state = state)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Large Accent Block: Detailed Video Engagement Progress & Breakdown
        BentoEngagementCard(state = state)

        Spacer(modifier = Modifier.height(12.dp))

        // Interactive Live Stream Feed Module
        BentoLiveStreamLog(state = state)
    }
}

@Composable
fun BentoSubscriberCard(state: AnalyticsState) {
    var animateTrigger by remember { mutableStateOf(false) }
    // Triggers transient visual flash whenever subscriber number changes
    LaunchedEffect(state.subscribers) {
        animateTrigger = true
        delay(400)
        animateTrigger = false
    }

    val glowColor by animateToColor(
        trigger = animateTrigger,
        activeColor = BentoBluePrimary.copy(alpha = 0.2f),
        normalColor = Color.Transparent
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(28.dp))
            .background(color = BentoBlueContainer, shape = RoundedCornerShape(28.dp))
            .drawBehind {
                drawRect(color = glowColor)
            }
            .padding(22.dp)
            .testTag("bento_subscriber_card")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Subscribers",
                    color = BentoBlueDarkText.copy(alpha = 0.7f),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                Text(
                    text = state.selectedChannel.category,
                    color = BentoBlueDarkText.copy(alpha = 0.5f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Sparkle Today Grow Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.5f))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+${formatNumber(state.subIncrementToday.toLong())} today",
                    color = BentoBluePrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Massive Display Numeric Count
        Text(
            text = formatDecimal(state.subscribers),
            color = BentoBlueDarkText,
            fontWeight = FontWeight.Bold,
            fontSize = 42.sp,
            letterSpacing = (-1).sp,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Interactive Live Flow Graph
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(65.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height
                val dataPoints = state.chartPoints

                if (dataPoints.isNotEmpty()) {
                    val stepX = width / (dataPoints.size - 1)
                    val maxVal = 100f
                    val minVal = 0f

                    // Draw Background Gradient Area
                    val fillPath = Path().apply {
                        moveTo(0f, height)
                        dataPoints.forEachIndexed { idx, value ->
                            val normalizedY = height - ((value - minVal) / (maxVal - minVal) * height)
                            lineTo(idx * stepX, normalizedY)
                        }
                        lineTo(width, height)
                        close()
                    }

                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(BentoBluePrimary.copy(alpha = 0.25f), Color.Transparent),
                            startY = 0f,
                            endY = height
                        )
                    )

                    // Draw Stroke Line
                    val strokePath = Path().apply {
                        dataPoints.forEachIndexed { idx, value ->
                            val normalizedY = height - ((value - minVal) / (maxVal - minVal) * height)
                            if (idx == 0) {
                                moveTo(0f, normalizedY)
                            } else {
                                lineTo(idx * stepX, normalizedY)
                            }
                        }
                    }

                    drawPath(
                        path = strokePath,
                        color = BentoBluePrimary,
                        style = Stroke(width = 3.dp.toPx())
                    )

                    // Draw Circle pulse on final point
                    val lastX = width
                    val lastY = height - ((dataPoints.last() - minVal) / (maxVal - minVal) * height)
                    drawCircle(
                        color = BentoBluePrimary,
                        radius = 5.dp.toPx(),
                        center = Offset(lastX, lastY)
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 2.5.dp.toPx(),
                        center = Offset(lastX, lastY)
                    )
                }
            }
        }
    }
}

@Composable
fun BentoViewsCard(state: AnalyticsState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(1.dp, RoundedCornerShape(24.dp))
            .background(color = BentoPurpleContainer, shape = RoundedCornerShape(24.dp))
            .padding(18.dp)
            .testTag("bento_views_card"),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Views",
                    color = BentoPurpleDarkText.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Icon(
                    imageVector = Icons.Filled.Visibility,
                    contentDescription = null,
                    tint = BentoPurpleDarkText.copy(alpha = 0.6f),
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = formatShortNumber(state.views),
                color = BentoPurpleDarkText,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                letterSpacing = (-0.5).sp
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Last 48 hours",
            color = BentoPurpleDarkText.copy(alpha = 0.6f),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun BentoRevenueCard(state: AnalyticsState) {
    val decimalFormat = remember { DecimalFormat("$#,##0.00") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(1.dp, RoundedCornerShape(24.dp))
            .border(width = 1.dp, color = BentoGreyBorder.copy(alpha = 0.5f), shape = RoundedCornerShape(24.dp))
            .background(color = BentoGreyContainer, shape = RoundedCornerShape(24.dp))
            .padding(18.dp)
            .testTag("bento_revenue_card"),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Revenue",
                    color = BentoTextSecondary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
                Icon(
                    imageVector = Icons.Filled.MonetizationOn,
                    contentDescription = null,
                    tint = BentoTextSecondary.copy(alpha = 0.6f),
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = decimalFormat.format(state.revenue),
                color = BentoTextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.TrendingUp,
                contentDescription = null,
                tint = GreenAlert,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = "+2.1% stream boost",
                color = GreenAlert,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun BentoEngagementCard(state: AnalyticsState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(1.dp, RoundedCornerShape(28.dp))
            .border(width = 1.dp, color = BentoCardBorder, shape = RoundedCornerShape(28.dp))
            .background(color = BentoCardBg, shape = RoundedCornerShape(28.dp))
            .padding(20.dp)
            .testTag("bento_engagement_card")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Engagement Overview",
                color = BentoTextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            Icon(
                imageVector = Icons.Filled.TrendingUp,
                contentDescription = null,
                tint = BentoBluePrimary,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // CTR Column
            Column(
                modifier = Modifier
                    .weight(1f)
                    .border(width = 1.dp, color = BentoCardBorder, shape = RoundedCornerShape(16.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = "CTR",
                    color = BentoTextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = String.format(Locale.US, "%.1f%%", state.ctr),
                    color = BentoBluePrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Avg channel: 8.2%",
                    color = BentoTextSecondary.copy(alpha = 0.7f),
                    fontSize = 10.sp
                )
            }

            // Watch Time Column
            Column(
                modifier = Modifier
                    .weight(1f)
                    .border(width = 1.dp, color = BentoCardBorder, shape = RoundedCornerShape(16.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = "Watch Time",
                    color = BentoTextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${String.format(Locale.US, "%.1f", state.watchTime / 1000f)}K",
                    color = BentoBluePrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Total Hours",
                    color = BentoTextSecondary.copy(alpha = 0.7f),
                    fontSize = 10.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Custom Double-Split Segmented Meter representing ratio (e.g., 65% Subscriber Watched vs 35% Guest)
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Subscriber views ratio",
                    color = BentoTextSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "65% subscribed",
                    color = BentoBluePrimary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(6.dp))

            // Dual Progress bar track
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape)
                    .background(color = BentoGreyBorder.copy(alpha = 0.3f))
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(0.65f)
                            .background(color = BentoBluePrimary)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(0.35f)
                            .background(color = BentoBlueContainer)
                    )
                }
            }
        }
    }
}

@Composable
fun BentoLiveStreamLog(state: AnalyticsState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(1.dp, RoundedCornerShape(24.dp))
            .border(width = 1.dp, color = BentoCardBorder, shape = RoundedCornerShape(24.dp))
            .background(color = BentoCardBg, shape = RoundedCornerShape(24.dp))
            .padding(20.dp)
            .heightIn(max = 240.dp)
            .testTag("bento_live_stream_log")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(color = GreenAlert)
                )
                Text(
                    text = "Live Stream Events",
                    color = BentoTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            Text(
                text = "${state.liveActivities.size} events logged",
                color = BentoTextSecondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (state.liveActivities.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Starting live stream simulation...",
                    color = BentoTextSecondary.copy(alpha = 0.5f),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.liveActivities, key = { it.id }) { activity ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateItem()
                            .background(
                                color = if (activity.highlight) BentoBlueContainer.copy(alpha = 0.4f) else Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Action Circular Mini Indicator
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(
                                    color = when (activity.type) {
                                        ActivityType.SUBSCRIBE -> BentoBluePrimary.copy(alpha = 0.15f)
                                        ActivityType.SUPERCHAT -> Color(0xFFFFF3CD)
                                        ActivityType.COMMENT -> BentoGreyBorder.copy(alpha = 0.25f)
                                        ActivityType.LIKE -> RedAlert.copy(alpha = 0.12f)
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when (activity.type) {
                                    ActivityType.SUBSCRIBE -> Icons.Filled.PersonAdd
                                    ActivityType.SUPERCHAT -> Icons.Filled.AttachMoney
                                    ActivityType.COMMENT -> Icons.Filled.ChatBubble
                                    ActivityType.LIKE -> Icons.Default.Favorite
                                },
                                contentDescription = null,
                                tint = when (activity.type) {
                                    ActivityType.SUBSCRIBE -> BentoBluePrimary
                                    ActivityType.SUPERCHAT -> Color(0xFF856404)
                                    ActivityType.COMMENT -> BentoTextSecondary
                                    ActivityType.LIKE -> RedAlert
                                },
                                modifier = Modifier.size(12.dp)
                            )
                        }

                        // Message details
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = activity.message,
                                color = if (activity.highlight) BentoBlueDarkText else BentoTextPrimary,
                                fontSize = 11.sp,
                                fontWeight = if (activity.highlight) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Timestamp tag
                        Text(
                            text = activity.timestamp,
                            color = BentoTextSecondary.copy(alpha = 0.6f),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BentoContentTab(state: AnalyticsState) {
    ScrollableColumnContainer {
        Text(
            text = "Latest Uploads",
            color = BentoTextPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            text = "Performance breakdown of published videos",
            color = BentoTextSecondary,
            fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        state.videos.forEach { video ->
            VideoPerformanceCard(video = video)
            Spacer(modifier = Modifier.height(12.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun VideoPerformanceCard(video: VideoItem) {
    val gradient = when (video.thumbnailGradientId) {
        1 -> Brush.linearGradient(colors = listOf(BentoBluePrimary, BentoBlueContainer))
        2 -> Brush.linearGradient(colors = listOf(BentoPurpleDarkText, BentoPurpleContainer))
        else -> Brush.linearGradient(colors = listOf(Color(0xFFFF8A80), Color(0xFFFF5252)))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(1.dp, RoundedCornerShape(20.dp))
            .border(width = 1.dp, color = BentoCardBorder, shape = RoundedCornerShape(20.dp))
            .background(color = BentoCardBg, shape = RoundedCornerShape(20.dp))
            .padding(14.dp)
            .testTag("video_card_${video.id}"),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Thumbnail preview
        Box(
            modifier = Modifier
                .size(70.dp, 70.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(brush = gradient),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.PlayCircle,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }

        // Stats details column
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = video.title,
                color = BentoTextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "Published ${video.publishTime}",
                color = BentoTextSecondary,
                fontSize = 10.sp,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // View tally
                Column {
                    Text(text = "Views", color = BentoTextSecondary.copy(alpha = 0.7f), fontSize = 9.sp)
                    Text(text = formatShortNumber(video.views), color = BentoTextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                // CTR tally
                Column {
                    Text(text = "CTR", color = BentoTextSecondary.copy(alpha = 0.7f), fontSize = 9.sp)
                    Text(text = "${video.ctr}%", color = BentoBluePrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                // Watch Time tally
                Column {
                    Text(text = "Watch Hrs", color = BentoTextSecondary.copy(alpha = 0.7f), fontSize = 9.sp)
                    Text(text = "${video.watchTimeHours.toInt()}", color = BentoTextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun BentoEarnTab(state: AnalyticsState) {
    ScrollableColumnContainer {
        Text(
            text = "Partner Program Status",
            color = BentoTextPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            text = "Track progression to monetization and extras",
            color = BentoTextSecondary,
            fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Progress card towards 1,000 Subs
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(1.dp, RoundedCornerShape(24.dp))
                .border(width = 1.dp, color = BentoCardBorder, shape = RoundedCornerShape(24.dp))
                .background(color = BentoCardBg, shape = RoundedCornerShape(24.dp))
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Core requirement: Subscribers", color = BentoTextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(text = "Unlocked ✓", color = GreenAlert, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = "${formatDecimal(state.subscribers)} / 1,000",
                    color = BentoBluePrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp
                )
                Text(text = "100%", color = BentoTextSecondary, fontSize = 11.sp)
            }
            Spacer(modifier = Modifier.height(6.dp))
            // Linear Progress indicators
            LinearProgressIndicator(
                progress = 1f,
                color = GreenAlert,
                trackColor = BentoGreyBorder.copy(alpha = 0.2f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Progress card towards 4,000 Public Watch Hours
        val currentWatchHrsFraction = (state.watchTime / 4000f).coerceIn(0.0, 1.0)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(1.dp, RoundedCornerShape(24.dp))
                .border(width = 1.dp, color = BentoCardBorder, shape = RoundedCornerShape(24.dp))
                .background(color = BentoCardBg, shape = RoundedCornerShape(24.dp))
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Requirement: Watch Hours", color = BentoTextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(
                    text = "${(currentWatchHrsFraction * 100).toInt()}% completed",
                    color = BentoBluePrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = "${formatDecimal(state.watchTime.toLong())} / 4,000 hrs",
                    color = BentoTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp
                )
                Text(text = "4K goal", color = BentoTextSecondary, fontSize = 11.sp)
            }
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = currentWatchHrsFraction.toFloat(),
                color = BentoBluePrimary,
                trackColor = BentoGreyBorder.copy(alpha = 0.2f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Bento block on program perks
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Super Chat Perk block
            Column(
                modifier = Modifier
                    .weight(1f)
                    .shadow(1.dp, RoundedCornerShape(20.dp))
                    .background(color = BentoPurpleContainer, shape = RoundedCornerShape(20.dp))
                    .padding(14.dp)
            ) {
                Icon(imageVector = Icons.Default.Favorite, contentDescription = null, tint = BentoPurpleDarkText, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = "Super Chats", color = BentoPurpleDarkText, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(text = "Viewer contributions unlocked during stream tracker.", color = BentoPurpleDarkText.copy(alpha = 0.7f), fontSize = 10.sp)
            }

            // Ads Perk block
            Column(
                modifier = Modifier
                    .weight(1f)
                    .shadow(1.dp, RoundedCornerShape(20.dp))
                    .background(color = BentoBlueContainer, shape = RoundedCornerShape(20.dp))
                    .padding(14.dp)
            ) {
                Icon(imageVector = Icons.Filled.MonetizationOn, contentDescription = null, tint = BentoBlueDarkText, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = "Watch Ads", color = BentoBlueDarkText, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(text = "Earn ad split revenue instantly upon stream.", color = BentoBlueDarkText.copy(alpha = 0.7f), fontSize = 10.sp)
            }
        }
    }
}

@Composable
fun BentoAnalyticsTab(state: AnalyticsState) {
    ScrollableColumnContainer {
        Text(
            text = "Deeper Insights",
            color = BentoTextPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            text = "Detailed charts reflecting past velocity metrics",
            color = BentoTextSecondary,
            fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Custom Bento visualizer on click CTR comparison
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(1.dp, RoundedCornerShape(24.dp))
                .border(width = 1.dp, color = BentoCardBorder, shape = RoundedCornerShape(24.dp))
                .background(color = BentoCardBg, shape = RoundedCornerShape(24.dp))
                .padding(20.dp)
        ) {
            Text(text = "Subscribers growth velocity", color = BentoTextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(14.dp))

            // Graph visualization with styled custom rows representation
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                state.chartPoints.forEachIndexed { idx, point ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "H${12 - idx}",
                            color = BentoTextSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(24.dp)
                        )

                        // Animated Row-based Horizontal bento bars
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(12.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(color = BentoGreyContainer)
                        ) {
                            val targetFraction = point / 100f
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(targetFraction)
                                    .background(
                                        brush = Brush.horizontalGradient(
                                            colors = listOf(BentoBluePrimary, BentoBluePrimary.copy(alpha = 0.5f))
                                        )
                                    )
                            )
                        }

                        Text(
                            text = "${point.toInt()}%",
                            color = BentoTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            modifier = Modifier.width(28.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ScrollableColumnContainer(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 6.dp)
    ) {
        content()
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun BentoBottomNavigation(
    currentTab: String,
    onTabSelect: (String) -> Unit
) {
    // Standard M3 styled and aligned Navigation Bar conforming bounds
    NavigationBar(
        containerColor = BentoGreyContainer,
        modifier = Modifier
            .shadow(4.dp)
            .border(width = 1.dp, color = BentoGreyBorder.copy(alpha = 0.25f))
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        NavigationBarItem(
            selected = currentTab == "home",
            onClick = { onTabSelect("home") },
            label = { Text("Home", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
            icon = { Icon(imageVector = Icons.Filled.Dashboard, contentDescription = "Home dashboard") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = BentoBlueDarkText,
                selectedTextColor = BentoBlueDarkText,
                indicatorColor = BentoBlueContainer
            ),
            modifier = Modifier.testTag("nav_home_tab")
        )

        NavigationBarItem(
            selected = currentTab == "analytics",
            onClick = { onTabSelect("analytics") },
            label = { Text("Analytics", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
            icon = { Icon(imageVector = Icons.Filled.BarChart, contentDescription = "Analytics specs") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = BentoBlueDarkText,
                selectedTextColor = BentoBlueDarkText,
                indicatorColor = BentoBlueContainer
            ),
            modifier = Modifier.testTag("nav_analytics_tab")
        )

        NavigationBarItem(
            selected = currentTab == "content",
            onClick = { onTabSelect("content") },
            label = { Text("Content", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
            icon = { Icon(imageVector = Icons.Filled.VideoLibrary, contentDescription = "Content list") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = BentoBlueDarkText,
                selectedTextColor = BentoBlueDarkText,
                indicatorColor = BentoBlueContainer
            ),
            modifier = Modifier.testTag("nav_content_tab")
        )

        NavigationBarItem(
            selected = currentTab == "earn",
            onClick = { onTabSelect("earn") },
            label = { Text("Earn", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
            icon = { Icon(imageVector = Icons.Filled.Payments, contentDescription = "Earn breakdown") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = BentoBlueDarkText,
                selectedTextColor = BentoBlueDarkText,
                indicatorColor = BentoBlueContainer
            ),
            modifier = Modifier.testTag("nav_earn_tab")
        )
    }
}

@Composable
fun ChannelSelectorOverlay(
    selectedChannel: ChannelInfo,
    channels: List<ChannelInfo>,
    onChannelSelect: (ChannelInfo) -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(8.dp, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(color = BentoBg, shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .clickable(enabled = false, onClick = {}) // block taps through
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Select Active Channel",
                    color = BentoTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close dialog")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            channels.forEach { channel ->
                val isSelected = channel.id == selectedChannel.id
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(color = if (isSelected) BentoBlueContainer else Color.Transparent)
                        .clickable { onChannelSelect(channel) }
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                        .testTag("select_channel_${channel.id}"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) BentoBluePrimary else BentoGreyBorder.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = channel.name.take(2).uppercase(),
                            color = if (isSelected) Color.White else BentoTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = channel.name,
                            color = if (isSelected) BentoBlueDarkText else BentoTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "${formatShortNumber(channel.initialSubscribers)} subscribers",
                            color = if (isSelected) BentoBlueDarkText.copy(alpha = 0.7f) else BentoTextSecondary,
                            fontSize = 11.sp
                        )
                    }

                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Selected",
                            tint = BentoBluePrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

@Composable
fun NotificationOverlay(
    activities: List<LiveActivity>,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .shadow(8.dp, RoundedCornerShape(24.dp))
                .background(color = BentoBg, shape = RoundedCornerShape(24.dp))
                .clickable(enabled = false, onClick = {})
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Notification Logs",
                    color = BentoTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close overlay")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (activities.isEmpty()) {
                Text(
                    text = "No new notices logged during this stream session.",
                    color = BentoTextSecondary.copy(alpha = 0.6f),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp)
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    activities.forEach { act ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = if (act.highlight) BentoBlueContainer.copy(alpha = 0.25f) else BentoGreyContainer.copy(
                                        alpha = 0.5f
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = when (act.type) {
                                    ActivityType.SUBSCRIBE -> Icons.Filled.PersonAdd
                                    ActivityType.SUPERCHAT -> Icons.Filled.AttachMoney
                                    ActivityType.COMMENT -> Icons.Filled.ChatBubble
                                    else -> Icons.Default.Favorite
                                },
                                contentDescription = null,
                                tint = BentoBluePrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = act.message,
                                color = BentoTextPrimary,
                                fontSize = 11.sp,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

// Custom simple cross-fade animator helper
@Composable
fun animateToColor(
    trigger: Boolean,
    activeColor: Color,
    normalColor: Color
): State<Color> {
    return animateColorAsState(
        targetValue = if (trigger) activeColor else normalColor,
        animationSpec = tween(if (trigger) 80 else 400),
        label = "color_glow"
    )
}

fun formatDecimal(count: Long): String {
    val formatter = DecimalFormat("#,###")
    return formatter.format(count)
}

fun formatNumber(count: Long): String {
    return if (count >= 1000) {
        val formatter = DecimalFormat("#,###")
        formatter.format(count)
    } else {
        count.toString()
    }
}

fun formatShortNumber(count: Long): String {
    return when {
        count >= 1_000_000 -> {
            val millions = count / 1_000_000.0
            String.format(Locale.US, "%.1fM", millions)
        }
        count >= 1000 -> {
            val thousands = count / 1000.0
            String.format(Locale.US, "%.1fK", thousands)
        }
        else -> count.toString()
    }
}
