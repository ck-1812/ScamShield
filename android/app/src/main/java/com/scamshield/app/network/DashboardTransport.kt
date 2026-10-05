package com.scamshield.app.network

import com.scamshield.core.domain.model.RiskScore
import com.scamshield.core.network.dto.DashboardPayload

interface DashboardTransport {
    fun start(port: Int = 8765)
    fun stop()
    fun broadcast(payload: DashboardPayload)
    fun isConnected(): Boolean
    fun getLocalIpAddress(): String
}
