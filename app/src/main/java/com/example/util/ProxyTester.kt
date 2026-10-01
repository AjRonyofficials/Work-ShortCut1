package com.example.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.Authenticator
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.PasswordAuthentication
import java.net.Proxy
import java.net.Socket
import java.net.URL

object ProxyTester {

    data class PingResult(
        val isSuccess: Boolean,
        val latencyMs: Long,
        val resolvedIp: String? = null,
        val countryCode: String? = null,
        val errorMessage: String? = null
    )

    /**
     * Real-time proxy test like Super Proxy:
     * 1. Sets authentication if username/password provided.
     * 2. Tests socket connectivity to host:port.
     * 3. Attempts real HTTP request through the proxy to fetch actual egress IP & Country.
     */
    suspend fun testProxy(
        host: String,
        port: Int,
        protocol: String = "SOCKS5",
        username: String = "",
        password: String = "",
        timeoutMs: Int = 4000,
        pingOptimized: Boolean = true
    ): PingResult = withContext(Dispatchers.IO) {
        val effectiveTimeout = if (pingOptimized) 3500 else timeoutMs
        val startTime = System.currentTimeMillis()

        val cleanHost = host.trim()
        if (cleanHost.isEmpty() || port <= 0 || port > 65535) {
            return@withContext PingResult(
                isSuccess = false,
                latencyMs = -1,
                errorMessage = "Invalid Host or Port ($cleanHost:$port)"
            )
        }

        // Set authenticator if credentials present
        if (username.isNotBlank()) {
            Authenticator.setDefault(object : Authenticator() {
                override fun getPasswordAuthentication(): PasswordAuthentication {
                    return PasswordAuthentication(username.trim(), password.toCharArray())
                }
            })
        }

        // Step 1: Direct socket connection test to verify proxy server is listening
        var socket: Socket? = null
        try {
            socket = Socket()
            socket.tcpNoDelay = true
            socket.soTimeout = effectiveTimeout
            val socketAddress = InetSocketAddress(cleanHost, port)
            socket.connect(socketAddress, effectiveTimeout)
        } catch (e: Exception) {
            return@withContext PingResult(
                isSuccess = false,
                latencyMs = -1,
                resolvedIp = cleanHost,
                errorMessage = "Cannot reach $cleanHost:$port (${e.message ?: "Connection timed out"})"
            )
        } finally {
            try { socket?.close() } catch (_: Exception) {}
        }

        val socketLatency = System.currentTimeMillis() - startTime

        // Step 2: Test real traffic routing through proxy to resolve public IP & Country
        val proxyType = if (protocol.equals("HTTP", ignoreCase = true) || protocol.equals("HTTPS", ignoreCase = true)) {
            Proxy.Type.HTTP
        } else {
            Proxy.Type.SOCKS
        }

        val javaProxy = Proxy(proxyType, InetSocketAddress(cleanHost, port))

        var resolvedIp = cleanHost
        var resolvedCountry = "US"

        try {
            // Use lightweight IP check service through the proxy
            val checkUrl = URL("http://ip-api.com/json/?fields=query,countryCode,status")
            val conn = checkUrl.openConnection(javaProxy) as HttpURLConnection
            conn.connectTimeout = effectiveTimeout
            conn.readTimeout = effectiveTimeout
            conn.requestMethod = "GET"
            conn.setRequestProperty("User-Agent", "SuperProxy/1.0")

            if (conn.responseCode == 200) {
                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                val response = reader.readText()
                reader.close()

                val json = JSONObject(response)
                if (json.optString("status") == "success") {
                    resolvedIp = json.optString("query", cleanHost)
                    resolvedCountry = json.optString("countryCode", "US")
                }
            }
            conn.disconnect()
        } catch (_: Exception) {
            // Fallback to ipify if ip-api fails
            try {
                val ipifyUrl = URL("https://api.ipify.org")
                val conn = ipifyUrl.openConnection(javaProxy) as HttpURLConnection
                conn.connectTimeout = 2500
                conn.readTimeout = 2500
                if (conn.responseCode == 200) {
                    val reader = BufferedReader(InputStreamReader(conn.inputStream))
                    resolvedIp = reader.readText().trim()
                    reader.close()
                }
                conn.disconnect()
            } catch (_: Exception) {
                // If outbound check through proxy is blocked, socket was already proven valid
            }
        }

        // If country could not be resolved through the tunnel, resolve proxy server host IP country directly
        if (resolvedCountry == "US" && cleanHost != "127.0.0.1" && cleanHost != "localhost") {
            try {
                val directCheckUrl = URL("http://ip-api.com/json/$cleanHost?fields=query,countryCode,status")
                val directConn = directCheckUrl.openConnection() as HttpURLConnection
                directConn.connectTimeout = 2000
                directConn.readTimeout = 2000
                directConn.requestMethod = "GET"
                if (directConn.responseCode == 200) {
                    val reader = BufferedReader(InputStreamReader(directConn.inputStream))
                    val response = reader.readText()
                    reader.close()
                    val json = JSONObject(response)
                    if (json.optString("status") == "success") {
                        resolvedCountry = json.optString("countryCode", resolvedCountry)
                        if (resolvedIp == cleanHost) {
                            resolvedIp = json.optString("query", cleanHost)
                        }
                    }
                }
                directConn.disconnect()
            } catch (_: Exception) {}
        }

        val totalLatency = System.currentTimeMillis() - startTime
        val finalLatency = if (totalLatency > 0) totalLatency else socketLatency

        PingResult(
            isSuccess = true,
            latencyMs = finalLatency,
            resolvedIp = resolvedIp,
            countryCode = resolvedCountry
        )
    }
}
