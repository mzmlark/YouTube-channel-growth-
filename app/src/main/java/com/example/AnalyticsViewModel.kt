package com.example

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random

data class ChannelInfo(
    val id: String,
    val name: String,
    val initialSubscribers: Long,
    val initialViews: Long,
    val initialRevenue: Double,
    val initialCtr: Double,
    val initialWatchTime: Double,
    val category: String
)

data class LiveActivity(
    val id: String,
    val timestamp: String,
    val message: String,
    val type: ActivityType,
    val highlight: Boolean = false
)

enum class ActivityType {
    SUBSCRIBE,
    SUPERCHAT,
    COMMENT,
    LIKE
}

data class VideoItem(
    val id: String,
    val title: String,
    val publishTime: String,
    val views: Long,
    val ctr: Double,
    val watchTimeHours: Double,
    val likes: Int,
    val thumbnailGradientId: Int
)

data class AnalyticsState(
    val selectedChannel: ChannelInfo,
    val subscribers: Long = 0,
    val subIncrementToday: Int = 12402,
    val views: Long = 0,
    val revenue: Double = 0.0,
    val ctr: Double = 0.0,
    val watchTime: Double = 0.0,
    val isLiveSimulating: Boolean = true,
    val chartPoints: List<Float> = emptyList(),
    val liveActivities: List<LiveActivity> = emptyList(),
    val videos: List<VideoItem> = emptyList(),
    val bottomTab: String = "home" // "home", "content", "earn", "settings"
)

class AnalyticsViewModel : ViewModel() {

    private val channels = listOf(
        ChannelInfo("coding_world", "Coding World", 1240582, 452100, 4281.50, 11.4, 24500.0, "Tech & Education"),
        ChannelInfo("design_vibe", "Design Vibe", 84320, 21240, 642.00, 8.7, 1200.0, "Creativity & Art"),
        ChannelInfo("tech_bytes", "Tech Bytes", 512400, 195420, 1850.75, 10.1, 8900.0, "Gadgets & Hardware")
    )

    private val _state = MutableStateFlow(
        AnalyticsState(
            selectedChannel = channels[0]
        )
    )
    val state: StateFlow<AnalyticsState> = _state.asStateFlow()

    private var simulationJob: Job? = null

    init {
        // Initialize with default selected channel parameters
        selectChannel(channels[0])
        startSimulation()
    }

    fun getChannels(): List<ChannelInfo> = channels

    fun selectChannel(channel: ChannelInfo) {
        // Build mock chart points representing subscriber speed
        val mockChart = List(12) { Random.nextDouble(30.0, 95.0).toFloat() }
        
        // Define representative videos for each channel
        val mockVideos = when (channel.id) {
            "coding_world" -> listOf(
                VideoItem("1", "Compose Bento Layout Mastery", "4 hours ago", 12450, 14.2, 850.5, 942, 1),
                VideoItem("2", "Kotlin Coroutines Live Stream Demo", "2 days ago", 24300, 11.2, 1840.0, 1820, 2),
                VideoItem("3", "Unlocking Room DB with KSP Ktx", "5 days ago", 8900, 9.8, 620.0, 410, 3),
                VideoItem("4", "Perfect Material 3 UI Guidelines", "1 week ago", 15400, 12.4, 1120.0, 1024, 1)
            )
            "design_vibe" -> listOf(
                VideoItem("1", "Why Rounded Corners Rule Your App", "10 hours ago", 2100, 9.5, 120.0, 310, 2),
                VideoItem("2", "Bento Grid vs Flex Layouts Design Decisions", "3 days ago", 5400, 12.1, 450.0, 520, 3),
                VideoItem("3", "My Figma Setup for Speed & Simplicity", "6 days ago", 4800, 8.4, 310.2, 390, 1)
            )
            else -> listOf(
                VideoItem("1", "We Reviewed the Ultimate Folding Phone", "1 day ago", 84500, 13.5, 12400.0, 8240, 3),
                VideoItem("2", "Is 2026 the Year of Wearable Tech?", "4 days ago", 56200, 10.3, 7600.0, 4800, 1),
                VideoItem("3", "Top 5 Desktop Upgrades You Actually Need", "1 week ago", 112000, 15.1, 19400.0, 14500, 2)
            )
        }

        val initialLogs = generateInitialLogs(channel)

        _state.update {
            it.copy(
                selectedChannel = channel,
                subscribers = channel.initialSubscribers,
                views = channel.initialViews,
                revenue = channel.initialRevenue,
                ctr = channel.initialCtr,
                watchTime = channel.initialWatchTime,
                chartPoints = mockChart,
                liveActivities = initialLogs,
                videos = mockVideos
            )
        }
    }

    fun setBottomTab(tab: String) {
        _state.update { it.copy(bottomTab = tab) }
    }

    fun toggleSimulation() {
        val isNowRunning = !_state.value.isLiveSimulating
        _state.update { it.copy(isLiveSimulating = isNowRunning) }
        if (isNowRunning) {
            startSimulation()
        } else {
            simulationJob?.cancel()
        }
    }

