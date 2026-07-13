# Agent Instructions

General readme: @README.md

General contribution notes (also applies here): @CONTRIBUTING.md

We primarily target the Bukkit/Spigot API, but also support Paper. Other server variants are untested and not explicitly supported by us.

## Project Structure

This is a multi-module Gradle project (Java).

- `build.gradle`: Gradle build config, including shared configuration for the other sub modules.
- `CHANGELOG.md`: Changelog of all plugin versions.
- `VERSIONING.md`: Information about our version format. However, for major Minecraft updates, we usually also bump the plugin's major version component even if not strictly backwards incompatible.
- `Assumptions.md`: Implicit assumptions the plugin makes about the implementation of the underlying Minecraft servers that implement the Bukkit API.
- `scripts/`: Build and helper scripts.
- `modules/`: Sub-module projects.
- `modules/api`: Plugin API project (mostly interfaces for other add-on plugins to depend on).
- `modules/dist`: Bundles all relevant project outputs into the final plugin jar.
- `modules/main`: Core plugin code, targeting the lowest supported Bukkit API version.
- `modules/v*` and `modules/v*_paper`: Minecraft/Server-version specific module projects that implement logic that might differ across Minecraft versions and server variants (Spigot vs Paper).
- `modules/external-annotations`: Custom external Eclipse annotations for null checking.
- `modules/test`: Core plugin tests.
- `modules/shared`: Shared Gradle scripts, e.g. for reusing Gradle logic across version modules.

## Code Style

- Use CRLF line endings and UTF-8 (without BOM) as file encoding.
- Stick to the existing code style. Match the conventions of the surrounding code.
- The Eclipse formatter settings (applies to all projects!) live in the main module's `modules/main/.settings/org.eclipse.jdt.core.prefs`.
- Indentation uses **tabs** (tab size 4), not spaces.
- Soft line length limit: 100 columns (wrap if possible without making the code unreadable).
- Add an empty line after a closing code block (e.g. `if`/`else`/`for`/`while`/`try`, etc.) when more statements follow in the same scope.
- The same prefs file also holds compiler compliance and null-analysis settings; keep code compatible with them (e.g. respect the `@NonNull`/`@Nullable` null-analysis annotations).

## Code Comments

- Be concise. Keep them minimal.
- If needed, focus on unexpected design decisions / workarounds, not on why the code was placed at a particular place.
- Do not refer to design iteration discussions!
- Avoid em dashes!
- Start sentences in upper-case. Also use upper-case after colons `:`!
- Only describe the particular component/code element itself. Do not mention internal implementation details of other referenced code or implicitly coupled components!

## Minecraft Updates

When updating to a new Minecraft/server version, follow the dedicated `Update-Checklist.md` instructions.

## Additional Agent Instructions

- Keep changes minimal: Aim for the smallest possible Git diff. Avoid unrelated reformatting, renames, or refactoring.
- We store line endings as LF inside the Git repository, but check them out as CRLF on Windows (`autocrlf=true`).
  - You might be running inside a Linux sandbox/container. I still want you to create and edit files with CRLF line endings so they correctly show up with CRLF line endings on my host system.
  - When you are running inside a Linux sandbox/container, consider configuring Git `autocrlf=true` for commands like `git diff` to properly ignore line ending differences.
- Whenever you generated new code or created new files, manually verify afterwards that CRLF line endings and UTF-8 encoding is used.
- Make changes directly in the checked out repository. Do not create new workspaces or Git worktrees unless the user explicitly instructs you to.

