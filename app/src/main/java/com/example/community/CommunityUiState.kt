package com.example.community

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object CommunityUiState {
    private val _open = MutableStateFlow(false)
    val open = _open.asStateFlow()

    fun show() { _open.value = true }
    fun hide() { _open.value = false }
}
