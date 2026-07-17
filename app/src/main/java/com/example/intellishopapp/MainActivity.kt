package com.example.intellishopapp

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.intellishopapp.network.RetrofitClient
import com.example.intellishopapp.utilities.SessionManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.textview.MaterialTextView

/**
 * Temporary landing screen shown after a successful login. The full bottom-nav
 * shell (Home / Coupons / Profile) replaces this later.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var main_LBL_welcome: MaterialTextView
    private lateinit var main_BTN_signOut: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_LAY_root)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
        findViews()
        initViews()
    }

    private fun findViews() {
        main_LBL_welcome = findViewById(R.id.main_LBL_welcome)
        main_BTN_signOut = findViewById(R.id.main_BTN_signOut)
    }

    private fun initViews() {
        val session = SessionManager.getInstance().get()
        val name = session?.username ?: session?.email ?: ""
        main_LBL_welcome.text = getString(R.string.main_welcome, name)
        main_BTN_signOut.setOnClickListener { signOut() }
    }

    private fun signOut() {
        SessionManager.getInstance().clear()
        RetrofitClient.getInstance().clearCookies()
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
