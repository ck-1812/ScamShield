package com.scamshield.core.demo

import com.scamshield.core.domain.model.ScamType

data class DemoTurn(
    val step: Int,
    val speaker: String,
    val text: String,
    val expectedSignals: List<ScamType>,
    val delayMs: Long = 2500L
)

data class DemoScenario(
    val scenarioId: String,
    val title: String,
    val description: String,
    val category: String,
    val turns: List<DemoTurn>
)
