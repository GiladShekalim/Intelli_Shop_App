package com.example.intellishopapp.utilities

class Constants {
    object Api {
        // Base address of the server. Must end with a trailing slash.
        // Point this at the host machine's LAN IP; keep network_security_config.xml in sync.
        const val BASE_URL = "http://192.168.1.100:8000/"
    }

    object Prefs {
        const val FILE = "intellishop_prefs"
        const val COOKIES = "cookies"
    }
}
