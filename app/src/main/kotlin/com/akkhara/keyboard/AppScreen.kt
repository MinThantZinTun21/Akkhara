package com.akkhara.keyboard

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.text.format.Formatter
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.akkhara.keyboard.engine.ModelTask
import com.akkhara.keyboard.translation.DownloadState
import com.akkhara.keyboard.translation.ModelImports
import com.akkhara.keyboard.translation.ModelShelf
import com.akkhara.keyboard.translation.TranslationModel
import com.akkhara.keyboard.translation.failureText
import com.akkhara.keyboard.translation.grammarCheckModel
import com.akkhara.keyboard.translation.progressText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.reflect.KMutableProperty0

/** Follows the system light or dark setting with two fixed schemes, as the keyboard does. */
@Composable
fun AkkharaTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors, content = content)
}

// The accent is the keyboard's own, so the screen and the keyboard look like one app.
private val LightColors = lightColorScheme(
    primary = Color(Theme.Light.palette.accent),
    onPrimary = Color(Theme.Light.palette.onAccent),
)

private val DarkColors = darkColorScheme(
    primary = Color(Theme.Dark.palette.accent),
    onPrimary = Color(Theme.Dark.palette.onAccent),
)

/** The app's one screen: setup, a place to try the keyboard, and the settings. */
@Composable
fun AppScreen(
    setup: Setup,
    settings: KeyboardSettings,
    models: ModelShelf,
    onOpenKeyboardSettings: () -> Unit,
    onChooseKeyboard: () -> Unit,
    readDownload: () -> DownloadState,
    onDownloadModel: (TranslationModel) -> Unit,
    onCancelDownload: () -> Unit,
    onImportModel: (Uri, Long) -> Unit,
    onCancelImport: () -> Unit,
    onRemoveModel: (TranslationModel) -> Unit,
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

            // The theme comes before Try it, so a newly picked theme can be tried right below it.
            SectionTitle(R.string.theme_title)
            Text(
                stringResource(R.string.theme_note),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            ThemePicker(settings)

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

            ModelsSection(
                models, readDownload, onDownloadModel, onCancelDownload, onImportModel, onCancelImport, onRemoveModel,
            )

            SectionTitle(R.string.prompts_title)
            Text(
                stringResource(R.string.prompts_note) + " " + stringResource(R.string.prompts_translates_only),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            ModelTask.entries.forEach { task -> PromptField(task, settings) }
        }
    }
}

/**
 * The choice of theme: small previews in a grid, following the system first,
 * then the classic themes and the trending ones, each group under its name.
 * See GLOSSARY.md, "Theme".
 */
@Composable
private fun ThemePicker(settings: KeyboardSettings) {
    var chosen by remember { mutableStateOf(settings.theme) }
    val choose: (Theme?) -> Unit = { theme ->
        chosen = theme
        settings.theme = theme
    }
    Column(modifier = Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Null is following the system, which comes first as it is what a new install does.
        ThemeGrid(listOf(null), chosen, choose)
        Theme.Group.entries.forEach { group ->
            Text(
                stringResource(group.nameRes),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(top = 8.dp),
            )
            ThemeGrid(Theme.entries.filter { it.group == group }, chosen, choose)
        }
    }
}

/** Rows of [ThemeCard]s, [THEME_COLUMNS] to a row. */
@Composable
private fun ThemeGrid(choices: List<Theme?>, chosen: Theme?, onChoose: (Theme?) -> Unit) {
    choices.chunked(THEME_COLUMNS).forEach { row ->
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            row.forEach { theme ->
                ThemeCard(
                    theme = theme,
                    isChosen = theme == chosen,
                    onChoose = { onChoose(theme) },
                    modifier = Modifier.weight(1f),
                )
            }
            // A short last row keeps its cards the width of the others.
            repeat(THEME_COLUMNS - row.size) { Spacer(Modifier.weight(1f)) }
        }
    }
}

private const val THEME_COLUMNS = 3

