package com.russhwolf.escalators

sealed interface UiState {
    data object Init : UiState
    data class Populated(val escalatorResponse: EscalatorResponse) : UiState {
        val isWorking: Boolean =
            escalatorResponse is EscalatorResponse.Success && escalatorResponse.escalators.all { it.isWorking }
    }
}

