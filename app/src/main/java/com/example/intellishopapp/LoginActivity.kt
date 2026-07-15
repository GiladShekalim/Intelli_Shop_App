package com.example.intellishopapp

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.textview.MaterialTextView

/**
 * Launcher Activity. Sets up edge-to-edge display and window insets, then
 * wires up views through findViews() / initViews().
 */
class LoginActivity : AppCompatActivity() {

    private lateinit var login_LBL_title: MaterialTextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.login_LAY_root)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        findViews()
        initViews()
    }

    private fun findViews() {
        login_LBL_title = findViewById(R.id.login_LBL_title)
    }

    private fun initViews() {
        login_LBL_title.text = getString(R.string.login_title)
    }
}