/** One choice in the [ThemePicker]: a few keys in the theme's colours, and its name. */
@Composable
private fun ThemeCard(theme: Theme?, isChosen: Boolean, onChoose: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(12.dp)
    val colours = MaterialTheme.colorScheme
    val (outline, nameColour) = if (isChosen) colours.primary to colours.primary else colours.outlineVariant to colours.onSurface
    Column(
        modifier = modifier
            .selectable(selected = isChosen, role = Role.RadioButton, onClick = onChoose)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .border(if (isChosen) 3.dp else 1.dp, outline, shape),
        ) {
            if (theme == null) {
                // Following the system is Light in light mode and Dark in dark mode, so it shows half of each.
                KeysPreview(Theme.Light.palette, letters = listOf("a"), modifier = Modifier.weight(1f))
                KeysPreview(Theme.Dark.palette, letters = listOf("က"), modifier = Modifier.weight(1f))
            } else {
                KeysPreview(theme.palette, letters = listOf("a", "က", "b"), modifier = Modifier.weight(1f))
            }
        }
        Text(
            text = (if (isChosen) "\u2713 " else "") + stringResource(theme?.nameRes ?: R.string.theme_follow_system),
            style = MaterialTheme.typography.labelLarge,
            color = nameColour,
            maxLines = 1,
        )
    }
}

/** A corner of the keyboard as [palette] draws it: letter keys, an action key and the enter key. */
@Composable
private fun KeysPreview(palette: Palette, letters: List<String>, modifier: Modifier = Modifier) {
    val gap = Arrangement.spacedBy(4.dp)
    Column(
        modifier = modifier.background(Color(palette.background)).padding(8.dp),
        verticalArrangement = gap,
    ) {
        Row(horizontalArrangement = gap) {
            letters.forEach { letter ->
                PreviewKey(Color(palette.key), Modifier.weight(1f)) {
                    Text(letter, color = Color(palette.label), fontSize = 13.sp, lineHeight = 16.sp)
                }
            }
        }
        Row(horizontalArrangement = gap) {
            PreviewKey(Color(palette.actionKey), Modifier.weight(1f)) {
                Text("12", color = Color(palette.actionLabel), fontSize = 11.sp, lineHeight = 16.sp)
            }
            PreviewKey(Color(palette.accent), Modifier.weight(1f)) {
                Text("\u2713", color = Color(palette.onAccent), fontSize = 11.sp, lineHeight = 16.sp)
            }
        }
    }
}

@Composable
private fun PreviewKey(colour: Color, modifier: Modifier = Modifier, label: @Composable () -> Unit) {
    Box(
        modifier = modifier.height(24.dp).background(colour, RoundedCornerShape(5.dp)),
        contentAlignment = Alignment.Center,
    ) { label() }
}

/**
 * The translation models: each listed model and the imported one, whether it
 * is on the phone, and a choice of the model in use among those that are.
 */
