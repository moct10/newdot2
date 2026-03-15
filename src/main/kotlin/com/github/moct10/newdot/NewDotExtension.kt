package com.github.moct10.newdot

import com.intellij.ide.scratch.ScratchFileService
import com.intellij.ide.scratch.ScratchRootType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.intellij.openapi.fileTypes.PlainTextLanguage
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.ui.JBColor
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.MappingMode
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.common.CommandAliasHandler
import com.maddyhome.idea.vim.ex.ranges.Range
import com.maddyhome.idea.vim.extension.ExtensionHandler
import com.maddyhome.idea.vim.extension.VimExtension
import com.maddyhome.idea.vim.extension.VimExtensionFacade
import com.maddyhome.idea.vim.newapi.ij
import com.maddyhome.idea.vim.newapi.vim
import com.maddyhome.idea.vim.state.mode.SelectionType
import java.awt.Font
import java.io.IOException
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.InvalidPathException
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import javax.swing.KeyStroke
import javax.swing.SwingConstants

class NewDotExtension : VimExtension {
  override fun getName(): String = EXTENSION_NAME

  override fun init() {
    VimExtensionFacade.addCommand("NewDot", 0, 1, NewCommandHandler())
    VimExtensionFacade.addCommand("NewDotEdit", 0, 1, EditCommandHandler())
    VimExtensionFacade.addCommand("NewDotTab", 0, 1, TabCommandHandler())
    VimExtensionFacade.addCommand("Netrw", 0, 1, NetrwCommandHandler())
    VimExtensionFacade.addCommand("NewDotOpenLine", OpenLineCommandHandler())
    installExplorerKeyMappings()
    VimExtensionFacade.addCommand("new", 0, 1, NewCommandHandler())
    VimExtensionFacade.addCommand("e", 0, 1, EditCommandHandler())
    VimExtensionFacade.addCommand("edit", 0, 1, EditCommandHandler())
    VimExtensionFacade.addCommand("tabe", 0, 1, TabCommandHandler())
    VimExtensionFacade.addCommand("tabedit", 0, 1, TabCommandHandler())
  }


  private fun installExplorerKeyMappings() {
    if (explorerKeyMappingInstalled) return

    VimExtensionFacade.putExtensionHandlerMapping(
      MappingMode.N,
      injector.parser.parseKeys("<CR>"),
      owner,
      ExplorerLineOpenOrFallbackHandler(injector.parser.parseKeys("<CR>"), ExplorerOpenMode.CURRENT),
      false,
    )
    VimExtensionFacade.putExtensionHandlerMapping(
      MappingMode.N,
      injector.parser.parseKeys("o"),
      owner,
      ExplorerLineOpenOrFallbackHandler(injector.parser.parseKeys("o"), ExplorerOpenMode.CURRENT),
      false,
    )
    VimExtensionFacade.putExtensionHandlerMapping(
      MappingMode.N,
      injector.parser.parseKeys("t"),
      owner,
      ExplorerLineOpenOrFallbackHandler(injector.parser.parseKeys("t"), ExplorerOpenMode.NEW_TAB),
      false,
    )
    VimExtensionFacade.putExtensionHandlerMapping(
      MappingMode.N,
      injector.parser.parseKeys("s"),
      owner,
      ExplorerLineOpenOrFallbackHandler(injector.parser.parseKeys("s"), ExplorerOpenMode.HORIZONTAL_SPLIT),
      false,
    )
    VimExtensionFacade.putExtensionHandlerMapping(
      MappingMode.N,
      injector.parser.parseKeys("N"),
      owner,
      ExplorerSortOrFallbackHandler(injector.parser.parseKeys("N"), ExplorerSortMode.NAME),
      false,
    )
    VimExtensionFacade.putExtensionHandlerMapping(
      MappingMode.N,
      injector.parser.parseKeys("T"),
      owner,
      ExplorerSortOrFallbackHandler(injector.parser.parseKeys("T"), ExplorerSortMode.TYPE),
      false,
    )
    VimExtensionFacade.putExtensionHandlerMapping(
      MappingMode.N,
      injector.parser.parseKeys("M"),
      owner,
      ExplorerSortOrFallbackHandler(injector.parser.parseKeys("M"), ExplorerSortMode.MTIME),
      false,
    )
    VimExtensionFacade.putExtensionHandlerMapping(
      MappingMode.N,
      injector.parser.parseKeys("S"),
      owner,
      ExplorerSortOrFallbackHandler(injector.parser.parseKeys("S"), ExplorerSortMode.SIZE),
      false,
    )
    VimExtensionFacade.putExtensionHandlerMapping(
      MappingMode.N,
      injector.parser.parseKeys("-"),
      owner,
      ExplorerNavigateUpOrFallbackHandler(injector.parser.parseKeys("-")),
      false,
    )
    VimExtensionFacade.putExtensionHandlerMapping(
      MappingMode.N,
      injector.parser.parseKeys("~"),
      owner,
      ExplorerNavigateHomeOrFallbackHandler(injector.parser.parseKeys("~")),
      false,
    )
    VimExtensionFacade.putExtensionHandlerMapping(
      MappingMode.N,
      injector.parser.parseKeys("p"),
      owner,
      ExplorerNavigateProjectRootOrFallbackHandler(injector.parser.parseKeys("p")),
      false,
    )
    VimExtensionFacade.putExtensionHandlerMapping(
      MappingMode.N,
      injector.parser.parseKeys("D"),
      owner,
      ExplorerDeleteOrFallbackHandler(injector.parser.parseKeys("D")),
      false,
    )
    VimExtensionFacade.putExtensionHandlerMapping(
      MappingMode.N,
      injector.parser.parseKeys("R"),
      owner,
      ExplorerRenameOrFallbackHandler(injector.parser.parseKeys("R")),
      false,
    )
    VimExtensionFacade.putExtensionHandlerMapping(
      MappingMode.N,
      injector.parser.parseKeys("%"),
      owner,
      ExplorerCreateFileOrFallbackHandler(injector.parser.parseKeys("%")),
      false,
    )
    VimExtensionFacade.putExtensionHandlerMapping(
      MappingMode.N,
      injector.parser.parseKeys("d"),
      owner,
      ExplorerCreateDirectoryOrFallbackHandler(injector.parser.parseKeys("d")),
      false,
    )
    VimExtensionFacade.putExtensionHandlerMapping(
      MappingMode.N,
      injector.parser.parseKeys("y"),
      owner,
      ExplorerYankOrFallbackHandler(injector.parser.parseKeys("y")),
      false,
    )

    explorerKeyMappingInstalled = true
  }

