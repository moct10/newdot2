# IdeaVim New Dot Explorer

This plugin adds `:new <path>` support to IdeaVim and opens a file explorer in an editor split (not the Project tool window).

## What it does

- Adds a `:NewDot` command handler.
- Adds a `:NewDotEdit` command handler.
- Adds a `:NewDotTab` command handler.
- Maps command-line `new` to `NewDot`, so typing `:new ...` routes to the plugin command.
- Maps command-line `e` / `edit` to `NewDotEdit`, so `:e .` replaces current view with the explorer.
- Maps command-line `tabe` / `tabedit` to `NewDotTab`, so `:tabe .` opens explorer in a new tab.
- Directory arguments work with absolute, relative, and home paths (for example `:new ~`, `:e ~/Downloads`, `:tabe /tmp`).
- For directory arguments (`:new .`, `:new /path`), opens an editor split with a directory listing.
- Entries are displayed with relative paths (`./file`, `./dir/`), plus `./` (current) and `../` (parent).
- Explorer header uses an `========== ... ==========` title line.
- Current folder is shown as absolute (`# root:`) and project-relative (`# root(project):`) paths.
- The explorer uses NERDTree-like coloring: headers are muted, directories are blue/orange, files are green.
- Each `:new .` explorer opens in its own buffer, so multiple folders can be browsed at once.
- Explorer uses scratch-backed buffers (so IdeaVim motions/yank work), and plugin auto-cleans old `newdot-explorer-*` scratch files on startup and when buffers close.
- Explorer buffer is read-only to avoid accidental edits (plugin still updates it for navigation).
- Relative paths are resolved from the displayed `# root:` directory.
- In the listing buffer, `<Enter>` (or `o`) on a directory (`[d]` / `[u]`) changes the listing in-place to that directory.
- In the listing buffer, `~` jumps to your home directory.
- In the listing buffer, `p` jumps to the project root directory.
- In the listing buffer, `y` yanks the file or folder path under the cursor into the same register as `yy` (so `p` works in another IdeaVim window).
- In the listing buffer, `<Enter>` (or `o`) on a file (`[f]`) opens that file in place (replaces the explorer in that split).
- In the listing buffer, `t` on a file opens it in a new tab.
- In the listing buffer, `s` on a file opens it in a horizontal split.
- Sorting in explorer buffer:
  - `N`: sort by name (ascending)
  - `T`: sort by file type/extension (ascending)
  - `M`: sort by last modified time (newest first)
  - `S`: sort by size (largest first)
- For non-directory arguments, falls back to normal `:split`.

## Build

This project does not include a Gradle wrapper, so use a local Gradle installation.

```bash
gradle buildPlugin
```

The plugin ZIP will be generated under:

`build/distributions/`

## Install

1. Open `Settings > Plugins` in your JetBrains IDE.
2. Click the gear icon and choose `Install Plugin from Disk...`.
3. Select the ZIP from `build/distributions/`.
4. Restart the IDE.

## Enable in IdeaVim

Add this line to your `~/.ideavimrc`:

```vim
set newdot
```

After restarting (or reloading IdeaVim config), you can run:

```vim
:new .
:new /path/to/dir
:new ~/Downloads
```

This stays in editor splits and does not use the Project tool window.
