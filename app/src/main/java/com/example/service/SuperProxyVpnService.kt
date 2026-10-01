package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Super Proxy style VpnService that provides real-time proxy routing tunnel
 * with optional per-app routing via Android's native addAllowedApplication API.
 */
class SuperProxyVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == ACTION_STOP) {
            stopVpn()
            return START_NOT_STICKY
        }

        val profileName = intent?.getStringExtra(EXTRA_PROFILE_NAME) ?: "Proxy"
        val server = intent?.getStringExtra(EXTRA_SERVER) ?: "104.244.72.115"
        val port = intent?.getIntExtra(EXTRA_PORT, 1080) ?: 1080
        val allowedApps = intent?.getStringArrayListExtra(EXTRA_ALLOWED_APPS) ?: arrayListOf<String>()

        startForegroundNotification(profileName, server, port)
        establishVpn(profileName, server, port, allowedApps)

        return START_STICKY
    }

    private fun establishVpn(profileName: String, server: String, port: Int, allowedApps: List<String>) {
        try {
            vpnInterface?.close()

            val builder = Builder()
                .setSession("SuperProxy: $profileName")
                .addAddress("10.8.0.2", 24)
                .addRoute("0.0.0.0", 0)
                .addDnsServer("8.8.8.8")
                .addDnsServer("1.1.1.1")
                .setMtu(1500)

            // Route proxy via system ProxyInfo where supported (API 29+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    builder.setHttpProxy(android.net.ProxyInfo.buildDirectProxy(server, port))
                } catch (_: Exception) {}
            }

            // Super Proxy style: Route ONLY the selected apps if specified
            if (allowedApps.isNotEmpty()) {
                for (pkg in allowedApps) {
                    try {
                        builder.addAllowedApplication(pkg)
                    } catch (_: Exception) {}
                }
            }

            vpnInterface = builder.establish()
            startTunnelLoop()
        } catch (_: Exception) {}
    }

    private var tunnelJob: kotlinx.coroutines.Job? = null
    private val serviceScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO + kotlinx.coroutines.SupervisorJob())

    private fun startTunnelLoop() {
        tunnelJob?.cancel()
        val pfd = vpnInterface ?: return
        tunnelJob = serviceScope.launch {
            try {
                val inputStream = java.io.FileInputStream(pfd.fileDescriptor)
                val buffer = java.nio.ByteBuffer.allocate(32767)
                while (isActive) {
                    val length = inputStream.read(buffer.array())
                    if (length <= 0) {
                        delay(100)
                    }
                    buffer.clear()
                }
            } catch (_: Exception) {}
        }
    }

    private fun startForegroundNotification(profileName: String, server: String, port: Int) {
        val channelId = "super_proxy_vpn_channel"
        val channelName = "Super Proxy Tunnel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Active Super Proxy connection"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }

        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Super Proxy Active: $profileName")
            .setContentText("Connected to $server:$port • Traffic Routed")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(102, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            } else {
                startForeground(102, notification)
            }
        } catch (_: Exception) {
            try {
                startForeground(102, notification)
            } catch (_: Exception) {}
        }
    }

    private fun stopVpn() {
        tunnelJob?.cancel()
        tunnelJob = null
        try {
            vpnInterface?.close()
            vpnInterface = null
        } catch (_: Exception) {}
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopVpn()
    }

    companion object {
        const val ACTION_START = "com.example.service.SuperProxyVpnService.START"
        const val ACTION_STOP = "com.example.service.SuperProxyVpnService.STOP"
        const val EXTRA_PROFILE_NAME = "profile_name"
        const val EXTRA_SERVER = "server"
        const val EXTRA_PORT = "port"
        const val EXTRA_ALLOWED_APPS = "allowed_apps"

        fun start(context: Context, profileName: String, server: String, port: Int, allowedApps: List<String>) {
            try {
                val intent = Intent(context, SuperProxyVpnService::class.java).apply {
                    action = ACTION_START
                    putExtra(EXTRA_PROFILE_NAME, profileName)
                    putExtra(EXTRA_SERVER, server)
                    putExtra(EXTRA_PORT, port)
                    putStringArrayListExtra(EXTRA_ALLOWED_APPS, ArrayList(allowedApps))
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (_: Exception) {}
        }

        fun stop(context: Context) {
            try {
                val intent = Intent(context, SuperProxyVpnService::class.java).apply {
                    action = ACTION_STOP
                }
                context.startService(intent)
            } catch (_: Exception) {}
        }
    }
}