  private class NewCommandHandler : CommandAliasHandler {
    override fun execute(command: String, range: Range, editor: VimEditor, context: ExecutionContext) {
      val arguments = command.substringAfter(' ', "").trim()
      if (arguments.isEmpty()) {
        runExCommand("split", editor, context)
        return
      }

      val project = editor.ij.project ?: return
      val baseDirectory = resolveBaseDirectory(editor, project)
      val targetPath = resolvePath(arguments, baseDirectory)
      if (targetPath != null && Files.exists(targetPath) && Files.isDirectory(targetPath)) {
        ExplorerBuffer.open(project, targetPath, editor)
        return
      }

      runExCommand("split ${escapeExArgument(arguments)}", editor, context)
    }
  }

  private class OpenLineCommandHandler : CommandAliasHandler {
    override fun execute(command: String, range: Range, editor: VimEditor, context: ExecutionContext) {
      val project = editor.ij.project ?: return
      ExplorerBuffer.openFromLineUnderCursor(project, editor, context, ExplorerOpenMode.CURRENT)
    }
  }

  private enum class ExplorerOpenMode {
    CURRENT,
    NEW_TAB,
    HORIZONTAL_SPLIT,
  }

  private enum class ExplorerSortMode(val id: String) {
    NAME("name"),
    TYPE("type"),
    MTIME("mtime"),
    SIZE("size");

    companion object {
      fun fromId(id: String): ExplorerSortMode {
        return entries.firstOrNull { mode -> mode.id == id.lowercase() } ?: NAME
      }
    }
  }

  private class EditCommandHandler : CommandAliasHandler {
    override fun execute(command: String, range: Range, editor: VimEditor, context: ExecutionContext) {
      val arguments = command.substringAfter(' ', "").trim()
      if (arguments.isEmpty()) {
        runExCommand("edit", editor, context)
        return
      }

      val project = editor.ij.project ?: return
      val baseDirectory = resolveBaseDirectory(editor, project)
      val targetPath = resolvePath(arguments, baseDirectory)
      if (targetPath != null && Files.exists(targetPath) && Files.isDirectory(targetPath)) {
        ExplorerBuffer.openReplacingCurrentView(project, targetPath, editor)
        return
      }

      runExCommand("edit ${escapeExArgument(arguments)}", editor, context)
    }
  }

  private class TabCommandHandler : CommandAliasHandler {
    override fun execute(command: String, range: Range, editor: VimEditor, context: ExecutionContext) {
      val arguments = command.substringAfter(' ', "").trim()
      if (arguments.isEmpty()) {
        runExCommand("tabnew", editor, context)
        return
      }

      val project = editor.ij.project ?: return
      val baseDirectory = resolveBaseDirectory(editor, project)
      val targetPath = resolvePath(arguments, baseDirectory)
      if (targetPath != null && Files.exists(targetPath) && Files.isDirectory(targetPath)) {
        ExplorerBuffer.openInTab(project, targetPath, editor)
        return
      }

      runExCommand("tabedit ${escapeExArgument(arguments)}", editor, context)
    }
  }

  private class NetrwCommandHandler : CommandAliasHandler {
    override fun execute(command: String, range: Range, editor: VimEditor, context: ExecutionContext) {
      val project = editor.ij.project ?: return
      val arguments = command.substringAfter(' ', "").trim()
      val baseDirectory = resolveBaseDirectory(editor, project)
      val targetPath = if (arguments.isBlank()) baseDirectory else resolvePath(arguments, baseDirectory)
      if (targetPath != null && Files.exists(targetPath) && Files.isDirectory(targetPath)) {
        ExplorerBuffer.openInTab(project, targetPath, editor)
        return
      }

      showError(editor, "newdot: Not a directory: ${targetPath ?: arguments}")
    }
  }

  private class ExplorerLineOpenOrFallbackHandler(
    private val fallbackKeys: List<KeyStroke>,
    private val openMode: ExplorerOpenMode,
  ) : ExtensionHandler {
    override fun execute(editor: VimEditor, context: ExecutionContext, operatorArguments: OperatorArguments) {
      val project = editor.ij.project
      if (project != null && isExplorerBuffer(editor)) {
        ExplorerBuffer.openFromLineUnderCursor(project, editor, context, openMode)
        return
      }
      VimExtensionFacade.executeNormalWithoutMapping(fallbackKeys, editor.ij)
    }
  }

  private class ExplorerSortOrFallbackHandler(
    private val fallbackKeys: List<KeyStroke>,
    private val sortMode: ExplorerSortMode,
  ) : ExtensionHandler {
    override fun execute(editor: VimEditor, context: ExecutionContext, operatorArguments: OperatorArguments) {
      val project = editor.ij.project
      if (project != null && isExplorerBuffer(editor)) {
        ExplorerBuffer.applySortMode(project, editor, sortMode)
        return
      }
      VimExtensionFacade.executeNormalWithoutMapping(fallbackKeys, editor.ij)
    }
  }

