package com.akkhara.keyboard

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteOpenHelper
import com.akkhara.keyboard.engine.HistoryEntry
import com.akkhara.keyboard.engine.HistoryStore
import com.akkhara.keyboard.engine.SourceApp
import com.akkhara.keyboard.engine.TypingHistoryRecorder
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
            // A full or broken disk loses the entry rather than taking the keyboard down with it.
            try {
                deleteExpired()
                if (newestText(entry.app.packageName) != entry.text) insert(entry)
            } catch (_: SQLiteException) {
            }
        }
    }

    private fun insert(entry: HistoryEntry) {
        writableDatabase.insert(
            TABLE,
            null,
            ContentValues().apply {
                put("text", entry.text)
                put("app", entry.app.packageName)
                put("app_name", entry.app.name)
                put("saved_at", entry.savedAt)
            },
        )
    }

    /** The text of [app]'s newest entry, if it has one. */
    private fun newestText(app: String): String? =
        readableDatabase.query(
            TABLE, arrayOf("text"), "app = ?", arrayOf(app), null, null, NEWEST_FIRST, "1",
        ).use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }

    /**
     * Deletes entries older than [TypingHistoryRecorder.KEPT_DAYS]. Writes the
     * disk, so call it off the main thread.
     */
    fun deleteExpired() {
        val since = TypingHistoryRecorder.keptSince(System.currentTimeMillis())
        writableDatabase.delete(TABLE, "saved_at < ?", arrayOf(since.toString()))
    }

    /** Deletes one entry. Writes the disk, so call it off the main thread. */
    fun delete(id: Long) {
        writableDatabase.delete(TABLE, "id = ?", arrayOf(id.toString()))
    }

    /** Deletes every entry. Writes the disk, so call it off the main thread. */
    fun deleteAll() {
        writableDatabase.delete(TABLE, null, null)
    }

    /**
     * Entries containing [search], newest first: at most [PAGE_LIMIT] and one
     * more, so the page knows when there are older ones. Reads the disk, so
     * call it off the main thread.
     */
    fun newestFirst(search: String = ""): List<SavedEntry> {
        val (where, args) =
            if (search.isBlank()) null to null
            else "text LIKE ? ESCAPE '\\'" to arrayOf(containsPattern(search.trim()))
        return readableDatabase.query(
            TABLE, COLUMNS, where, args, null, null, NEWEST_FIRST, (PAGE_LIMIT + 1).toString(),
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
    }

    companion object {
        private const val FILE = "typing_history.db"
        private const val VERSION = 1
        private const val TABLE = "entries"
        private const val NEWEST_FIRST = "saved_at DESC, id DESC"
        private val COLUMNS = arrayOf("id", "text", "app", "app_name", "saved_at")

        /**
         * The LIKE pattern for entries that contain [search] anywhere. Plain
         * "contains" works for Myanmar, which has no spaces between words
         * (ADR 0009). The user's %, _ and \ match only themselves.
         */
        fun containsPattern(search: String): String =
            "%" + search.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%"

        /** The most entries the history page shows at once. */
        const val PAGE_LIMIT = 500

        @Volatile private var instance: TypingHistoryDatabase? = null

        fun get(context: Context): TypingHistoryDatabase =
            instance ?: synchronized(this) {
                instance ?: TypingHistoryDatabase(context.applicationContext).also { instance = it }
            }
    }
}
