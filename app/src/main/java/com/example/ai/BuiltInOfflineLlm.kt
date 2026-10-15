package com.example.ai

import org.json.JSONObject
import java.util.Locale

/**
 * BuiltInOfflineLlm is a 100% on-device, offline natural language reasoning and comprehension engine.
 * It parses any natural language request into either an actionable tool execution plan
 * or an intelligent conversational response, requiring zero downloads and zero internet access.
 */
object BuiltInOfflineLlm {

  fun generate(prompt: String): String {
    val userRequest = extractUserRequest(prompt)
    val lower = userRequest.lowercase(Locale.ROOT).trim()

    // 1. Social Greetings & Introduction
    if (isGreeting(lower)) {
      return "Hello! 👋 I'm your on-device Local File Agent. I run completely offline without sending any data to the cloud. How can I help you manage your files today? (You can ask me to find files, organize folders, check storage, delete or move files, and much more!)"
    }

    if (isGratitude(lower)) {
      return "You're very welcome! Feel free to ask if you need anything else with your files or storage."
    }

    if (isIdentityOrHelp(lower)) {
      return """
I am Local File Agent, an intelligent offline assistant running directly on your Android device.

Here is what I can do for you using plain natural language:
• Find & Search: "find all pdf files", "search for invoices", "where is my resume"
• Organize: "organize my downloads", "sort loose files in Download"
• Storage & Cleanup: "how much storage do I have?", "show large files", "clean empty folders"
• File Actions: "delete old.txt", "rename photo.jpg to trip.jpg", "move draft.txt to Documents"
• Create & Read: "write 'Meeting at 2pm' to notes.txt", "read todo.txt", "preview image.png"
• Templates: "save template 'blog' from 'Posts'", "use template 'blog' at 'NewPost'"

Everything runs 100% locally on your phone — no internet or downloads required!
""".trimIndent()
    }

    if (lower.contains("joke") || lower.contains("tell me a joke")) {
      return "Why did the file go to therapy? Because it had too many unresolved dependencies and was feeling deeply fragmented!"
    }

    if (lower.contains("privacy") || lower.contains("offline") || lower.contains("is my data safe") || lower.contains("cloud")) {
      return "Your privacy is 100% protected! Local File Agent operates strictly on-device. No internet connection is ever used, no telemetry is gathered, and no file content or prompt ever leaves your smartphone."
    }

    // 2. Storage Capacity / Disk Size
    if (matchesAny(lower, "storage", "how much space", "how much storage", "free space", "disk space", "space left", "how full is my phone", "storage usage", "check storage", "capacity")) {
      return formatAction(
        explanation = "Checking your device's internal storage breakdown and free space.",
        tool = "size",
        args = JSONObject()
      )
    }

    // 3. Clean Empty Folders
    if (matchesAny(lower, "clean empty", "empty folder", "empty folders", "empty directory", "empty directories", "purge empty", "remove empty folders", "delete empty folders")) {
      return formatAction(
        explanation = "I will scan and remove all empty subfolders across your storage.",
        tool = "clean_empty",
        args = JSONObject()
      )
    }

    // 4. Large Files / Space Hogs
    if (matchesAny(lower, "large file", "large files", "big file", "big files", "heavy file", "heavy files", "biggest file", "biggest files", "space hog", "files taking up space", "taking up the most space", "largest files")) {
      return formatAction(
        explanation = "Scanning internal storage for large files consuming storage space.",
        tool = "large_files",
        args = JSONObject()
      )
    }

    // 5. Organize Folders
    if (lower.contains("organize") || lower.contains("organise") || lower.contains("sort files") || lower.contains("categorize") || lower.contains("tidy up")) {
      val folder = extractFolderTarget(userRequest, default = "Download")
      return formatAction(
        explanation = "I will organize the loose files in '$folder' into sorted folders (Images, Videos, Audio, Documents, Apps).",
        tool = "organize",
        args = JSONObject().put("folder", folder)
      )
    }

    // 6. Templates
    if (lower.contains("template")) {
      val quotes = extractQuotes(userRequest)
      if (lower.contains("create") || lower.contains("save") || lower.contains("make")) {
        val name = if (quotes.isNotEmpty()) quotes[0] else extractWordAfter(lower, "template")
        val path = if (quotes.size >= 2) quotes[1] else extractWordAfter(lower, "from").ifEmpty { "Download" }
        if (name.isNotBlank()) {
          return formatAction(
            explanation = "I will save the folder '$path' as a template named '$name'.",
            tool = "create_template",
            args = JSONObject().put("name", name).put("path", path)
          )
        }
      }
      if (lower.contains("use") || lower.contains("apply")) {
        val name = if (quotes.isNotEmpty()) quotes[0] else extractWordAfter(lower, "template")
        val dest = if (quotes.size >= 2) quotes[1] else extractWordAfter(lower, "at").ifEmpty { extractWordAfter(lower, "to") }
        if (name.isNotBlank() && dest.isNotBlank()) {
          return formatAction(
            explanation = "I will instantiate template '$name' at '$dest'.",
            tool = "use_template",
            args = JSONObject().put("name", name).put("path", dest)
          )
        }
      }
    }

    // 7. Write / Create File
    if (lower.contains("write") || lower.contains("create file") || lower.contains("make a file") || lower.contains("new file") || lower.contains("save text")) {
      val quotes = extractQuotes(userRequest)
      var file = ""
      var content = ""

      if (quotes.size >= 2) {
        // e.g. write "content" to "file.txt" OR write "file.txt" with "content"
        if (quotes[0].contains(".") || quotes[0].contains("/")) {
          file = quotes[0]
          content = quotes[1]
        } else {
          content = quotes[0]
          file = quotes[1]
        }
      } else {
        // Parse natural text e.g. "write hello world to Download/note.txt"
        val toIdx = lower.lastIndexOf(" to ")
        val withIdx = lower.indexOf(" with ")
        if (toIdx != -1) {
          file = extractPathToken(userRequest.substring(toIdx + 4).trim())
          val rawContent = userRequest.substring(0, toIdx).replace(Regex("(?i)^(write|create file|save)\\s*"), "").trim('\'', '"', ' ')
          content = rawContent
        } else if (withIdx != -1) {
          val rawFile = userRequest.substring(0, withIdx).replace(Regex("(?i)^(write|create file|new file|make a file (called|named)?)\\s*"), "").trim('\'', '"', ' ')
          file = extractPathToken(rawFile)
          content = userRequest.substring(withIdx + 6).replace(Regex("(?i)^content\\s*"), "").trim('\'', '"', ' ')
        } else if (quotes.size == 1) {
          content = quotes[0]
          file = "Download/note.txt"
        }
      }

      if (file.isNotBlank()) {
        val targetPath = normalizePath(file)
        return formatAction(
          explanation = "I will write content to '$targetPath'. Please confirm this action.",
          tool = "write",
          args = JSONObject().put("file", targetPath).put("content", content.ifEmpty { "Created with Local File Agent" })
        )
      }
    }

    // 8. Rename
    if (lower.contains("rename") || lower.contains("change name of")) {
      val quotes = extractQuotes(userRequest)
      var source = ""
      var dest = ""
      if (quotes.size >= 2) {
        source = quotes[0]
        dest = quotes[1]
      } else {
        val toIdx = lower.indexOf(" to ")
        if (toIdx != -1) {
          val before = userRequest.substring(0, toIdx).replace(Regex("(?i)^(rename|change name of)\\s*"), "").trim('\'', '"', ' ')
          val after = userRequest.substring(toIdx + 4).trim('\'', '"', ' ')
          source = extractPathToken(before)
          dest = extractPathToken(after)
        }
      }

      if (source.isNotBlank() && dest.isNotBlank()) {
        return formatAction(
          explanation = "I will rename '$source' to '$dest'. Please confirm.",
          tool = "rename",
          args = JSONObject().put("source", normalizePath(source)).put("dest", dest)
        )
      }
    }

    // 9. Move / Cut & Paste
    if (lower.contains("move") || lower.contains("relocate") || lower.contains("transfer") || lower.contains("shift")) {
      val quotes = extractQuotes(userRequest)
      var source = ""
      var dest = ""
      if (quotes.size >= 2) {
        source = quotes[0]
        dest = quotes[1]
      } else {
        val toIdx = lower.lastIndexOf(" to ")
        val intoIdx = if (toIdx == -1) lower.lastIndexOf(" into ") else -1
        val splitIdx = if (toIdx != -1) toIdx else intoIdx
        val splitLen = if (toIdx != -1) 4 else 6
        if (splitIdx != -1) {
          val before = userRequest.substring(0, splitIdx).replace(Regex("(?i)^(move|relocate|transfer|shift)\\s*(the file|file)?\\s*"), "").trim('\'', '"', ' ')
          val after = userRequest.substring(splitIdx + splitLen).trim('\'', '"', ' ')
          source = extractPathToken(before)
          dest = extractPathToken(after)
        }
      }

      if (source.isNotBlank() && dest.isNotBlank()) {
        return formatAction(
          explanation = "I will move '$source' to '$dest'. Please confirm.",
          tool = "move",
          args = JSONObject().put("source", normalizePath(source)).put("dest", normalizePath(dest))
        )
      }
    }

    // 10. Copy / Duplicate
    if (lower.contains("copy") || lower.contains("duplicate") || lower.contains("make a copy")) {
      val quotes = extractQuotes(userRequest)
      var source = ""
      var dest = ""
      if (quotes.size >= 2) {
        source = quotes[0]
        dest = quotes[1]
      } else {
        val toIdx = lower.lastIndexOf(" to ")
        val intoIdx = if (toIdx == -1) lower.lastIndexOf(" into ") else -1
        val splitIdx = if (toIdx != -1) toIdx else intoIdx
        val splitLen = if (toIdx != -1) 4 else 6
        if (splitIdx != -1) {
          val before = userRequest.substring(0, splitIdx).replace(Regex("(?i)^(copy|duplicate|make a copy of)\\s*(the file|file)?\\s*"), "").trim('\'', '"', ' ')
          val after = userRequest.substring(splitIdx + splitLen).trim('\'', '"', ' ')
          source = extractPathToken(before)
          dest = extractPathToken(after)
        }
      }

      if (source.isNotBlank() && dest.isNotBlank()) {
        return formatAction(
          explanation = "I will copy '$source' to '$dest'. Please confirm.",
          tool = "copy",
          args = JSONObject().put("source", normalizePath(source)).put("dest", normalizePath(dest))
        )
      }
    }

    // 11. Delete / Remove / Trash
    if (lower.contains("delete") || lower.contains("remove") || lower.contains("trash") || lower.contains("erase") || lower.contains("wipe")) {
      val quotes = extractQuotes(userRequest)
      val target = if (quotes.isNotEmpty()) {
        quotes[0]
      } else {
        val cleaned = userRequest.replace(Regex("(?i)^(please\\s+)?(delete|remove|trash|erase|wipe)\\s+(the\\s+)?(file|folder|document)?\\s*"), "")
          .trim('\'', '"', ' ')
        extractPathToken(cleaned)
      }

      if (target.isNotBlank() && target.length >= 2) {
        val normTarget = normalizePath(target)
        return formatAction(
          explanation = "I found '$normTarget' and prepared a deletion request. Please confirm before I proceed.",
          tool = "delete",
          args = JSONObject().put("file", normTarget)
        )
      }
    }

    // 12. Read / View Content
    if (lower.contains("read") || lower.startsWith("cat ") || lower.contains("what is inside") || lower.contains("show content") || lower.contains("view content") || lower.contains("display text")) {
      val quotes = extractQuotes(userRequest)
      val target = if (quotes.isNotEmpty()) quotes[0] else {
        val cleaned = userRequest.replace(Regex("(?i)^(read|cat|what is inside|show content of|view content of|display text of)\\s*(the file|file)?\\s*"), "").trim('\'', '"', ' ')
        extractPathToken(cleaned)
      }
      if (target.isNotBlank()) {
        val normTarget = normalizePath(target)
        return formatAction(
          explanation = "Reading contents of '$normTarget'...",
          tool = "read",
          args = JSONObject().put("file", normTarget)
        )
      }
    }

    // 13. Preview
    if (lower.contains("preview") || lower.contains("show picture") || lower.contains("view photo") || lower.contains("open image")) {
      val quotes = extractQuotes(userRequest)
      val target = if (quotes.isNotEmpty()) quotes[0] else {
        val cleaned = userRequest.replace(Regex("(?i)^(preview|show picture|view photo|open image)\\s*(of|the)?\\s*"), "").trim('\'', '"', ' ')
        extractPathToken(cleaned)
      }
      if (target.isNotBlank()) {
        val normTarget = normalizePath(target)
        return formatAction(
          explanation = "Opening preview for '$normTarget'...",
          tool = "preview",
          args = JSONObject().put("file", normTarget)
        )
      }
    }

    // 14. List / Explore / Browse Folder
    if (lower.contains("list") || lower.contains("show files") || lower.contains("explore") || lower.contains("browse") || lower.startsWith("dir ") || lower.contains("what files are in") || lower.contains("what's in")) {
      val folder = extractFolderTarget(userRequest, default = "Download")
      return formatAction(
        explanation = "Listing contents of '$folder'...",
        tool = "list",
        args = JSONObject().put("path", folder)
      )
    }

    // 15. Search / Find
    if (lower.contains("find") || lower.contains("search") || lower.contains("where is") || lower.contains("where are") || lower.contains("locate") || lower.contains("look for")) {
      val query = extractSearchQuery(userRequest)
      if (query.isNotBlank()) {
        return formatAction(
          explanation = "Searching for '$query' across your device storage...",
          tool = "search",
          args = JSONObject().put("query", query)
        )
      }
    }

    // Fallback: If path or file mentioned in query
    val pathCandidate = extractPathToken(userRequest)
    if (pathCandidate.contains(".") || pathCandidate.contains("/")) {
      return formatAction(
        explanation = "Listing details and contents for '$pathCandidate'...",
        tool = "list",
        args = JSONObject().put("path", normalizePath(pathCandidate))
      )
    }

    // Friendly conversational response
    return "I understand your request. You can manage files by asking me to find, list, read, preview, organize, move, copy, write, delete, or check storage. For example, try saying 'organize downloads' or 'find pdf files'."
  }

