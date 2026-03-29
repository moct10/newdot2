package com.github.moct10.newdot

import org.junit.AfterClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path

class BookmarkStorageTest {
  @Before
  fun clearStorageBeforeEach() {
    BookmarkStorage.clearAllBookmarks()
  }

  @Test
  fun addBookmark_persistsAndLoads() {
    val root = testPluginsPath.resolve("project-a").toAbsolutePath().normalize()
    val bookmark = Bookmark("work", root, 123L)

    assertTrue(BookmarkStorage.addBookmark(bookmark))

    val all = BookmarkStorage.getAllBookmarks()
    assertEquals(1, all.size)
    assertEquals("work", all[0].name)
    assertEquals(root, all[0].path.toAbsolutePath().normalize())
    assertNotNull(BookmarkStorage.findBookmark("work"))
    assertTrue(Files.exists(BookmarkStorage.getStoragePath()))
  }

  @Test
  fun addBookmark_sameName_replacesExistingBookmark() {
    val first = Bookmark("work", testPluginsPath.resolve("first"), 1L)
    val second = Bookmark("work", testPluginsPath.resolve("second"), 2L)

    assertTrue(BookmarkStorage.addBookmark(first))
    assertTrue(BookmarkStorage.addBookmark(second))

    val all = BookmarkStorage.getAllBookmarks()
    assertEquals(1, all.size)
    assertEquals("work", all[0].name)
    assertEquals(second.path.toAbsolutePath().normalize(), all[0].path.toAbsolutePath().normalize())
    assertEquals(2L, all[0].timestamp)
  }

  @Test
  fun removeBookmark_persistsDeletion() {
    assertTrue(BookmarkStorage.addBookmark(Bookmark("keep", testPluginsPath.resolve("keep"), 1L)))
    assertTrue(BookmarkStorage.addBookmark(Bookmark("drop", testPluginsPath.resolve("drop"), 2L)))

    assertTrue(BookmarkStorage.removeBookmark("drop"))
    assertNull(BookmarkStorage.findBookmark("drop"))
    assertNotNull(BookmarkStorage.findBookmark("keep"))
    assertEquals(1, BookmarkStorage.getAllBookmarks().size)
    assertFalse(BookmarkStorage.getAllBookmarks().any { it.name == "drop" })
  }

  companion object {
    private lateinit var testPluginsPath: Path
    private var originalPluginsPath: String? = null

    @BeforeClass
    @JvmStatic
    fun setupClass() {
      testPluginsPath = Files.createTempDirectory("newdot-bookmark-tests")
      originalPluginsPath = System.getProperty("idea.plugins.path")
      System.setProperty("idea.plugins.path", testPluginsPath.toString())
      BookmarkStorage.clearAllBookmarks()
    }

    @AfterClass
    @JvmStatic
    fun tearDownClass() {
      BookmarkStorage.clearAllBookmarks()
      if (originalPluginsPath == null) {
        System.clearProperty("idea.plugins.path")
      } else {
        System.setProperty("idea.plugins.path", originalPluginsPath)
      }
      deleteRecursively(testPluginsPath)
    }

    private fun deleteRecursively(path: Path) {
      if (!Files.exists(path)) return
      Files.walk(path)
        .sorted(Comparator.reverseOrder())
        .forEach { Files.deleteIfExists(it) }
    }
  }
}