    private fun startSimulation() {
        simulationJob?.cancel()
        simulationJob = viewModelScope.launch {
            while (isActive) {
                delay(Random.nextLong(1500, 3200)) // live ticks every 1.5 - 3.2s
                
                // Real-time incremental additions
                val subDelta = if (Random.nextFloat() > 0.4f) Random.nextInt(1, 4) else 0
                val viewDelta = Random.nextInt(12, 45).toLong()
                val revDelta = Random.nextDouble(0.12, 1.45)
                val ctrShift = Random.nextDouble(-0.1, 0.15)
                val watchShift = Random.nextDouble(0.4, 2.8)

                val newSubscribers = _state.value.subscribers + subDelta
                val newTodaySubs = _state.value.subIncrementToday + subDelta
                val newViews = _state.value.views + viewDelta
                val newRevenue = _state.value.revenue + revDelta
                val newCtr = (_state.value.ctr + ctrShift).coerceIn(4.0, 22.0)
                val newWatchTime = _state.value.watchTime + watchShift

                // Update chart points to simulate moving trends
                val updatedChart = _state.value.chartPoints.toMutableList()
                if (updatedChart.isNotEmpty()) {
                    updatedChart.removeAt(0)
                    // new point mimics last point with subtle random variance
                    val lastVal = _state.value.chartPoints.last()
                    val nextVal = (lastVal + Random.nextDouble(-12.0, 15.0).toFloat()).coerceIn(20f, 100f)
                    updatedChart.add(nextVal)
                }

                // Add live stream activity occasionally
                val currentLogs = _state.value.liveActivities.toMutableList()
                if (Random.nextFloat() > 0.6f) {
                    val nextLog = generateRandomActivity(subDelta > 0)
                    currentLogs.add(0, nextLog) // insert at start
                    if (currentLogs.size > 25) {
                        currentLogs.removeAt(currentLogs.size - 1)
                    }
                }

                _state.update {
                    it.copy(
                        subscribers = newSubscribers,
                        subIncrementToday = newTodaySubs,
                        views = newViews,
                        revenue = newRevenue,
                        ctr = newCtr,
                        watchTime = newWatchTime,
                        chartPoints = updatedChart,
                        liveActivities = currentLogs
                    )
                }
            }
        }
    }

    private fun generateInitialLogs(channel: ChannelInfo): List<LiveActivity> {
        val names = listOf("muzzamal", "alex_dev", "sarah_c", "johnny99", "k_compose", "emma_designer", "robot_tester", "android_nerd")
        val messages = listOf(
            "Incredible tutorial on beautiful layout systems!",
            "Finally an explanation that makes complete sense.",
            "Loved the bento design concept. Keep it up!",
            "Real-time state flows are perfect.",
            "Greetings from SF! Best Android dev content out there."
        )
        return List(6) { idx ->
            LiveActivity(
                id = "init_$idx",
                timestamp = "${idx + 1}m ago",
                message = when (idx % 3) {
                    0 -> "${names[idx % names.size]} subscribed to ${channel.name}!"
                    1 -> "SuperChat $${Random.nextInt(5, 50)} from ${names[idx % names.size]}: ${messages[idx % messages.size]}"
                    else -> "${names[idx % names.size]} commented: \"${messages[idx % messages.size]}\""
                },
                type = when (idx % 3) {
                    0 -> ActivityType.SUBSCRIBE
                    1 -> ActivityType.SUPERCHAT
                    else -> ActivityType.COMMENT
                },
                highlight = idx % 3 == 1
            )
        }
    }

    private fun generateRandomActivity(hadSubTick: Boolean): LiveActivity {
        val names = listOf(
            "pixel_architect", "kotlin_ninja", "muzzamal_o", "jetpack_guru", "coder_bee", 
            "charlie_ux", "anna_k", "david_compose", "sam_flutter", "sophie_codes"
        )
        val comments = listOf(
            "This Bento layout is buttery smooth!",
            "Is this app using Material 3?",
            "Can you upload the source code to Github?",
            "Realtime updates are working perfectly in my emulator!",
            "I'm learning so much from your streams.",
            "Wow! Best YouTube studio mockup tool out there.",
            "Those live meters are stellar!"
        )

        val type = if (hadSubTick && Random.nextFloat() > 0.3f) {
            ActivityType.SUBSCRIBE
        } else {
            val roll = Random.nextFloat()
            if (roll < 0.25f) ActivityType.SUPERCHAT
            else if (roll < 0.7f) ActivityType.COMMENT
            else ActivityType.LIKE
        }

        val name = names.random()
        val comment = comments.random()
        val superChatAmount = Random.nextInt(2, 100)

        val message = when (type) {
            ActivityType.SUBSCRIBE -> "$name subscribed to the channel!"
            ActivityType.SUPERCHAT -> "SuperChat $$superChatAmount.00 from $name: \"$comment\""
            ActivityType.COMMENT -> "$name: \"$comment\""
            ActivityType.LIKE -> "$name liked the active stream!"
        }

        return LiveActivity(
            id = System.nanoTime().toString(),
            timestamp = "Just now",
            message = message,
            type = type,
            highlight = type == ActivityType.SUPERCHAT
        )
    }

    override fun onCleared() {
        super.onCleared()
        simulationJob?.cancel()
    }
}