@Composable
private fun ModelsSection(
    models: ModelShelf,
    readDownload: () -> DownloadState,
    onDownload: (TranslationModel) -> Unit,
    onCancelDownload: () -> Unit,
    onImport: (Uri, Long) -> Unit,
    onCancelImport: () -> Unit,
    onRemove: (TranslationModel) -> Unit,
) {
    val context = LocalContext.current
    // Android runs the download, so the screen asks it how the download stands, twice a second.
    val download by produceState(readDownload()) {
        while (true) {
            value = withContext(Dispatchers.IO) { readDownload() }
            delay(DOWNLOAD_POLL_MILLIS)
        }
    }
    val import = ModelImports.state
    // What is on the phone is read from the files, and read again after each change.
    var shelf by remember { mutableStateOf(ShelfView(models)) }
    LaunchedEffect(download is DownloadState.Running, import is DownloadState.Running) { shelf = ShelfView(models) }
    var downloading by remember { mutableStateOf<TranslationModel?>(null) }
    // The download runs whether or not notifications are allowed; asking only lets the app say when it is ready.
    var askedFor by remember { mutableStateOf<TranslationModel?>(null) }
    val askForNotifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        askedFor?.let(onDownload)
        askedFor = null
    }
    var removing by remember { mutableStateOf<TranslationModel?>(null) }
    val (all, onPhone, inUse) = shelf
    val running = download as? DownloadState.Running
    // One download or import at a time.
    val busy = running != null || import is DownloadState.Running

    SectionTitle(R.string.models_title)
    Text(
        stringResource(R.string.models_note),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Column {
        all.forEach { model ->
            val isOnPhone = model in onPhone
            val isRunning = running?.model == model
            val failed = (download as? DownloadState.Failed)?.takeIf { it.model == model }
            val status = when {
                isRunning -> progressText(context, running.step, running.bytes, running.total)
                failed != null -> failureText(context, failed.reason, model)
                model == inUse -> stringResource(R.string.model_in_use)
                isOnPhone -> stringResource(R.string.model_on_phone)
                else -> stringResource(R.string.model_not_downloaded)
            }
            ModelRow(
                model = model,
                status = status,
                note = if (model.translatesOnly) translatesOnlyNote(model, onPhone) else null,
                isInUse = model == inUse,
                isOnPhone = isOnPhone,
                progress = running?.takeIf { isRunning && it.step != DownloadState.Step.WaitingForWifi }
                    ?.let { it.bytes.toFloat() / it.total },
                onUse = {
                    models.use(model)
                    shelf = ShelfView(models)
                },
            ) {
                when {
                    isRunning -> TextButton(onClick = onCancelDownload) { Text(stringResource(R.string.cancel)) }
                    isOnPhone -> TextButton(onClick = { removing = model }) { Text(stringResource(R.string.model_remove)) }
                    model.source != null -> TextButton(
                        onClick = { downloading = model },
                        enabled = !busy,
                    ) { Text(stringResource(R.string.model_download)) }
                }
            }
        }
    }

    ImportButton(import, busy, onImport, onCancelImport)

    downloading?.let { model ->
        AlertDialog(
            onDismissRequest = { downloading = null },
            title = { Text(stringResource(R.string.model_download_title, model.name)) },
            text = { Text(stringResource(R.string.model_download_body, fileSize(model.sizeBytes))) },
            confirmButton = {
                TextButton(onClick = {
                    downloading = null
                    if (needsNotificationPermission(context)) {
                        askedFor = model
                        askForNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        onDownload(model)
                    }
                }) { Text(stringResource(R.string.model_download)) }
            },
            dismissButton = {
                TextButton(onClick = { downloading = null }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }

    removing?.let { model ->
        val next = onPhone.firstOrNull { it != model }
        AlertDialog(
            onDismissRequest = { removing = null },
            title = { Text(stringResource(R.string.model_remove_title, model.name)) },
            text = {
                Text(
                    stringResource(R.string.model_remove_body, fileSize(model.sizeBytes)) +
                        if (model != inUse) "" else " " + if (next != null) {
                            stringResource(R.string.model_remove_next, next.name)
                        } else {
                            stringResource(R.string.model_remove_last)
                        },
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    removing = null
                    onRemove(model)
                    shelf = ShelfView(models)
                }) { Text(stringResource(R.string.model_remove)) }
            },
            dismissButton = {
                TextButton(onClick = { removing = null }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

/**
 * Import model file: Android's file picker, then the copy's progress or why it
 * failed. Only a file named `.litertlm` is taken.
 */
@Composable
private fun ImportButton(import: DownloadState, busy: Boolean, onImport: (Uri, Long) -> Unit, onCancel: () -> Unit) {
    val context = LocalContext.current
    var wrongFile by remember { mutableStateOf(false) }
    val chooseFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val (name, size) = describe(context, uri)
        wrongFile = !name.endsWith(".litertlm", ignoreCase = true)
        if (!wrongFile) onImport(uri, size)
    }
    val importing = import as? DownloadState.Running
    val failed = import as? DownloadState.Failed
    val status = when {
        importing != null -> progressText(context, importing.step, importing.bytes, importing.total)
        failed != null -> failureText(context, failed.reason, failed.model)
        wrongFile -> stringResource(R.string.import_wrong_file)
        else -> stringResource(R.string.model_import_note)
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            status,
            style = MaterialTheme.typography.bodyMedium,
            color = if (failed != null || wrongFile) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (importing != null && importing.step == DownloadState.Step.Copying && importing.total > 0) {
            LinearProgressIndicator(
                progress = { importing.bytes.toFloat() / importing.total },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { chooseFile.launch(arrayOf("*/*")) },
                enabled = !busy,
            ) { Text(stringResource(R.string.model_import)) }
            if (importing != null) TextButton(onClick = onCancel) { Text(stringResource(R.string.cancel)) }
        }
    }
}

/** The chosen file's name and size in bytes (-1 when Android does not say). */
private fun describe(context: Context, uri: Uri): Pair<String, Long> {
    val columns = arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE)
    context.contentResolver.query(uri, columns, null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            val name = cursor.getString(0).orEmpty()
            val size = if (cursor.isNull(1)) -1 else cursor.getLong(1)
            return name to size
        }
    }
    return "" to -1
}

private fun needsNotificationPermission(context: Context) =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED

/** What the models section shows, read from [ModelShelf] in one go. */
private data class ShelfView(
    val all: List<TranslationModel>,
    val onPhone: List<TranslationModel>,
    val inUse: TranslationModel?,
) {
    constructor(models: ModelShelf) : this(
        all = models.models(),
        onPhone = models.models().filter(models::isOnPhone),
        inUse = models.inUse(),
    )
}

/** For a model that translates only: which model on the phone would check grammar while it is in use, if any. */
@Composable
private fun translatesOnlyNote(model: TranslationModel, onPhone: List<TranslationModel>): String =
    grammarCheckModel(inUse = model, onPhone = onPhone)
        ?.let { stringResource(R.string.model_translates_only, it.name) }
        ?: stringResource(R.string.model_translates_only_no_grammar_check)

/** One translation model: a choice of it as the model in use, its size and state, [action], and the download's [progress]. */
@Composable
private fun ModelRow(
    model: TranslationModel,
    status: String,
    /** A second line under the status, or null for none. */
    note: String?,
    isInUse: Boolean,
    isOnPhone: Boolean,
    progress: Float?,
    onUse: () -> Unit,
    action: @Composable () -> Unit,
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .selectable(selected = isInUse, enabled = isOnPhone, role = Role.RadioButton, onClick = onUse)
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(selected = isInUse, onClick = null, enabled = isOnPhone)
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(model.name, style = MaterialTheme.typography.bodyLarge)
                Text(
                    stringResource(R.string.model_status, fileSize(model.sizeBytes), status),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (note != null) {
                    Text(note, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            action()
        }
        if (progress != null) LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun fileSize(bytes: Long): String = Formatter.formatFileSize(LocalContext.current, bytes)

/**
 * The prompt for [task], ready to rewrite. It shows the default until the
 * user changes it; clearing the box also means the default.
 */
@Composable
private fun PromptField(task: ModelTask, settings: KeyboardSettings) {
    var prompt by remember { mutableStateOf(settings.prompt(task).ifBlank { task.defaultPrompt }) }
    Column {
        OutlinedTextField(
            value = prompt,
            onValueChange = {
                prompt = it.take(ModelTask.MAX_PROMPT_CHARS)
                settings.setPrompt(task, prompt)
            },
            label = {
                Text(
                    stringResource(
                        when (task) {
                            ModelTask.EnglishToMyanmar -> R.string.prompt_english_to_myanmar
                            ModelTask.MyanmarToEnglish -> R.string.prompt_myanmar_to_english
                            ModelTask.GrammarCheck -> R.string.prompt_grammar_check
                        },
                    ),
                )
            },
            supportingText = {
                Text(stringResource(R.string.prompt_length, prompt.length, ModelTask.MAX_PROMPT_CHARS))
            },
            minLines = 3,
            maxLines = 8,
            modifier = Modifier.fillMaxWidth(),
        )
        TextButton(
            onClick = {
                prompt = task.defaultPrompt
                settings.setPrompt(task, prompt)
            },
            enabled = prompt != task.defaultPrompt,
            modifier = Modifier.align(Alignment.End),
        ) { Text(stringResource(R.string.prompt_reset)) }
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

private const val DOWNLOAD_POLL_MILLIS = 500L