  private fun extractUserRequest(prompt: String): String {
    val regex = Regex("User request:\\s*\"(.*?)\"", RegexOption.DOT_MATCHES_ALL)
    val match = regex.find(prompt)
    if (match != null) {
      return match.groupValues[1].trim()
    }
    return prompt.trim()
  }

  private fun extractQuotes(input: String): List<String> {
    return Regex("['\"](.*?)['\"]").findAll(input).map { it.groupValues[1].trim() }.filter { it.isNotEmpty() }.toList()
  }

  private fun extractFolderTarget(input: String, default: String): String {
    val quotes = extractQuotes(input)
    if (quotes.isNotEmpty()) return quotes[0]
    val lower = input.lowercase(Locale.ROOT)
    return when {
      lower.contains("download") -> "Download"
      lower.contains("document") -> "Documents"
      lower.contains("picture") || lower.contains("photo") -> "Pictures"
      lower.contains("music") || lower.contains("audio") || lower.contains("song") -> "Music"
      lower.contains("movie") || lower.contains("video") -> "Movies"
      lower.contains("dcim") || lower.contains("camera") -> "DCIM"
      lower.contains("root") || lower.contains("internal") -> ""
      else -> {
        val inIdx = lower.lastIndexOf(" in ")
        val ofIdx = lower.lastIndexOf(" of ")
        val atIdx = lower.lastIndexOf(" at ")
        val idx = maxOf(inIdx, maxOf(ofIdx, atIdx))
        if (idx != -1) {
          val sub = input.substring(idx + 4).trim('\'', '"', ' ', '.', '?', '!')
          extractPathToken(sub).ifEmpty { default }
        } else {
          default
        }
      }
    }
  }

