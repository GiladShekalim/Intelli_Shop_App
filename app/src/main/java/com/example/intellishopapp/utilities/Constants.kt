package com.example.intellishopapp.utilities

class Constants {
    object Api {
        // Base address of the server. Must end with a trailing slash.
        // Point this at the host machine's LAN IP; keep network_security_config.xml in sync.
        // Emulator: 10.0.2.2 is the host machine's loopback (Django on 0.0.0.0:8000).
        // For a physical phone on the same Wi-Fi, use the Mac's LAN IP instead (e.g. 192.168.68.109).
        const val BASE_URL = "http://10.0.2.2:8000/"

        // Google OAuth 2.0 Web client ID (from Google Cloud Console). Must match the
        // backend's GOOGLE_CLIENT_ID. Fill this in before using "Continue with Google".
        const val GOOGLE_WEB_CLIENT_ID = "516608219252-t8p0ha4t2ubvth9sslvsb4otu9hht21e.apps.googleusercontent.com"
    }

    object Prefs {
        const val FILE = "intellishop_prefs"
        const val COOKIES = "cookies"
        const val SESSION = "session"
    }

    // Interests — exact backend CATEGORIES values (order and case matter).
    object Categories {
        val ALL = listOf(
            "Consumerism", "Travel and Vacation", "Culture and Leisure", "Cars", "Insurance",
            "Finance and Banking", "lifestyle", "home", "electronics", "books"
        )
    }

    // Statuses — exact backend CONSUMER_STATUS values.
    object ConsumerStatus {
        val ALL = listOf(
            "Young", "Senior", "Homeowner", "Traveler", "Tech", "Pets", "Fitness", "Student",
            "Remote", "Family", "Parent", "Military/Veteran", "Digital Nomad",
            "First-time Buyer", "Retiree", "Single", "Renter"
        )
    }
}
