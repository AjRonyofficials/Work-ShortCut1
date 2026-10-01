package com.example.util

/**
 * Intelligent Proxy Formatter & Parser:
 * Supports one-click parsing of standard proxy formats:
 * 1. host:port:username:password
 * 2. username:password@host:port
 * 3. protocol://username:password@host:port (e.g. socks5://user:pass@host:port)
 * 4. protocol://host:port
 * 5. host:port
 * 6. host:port:user:pass:country
 * 7. Comma, tab or semicolon-separated formats
 */
data class ParsedProxy(
    val protocol: String = "SOCKS5",
    val host: String = "",
    val port: Int = 1080,
    val username: String = "",
    val password: String = "",
    val countryCode: String = "US",
    val suggestedName: String = "Imported Proxy"
)

object ProxyFormatHelper {

    fun parse(rawInput: String): ParsedProxy? {
        val input = rawInput.trim()
        if (input.isEmpty()) return null

        var working = input
        var protocol = "SOCKS5"
        var countryCode = "US"

        // Strip known protocol prefixes
        if (working.startsWith("socks5://", ignoreCase = true)) {
            protocol = "SOCKS5"
            working = working.substring("socks5://".length)
        } else if (working.startsWith("socks4://", ignoreCase = true)) {
            protocol = "SOCKS5"
            working = working.substring("socks4://".length)
        } else if (working.startsWith("http://", ignoreCase = true)) {
            protocol = "HTTP"
            working = working.substring("http://".length)
        } else if (working.startsWith("https://", ignoreCase = true)) {
            protocol = "HTTP"
            working = working.substring("https://".length)
        }

        // Check for trailing hash tags or labels (e.g. #US or #DataImpulse)
        if (working.contains("#")) {
            val hashParts = working.split("#", limit = 2)
            working = hashParts[0].trim()
            val tag = hashParts.getOrNull(1)?.trim()?.uppercase() ?: ""
            if (tag.length == 2 && tag.all { it.isLetter() }) {
                countryCode = tag
            }
        }

        var host = ""
        var port = 1080
        var username = ""
        var password = ""

        // Format A: username:password@host:port
        if (working.contains("@")) {
            val atParts = working.split("@", limit = 2)
            val authPart = atParts[0].trim()
            val hostPart = atParts[1].trim()

            val authSub = authPart.split(":", limit = 2)
            username = authSub.getOrNull(0)?.trim() ?: ""
            password = authSub.getOrNull(1)?.trim() ?: ""

            val hostSub = hostPart.split(":")
            host = hostSub.getOrNull(0)?.trim() ?: ""
            port = hostSub.getOrNull(1)?.trim()?.toIntOrNull() ?: 1080
        } else {
            // Delimiter can be colon (:), comma (,), semicolon (;), or tab (\t)
            val delimiter = when {
                working.contains(":") -> ":"
                working.contains(",") -> ","
                working.contains(";") -> ";"
                working.contains("\t") -> "\t"
                else -> ":"
            }
            val parts = working.split(delimiter).map { it.trim() }.filter { it.isNotEmpty() }

            if (parts.size >= 4) {
                // Determine whether it is host:port:username:password OR username:password:host:port
                val portAt1 = parts[1].toIntOrNull()
                val portAt3 = parts[3].toIntOrNull()

                if (portAt1 != null && portAt1 in 1..65535) {
                    // Standard host:port:username:password
                    host = parts[0]
                    port = portAt1
                    username = parts[2]
                    password = parts[3]
                    if (parts.size >= 5 && parts[4].length == 2 && parts[4].all { it.isLetter() }) {
                        countryCode = parts[4].uppercase()
                    }
                } else if (portAt3 != null && portAt3 in 1..65535) {
                    // username:password:host:port
                    username = parts[0]
                    password = parts[1]
                    host = parts[2]
                    port = portAt3
                } else {
                    host = parts[0]
                    port = portAt1 ?: 1080
                    username = parts[2]
                    password = parts[3]
                }
            } else if (parts.size == 3) {
                host = parts[0]
                port = parts[1].toIntOrNull() ?: 1080
                username = parts[2]
            } else if (parts.size == 2) {
                host = parts[0]
                port = parts[1].toIntOrNull() ?: 1080
            } else if (parts.size == 1) {
                host = parts[0]
            }
        }

        if (host.isBlank()) return null

        val shortHost = if (host.length > 14) host.take(11) + ".." else host
        val suggestedName = "$protocol $shortHost:$port"

        return ParsedProxy(
            protocol = protocol,
            host = host,
            port = port,
            username = username,
            password = password,
            countryCode = countryCode,
            suggestedName = suggestedName
        )
    }
}