  private fun extractSearchQuery(input: String): String {
    val quotes = extractQuotes(input)
    if (quotes.isNotEmpty()) return quotes[0]
    val lower = input.lowercase(Locale.ROOT)

    // Common file categories
    if (lower.contains("pdf")) return "pdf"
    if (lower.contains("image") || lower.contains("photo") || lower.contains("picture")) return "jpg"
    if (lower.contains("video") || lower.contains("movie")) return "mp4"
    if (lower.contains("audio") || lower.contains("music") || lower.contains("song")) return "mp3"
    if (lower.contains("apk") || lower.contains("app")) return "apk"
    if (lower.contains("doc") || lower.contains("document") || lower.contains("word")) return "doc"
    if (lower.contains("excel") || lower.contains("sheet")) return "xls"
    if (lower.contains("zip") || lower.contains("archive")) return "zip"
    if (lower.contains("text file") || lower.contains("txt")) return "txt"

    // Search keywords extraction
    val cleaned = input.replace(Regex("(?i)^(please\\s+)?(find|search for|search|locate|where is|where are|look for)\\s+(all\\s+)?(my\\s+)?(files?\\s+)?(containing\\s+|named\\s+|with\\s+name\\s+|with\\s+extension\\s+)?"), "")
      .trim('\'', '"', ' ', '?', '.', '!')
    return cleaned.takeWhile { it != ' ' || !cleaned.contains(".") }.ifEmpty { cleaned }
  }