  private class ExplorerNavigateUpOrFallbackHandler(
    private val fallbackKeys: List<KeyStroke>,
  ) : ExtensionHandler {
    override fun execute(editor: VimEditor, context: ExecutionContext, operatorArguments: OperatorArguments) {
      val project = editor.ij.project
      if (project != null && isExplorerBuffer(editor)) {
        ExplorerBuffer.navigateUp(project, editor)
        return
      }
      VimExtensionFacade.executeNormalWithoutMapping(fallbackKeys, editor.ij)
    }
  }

  private class ExplorerNavigateHomeOrFallbackHandler(
    private val fallbackKeys: List<KeyStroke>,
  ) : ExtensionHandler {
    override fun execute(editor: VimEditor, context: ExecutionContext, operatorArguments: OperatorArguments) {
      val project = editor.ij.project
      if (project != null && isExplorerBuffer(editor)) {
        ExplorerBuffer.navigateHome(project, editor)
        return
      }
      VimExtensionFacade.executeNormalWithoutMapping(fallbackKeys, editor.ij)
    }
  }

  private class ExplorerNavigateProjectRootOrFallbackHandler(
    private val fallbackKeys: List<KeyStroke>,
  ) : ExtensionHandler {
    override fun execute(editor: VimEditor, context: ExecutionContext, operatorArguments: OperatorArguments) {
      val project = editor.ij.project
      if (project != null && isExplorerBuffer(editor)) {
        ExplorerBuffer.navigateProjectRoot(project, editor)
        return
      }
      VimExtensionFacade.executeNormalWithoutMapping(fallbackKeys, editor.ij)
    }
  }

  private class ExplorerDeleteOrFallbackHandler(
    private val fallbackKeys: List<KeyStroke>,
  ) : ExtensionHandler {
    override fun execute(editor: VimEditor, context: ExecutionContext, operatorArguments: OperatorArguments) {
      val project = editor.ij.project
      if (project != null && isExplorerBuffer(editor)) {
        ExplorerBuffer.deleteUnderCursor(project, editor)
        return
      }
      VimExtensionFacade.executeNormalWithoutMapping(fallbackKeys, editor.ij)
    }
  }

  private class ExplorerRenameOrFallbackHandler(
    private val fallbackKeys: List<KeyStroke>,
  ) : ExtensionHandler {
    override fun execute(editor: VimEditor, context: ExecutionContext, operatorArguments: OperatorArguments) {
      val project = editor.ij.project
      if (project != null && isExplorerBuffer(editor)) {
        ExplorerBuffer.renameUnderCursor(project, editor)
        return
      }
      VimExtensionFacade.executeNormalWithoutMapping(fallbackKeys, editor.ij)
    }
  }

  private class ExplorerCreateFileOrFallbackHandler(
    private val fallbackKeys: List<KeyStroke>,
  ) : ExtensionHandler {
    override fun execute(editor: VimEditor, context: ExecutionContext, operatorArguments: OperatorArguments) {
      val project = editor.ij.project
      if (project != null && isExplorerBuffer(editor)) {
        ExplorerBuffer.createFileInCurrentDirectory(project, editor)
        return
      }
      VimExtensionFacade.executeNormalWithoutMapping(fallbackKeys, editor.ij)
    }
  }

  private class ExplorerCreateDirectoryOrFallbackHandler(
    private val fallbackKeys: List<KeyStroke>,
  ) : ExtensionHandler {
    override fun execute(editor: VimEditor, context: ExecutionContext, operatorArguments: OperatorArguments) {
      val project = editor.ij.project
      if (project != null && isExplorerBuffer(editor)) {
        ExplorerBuffer.createDirectoryInCurrentDirectory(project, editor)
        return
      }
      VimExtensionFacade.executeNormalWithoutMapping(fallbackKeys, editor.ij)
    }
  }

  private class ExplorerYankOrFallbackHandler(
    private val fallbackKeys: List<KeyStroke>,
  ) : ExtensionHandler {
    override fun execute(editor: VimEditor, context: ExecutionContext, operatorArguments: OperatorArguments) {
      val project = editor.ij.project
      if (project != null && isExplorerBuffer(editor)) {
        ExplorerBuffer.yankUnderCursor(project, editor, context)
        return
      }
      VimExtensionFacade.executeNormalWithoutMapping(fallbackKeys, editor.ij)
    }
  }

  private object ExplorerBuffer {
    private data class ExplorerEntry(
      val path: Path,
      val isDirectory: Boolean,
      val nameLower: String,
      val typeLower: String,
      val lastModifiedMillis: Long,
      val sizeBytes: Long,
    )

    private data class CursorEntry(
      val kind: Char,
      val targetText: String,
      val path: Path,
    )

    fun open(project: Project, directory: Path, editorForMessages: VimEditor) {
      val root = directory.toAbsolutePath().normalize()
      if (!Files.exists(root) || !Files.isDirectory(root)) {
        showError(editorForMessages, "newdot: Not a directory: $root")
        return
      }
      installCleanupListener(project)

      val explorer = ScratchRootType.getInstance().createScratchFile(
        project,
        scratchFileName(root),
        PlainTextLanguage.INSTANCE,
        render(root, project, ExplorerSortMode.NAME),
        ScratchFileService.Option.create_new_always,
      ) ?: run {
        showError(editorForMessages, "newdot: Could not create explorer buffer")
        return
      }

      val explorerDocument = FileDocumentManager.getInstance().getDocument(explorer)
      if (explorerDocument == null) {
        showError(editorForMessages, "newdot: Could not access explorer buffer document")
        return
      }
      explorer.putUserData(EXPLORER_FILE_KEY, true)
      setExplorerReadOnly(explorerDocument, true)

      val editorManagerEx = FileEditorManagerEx.getInstanceEx(project)
      val currentWindow = editorManagerEx.currentWindow
      if (currentWindow != null) {
        if (!splitWindow(currentWindow, explorer, SwingConstants.HORIZONTAL, true)) {
          editorManagerEx.openFile(explorer, true)
        }
      } else {
        editorManagerEx.openFile(explorer, true)
      }
      scheduleHighlighting(project, explorerDocument)
    }

