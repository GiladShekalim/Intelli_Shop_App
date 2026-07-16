package com.example.intellishopapp.utilities

class Constants {
    object Api {
        // Base address of the server. Must end with a trailing slash.
        // Point this at the host machine's LAN IP; keep network_security_config.xml in sync.
        // Emulator: 10.0.2.2 is the host machine's loopback (Django on 0.0.0.0:8000).
        // For a physical phone on the same Wi-Fi, use the Mac's LAN IP instead (e.g. 192.168.68.109).
        const val BASE_URL = "http://10.0.2.2:8000/"
    }

    object Prefs {
        const val FILE = "intellishop_prefs"
        const val COOKIES = "cookies"
        const val SESSION = "session"
    }
}
