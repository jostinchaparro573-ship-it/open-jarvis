package com.openjarvis.ui

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.ViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.openjarvis.R
import com.openjarvis.agent.AgentCore
import com.openjarvis.graphify.GraphifyRepository
import com.openjarvis.graphify.nodes.TaskNode
import com.openjarvis.ui.overlay.FloatingOverlayWidget
import com.openjarvis.ui.theme.OpenJarvisTheme
import com.openjarvis.voice.VoiceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class OverlayService : LifecycleService() {

    private var windowManager: WindowManager? = null
    private var overlayView: ComposeView? = null

    private lateinit var agentCore: AgentCore
    private lateinit var voiceManager: VoiceManager
    private lateinit var graphifyRepo: GraphifyRepository
    private lateinit var socketServer: com.openjarvis.bridge.SocketServer
    private var recentTasks = emptyList<TaskNode>()
    private var isInitialized = false

    override fun onCreate() {
        super.onCreate()

        agentCore = AgentCore(this)
        voiceManager = VoiceManager(this)
        graphifyRepo = GraphifyRepository(this)
        socketServer = com.openjarvis.bridge.SocketServer(this, agentCore, graphifyRepo)

        startForeground(NOTIFICATION_ID, createNotification())
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                recentTasks = graphifyRepo.getRecentTasks(10)
            } catch (_: Exception) {
                // Dashboard memory remains available even if initial loading fails.
            }
        }

        socketServer.start()
        initializeServices()
    }

    private fun initializeServices() {
        if (isInitialized) return
        isInitialized = true
        // Voice engines initialize in VoiceManager's constructor.
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (!Settings.canDrawOverlays(this)) {
            stopSelf(startId)
            return START_NOT_STICKY
        }
        showOverlay()
        return START_STICKY
    }

    private fun showOverlay() {
        if (overlayView?.isAttachedToWindow == true) return

        val view = ComposeView(this).apply {
            ViewTreeLifecycleOwner.set(this, this@OverlayService)
            setContent {
                OpenJarvisTheme {
                    val agentState = agentCore.state.collectAsState().value
                    val recent = graphifyRepo.getRecentTasksFlow(10)
                        .collectAsState(initial = recentTasks).value
                    val suggestions = graphifyRepo.getSuggestions()
                        .collectAsState(initial = emptyList()).value

                    Box(
                        modifier = Modifier.fillMaxWidth().wrapContentHeight(),
                        contentAlignment = Alignment.TopEnd
                    ) {
                        FloatingOverlayWidget(
                            agentState = agentState,
                            voiceManager = voiceManager,
                            recentTasks = recent,
                            suggestions = suggestions,
                            onCommand = { command -> executeCommand(command) },
                            onCollapse = {},
                            modifier = Modifier
                        )
                    }
                }
            }
        }

        val metrics = resources.displayMetrics
        val params = WindowManager.LayoutParams(
            (metrics.widthPixels * 0.92f).toInt(),
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = (16 * metrics.density).toInt()
            y = (150 * metrics.density).toInt()
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        }

        try {
            windowManager?.addView(view, params)
            overlayView = view
        } catch (_: Exception) {
            overlayView = null
            stopSelf()
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }

    override fun onDestroy() {
        overlayView?.let { view ->
            try {
                if (view.isAttachedToWindow) windowManager?.removeView(view)
            } catch (_: Exception) {
                // The window may already have been detached by the system.
            }
        }
        overlayView = null
        socketServer.stop()
        voiceManager.release()
        super.onDestroy()
    }

    private fun createNotification(): Notification {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.notification_channel_description)
        }

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    fun getAgentCore() = agentCore
    fun getVoiceManager() = voiceManager
    fun getRecentTasks() = recentTasks

    fun executeCommand(command: String) {
        agentCore.executeTask(command)
        lifecycleScope.launch(Dispatchers.IO) {
            recentTasks = graphifyRepo.getRecentTasks(10)
        }
    }

    companion object {
        private const val CHANNEL_ID = "jarvis_overlay_channel"
        private const val NOTIFICATION_ID = 1

        fun start(context: Context) {
            val intent = Intent(context, OverlayService::class.java)
            context.startForegroundService(intent)
        }
    }
}