    fun openFromLineUnderCursor(
      project: Project,
      editor: VimEditor,
      context: ExecutionContext,
      openMode: ExplorerOpenMode = ExplorerOpenMode.CURRENT,
    ) {
      val ijEditor = editor.ij
      val entry = resolveEntryUnderCursor(project, editor) ?: return
      val currentSortMode = readSortMode(ijEditor.document)
      val kind = entry.kind
      val targetPath = entry.path

      if (kind == 'u' || kind == 'd') {
        if (!Files.isDirectory(targetPath)) {
          showError(editor, "newdot: Not a directory: $targetPath")
          return
        }
        replaceExplorerContents(project, ijEditor.document, targetPath.toAbsolutePath().normalize(), currentSortMode)
        return
      }

      val targetPathString = targetPath.toString()
      val escapedTarget = escapeExArgument(targetPathString)

      when (openMode) {
        ExplorerOpenMode.NEW_TAB -> {
          // Some IdeaVim builds reject `tabedit` in programmatic execution even if `:tabe` works via mappings.
          // Use `tabnew` + `edit` for broader compatibility.
          runExCommand("tabnew", editor, context)
          runExCommand("edit $escapedTarget", editor, context)
          return
        }
        ExplorerOpenMode.HORIZONTAL_SPLIT -> {
          runExCommand("split $escapedTarget", editor, context)
          return
        }
        ExplorerOpenMode.CURRENT -> Unit
      }

      val targetVirtualFile = LocalFileSystem.getInstance()
        .refreshAndFindFileByPath(targetPathString.replace('\\', '/'))
      if (targetVirtualFile != null) {
        val editorManager = FileEditorManagerEx.getInstanceEx(project)
        val explorerVirtualFile = FileDocumentManager.getInstance().getFile(ijEditor.document)
        val explorerWindow = explorerVirtualFile?.let { sourceFile ->
          editorManager.windows.firstOrNull { window -> window.selectedFile == sourceFile }
            ?: editorManager.windows.firstOrNull { window -> window.isFileOpen(sourceFile) }
        } ?: editorManager.currentWindow

        if (explorerWindow != null && openFileInWindow(editorManager, targetVirtualFile, explorerWindow)) {
          if (explorerVirtualFile != null && explorerWindow.isFileOpen(explorerVirtualFile)) {
            explorerWindow.closeFile(explorerVirtualFile, false, false)
          }
          return
        }
      }

      runExCommand("bd! | edit $escapedTarget", editor, context)
    }

    fun openReplacingCurrentView(project: Project, directory: Path, editorForMessages: VimEditor) {
      val root = directory.toAbsolutePath().normalize()
      if (!Files.exists(root) || !Files.isDirectory(root)) {
        showError(editorForMessages, "newdot: Not a directory: $root")
        return
      }
      installCleanupListener(project)

      val explorer = ScratchRootType.getInstance().createScratchFile(
        project,
        scratchFileName(root),
        PlainTextLanguage.INSTANCE,
        render(root, project, ExplorerSortMode.NAME),
        ScratchFileService.Option.create_new_always,
      ) ?: run {
        showError(editorForMessages, "newdot: Could not create explorer buffer")
        return
      }

      val explorerDocument = FileDocumentManager.getInstance().getDocument(explorer)
      if (explorerDocument == null) {
        showError(editorForMessages, "newdot: Could not access explorer buffer document")
        return
      }
      explorer.putUserData(EXPLORER_FILE_KEY, true)
      setExplorerReadOnly(explorerDocument, true)

      val editorManager = FileEditorManagerEx.getInstanceEx(project)
      val currentWindow = editorManager.currentWindow
      val previousFile = currentWindow?.selectedFile

      if (currentWindow != null && openFileInWindow(editorManager, explorer, currentWindow)) {
        if (previousFile != null && previousFile != explorer && currentWindow.isFileOpen(previousFile)) {
          currentWindow.closeFile(previousFile, false, false)
        }
      } else {
        editorManager.openFile(explorer, true)
      }

      scheduleHighlighting(project, explorerDocument)
    }

    fun openInTab(project: Project, directory: Path, editorForMessages: VimEditor) {
      val root = directory.toAbsolutePath().normalize()
      if (!Files.exists(root) || !Files.isDirectory(root)) {
        showError(editorForMessages, "newdot: Not a directory: $root")
        return
      }
      installCleanupListener(project)

      val explorer = ScratchRootType.getInstance().createScratchFile(
        project,
        scratchFileName(root),
        PlainTextLanguage.INSTANCE,
        render(root, project, ExplorerSortMode.NAME),
        ScratchFileService.Option.create_new_always,
      ) ?: run {
        showError(editorForMessages, "newdot: Could not create explorer buffer")
        return
      }

      val explorerDocument = FileDocumentManager.getInstance().getDocument(explorer)
      if (explorerDocument == null) {
        showError(editorForMessages, "newdot: Could not access explorer buffer document")
        return
      }
      explorer.putUserData(EXPLORER_FILE_KEY, true)
      setExplorerReadOnly(explorerDocument, true)

      FileEditorManagerEx.getInstanceEx(project).openFile(explorer, true)
      scheduleHighlighting(project, explorerDocument)
    }

    fun applySortMode(project: Project, editor: VimEditor, sortMode: ExplorerSortMode) {
      val root = resolveBaseDirectory(editor, project).toAbsolutePath().normalize()
      if (!Files.exists(root) || !Files.isDirectory(root)) {
        showError(editor, "newdot: Not a directory: $root")
        return
      }
      replaceExplorerContents(project, editor.ij.document, root, sortMode)
    }

    fun navigateUp(project: Project, editor: VimEditor) {
      val root = resolveBaseDirectory(editor, project).toAbsolutePath().normalize()
      val parent = root.parent ?: return
      replaceExplorerContents(project, editor.ij.document, parent, readSortMode(editor.ij.document))
    }

