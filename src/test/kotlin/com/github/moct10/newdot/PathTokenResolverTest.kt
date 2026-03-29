package com.github.moct10.newdot

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PathTokenResolverTest {
  @Test
  fun `extract token under cursor and strip wrappers`() {
    val line = """include("./foo/bar.txt",)"""
    val cursor = line.indexOf("bar")

    val token = PathTokenResolver.extractPathTokenFromLine(line, cursor)

    assertEquals("./foo/bar.txt", token)
  }

  @Test
  fun `extract token when cursor is on nearby whitespace`() {
    val line = "open ./dir/file.txt"
    val cursor = line.indexOf("./dir") - 1

    val token = PathTokenResolver.extractPathTokenFromLine(line, cursor)

    assertEquals("./dir/file.txt", token)
  }

  @Test
  fun `trim trailing punctuation colon for non-drive paths`() {
    val line = "src/Main.kt:"
    val token = PathTokenResolver.extractPathTokenFromLine(line, 3)
    assertEquals("src/Main.kt", token)
  }

  @Test
  fun `preserve windows drive root token`() {
    val line = "C:"
    val token = PathTokenResolver.extractPathTokenFromLine(line, 0)
    assertEquals("C:", token)
  }

  @Test
  fun `normalize escaped ex argument characters`() {
    val normalized = PathTokenResolver.normalizeExPathArgument("""a\ b\|c\\d\"e\'f""")
    assertEquals("""a b|c\d"e'f""", normalized)
  }

  @Test
  fun `returns null for blank line`() {
    assertNull(PathTokenResolver.extractPathTokenFromLine("   ", 0))
  }
}
