package com.example.todoapp.mascot

internal enum class MascotState {
    IDLE,
    WALKING,
    DRAGGING,
    SPEAKING,
    SCREEN_OFF,
}

internal class MascotStateController(
    private val onStateChanged: (MascotState) -> Unit = {},
) {
    var current: MascotState = MascotState.IDLE
        private set

    fun transitionTo(state: MascotState) {
        if (current == state) return
        current = state
        onStateChanged(state)
    }
}
