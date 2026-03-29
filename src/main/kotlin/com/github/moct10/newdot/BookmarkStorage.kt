package com.github.moct10.newdot

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

data class Bookmark(
    val name: String,
    val path: Path,
    val timestamp: Long,
)

private data class BookmarkRecord(
    val name: String,
    val path: String,
    val timestamp: Long,
)

object BookmarkStorage {
    private const val STORAGE_FILE = ".ideavim-newdot-bookmarks.json"
    private val gson = Gson()
    private val bookmarkRecordListType = object : TypeToken<List<BookmarkRecord>>() {}.type
    private val bookmarksFile = getStorageFilePath()
    private val lock = Any()

    /**
     * Load bookmarks from persistent storage.
     * Returns empty list if file doesn't exist or is empty.
     */
    fun loadBookmarks(): List<Bookmark> {
        synchronized(lock) {
            return try {
                if (!Files.exists(bookmarksFile)) return emptyList()

                val json = Files.readString(bookmarksFile)
                if (json.trim().isEmpty()) return emptyList()

                val records: List<BookmarkRecord> = gson.fromJson(json, bookmarkRecordListType) ?: emptyList()
                records.mapNotNull { record ->
                    try {
                        Bookmark(
                            name = record.name,
                            path = Paths.get(record.path).toAbsolutePath().normalize(),
                            timestamp = record.timestamp,
                        )
                    } catch (_: Exception) {
                        null
                    }
                }
            } catch (e: Exception) {
                // Log error but return empty list to avoid crashing plugin
                System.err.println("BookmarkStorage: Failed to load bookmarks: ${e.message}")
                emptyList()
            }
        }
    }

    /**
     * Save bookmarks to persistent storage.
     * Overwrites existing file.
     */
    fun saveBookmarks(bookmarks: List<Bookmark>): Boolean {
        synchronized(lock) {
            return try {
                val parent = bookmarksFile.parent
                if (parent != null && !Files.exists(parent)) {
                    Files.createDirectories(parent)
                }

                val records = bookmarks.map { bookmark ->
                    BookmarkRecord(
                        name = bookmark.name,
                        path = bookmark.path.toAbsolutePath().normalize().toString(),
                        timestamp = bookmark.timestamp,
                    )
                }

                val json = gson.toJson(records)
                Files.writeString(bookmarksFile, json)
                true
            } catch (e: Exception) {
                System.err.println("BookmarkStorage: Failed to save bookmarks: ${e.message}")
                false
            }
        }
    }

    /**
     * Add a new bookmark, replacing any existing bookmark with the same name.
     */
    fun addBookmark(bookmark: Bookmark): Boolean {
        val existingBookmarks = loadBookmarks()
        val updatedBookmarks = existingBookmarks.filterNot { it.name == bookmark.name } + bookmark
        return saveBookmarks(updatedBookmarks)
    }

    /**
     * Remove a bookmark by name.
     */
    fun removeBookmark(name: String): Boolean {
        val existingBookmarks = loadBookmarks()
        val updatedBookmarks = existingBookmarks.filterNot { it.name == name }
        return saveBookmarks(updatedBookmarks)
    }

    /**
     * Update an existing bookmark.
     */
    fun updateBookmark(bookmark: Bookmark): Boolean {
        val existingBookmarks = loadBookmarks()
        val updatedBookmarks = existingBookmarks.filterNot { it.name == bookmark.name } + bookmark
        return saveBookmarks(updatedBookmarks)
    }

    /**
     * Find a bookmark by name (case-sensitive).
     */
    fun findBookmark(name: String): Bookmark? {
        return loadBookmarks().find { it.name == name }
    }

    /**
     * Get all bookmarks.
     */
    fun getAllBookmarks(): List<Bookmark> {
        return loadBookmarks()
    }

    /**
     * Get bookmarks that match a specific directory path.
     */
    fun getBookmarksForPath(path: Path): List<Bookmark> {
        return loadBookmarks().filter { it.path == path }
    }

    /**
     * Get bookmarks that match a specific directory path (case-insensitive).
     */
    fun getBookmarksForPathIgnoreCase(path: Path): List<Bookmark> {
        return loadBookmarks().filter { it.path.toString().equals(path.toString(), ignoreCase = true) }
    }

    /**
     * Check if a path is bookmarked.
     */
    fun isBookmarked(path: Path): Boolean {
        return getBookmarksForPath(path).isNotEmpty()
    }

    /**
     * Get the storage file path.
     */
    private fun getStorageFilePath(): Path {
        // Use IDE plugin storage if available, fallback to user home
        val pluginStorage = System.getProperty("idea.plugins.path")
        return if (pluginStorage != null) {
            Paths.get(pluginStorage, STORAGE_FILE)
        } else {
            Paths.get(System.getProperty("user.home"), ".config", STORAGE_FILE)
        }
    }

    /**
     * Clear all bookmarks (for testing or reset).
     */
    fun clearAllBookmarks() {
        synchronized(lock) {
            try {
                if (Files.exists(bookmarksFile)) {
                    Files.delete(bookmarksFile)
                }
            } catch (e: Exception) {
                System.err.println("BookmarkStorage: Failed to clear bookmarks: ${e.message}")
            }
        }
    }

    /**
     * Get the number of bookmarks.
     */
    fun getBookmarkCount(): Int {
        return loadBookmarks().size
    }

    fun getStoragePath(): Path {
        return bookmarksFile
    }
}
