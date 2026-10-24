package ai.gradium.phonon.tts

import android.os.Build
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

internal data class InstalledChoice(val id: String, val label: String, val details: String)
internal data class SettingsUiState(
    val installed: List<InstalledChoice> = emptyList(), val selectedInstalled: String? = null, val installedCatalog: Set<String> = emptySet(),
    val ready: Boolean = false, val busy: Boolean = false, val loading: Boolean = false,
    val error: Boolean = false, val status: String = "", val installedDetails: String = "",
    val operation: String = "", val selectedModel: Int = 0,
    val voices: List<Pair<String, String>> = emptyList(), val selectedVoice: Int = 0,
    val sample: String = "", val rate: Int = 100, val speaking: Boolean = false,
    val previewError: String = "", val token: String = "", val tokenStatus: String = "",
    val confirmReplacement: Boolean = false, val licenseText: String? = null
)
internal data class SettingsActions(
    val selectInstalled: (String) -> Unit, val preferModel: () -> Unit,
    val selectModel: (Int) -> Unit, val selectVoice: (Int) -> Unit,
    val editSample: (String) -> Unit, val changeRate: (Int) -> Unit,
    val editToken: (String) -> Unit, val saveToken: () -> Unit, val deleteToken: () -> Unit,
    val download: () -> Unit, val importModel: () -> Unit, val cancelInstall: () -> Unit,
    val preview: () -> Unit, val stopPreview: () -> Unit, val terms: () -> Unit,
    val systemSettings: () -> Unit, val confirmReplacement: () -> Unit,
    val dismissReplacement: () -> Unit, val licenses: () -> Unit, val dismissLicenses: () -> Unit
)

