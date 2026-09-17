package com.pushplatform.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.pushplatform.sdk.PushPlatform
import com.pushplatform.sdk.utils.Logger

class MainActivity : AppCompatActivity() {

    private lateinit var tvInstallationId: TextView
    private lateinit var tvPermissionStatus: TextView
    private lateinit var tvLoginStatus: TextView
    private lateinit var etUserId: EditText
    private lateinit var btnRequestPermission: Button
    private lateinit var btnLogin: Button
    private lateinit var btnLogout: Button
    private lateinit var btnRefresh: Button

    private val PERMISSION_REQUEST_CODE = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvInstallationId = findViewById(R.id.tv_installation_id)
        tvPermissionStatus = findViewById(R.id.tv_permission_status)
        tvLoginStatus = findViewById(R.id.tv_login_status)
        etUserId = findViewById(R.id.et_user_id)
        btnRequestPermission = findViewById(R.id.btn_request_permission)
        btnLogin = findViewById(R.id.btn_login)
        btnLogout = findViewById(R.id.btn_logout)
        btnRefresh = findViewById(R.id.btn_refresh)

        setupClickListeners()
        refreshStatus()
    }

    private fun setupClickListeners() {
        btnRequestPermission.setOnClickListener {
            requestNotificationPermission()
        }

        btnLogin.setOnClickListener {
            val userId = etUserId.text.toString().trim()
            if (userId.isNotEmpty()) {
                login(userId)
            } else {
                tvLoginStatus.text = "Error: User ID cannot be empty"
            }
        }

        btnLogout.setOnClickListener {
            logout()
        }

        btnRefresh.setOnClickListener {
            refreshStatus()
        }
    }

    private fun refreshStatus() {
        val installationId = PushPlatform.getInstance().getInstallationId()
        tvInstallationId.text = "Installation ID: ${installationId ?: "Not initialized"}"

        val hasPermission = PushPlatform.getInstance().hasNotificationPermission()
        tvPermissionStatus.text = "Permission: ${if (hasPermission) "Granted" else "Denied"}"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            btnRequestPermission.visibility = View.VISIBLE
        } else {
            btnRequestPermission.visibility = View.GONE
            tvPermissionStatus.text = "Permission: Granted (< Android 13)"
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    PERMISSION_REQUEST_CODE
                )
            } else {
                tvPermissionStatus.text = "Permission: Already granted"
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSION_REQUEST_CODE) {
            PushPlatform.getInstance().onRequestPermissionsResult(requestCode, grantResults)
            refreshStatus()
        }
    }

    private fun login(userId: String) {
        tvLoginStatus.text = "Logging in..."
        btnLogin.isEnabled = false

        PushPlatform.getInstance().login(userId) { result ->
            runOnUiThread {
                btnLogin.isEnabled = true
                when (result) {
                    is com.pushplatform.sdk.core.UserManager.Result.Success -> {
                        tvLoginStatus.text = "Logged in as: $userId"
                        Logger.info("Login successful: $userId")
                    }
                    is com.pushplatform.sdk.core.UserManager.Result.Failure -> {
                        tvLoginStatus.text = "Login failed: ${result.error.message}"
                        Logger.error("Login failed: ${result.error.message}")
                    }
                }
            }
        }
    }

    private fun logout() {
        tvLoginStatus.text = "Logging out..."
        btnLogout.isEnabled = false

        PushPlatform.getInstance().logout { result ->
            runOnUiThread {
                btnLogout.isEnabled = true
                when (result) {
                    is com.pushplatform.sdk.core.UserManager.Result.Success -> {
                        tvLoginStatus.text = "Logged out"
                        etUserId.text.clear()
                        Logger.info("Logout successful")
                    }
                    is com.pushplatform.sdk.core.UserManager.Result.Failure -> {
                        tvLoginStatus.text = "Logout failed: ${result.error.message}"
                        Logger.error("Logout failed: ${result.error.message}")
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
    }
}