    fun navigateHome(project: Project, editor: VimEditor) {
      val home = Paths.get(System.getProperty("user.home")).toAbsolutePath().normalize()
      if (!Files.exists(home) || !Files.isDirectory(home)) {
        showError(editor, "newdot: Not a directory: $home")
        return
      }
      replaceExplorerContents(project, editor.ij.document, home, readSortMode(editor.ij.document))
    }

    fun navigateProjectRoot(project: Project, editor: VimEditor) {
      val projectBase = project.basePath?.let { Paths.get(it).toAbsolutePath().normalize() } ?: run {
        showError(editor, "newdot: Project base path not available")
        return
      }
      if (!Files.exists(projectBase) || !Files.isDirectory(projectBase)) {
        showError(editor, "newdot: Not a directory: $projectBase")
        return
      }
      replaceExplorerContents(project, editor.ij.document, projectBase, readSortMode(editor.ij.document))
    }

    fun deleteUnderCursor(project: Project, editor: VimEditor) {
      val root = resolveBaseDirectory(editor, project).toAbsolutePath().normalize()
      val entry = resolveEntryUnderCursor(project, editor) ?: return
      if (entry.targetText == "./" || entry.targetText == "../") {
        showError(editor, "newdot: Cannot delete special entry: ${entry.targetText}")
        return
      }

      val target = entry.path
      val targetName = target.fileName?.toString() ?: target.toString()
      val message = if (Files.isDirectory(target)) {
        "Delete directory '$targetName' and all its contents?"
      } else {
        "Delete file '$targetName'?"
      }
      if (Messages.showYesNoDialog(project, message, "newdot delete", null) != Messages.YES) return

      try {
        deleteRecursively(target)
      } catch (e: IOException) {
        showError(editor, "newdot: Delete failed: ${e.message ?: "unknown error"}")
        return
      }

      replaceExplorerContents(project, editor.ij.document, root, readSortMode(editor.ij.document))
    }

    fun renameUnderCursor(project: Project, editor: VimEditor) {
      val root = resolveBaseDirectory(editor, project).toAbsolutePath().normalize()
      val entry = resolveEntryUnderCursor(project, editor) ?: return
      if (entry.targetText == "./" || entry.targetText == "../") {
        showError(editor, "newdot: Cannot rename special entry: ${entry.targetText}")
        return
      }

      val source = entry.path
      val sourceName = source.fileName?.toString() ?: return
      val newName = Messages.showInputDialog(project, "Rename '$sourceName' to:", "newdot rename", null)?.trim() ?: return
      if (newName.isEmpty()) {
        showError(editor, "newdot: Name cannot be empty")
        return
      }

      val target = source.resolveSibling(newName).normalize()
      if (target == source) return
      if (Files.exists(target)) {
        showError(editor, "newdot: Target already exists: $target")
        return
      }

      try {
        Files.move(source, target)
      } catch (e: IOException) {
        showError(editor, "newdot: Rename failed: ${e.message ?: "unknown error"}")
        return
      }

      replaceExplorerContents(project, editor.ij.document, root, readSortMode(editor.ij.document))
    }

    fun createFileInCurrentDirectory(project: Project, editor: VimEditor) {
      val root = resolveBaseDirectory(editor, project).toAbsolutePath().normalize()
      val name = Messages.showInputDialog(project, "New file name:", "newdot new file", null)?.trim() ?: return
      if (name.isEmpty()) {
        showError(editor, "newdot: Name cannot be empty")
        return
      }

      val target = resolvePath(name, root) ?: run {
        showError(editor, "newdot: Invalid file name: $name")
        return
      }
      if (Files.exists(target)) {
        showError(editor, "newdot: File already exists: $target")
        return
      }

      try {
        target.parent?.let { parent ->
          if (!Files.exists(parent)) Files.createDirectories(parent)
        }
        Files.createFile(target)
      } catch (e: IOException) {
        showError(editor, "newdot: Create file failed: ${e.message ?: "unknown error"}")
        return
      }

      replaceExplorerContents(project, editor.ij.document, root, readSortMode(editor.ij.document))
    }

    fun createDirectoryInCurrentDirectory(project: Project, editor: VimEditor) {
      val root = resolveBaseDirectory(editor, project).toAbsolutePath().normalize()
      val name = Messages.showInputDialog(project, "New directory name:", "newdot new dir", null)?.trim() ?: return
      if (name.isEmpty()) {
        showError(editor, "newdot: Name cannot be empty")
        return
      }

      val target = resolvePath(name, root) ?: run {
        showError(editor, "newdot: Invalid directory name: $name")
        return
      }
      if (Files.exists(target)) {
        showError(editor, "newdot: Directory already exists: $target")
        return
      }

      try {
        Files.createDirectories(target)
      } catch (e: IOException) {
        showError(editor, "newdot: Create directory failed: ${e.message ?: "unknown error"}")
        return
      }

      replaceExplorerContents(project, editor.ij.document, root, readSortMode(editor.ij.document))
    }

    fun yankUnderCursor(project: Project, editor: VimEditor, context: ExecutionContext) {
      val entry = resolveEntryUnderCursor(project, editor) ?: return
      val normalized = entry.path.toAbsolutePath().normalize()
      val text = if (Files.isDirectory(normalized)) "${normalized}/" else normalized.toString()
      val registerGroup = injector.registerGroup
      val registerChar = if (editor.ij.caretModel.caretCount == 1) {
        registerGroup.currentRegister
      } else {
        registerGroup.getCurrentRegisterForMulticaret()
      }
      val stored = registerGroup.storeText(editor, context, registerChar, text, SelectionType.LINE_WISE)
      if (!stored) {
        showError(editor, "newdot: Register not writable: $registerChar")
        return
      }
      showInfo(editor, "newdot: Yanked to register $registerChar")
    }

