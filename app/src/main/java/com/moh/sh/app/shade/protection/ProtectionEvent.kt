package com.moh.sh.app.shade.protection

data class ProtectionEvent(
    val id: Long = System.currentTimeMillis(),
    val timestamp: Long = System.currentTimeMillis(),
    val confidencePercent: Int,
    val packageName: String? = null,
    val actionTaken: String = "Blocked & Redirected Home"
)