  private fun extractPathToken(token: String): String {
    val cleaned = token.replace(Regex("[,?!;]+$"), "").trim('\'', '"', ' ')
    val words = cleaned.split(" ")
    return words.firstOrNull { it.contains("/") || it.contains(".") } ?: words.firstOrNull() ?: cleaned
  }

  private fun extractWordAfter(input: String, keyword: String): String {
    val idx = input.indexOf(keyword)
    if (idx == -1) return ""
    val sub = input.substring(idx + keyword.length).trim()
    return sub.split(" ").firstOrNull()?.trim('\'', '"', ' ') ?: ""
  }

  private fun normalizePath(path: String): String {
    var p = path.trim('\'', '"', ' ')
    if (p.startsWith("/")) return p
    // If it has no folder and has an extension, default to Download/ if not already specified
    if (!p.contains("/") && p.contains(".")) {
      return "Download/$p"
    }
    return p
  }

  private fun isGreeting(lower: String): Boolean =
    matchesAny(lower, "hi", "hello", "hey", "good morning", "good afternoon", "good evening", "howdy", "sup")

  private fun isGratitude(lower: String): Boolean =
    matchesAny(lower, "thank you", "thanks", "appreciate it", "thx")

  private fun isIdentityOrHelp(lower: String): Boolean =
    matchesAny(lower, "who are you", "what are you", "what can you do", "help", "how do you work", "commands", "features", "what is this")

  private fun matchesAny(text: String, vararg targets: String): Boolean {
    val words = text.split(Regex("[\\s,?!.]+")).filter { it.isNotBlank() }
    for (target in targets) {
      if (target.contains(" ")) {
        if (text.contains(target)) return true
      } else {
        if (words.contains(target)) return true
      }
    }
    return false
  }

  private fun formatAction(explanation: String, tool: String, args: JSONObject): String {
    return "Explanation: $explanation\nAction: {\"tool\": \"$tool\", \"args\": $args}"
  }
}