    private fun replaceExplorerContents(
      project: Project,
      document: Document,
      directory: Path,
      sortMode: ExplorerSortMode,
    ) {
      WriteCommandAction.runWriteCommandAction(project) {
        setExplorerReadOnly(document, false)
        document.setText(render(directory, project, sortMode))
        setExplorerReadOnly(document, true)
      }
      scheduleHighlighting(project, document)
    }

    private fun scheduleHighlighting(project: Project, document: Document, attempt: Int = 0) {
      val maxAttempts = 12
      ApplicationManager.getApplication().invokeLater {
        if (!isExplorerDocument(document)) return@invokeLater

        val editors = EditorFactory.getInstance().getEditors(document, project)
        if (editors.isEmpty()) {
          if (attempt < maxAttempts) scheduleHighlighting(project, document, attempt + 1)
          return@invokeLater
        }

        editors.forEach { editor -> applyHighlighting(editor, document) }
      }
    }

    private fun applyHighlighting(editor: Editor, document: Document) {
      clearExplorerHighlighting(editor)

      for (line in 0 until document.lineCount) {
        val lineText = readLine(document, line).trim()
        val attributes = when {
          lineText.startsWith("#") || lineText.startsWith("=") -> HEADER_ATTRIBUTES
          lineText.startsWith("[u] ") -> PARENT_DIRECTORY_ATTRIBUTES
          lineText.startsWith("[d] ") -> DIRECTORY_ATTRIBUTES
          lineText.startsWith("[f] ") -> FILE_ATTRIBUTES
          else -> null
        } ?: continue

        val start = document.getLineStartOffset(line)
        val end = document.getLineEndOffset(line)
        if (end <= start) continue

        val highlighter = editor.markupModel.addRangeHighlighter(
          start,
          end,
          HighlighterLayer.ADDITIONAL_SYNTAX,
          attributes,
          HighlighterTargetArea.EXACT_RANGE,
        )
        highlighter.putUserData(EXPLORER_HIGHLIGHT_KEY, true)
      }
    }

    private fun clearExplorerHighlighting(editor: Editor) {
      val markup = editor.markupModel
      val existing = markup.allHighlighters.filter { it.getUserData(EXPLORER_HIGHLIGHT_KEY) == true }
      existing.forEach { highlighter -> markup.removeHighlighter(highlighter) }
    }

    private fun render(directory: Path, project: Project, sortMode: ExplorerSortMode): String {
      val lines = mutableListOf<String>()
      lines += EXPLORER_HEADER
      lines += "# root: $directory"
      lines += "# root(project): ${projectRelativePath(directory, project)}"
      lines += "# sort: ${sortMode.id} (N:name T:type M:mtime--newest 1st S:size--largest 1st)"
      lines += "# o: open | -: up | ~: home | p: project root | t: tab | s: split | y: yank"
      lines += "# D: delete | R: rename | %: new file | d: new dir"
      lines += EXPLORER_HEADER_FOOTER
      lines += "[d] ./"
      if (directory.parent != null) {
        lines += "[u] ../"
      }

      val entries = mutableListOf<ExplorerEntry>()
      try {
        Files.newDirectoryStream(directory).use { stream ->
          stream.forEach { entry ->
            val normalized = entry.toAbsolutePath().normalize()
            val isDirectory = Files.isDirectory(normalized)
            val fileName = normalized.fileName?.toString() ?: normalized.toString()
            entries += ExplorerEntry(
              path = normalized,
              isDirectory = isDirectory,
              nameLower = fileName.lowercase(),
              typeLower = if (isDirectory) "" else fileTypeOf(fileName),
              lastModifiedMillis = safeLastModifiedMillis(normalized),
              sizeBytes = if (isDirectory) 0L else safeSizeBytes(normalized),
            )
          }
        }
      } catch (e: IOException) {
        lines += "# Failed to read directory: ${e.message ?: "unknown error"}"
        return lines.joinToString("\n")
      }

      entries.sortWith(sortComparator(sortMode))
      for (entry in entries) {
        if (entry.isDirectory) {
          lines += "[d] ./${entry.path.fileName}/"
        } else {
          lines += "[f] ./${entry.path.fileName}"
        }
      }
      return lines.joinToString("\n")
    }

    private fun sortComparator(sortMode: ExplorerSortMode): Comparator<ExplorerEntry> {
      return Comparator { left, right ->
        val byKind = compareValues(left.isDirectory, right.isDirectory)
        if (byKind != 0) return@Comparator byKind

        when (sortMode) {
          ExplorerSortMode.NAME -> left.nameLower.compareTo(right.nameLower)
          ExplorerSortMode.TYPE -> {
            val byType = left.typeLower.compareTo(right.typeLower)
            if (byType != 0) byType else left.nameLower.compareTo(right.nameLower)
          }
          ExplorerSortMode.MTIME -> {
            val byMtime = right.lastModifiedMillis.compareTo(left.lastModifiedMillis)
            if (byMtime != 0) byMtime else left.nameLower.compareTo(right.nameLower)
          }
          ExplorerSortMode.SIZE -> {
            val bySize = right.sizeBytes.compareTo(left.sizeBytes)
            if (bySize != 0) bySize else left.nameLower.compareTo(right.nameLower)
          }
        }
      }
    }

    private fun fileTypeOf(fileName: String): String {
      val dot = fileName.lastIndexOf('.')
      if (dot <= 0 || dot == fileName.lastIndex) return ""
      return fileName.substring(dot + 1).lowercase()
    }

    private fun safeLastModifiedMillis(path: Path): Long {
      return try {
        Files.getLastModifiedTime(path).toMillis()
      } catch (_: IOException) {
        0L
      }
    }

    private fun safeSizeBytes(path: Path): Long {
      return try {
        Files.size(path)
      } catch (_: IOException) {
        0L
      }
    }

