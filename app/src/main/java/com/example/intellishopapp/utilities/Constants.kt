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
        const val NIGHT_MODE = "night_mode"
        const val NOTIFICATIONS = "notifications"
    }

    // Interests — exact backend CATEGORIES values (order and case matter).
    object Categories {
        val ALL = listOf(
            "Consumerism", "Travel and Vacation", "Culture and Leisure", "Cars", "Insurance",
            "Finance and Banking", "lifestyle", "home", "electronics", "books"
        )
    }

    // Memberships — the clubs a coupon can belong to (matches coupon.club_name, lower-case).
    // Stored as the key ("hot"); shown with its label ("HOT"). Selecting memberships hard-
    // filters discovery surfaces to only those clubs; an empty selection means "no filter".
    object Memberships {
        val ALL = listOf(
            "hot" to "HOT",
            "adif" to "Adif"
        )
        val KEYS = ALL.map { it.first }
        fun label(key: String): String = ALL.firstOrNull { it.first == key }?.second ?: key
    }

    // Statuses — exact backend CONSUMER_STATUS values.
    object ConsumerStatus {
        val ALL = listOf(
            "Young", "Senior", "Homeowner", "Traveler", "Tech", "Pets", "Fitness", "Student",
            "Remote", "Family", "Parent", "Military/Veteran", "Digital Nomad",
            "First-time Buyer", "Retiree", "Single", "Renter"
        )
    }

    // Percentage buckets — keys MUST match backend FILTER_CONFIG.PERCENTAGE_BUCKETS.
    object PercentageBuckets {
        val ALL = listOf(
            "up_to_20" to "Up to 20%",
            "between_20_30" to "20-30%",
            "between_30_40" to "30-40%",
            "between_40_50" to "40-50%",
            "between_50_60" to "50-60%",
            "more_than_60" to "60%+"
        )
    }
}
