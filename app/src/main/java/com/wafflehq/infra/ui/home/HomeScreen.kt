package com.wafflehq.infra.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wafflehq.infra.R
import com.wafflehq.infra.ir.NecCodec
import com.wafflehq.infra.ui.theme.AppRadius
import com.wafflehq.infra.ui.theme.AppSpacing
import com.wafflehq.infra.ui.theme.MonoNumeralStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenSettings: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = stringResource(R.string.cd_open_settings)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(AppSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.lg)
        ) {
            if (!uiState.hasIrEmitter) {
                NoticeCard(
                    title = stringResource(R.string.scan_no_emitter_title),
                    body = stringResource(R.string.scan_no_emitter_body),
                    container = MaterialTheme.colorScheme.errorContainer,
                    content = MaterialTheme.colorScheme.onErrorContainer,
                )
            }

            CodeDisplayCard(currentIndex = uiState.currentIndex)

            Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                LinearProgressIndicator(
                    progress = { uiState.currentIndex / NecCodec.MAX_INDEX.toFloat() },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = stringResource(
                        R.string.scan_position,
                        uiState.currentIndex,
                        NecCodec.MAX_INDEX
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (uiState.isFinished) {
                NoticeCard(
                    title = stringResource(R.string.scan_finished_title),
                    body = stringResource(R.string.scan_finished_body),
                    container = MaterialTheme.colorScheme.tertiaryContainer,
                    content = MaterialTheme.colorScheme.onTertiaryContainer,
                )
            }

            Button(
                onClick = viewModel::onPlayPauseClicked,
                enabled = uiState.hasIrEmitter,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector = if (uiState.isRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = null,
                )
                Text(
                    text = stringResource(if (uiState.isRunning) R.string.scan_pause else R.string.scan_play),
                    modifier = Modifier.padding(start = AppSpacing.sm),
                )
            }

            StartValueRow(
                text = uiState.startValueText,
                enabled = !uiState.isRunning,
                onTextChanged = viewModel::onStartValueTextChanged,
                onConfirm = viewModel::onStartValueConfirmed,
            )

            StepButtonsSection(
                enabled = !uiState.isRunning,
                onStep = viewModel::onStep,
            )
        }
    }
}

@Composable
private fun CodeDisplayCard(currentIndex: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppRadius.card),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppSpacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AppSpacing.xs),
        ) {
            Text(
                text = currentIndex.toString(),
                style = MaterialTheme.typography.displayMedium.merge(MonoNumeralStyle),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(
                    R.string.scan_address_command,
                    NecCodec.addressOf(currentIndex),
                    NecCodec.commandOf(currentIndex),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun NoticeCard(title: String, body: String, container: Color, content: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppRadius.card),
        colors = CardDefaults.cardColors(containerColor = container, contentColor = content),
    ) {
        Column(
            modifier = Modifier.padding(AppSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.xs),
        ) {
            Text(text = title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(text = body, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StartValueRow(
    text: String,
    enabled: Boolean,
    onTextChanged: (String) -> Unit,
    onConfirm: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = onTextChanged,
            label = { Text(stringResource(R.string.scan_start_value_label)) },
            enabled = enabled,
            singleLine = true,
            shape = RoundedCornerShape(AppRadius.textField),
            textStyle = MaterialTheme.typography.bodyLarge.merge(MonoNumeralStyle).copy(textAlign = TextAlign.Start),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onConfirm() }),
            modifier = Modifier.weight(1f),
        )
        OutlinedButton(onClick = onConfirm, enabled = enabled) {
            Text(stringResource(R.string.scan_apply))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StepButtonsSection(enabled: Boolean, onStep: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.sm),
        ) {
            STEP_SIZES.forEach { step ->
                OutlinedButton(onClick = { onStep(-step) }, enabled = enabled) {
                    Text("-$step")
                }
            }
        }
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.sm),
        ) {
            STEP_SIZES.reversed().forEach { step ->
                OutlinedButton(onClick = { onStep(step) }, enabled = enabled) {
                    Text("+$step")
                }
            }
        }
    }
}
