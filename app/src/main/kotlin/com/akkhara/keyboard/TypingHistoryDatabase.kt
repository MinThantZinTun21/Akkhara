package com.akkhara.keyboard

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.akkhara.keyboard.engine.HistoryEntry
import com.akkhara.keyboard.engine.HistoryStore
import com.akkhara.keyboard.engine.SourceApp
import java.util.concurrent.Executors

/** A saved entry of typing history, with the [id] it is deleted by. */
data class SavedEntry(val id: Long, val entry: HistoryEntry)

/**
 * Typing history, in the app's private SQLite database (ADR 0009). It never
 * leaves the phone: app backup is off and nothing here is sent anywhere.
 *
 * Saving happens on a background thread so typing never waits for the disk.
 * There is one per process, shared by the keyboard and the app screen.
 */
class TypingHistoryDatabase private constructor(context: Context) :
    SQLiteOpenHelper(context, FILE, null, VERSION), HistoryStore {

    private val writer = Executors.newSingleThreadExecutor()

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE entries (id INTEGER PRIMARY KEY AUTOINCREMENT, text TEXT NOT NULL, " +
                "app TEXT NOT NULL, app_name TEXT NOT NULL, saved_at INTEGER NOT NULL)",
        )
        db.execSQL("CREATE INDEX entries_saved_at ON entries (saved_at)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit

    override fun save(entry: HistoryEntry) {
        writer.execute {
            writableDatabase.insert(
                "entries",
                null,
                ContentValues().apply {
                    put("text", entry.text)
                    put("app", entry.app.packageName)
                    put("app_name", entry.app.name)
                    put("saved_at", entry.savedAt)
                },
            )
        }
    }

    /** Entries, newest first. Reads the disk, so call it off the main thread. */
    fun newestFirst(limit: Int = PAGE_LIMIT): List<SavedEntry> =
        readableDatabase.query(
            "entries", COLUMNS, null, null, null, null, "saved_at DESC, id DESC", limit.toString(),
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(
                        SavedEntry(
                            id = cursor.getLong(0),
                            entry = HistoryEntry(
                                text = cursor.getString(1),
                                app = SourceApp(cursor.getString(2), cursor.getString(3)),
                                savedAt = cursor.getLong(4),
                            ),
                        ),
                    )
                }
            }
        }

    companion object {
        private const val FILE = "typing_history.db"
        private const val VERSION = 1
        private val COLUMNS = arrayOf("id", "text", "app", "app_name", "saved_at")

        /** The most entries the history page shows at once. */
        const val PAGE_LIMIT = 500

        @Volatile private var instance: TypingHistoryDatabase? = null

        fun get(context: Context): TypingHistoryDatabase =
            instance ?: synchronized(this) {
                instance ?: TypingHistoryDatabase(context.applicationContext).also { instance = it }
            }
    }
}
