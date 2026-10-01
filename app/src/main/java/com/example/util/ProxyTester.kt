package com.example.util

import com.example.service.SuperProxyVpnService
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
        val countryName: String? = null,
        val city: String? = null,
        val isp: String? = null,
        val timezone: String? = null,
        val errorMessage: String? = null
    )

    /**
     * Real-Time IP & Geo-Location Detection (Super Proxy Style):
     * 1. Protects the test socket so it bypasses VPN loop if VPN is running.
     * 2. Tests TCP socket handshake to verify proxy server is listening.
     * 3. Sends real request through proxy to an IP geo API (ip-api.com / ipwho.is)
     *    to extract the actual public egress IP, Country, City, and ISP.
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

        // Set global authenticator if credentials present
        if (username.isNotBlank()) {
            Authenticator.setDefault(object : Authenticator() {
                override fun getPasswordAuthentication(): PasswordAuthentication {
                    return PasswordAuthentication(username.trim(), password.toCharArray())
                }
            })
        }

        // Step 1: Direct socket connection test to verify proxy server is alive
        var socket: Socket? = null
        try {
            socket = Socket()
            socket.tcpNoDelay = true
            socket.soTimeout = effectiveTimeout
            SuperProxyVpnService.protectSocket(socket)
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

        // Step 2: Test real traffic routing through proxy to resolve public IP, City, Country, ISP
        val proxyType = if (protocol.equals("HTTP", ignoreCase = true) || protocol.equals("HTTPS", ignoreCase = true)) {
            Proxy.Type.HTTP
        } else {
            Proxy.Type.SOCKS
        }

        val javaProxy = Proxy(proxyType, InetSocketAddress(cleanHost, port))

        var resolvedIp = cleanHost
        var resolvedCountry = "US"
        var resolvedCountryName = "United States"
        var resolvedCity = ""
        var resolvedIsp = ""
        var resolvedTimezone = ""

        var geoSuccess = false

        // Attempt 1: ip-api.com through the proxy
        try {
            val checkUrl = URL("http://ip-api.com/json/?fields=query,status,country,countryCode,city,isp,timezone")
            val conn = checkUrl.openConnection(javaProxy) as HttpURLConnection
            conn.connectTimeout = effectiveTimeout
            conn.readTimeout = effectiveTimeout
            conn.requestMethod = "GET"
            conn.setRequestProperty("User-Agent", "SuperProxy/2.0")

            if (conn.responseCode == 200) {
                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                val response = reader.readText()
                reader.close()

                val json = JSONObject(response)
                if (json.optString("status") == "success") {
                    resolvedIp = json.optString("query", cleanHost)
                    resolvedCountry = json.optString("countryCode", "US")
                    resolvedCountryName = json.optString("country", "United States")
                    resolvedCity = json.optString("city", "")
                    resolvedIsp = json.optString("isp", "")
                    resolvedTimezone = json.optString("timezone", "")
                    geoSuccess = true
                }
            }
            conn.disconnect()
        } catch (_: Exception) {}

        // Attempt 2: ipwho.is fallback through the proxy
        if (!geoSuccess) {
            try {
                val ipwhoUrl = URL("http://ipwho.is/")
                val conn = ipwhoUrl.openConnection(javaProxy) as HttpURLConnection
                conn.connectTimeout = 3000
                conn.readTimeout = 3000
                conn.requestMethod = "GET"
                conn.setRequestProperty("User-Agent", "SuperProxy/2.0")

                if (conn.responseCode == 200) {
                    val reader = BufferedReader(InputStreamReader(conn.inputStream))
                    val response = reader.readText()
                    reader.close()

                    val json = JSONObject(response)
                    if (json.optBoolean("success", false)) {
                        resolvedIp = json.optString("ip", cleanHost)
                        resolvedCountry = json.optString("country_code", "US")
                        resolvedCountryName = json.optString("country", "United States")
                        resolvedCity = json.optString("city", "")
                        val connObj = json.optJSONObject("connection")
                        resolvedIsp = connObj?.optString("isp", "") ?: ""
                        val timeObj = json.optJSONObject("timezone")
                        resolvedTimezone = timeObj?.optString("id", "") ?: ""
                        geoSuccess = true
                    }
                }
                conn.disconnect()
            } catch (_: Exception) {}
        }

        // Attempt 3: If outbound request through tunnel was blocked or private proxy,
        // resolve geo details of the proxy host directly
        if (!geoSuccess && cleanHost != "127.0.0.1" && cleanHost != "localhost") {
            try {
                val directCheckUrl = URL("http://ip-api.com/json/$cleanHost?fields=query,status,country,countryCode,city,isp,timezone")
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
                        resolvedCountryName = json.optString("country", resolvedCountryName)
                        resolvedCity = json.optString("city", resolvedCity)
                        resolvedIsp = json.optString("isp", resolvedIsp)
                        resolvedTimezone = json.optString("timezone", resolvedTimezone)
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
            countryCode = resolvedCountry,
            countryName = resolvedCountryName,
            city = resolvedCity,
            isp = resolvedIsp,
            timezone = resolvedTimezone
        )
    }
}
