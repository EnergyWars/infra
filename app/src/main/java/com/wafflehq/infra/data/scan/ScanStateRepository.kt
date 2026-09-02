package com.wafflehq.infra.data.scan

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.scanDataStore by preferencesDataStore(name = "scan_state")

@Singleton
class ScanStateRepository @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val currentIndexKey = intPreferencesKey("current_index")
    private val currentExtendedIndexKey = intPreferencesKey("current_extended_index")
    private val extendedModeKey = booleanPreferencesKey("extended_mode")

    val currentIndex: Flow<Int> = context.scanDataStore.data.map { prefs ->
        prefs[currentIndexKey] ?: 0
    }

    val currentExtendedIndex: Flow<Int> = context.scanDataStore.data.map { prefs ->
        prefs[currentExtendedIndexKey] ?: 0
    }

    val isExtendedMode: Flow<Boolean> = context.scanDataStore.data.map { prefs ->
        prefs[extendedModeKey] ?: false
    }

    suspend fun setCurrentIndex(value: Int) {
        context.scanDataStore.edit { prefs ->
            prefs[currentIndexKey] = value
        }
    }

    suspend fun setCurrentExtendedIndex(value: Int) {
        context.scanDataStore.edit { prefs ->
            prefs[currentExtendedIndexKey] = value
        }
    }

    suspend fun setExtendedMode(value: Boolean) {
        context.scanDataStore.edit { prefs ->
            prefs[extendedModeKey] = value
        }
    }
}
