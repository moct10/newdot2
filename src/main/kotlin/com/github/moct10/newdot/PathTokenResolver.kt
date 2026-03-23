package com.github.moct10.newdot

internal object PathTokenResolver {
  fun extractPathTokenFromLine(lineText: String, cursorInLine: Int): String? {
    if (lineText.isBlank()) return null

    val safeCursor = cursorInLine.coerceIn(0, lineText.length - 1)
    val anchor = when {
      !isTokenBoundary(lineText[safeCursor]) -> safeCursor
      safeCursor + 1 < lineText.length && !isTokenBoundary(lineText[safeCursor + 1]) -> safeCursor + 1
      safeCursor > 0 && !isTokenBoundary(lineText[safeCursor - 1]) -> safeCursor - 1
      else -> return null
    }

    var start = anchor
    while (start > 0 && !isTokenBoundary(lineText[start - 1])) start--
    var endExclusive = anchor + 1
    while (endExclusive < lineText.length && !isTokenBoundary(lineText[endExclusive])) endExclusive++

    return sanitizePathToken(lineText.substring(start, endExclusive))
  }

  fun normalizeExPathArgument(argument: String): String {
    var normalized = argument.trim()
    if (normalized.length >= 2) {
      val first = normalized.first()
      val last = normalized.last()
      if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
        normalized = normalized.substring(1, normalized.length - 1)
      }
    }

    if (!normalized.contains('\\')) return normalized

    val decoded = StringBuilder(normalized.length)
    var index = 0
    while (index < normalized.length) {
      val ch = normalized[index]
      if (ch == '\\' && index + 1 < normalized.length) {
        val next = normalized[index + 1]
        if (next == ' ' || next == '\\' || next == '|' || next == '"' || next == '\'') {
          decoded.append(next)
          index += 2
          continue
        }
      }
      decoded.append(ch)
      index++
    }
    return decoded.toString()
  }

  private fun sanitizePathToken(raw: String): String? {
    var token = raw.trim()
    if (token.isEmpty()) return null

    val leadingTrim = charArrayOf('"', '\'', '`', '(', '[', '{', '<')
    while (token.isNotEmpty() && leadingTrim.contains(token.first())) {
      token = token.substring(1)
    }

    val trailingTrim = charArrayOf('"', '\'', '`', ')', ']', '}', '>', ',', ';')
    while (token.isNotEmpty() && trailingTrim.contains(token.last())) {
      token = token.dropLast(1)
    }

    if (token.endsWith(":") && !(token.length == 2 && token[0].isLetter())) {
      token = token.dropLast(1)
    }

    if (token.isEmpty()) return null
    val normalized = normalizeExPathArgument(token)
    return normalized.ifBlank { null }
  }

  private fun isTokenBoundary(ch: Char): Boolean {
    if (ch.isWhitespace()) return true
    return ch == '(' || ch == ')' ||
      ch == '[' || ch == ']' ||
      ch == '{' || ch == '}' ||
      ch == '<' || ch == '>' ||
      ch == '"' || ch == '\'' || ch == '`' ||
      ch == ',' || ch == ';'
  }
}