    private fun readSortMode(document: Document): ExplorerSortMode {
      val maxHeaderLines = minOf(document.lineCount, 12)
      for (line in 0 until maxHeaderLines) {
        val text = readLine(document, line).trim()
        if (!text.startsWith("# sort: ")) continue
        val modeToken = text.removePrefix("# sort: ").trim().substringBefore(' ').substringBefore('(')
        return ExplorerSortMode.fromId(modeToken)
      }
      return ExplorerSortMode.NAME
    }

    private fun resolveEntryUnderCursor(project: Project, editor: VimEditor): CursorEntry? {
      val currentLine = readLine(editor.ij.document, editor.ij.caretModel.logicalPosition.line).trim()
      val parsed = when {
        currentLine.startsWith("[u] ") -> 'u' to currentLine.removePrefix("[u] ").trim()
        currentLine.startsWith("[d] ") -> 'd' to currentLine.removePrefix("[d] ").trim()
        currentLine.startsWith("[f] ") -> 'f' to currentLine.removePrefix("[f] ").trim()
        else -> null
      } ?: return null

      val (kind, targetText) = parsed
      val targetPath = resolvePath(targetText, resolveBaseDirectory(editor, project)) ?: return null
      if (!Files.exists(targetPath)) {
        showError(editor, "newdot: Path not found: $targetPath")
        return null
      }
      return CursorEntry(kind, targetText, targetPath)
    }

    private fun deleteRecursively(target: Path) {
      if (!Files.exists(target)) return
      if (!Files.isDirectory(target)) {
        Files.deleteIfExists(target)
        return
      }
      Files.walkFileTree(
        target,
        object : SimpleFileVisitor<Path>() {
          override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
            Files.deleteIfExists(file)
            return FileVisitResult.CONTINUE
          }

          override fun postVisitDirectory(dir: Path, exc: IOException?): FileVisitResult {
            if (exc != null) throw exc
            Files.deleteIfExists(dir)
            return FileVisitResult.CONTINUE
          }
        },
      )
    }

    private fun projectRelativePath(directory: Path, project: Project): String {
      val projectBase = project.basePath?.let { Paths.get(it).toAbsolutePath().normalize() } ?: return "(unknown)"
      val normalizedDirectory = directory.toAbsolutePath().normalize()
      return try {
        projectBase.relativize(normalizedDirectory).toString().replace('\\', '/').ifEmpty { "." }
      } catch (_: IllegalArgumentException) {
        normalizedDirectory.toString()
      }
    }

    private fun scratchFileName(root: Path): String {
      val tail = (root.fileName?.toString() ?: "root")
        .replace(INVALID_FILE_NAME_CHARS, "_")
        .ifBlank { "root" }
      return "$EXPLORER_FILE_PREFIX$tail-${System.nanoTime()}.txt"
    }

    private fun setExplorerReadOnly(document: Document, readOnly: Boolean) {
      document.setReadOnly(readOnly)
    }

    private fun installCleanupListener(project: Project) {
      if (project.getUserData(CLEANUP_LISTENER_INSTALLED_KEY) == true) return
      project.putUserData(CLEANUP_LISTENER_INSTALLED_KEY, true)

      cleanupOrphanedExplorerScratchFiles(project)

      project.messageBus.connect(project).subscribe(
        FileEditorManagerListener.FILE_EDITOR_MANAGER,
        object : FileEditorManagerListener {
          override fun fileClosed(source: FileEditorManager, file: com.intellij.openapi.vfs.VirtualFile) {
            if (file.getUserData(EXPLORER_FILE_KEY) != true && !file.name.startsWith(EXPLORER_FILE_PREFIX)) return
            if (source.isFileOpen(file) || !file.isValid) return
            try {
              WriteAction.run<Throwable> { file.delete(this) }
            } catch (_: Throwable) {
              // Ignore cleanup failures; orphaned scratch files are harmless.
            }
          }
        },
      )
    }