private val SageLight = lightColorScheme(
    primary = Color(0xFF35694D), onPrimary = Color.White,
    primaryContainer = Color(0xFFB8ECCE), onPrimaryContainer = Color(0xFF123D28),
    secondary = Color(0xFF506357), secondaryContainer = Color(0xFFD3E8D8),
    background = Color(0xFFF6FAF5), surface = Color(0xFFF6FAF5),
    surfaceContainer = Color(0xFFEAF0E9), surfaceContainerLow = Color(0xFFF0F5EE),
    onSurface = Color(0xFF18201A), onSurfaceVariant = Color(0xFF424B44), outline = Color(0xFF737D74)
)
private val SageDark = darkColorScheme(
    primary = Color(0xFF9DD5B3), onPrimary = Color(0xFF003921),
    primaryContainer = Color(0xFF1B5037), onPrimaryContainer = Color(0xFFB8ECCE),
    secondary = Color(0xFFB7CCBD), secondaryContainer = Color(0xFF394C40),
    background = Color(0xFF0E1510), surface = Color(0xFF0E1510),
    surfaceContainer = Color(0xFF1A231C), surfaceContainerLow = Color(0xFF151D17),
    onSurface = Color(0xFFDFE7DD), onSurfaceVariant = Color(0xFFBFC9BF), outline = Color(0xFF89948A)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsScreen(state: SettingsUiState, actions: SettingsActions) {
    val dark = isSystemInDarkTheme()
    val context = LocalContext.current
    val localeList = context.resources.configuration.locales
    val recommendedLanguage = Routing.systemLanguage((0 until localeList.size()).map { localeList[it].toLanguageTag() },Model.SUPPORTED)
    val colors = if (Build.VERSION.SDK_INT >= 31) {
        if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else if (dark) SageDark else SageLight
    MaterialTheme(colorScheme = colors, typography = Typography(), shapes = Shapes()) {
        var modelsOpen by rememberSaveable { mutableStateOf(false) }
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = { TopAppBar(title = { Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge) }) }
        ) { padding ->
            BoxWithConstraints(Modifier.fillMaxSize().padding(padding).imePadding(), contentAlignment = Alignment.TopCenter) {
                val horizontalPadding = if (maxWidth < 400.dp) 16.dp else 24.dp
                Column(Modifier.widthIn(max = 720.dp).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = horizontalPadding, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    ElevatedCard(Modifier.fillMaxWidth(), colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            SectionLabel(stringResource(R.string.installed_model))
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Surface(shape = MaterialTheme.shapes.medium, color = if (state.error) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer) {
                                    val ink = if (state.error) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
                                    Canvas(Modifier.padding(14.dp).size(24.dp)) {
                                        if (state.error) {
                                            drawLine(ink, Offset(size.width / 2, 2.dp.toPx()), Offset(size.width / 2, 14.dp.toPx()), 2.dp.toPx(), StrokeCap.Round)
                                            drawCircle(ink, 1.5.dp.toPx(), Offset(size.width / 2, 21.dp.toPx()))
                                        } else {
                                            listOf(8f, 16f, 24f, 16f, 8f).forEachIndexed { index, height ->
                                                val x = (index * 5 + 2).dp.toPx()
                                                drawLine(ink, Offset(x, (24f - height).dp.toPx() / 2), Offset(x, (24f + height).dp.toPx() / 2), 2.dp.toPx(), StrokeCap.Round)
                                            }
                                        }
                                    }
                                }
                                Text(state.status, Modifier.weight(1f).semantics { liveRegion = LiveRegionMode.Polite }, style = MaterialTheme.typography.headlineSmall, color = if (state.error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
                            }
                            state.installed.forEach { entry ->
                                OutlinedCard(onClick = { actions.selectInstalled(entry.id) }, enabled = !state.busy) {
                                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                        RadioButton(selected = entry.id == state.selectedInstalled, onClick = null)
                                        Column { Text(entry.label); Text(entry.details, style = MaterialTheme.typography.bodySmall) }
                                    }
                                }
                            }
                            if (state.busy || state.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
                            if (state.operation.isNotBlank()) Text(state.operation, Modifier.semantics { liveRegion = LiveRegionMode.Polite }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (state.busy) OutlinedButton(onClick = actions.cancelInstall) { Text(stringResource(R.string.cancel)) }
                        }
                    }
                    if (state.ready) {
                        Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = if (dark) .35f else .28f)) {
                            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                SectionLabel(stringResource(R.string.voice_preview))
                                VoiceSelect(state, actions)
                                OutlinedButton(onClick = actions.preferModel) { Text(stringResource(R.string.prefer_language)) }
                                OutlinedTextField(value = state.sample, onValueChange = actions.editSample, label = { Text(stringResource(R.string.sample_text)) }, modifier = Modifier.fillMaxWidth(), minLines = 3, maxLines = 7, shape = MaterialTheme.shapes.medium)
                                Column {
                                    val rateLabel = stringResource(R.string.preview_rate, state.rate)
                                    Text(rateLabel, style = MaterialTheme.typography.labelLarge)
                                    Slider(value = state.rate.toFloat(), onValueChange = { actions.changeRate(it.toInt().coerceIn(50, 200)) }, valueRange = 50f..200f, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).semantics { contentDescription = rateLabel })
                                }
                                Button(onClick = actions.preview, enabled = state.sample.isNotBlank(), modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text(stringResource(R.string.preview)) }
                                if (state.speaking) OutlinedButton(onClick = actions.stopPreview, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.stop_preview)) }
                                if (state.previewError.isNotBlank()) Text(state.previewError, color = MaterialTheme.colorScheme.error, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                            }
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SectionLabel(stringResource(R.string.model_storage))
                        Text(stringResource(R.string.replacement_help), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        FilledTonalButton(onClick = { modelsOpen = true }, enabled = !state.busy, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(stringResource(R.string.choose_download)) }
                        OutlinedButton(onClick = actions.importModel, enabled = !state.busy, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(stringResource(R.string.local_import)) }
                        Text(stringResource(R.string.import_help), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    HorizontalDivider()
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(onClick = actions.systemSettings, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.system_settings)) }
                        TextButton(onClick = actions.licenses, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.licenses)) }
                        Text(stringResource(R.string.about_fork), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
        if (modelsOpen) ModalBottomSheet(onDismissRequest = { modelsOpen = false }) {
            Column(Modifier.widthIn(max = 720.dp).fillMaxWidth().verticalScroll(rememberScrollState()).imePadding().padding(horizontal = 24.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                SectionLabel(stringResource(R.string.model_actions))
                ModelCatalog.all.forEachIndexed { index, model ->
                    val selected = state.selectedModel == index
                    OutlinedCard(onClick = { actions.selectModel(index) }, enabled = !state.busy, modifier = Modifier.fillMaxWidth().semantics { this.selected = selected; role = Role.RadioButton }, colors = CardDefaults.outlinedCardColors(containerColor = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface)) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = selected, onClick = null)
                            Column(Modifier.padding(start = 12.dp)) {
                                Text(stringResource(model.label), style = MaterialTheme.typography.titleMedium)
                                if(model.id in state.installedCatalog) Text(stringResource(R.string.already_installed), style = MaterialTheme.typography.labelMedium)
                                if (model.languages.size == 1 && model.languages.first() == recommendedLanguage) Text(stringResource(R.string.recommended), style = MaterialTheme.typography.labelMedium)
                                Text(stringResource(R.string.model_details, model.total / 1_000_000.0, if (model.gated) "CC-BY-NC 4.0" else "Apache 2.0"), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
                val model = ModelCatalog.all[state.selectedModel]
                if (model.gated) {
                    Text(stringResource(R.string.gated_help), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TextButton(onClick = actions.terms) { Text(stringResource(R.string.open_terms)) }
                    Text(state.tokenStatus, style = MaterialTheme.typography.labelMedium, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                    OutlinedTextField(value = state.token, onValueChange = actions.editToken, label = { Text(stringResource(R.string.hf_token)) }, supportingText = { Text(stringResource(R.string.token_hint)) }, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), singleLine = true, modifier = Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalButton(onClick = actions.saveToken, enabled = state.token.isNotBlank()) { Text(stringResource(R.string.save_token)) }
                        TextButton(onClick = actions.deleteToken) { Text(stringResource(R.string.delete_token)) }
                    }
                } else Text(stringResource(R.string.free_help), style = MaterialTheme.typography.bodySmall)
                Button(onClick = { modelsOpen = false; actions.download() }, enabled = !state.busy && (!model.gated || state.token.isBlank()), modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text(stringResource(R.string.download)) }
            }
        }
        if (state.confirmReplacement) AlertDialog(onDismissRequest = actions.dismissReplacement,
            title = { Text(stringResource(R.string.replace_title)) }, text = { Text(stringResource(R.string.replace_message)) },
            confirmButton = { TextButton(onClick = actions.confirmReplacement) { Text(stringResource(R.string.replace_confirm)) } },
            dismissButton = { TextButton(onClick = actions.dismissReplacement) { Text(stringResource(android.R.string.cancel)) } })
        state.licenseText?.let { text -> AlertDialog(onDismissRequest = actions.dismissLicenses,
            title = { Text(stringResource(R.string.licenses)) },
            text = { SelectionContainer { Text(text, Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()), style = MaterialTheme.typography.bodySmall) } },
            confirmButton = { TextButton(onClick = actions.dismissLicenses) { Text(stringResource(android.R.string.ok)) } }) }
    }
}
@Composable private fun SectionLabel(text: String) {
    Text(text, Modifier.semantics { heading() }, style = MaterialTheme.typography.titleMedium)
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun VoiceSelect(state: SettingsUiState, actions: SettingsActions) {
    var expanded by remember { mutableStateOf(false) }
    val defaultLabel = stringResource(R.string.default_voice)
    val displayLocale = LocalContext.current.resources.configuration.locales[0]
    fun label(choice: Pair<String, String>): String {
        val speaker = if (choice.second == "default") defaultLabel else BundledVoices.label(choice.second)
        return "$speaker · ${java.util.Locale.forLanguageTag(choice.first).getDisplayLanguage(displayLocale)}"
    }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(value = state.voices.getOrNull(state.selectedVoice)?.let { label(it) }.orEmpty(), onValueChange = {}, readOnly = true,
            label = { Text(stringResource(R.string.voice)) }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth())
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            state.voices.forEachIndexed { index, voice -> DropdownMenuItem(text = { Text(label(voice)) }, onClick = { actions.selectVoice(index); expanded = false }) }
        }
    }
}
