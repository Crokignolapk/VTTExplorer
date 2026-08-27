package com.vttexplorer.app.presentation.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class SettingsState(
    val voiceEnabled: Boolean = true,
    val autoRecalc: Boolean = true,
    val darkMap: Boolean = true,
    val avoidMainRoads: Boolean = true,
    val avoidPaved: Boolean = true
)

class SettingsViewModel(context: Context) : ViewModel() {
    private val prefs = context.getSharedPreferences("vtt_settings", Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(
        SettingsState(
            voiceEnabled = prefs.getBoolean("voice", true),
            autoRecalc = prefs.getBoolean("auto_recalc", true),
            darkMap = prefs.getBoolean("dark_map", true),
            avoidMainRoads = prefs.getBoolean("avoid_main", true),
            avoidPaved = prefs.getBoolean("avoid_paved", true)
        )
    )
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    fun setVoice(v: Boolean) {
        prefs.edit().putBoolean("voice", v).apply()
        _state.update { it.copy(voiceEnabled = v) }
    }

    fun setAutoRecalc(v: Boolean) {
        prefs.edit().putBoolean("auto_recalc", v).apply()
        _state.update { it.copy(autoRecalc = v) }
    }

    fun setDarkMap(v: Boolean) {
        prefs.edit().putBoolean("dark_map", v).apply()
        _state.update { it.copy(darkMap = v) }
    }

    fun setAvoidMain(v: Boolean) {
        prefs.edit().putBoolean("avoid_main", v).apply()
        _state.update { it.copy(avoidMainRoads = v) }
    }

    fun setAvoidPaved(v: Boolean) {
        prefs.edit().putBoolean("avoid_paved", v).apply()
        _state.update { it.copy(avoidPaved = v) }
    }
}