    private fun cleanupOrphanedExplorerScratchFiles(project: Project) {
      val scratchRoot = ScratchFileService.getInstance().getVirtualFile(ScratchRootType.getInstance()) ?: return
      val fileEditorManager = FileEditorManager.getInstance(project)
      val candidates = scratchRoot.children.filter { it.name.startsWith(EXPLORER_FILE_PREFIX) && !fileEditorManager.isFileOpen(it) }
      if (candidates.isEmpty()) return

      try {
        WriteAction.run<Throwable> {
          candidates.forEach { file ->
            if (file.isValid && !fileEditorManager.isFileOpen(file)) {
              file.delete(this)
            }
          }
        }
      } catch (_: Throwable) {
        // Ignore cleanup failures on startup.
      }
    }

  }

  companion object {
    private const val EXTENSION_NAME = "newdot"
    private val EXPLORER_HEADER = "${"=".repeat(10)} newdot explorer ${"=".repeat(10)}"
    private val EXPLORER_HEADER_FOOTER = "=".repeat(EXPLORER_HEADER.length)
    private const val EXPLORER_FILE_PREFIX = "newdot-explorer-"
    private val INVALID_FILE_NAME_CHARS = Regex("""[\\/:*?"<>|]""")
    private val CLEANUP_LISTENER_INSTALLED_KEY = Key.create<Boolean>("newdot.cleanup.listener.installed")
    private val EXPLORER_FILE_KEY = Key.create<Boolean>("newdot.explorer.file")
    private val EXPLORER_HIGHLIGHT_KEY = Key.create<Boolean>("newdot.explorer.highlight")
    private val HEADER_ATTRIBUTES = TextAttributes(JBColor(0x6B7280, 0x9CA3AF), null, null, null, Font.ITALIC)
    private val PARENT_DIRECTORY_ATTRIBUTES = TextAttributes(JBColor(0xB45309, 0xF59E0B), null, null, null, Font.BOLD)
    private val DIRECTORY_ATTRIBUTES = TextAttributes(JBColor(0x2563EB, 0x60A5FA), null, null, null, Font.BOLD)
    private val FILE_ATTRIBUTES = TextAttributes(JBColor(0x15803D, 0x86EFAC), null, null, null, Font.PLAIN)
    private var explorerKeyMappingInstalled = false

    private fun resolveBaseDirectory(editor: VimEditor, project: Project): Path {
      if (isExplorerBuffer(editor)) {
        val rootLine = readLine(editor.ij.document, 1).trim()
        if (rootLine.startsWith("# root: ")) {
          val rootPath = rootLine.removePrefix("# root: ").trim()
          if (rootPath.isNotEmpty()) {
            return Paths.get(rootPath)
          }
        }
      }

      val currentVirtualFile = FileDocumentManager.getInstance().getFile(editor.ij.document)
      if (currentVirtualFile != null && currentVirtualFile.isInLocalFileSystem) {
        val currentPath = Paths.get(currentVirtualFile.path)
        return if (Files.isDirectory(currentPath)) currentPath else currentPath.parent ?: currentPath
      }

      val projectBase = project.basePath
      if (!projectBase.isNullOrBlank()) return Paths.get(projectBase)

      return Paths.get(System.getProperty("user.home"))
    }

    private fun resolvePath(argument: String, baseDirectory: Path): Path? {
      val normalizedArgument = normalizeExPathArgument(argument)
      if (normalizedArgument.isBlank()) return null

      val home = Paths.get(System.getProperty("user.home"))
      val expanded = when {
        normalizedArgument == "~" -> home
        normalizedArgument.startsWith("~/") -> home.resolve(normalizedArgument.removePrefix("~/"))
        else -> {
          try {
            Paths.get(normalizedArgument)
          } catch (_: InvalidPathException) {
            return null
          }
        }
      }
      return if (expanded.isAbsolute) expanded.normalize() else baseDirectory.resolve(expanded).normalize()
    }

    private fun normalizeExPathArgument(argument: String): String {
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

    private fun openFileInWindow(
      editorManager: FileEditorManagerEx,
      targetFile: com.intellij.openapi.vfs.VirtualFile,
      window: com.intellij.openapi.fileEditor.impl.EditorWindow,
    ): Boolean {
      return try {
        val method = editorManager.javaClass.methods.firstOrNull { candidate ->
          candidate.name == "openFileWithProviders" &&
            candidate.parameterTypes.size == 3 &&
            candidate.parameterTypes[0].name == "com.intellij.openapi.vfs.VirtualFile" &&
            candidate.parameterTypes[1] == Boolean::class.javaPrimitiveType
        } ?: return false
        method.invoke(editorManager, targetFile, true, window)
        true
      } catch (_: Throwable) {
        false
      }
    }

    private fun splitWindow(
      window: com.intellij.openapi.fileEditor.impl.EditorWindow,
      file: com.intellij.openapi.vfs.VirtualFile,
      orientation: Int,
      focus: Boolean,
    ): Boolean {
      val methods = window.javaClass.methods
      val fourArg = methods.firstOrNull { candidate ->
        candidate.name == "split" &&
          candidate.parameterTypes.size == 4 &&
          candidate.parameterTypes[0] == Int::class.javaPrimitiveType &&
          candidate.parameterTypes[1] == Boolean::class.javaPrimitiveType &&
          candidate.parameterTypes[2].name == "com.intellij.openapi.vfs.VirtualFile" &&
          candidate.parameterTypes[3] == Boolean::class.javaPrimitiveType
      }
      if (fourArg != null) {
        return try {
          fourArg.invoke(window, orientation, true, file, focus)
          true
        } catch (_: Throwable) {
          false
        }
      }

      val fiveArg = methods.firstOrNull { candidate ->
        candidate.name == "split" &&
          candidate.parameterTypes.size == 5 &&
          candidate.parameterTypes[0] == Int::class.javaPrimitiveType &&
          candidate.parameterTypes[1] == Boolean::class.javaPrimitiveType &&
          candidate.parameterTypes[2].name == "com.intellij.openapi.vfs.VirtualFile" &&
          candidate.parameterTypes[3] == Boolean::class.javaPrimitiveType &&
          candidate.parameterTypes[4] == Boolean::class.javaPrimitiveType
      }
      if (fiveArg != null) {
        return try {
          fiveArg.invoke(window, orientation, true, file, focus, true)
          true
        } catch (_: Throwable) {
          false
        }
      }

      return false
    }

    private fun isExplorerBuffer(editor: VimEditor): Boolean {
      return readLine(editor.ij.document, 0).trim() == EXPLORER_HEADER
    }

    private fun isExplorerDocument(document: Document): Boolean {
      return readLine(document, 0).trim() == EXPLORER_HEADER
    }

    private fun runExCommand(
      command: String,
      editor: VimEditor,
      context: ExecutionContext,
      indicateErrors: Boolean = true,
    ) {
      injector.vimscriptExecutor.execute(command, editor, context, skipHistory = true, indicateErrors = indicateErrors)
    }

    private fun escapeExArgument(value: String): String {
      val escaped = StringBuilder(value.length * 2)
      for (ch in value) {
        if (ch == ' ' || ch == '\\' || ch == '|') escaped.append('\\')
        escaped.append(ch)
      }
      return escaped.toString()
    }

    private fun readLine(document: Document, line: Int): String {
      if (document.lineCount == 0) return ""
      val target = line.coerceIn(0, document.lineCount - 1)
      val start = document.getLineStartOffset(target)
      val end = document.getLineEndOffset(target)
      return document.getText(TextRange(start, end))
    }

    private fun showError(editor: VimEditor, message: String) {
      injector.messages.showStatusBarMessage(editor, message)
      injector.messages.indicateError()
    }

    private fun showInfo(editor: VimEditor, message: String) {
      injector.messages.showStatusBarMessage(editor, message)
    }
  }
}
