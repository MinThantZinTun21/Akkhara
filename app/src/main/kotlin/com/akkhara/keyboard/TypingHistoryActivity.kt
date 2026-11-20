package com.akkhara.keyboard

import android.os.Bundle
import android.text.format.DateUtils
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** The Typing history page: what the keyboard has kept, newest first. See GLOSSARY.md, "Typing history". */
class TypingHistoryActivity : ComponentActivity() {

    /** Bumped each time the page comes into view, so it shows what was saved meanwhile. */
    private var shown by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val history = TypingHistoryDatabase.get(this)
        setContent {
            AkkharaTheme {
                TypingHistoryScreen(history, shown)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        shown++
    }
}

@Composable
private fun TypingHistoryScreen(history: TypingHistoryDatabase, shown: Int) {
    var entries by remember { mutableStateOf<List<SavedEntry>?>(null) }
    var askDeleteAll by remember { mutableStateOf(false) }
    var search by remember { mutableStateOf("") }
    // Bumped after each delete. Only the effect below sets the entries, and a newer load cancels an older one.
    var changes by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(shown, search, changes) {
        entries = withContext(Dispatchers.IO) {
            history.deleteExpired()
            history.newestFirst(search)
        }
    }
    fun change(work: () -> Unit) {
        scope.launch {
            withContext(Dispatchers.IO) { work() }
            changes++
        }
    }
    if (askDeleteAll) {
        AlertDialog(
            onDismissRequest = { askDeleteAll = false },
            title = { Text(stringResource(R.string.typing_history_delete_all_title)) },
            text = { Text(stringResource(R.string.typing_history_delete_all_text)) },
            confirmButton = {
                TextButton(onClick = {
                    askDeleteAll = false
                    change(history::deleteAll)
                }) { Text(stringResource(R.string.typing_history_delete_all_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { askDeleteAll = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
    Scaffold(contentWindowInsets = WindowInsets.safeDrawing) { insets ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(insets)
                .consumeWindowInsets(insets)
                .padding(horizontal = 16.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(stringResource(R.string.typing_history_title), style = MaterialTheme.typography.headlineMedium)
            Text(
                stringResource(R.string.typing_history_note),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            val shownEntries = entries ?: return@Column
            if (shownEntries.isEmpty() && search.isEmpty()) {
                Text(stringResource(R.string.typing_history_empty), style = MaterialTheme.typography.bodyLarge)
                return@Column
            }
            // Akkhara's own fields are never saved, so what is typed here stays out of the history.
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.typing_history_search)) },
                singleLine = true,
            )
            TextButton(onClick = { askDeleteAll = true }) {
                Text(stringResource(R.string.typing_history_delete_all))
            }
            if (shownEntries.isEmpty()) {
                Text(stringResource(R.string.typing_history_no_match), style = MaterialTheme.typography.bodyLarge)
                return@Column
            }
            if (shownEntries.size == TypingHistoryDatabase.PAGE_LIMIT) {
                Text(
                    stringResource(R.string.typing_history_newest_only, TypingHistoryDatabase.PAGE_LIMIT),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(shownEntries, key = { it.id }) { saved ->
                    EntryRow(saved, onDelete = { change { history.delete(saved.id) } })
                    HorizontalDivider()
                }
            }
        }
    }
}

/** One entry: its text, which app it came from and when, and a way to delete it. */
@Composable
private fun EntryRow(saved: SavedEntry, onDelete: () -> Unit) {
    val context = LocalContext.current
    val time = DateUtils.formatDateTime(
        context,
        saved.entry.savedAt,
        DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_ABBREV_MONTH,
    )
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(saved.entry.text, style = MaterialTheme.typography.bodyLarge)
            Text(
                "${saved.entry.app.name} · $time",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        TextButton(onClick = onDelete) { Text(stringResource(R.string.typing_history_delete)) }
    }
}
