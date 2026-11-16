package com.akkhara.keyboard

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import kotlin.reflect.KMutableProperty0

/** Follows the system light or dark setting with two fixed schemes, as the keyboard does. */
@Composable
fun AkkharaTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors, content = content)
}

// The accent is the keyboard's own, so the screen and the keyboard look like one app.
private val LightColors = lightColorScheme(
    primary = Color(Palette.Light.accent),
    onPrimary = Color(Palette.Light.onAccent),
)

private val DarkColors = darkColorScheme(
    primary = Color(Palette.Dark.accent),
    onPrimary = Color(Palette.Dark.onAccent),
)

/** The app's one screen: setup, a place to try the keyboard, and the settings. */
@Composable
fun AppScreen(
    setup: Setup,
    settings: KeyboardSettings,
    onOpenKeyboardSettings: () -> Unit,
    onChooseKeyboard: () -> Unit,
) {
    Scaffold(contentWindowInsets = WindowInsets.safeDrawing) { insets ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(insets)
                .consumeWindowInsets(insets)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium)

            SectionTitle(R.string.setup_title)
            SetupStep(
                title = R.string.setup_enable_title,
                body = R.string.setup_enable_body,
                button = R.string.setup_enable_button,
                isDone = setup.isEnabled,
                isAvailable = true,
                onClick = onOpenKeyboardSettings,
            )
            SetupStep(
                title = R.string.setup_select_title,
                body = R.string.setup_select_body,
                button = R.string.setup_select_button,
                isDone = setup.isSelected,
                isAvailable = setup.isEnabled,
                onClick = onChooseKeyboard,
            )

            SectionTitle(R.string.try_title)
            // Typed text is never saved, not even across a rotation (docs/adr/0002-no-network-permission.md).
            var trial by remember { mutableStateOf("") }
            OutlinedTextField(
                value = trial,
                onValueChange = { trial = it },
                label = { Text(stringResource(R.string.try_label)) },
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )

            SectionTitle(R.string.settings_title)
            Text(
                stringResource(R.string.settings_note),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Column {
                Toggle(R.string.setting_haptic_feedback, settings::hapticFeedback)
                Toggle(R.string.setting_key_preview, settings::keyPreview, note = R.string.setting_key_preview_note)
                Toggle(
                    R.string.setting_capitalise_sentences,
                    settings::capitaliseSentences,
                    note = R.string.setting_english_only_note,
                )
                Toggle(
                    R.string.setting_double_space_full_stop,
                    settings::doubleSpaceFullStop,
                    note = R.string.setting_english_only_note,
                )
                Toggle(
                    R.string.setting_translate_as_you_type,
                    settings::translateAsYouType,
                    note = R.string.setting_translate_as_you_type_note,
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(text: Int) {
    Text(
        stringResource(text),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp),
    )
}

/** One of the two setup steps: what to do, whether it is done, and a button to the Android screen for it. */
@Composable
private fun SetupStep(
    title: Int,
    body: Int,
    button: Int,
    isDone: Boolean,
    isAvailable: Boolean,
    onClick: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(title), style = MaterialTheme.typography.titleMedium)
            Text(
                text = if (isDone) "\u2713 " + stringResource(R.string.setup_step_done)
                else stringResource(R.string.setup_step_not_done),
                style = MaterialTheme.typography.labelLarge,
                color = if (isDone) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(stringResource(body), style = MaterialTheme.typography.bodyMedium)
            // A finished step stays reachable, but no longer asks for attention.
            if (isDone) {
                OutlinedButton(onClick = onClick) { Text(stringResource(button)) }
            } else {
                Button(onClick = onClick, enabled = isAvailable) { Text(stringResource(button)) }
            }
        }
    }
}

/** A [setting] that is either on or off. The whole row is the switch. */
@Composable
private fun Toggle(
    title: Int,
    setting: KMutableProperty0<Boolean>,
    note: Int? = null,
) {
    var isOn by remember { mutableStateOf(setting.get()) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .toggleable(
                value = isOn,
                role = Role.Switch,
                onValueChange = {
                    isOn = it
                    setting.set(it)
                },
            )
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(stringResource(title), style = MaterialTheme.typography.bodyLarge)
            if (note != null) {
                Text(
                    stringResource(note),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.width(16.dp))
        Switch(checked = isOn, onCheckedChange = null)
    }
}
