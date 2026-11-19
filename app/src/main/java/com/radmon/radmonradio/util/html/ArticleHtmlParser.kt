/** Turns Squarespace article HTML into blocks the app can draw. Mirrors the iOS app's ArticleHTMLParser. */
package com.radmon.radmonradio.util.html

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import java.net.URI

object ArticleHtmlParser {
    // relative links like "/events" are resolved against the station's site
    private val baseUri = URI("https://www.radiomonash.online")

    // the article as blocks, for ArticleScreen
    fun blocks(html: String): List<ArticleBlock> {
        val parser = Parser()
        parser.run(html)
        return parser.blocks
    }

    // the article as plain text, for word counts and excerpts
    fun plainText(html: String): String =
        blocks(html).mapNotNull { block ->
            when (block) {
                is ArticleBlock.Paragraph -> block.text.text
                is ArticleBlock.Heading -> block.text.text
                is ArticleBlock.Quote -> block.text.text
                is ArticleBlock.ListItem -> block.text.text
                // images only add their caption, if any
                is ArticleBlock.Image -> block.caption?.text
            }
        }.joinToString("\n\n")

    private fun resolve(href: String): String? =
        runCatching { baseUri.resolve(href).toString() }.getOrNull()

    // MARK: - Parser state

    // the kind of block currently being built
    private sealed class BlockKind {
        data object Paragraph : BlockKind()
        data class Heading(val level: Int) : BlockKind()
        data object Quote : BlockKind()
        data class ListItem(val marker: String) : BlockKind()
        data object Caption : BlockKind()
    }

    // one styled run of text appended to `current`, in source-string offsets
    private class Run(val start: Int, val end: Int, val bold: Boolean, val italic: Boolean, val link: String?)

    private class ListState(val ordered: Boolean) {
        var count = 0
    }

    private class Parser {
        // tags whose contents never belong in the article
        private val skippedTags = setOf("script", "style", "noscript", "iframe", "svg", "button", "select")

        val blocks = mutableListOf<ArticleBlock>()
        // text of the block being built, with its style runs
        val current = StringBuilder()
        val runs = mutableListOf<Run>()
        var kind: BlockKind? = null
        // how many <strong>/<em> tags we're inside
        var boldDepth = 0
        var italicDepth = 0
        // open <a> tags, innermost last
        val links = mutableListOf<String?>()
        // open <ul>/<ol> lists, innermost last
        val lists = mutableListOf<ListState>()
        // a tag whose contents are ignored, e.g. <script>
        var skipTag: String? = null
        var skipDepth = 0
        // images already added, so none appear twice
        val seenImages = mutableSetOf<String>()
        // used to collapse runs of spaces into one
        var lastWasSpace = true

        // walks the html, handing each tag and each piece of text to the handlers below
        fun run(html: String) {
            var index = 0
            val length = html.length
            while (index < length) {
                if (html[index] == '<') {
                    // skip <!-- comments -->
                    if (html.startsWith("<!--", index)) {
                        val end = html.indexOf("-->", index)
                        if (end == -1) break
                        index = end + 3
                        continue
                    }
                    // a tag runs up to the next ">"
                    val close = html.indexOf('>', index)
                    if (close == -1) break
                    HtmlTag.parse(html.substring(index + 1, close))?.let { handleTag(it) }
                    index = close + 1
                } else {
                    // text runs up to the next "<"
                    val next = html.indexOf('<', index).let { if (it == -1) length else it }
                    handleText(html.substring(index, next))
                    index = next
                }
            }
            // finish whatever block is still open
            flush()
        }

        // finish the open block and start a new kind
        fun begin(newKind: BlockKind) {
            flush()
            kind = newKind
        }

        // finishes the open block and adds it to `blocks`, unless it's empty
        fun flush() {
            val finishedKind = kind
            val text = current.toString()
            val finishedRuns = runs.toList()
            // reset for the next block, whichever way this function exits
            current.clear()
            runs.clear()
            kind = null
            lastWasSpace = true

            if (finishedKind == null) return

            // trim spaces from both ends
            var start = 0
            while (start < text.length && text[start].isWhitespace()) start++
            var end = text.length
            while (end > start && text[end - 1].isWhitespace()) end--
            if (start >= end) return

            val trimmed = buildAnnotatedString(text, finishedRuns, start, end)

            when (finishedKind) {
                is BlockKind.Paragraph -> blocks.add(ArticleBlock.Paragraph(trimmed))
                is BlockKind.Heading -> blocks.add(ArticleBlock.Heading(trimmed, finishedKind.level))
                is BlockKind.Quote -> blocks.add(ArticleBlock.Quote(trimmed))
                is BlockKind.ListItem -> blocks.add(ArticleBlock.ListItem(trimmed, finishedKind.marker))
                is BlockKind.Caption -> {
                    // a caption attaches to the image just before it
                    val lastIndex = blocks.lastIndex
                    val lastBlock = blocks.getOrNull(lastIndex)
                    if (lastBlock is ArticleBlock.Image && lastBlock.caption == null) {
                        blocks[lastIndex] = lastBlock.copy(caption = trimmed)
                    }
                }
            }
        }

        private fun buildAnnotatedString(text: String, textRuns: List<Run>, trimStart: Int, trimEnd: Int): AnnotatedString {
            val builder = AnnotatedString.Builder(text.substring(trimStart, trimEnd))
            for (run in textRuns) {
                val s = maxOf(run.start, trimStart)
                val e = minOf(run.end, trimEnd)
                if (s >= e) continue
                val relativeStart = s - trimStart
                val relativeEnd = e - trimStart
                if (run.bold || run.italic) {
                    builder.addStyle(
                        SpanStyle(
                            fontWeight = if (run.bold) FontWeight.Bold else null,
                            fontStyle = if (run.italic) FontStyle.Italic else null
                        ),
                        relativeStart,
                        relativeEnd
                    )
                }
                if (run.link != null) {
                    builder.addLink(
                        LinkAnnotation.Url(
                            run.link,
                            styles = TextLinkStyles(SpanStyle(textDecoration = TextDecoration.Underline))
                        ),
                        relativeStart,
                        relativeEnd
                    )
                }
            }
            return builder.toAnnotatedString()
        }

        // MARK: - Tags

        fun handleTag(tag: HtmlTag) {
            val name = tag.name
            val isClosing = tag.isClosing

            // inside a skipped tag, wait for its matching close and ignore everything else
            val currentSkipTag = skipTag
            if (currentSkipTag != null) {
                if (name == currentSkipTag) {
                    skipDepth += if (isClosing) -1 else 1
                    if (skipDepth == 0) skipTag = null
                }
                return
            }
            // start skipping
            if (!isClosing && name in skippedTags && !tag.isSelfClosing) {
                skipTag = name
                skipDepth = 1
                return
            }

            when {
                // paragraphs
                name == "p" && !isClosing ->
                    // a <p> inside a quote or list item stays part of it
                    if (kind == null || kind == BlockKind.Paragraph) begin(BlockKind.Paragraph)
                name == "p" && isClosing ->
                    if (kind == BlockKind.Paragraph) flush()

                // headings, squeezed into three sizes
                (name == "h1" || name == "h2") && !isClosing -> begin(BlockKind.Heading(1))
                name == "h3" && !isClosing -> begin(BlockKind.Heading(2))
                (name == "h4" || name == "h5" || name == "h6") && !isClosing -> begin(BlockKind.Heading(3))

                name == "blockquote" && !isClosing -> begin(BlockKind.Quote)
                name == "figcaption" && !isClosing -> begin(BlockKind.Caption)

                // lists
                name == "ul" && !isClosing -> lists.add(ListState(ordered = false))
                name == "ol" && !isClosing -> lists.add(ListState(ordered = true))
                (name == "ul" || name == "ol") && isClosing -> {
                    flush()
                    if (lists.isNotEmpty()) lists.removeAt(lists.lastIndex)
                }
                name == "li" && !isClosing -> {
                    // bullet, or the next number in a numbered list
                    val last = lists.lastOrNull()
                    val marker = if (last != null && last.ordered) {
                        last.count += 1
                        "${last.count}."
                    } else {
                        "•"
                    }
                    begin(BlockKind.ListItem(marker))
                }

                // closing any block tag finishes the block
                (name in setOf("h1", "h2", "h3", "h4", "h5", "h6", "blockquote", "figcaption", "li")) && isClosing -> flush()

                // bold and italic count up on open, down on close
                name == "strong" || name == "b" -> boldDepth = maxOf(0, boldDepth + if (isClosing) -1 else 1)
                name == "em" || name == "i" -> italicDepth = maxOf(0, italicDepth + if (isClosing) -1 else 1)

                // links
                name == "a" && !isClosing -> {
                    // null when there's no usable href, so the matching </a> still pops one
                    val url = tag.attribute("href")?.let { resolve(HtmlEntities.decode(it)) }
                    links.add(url)
                }
                name == "a" && isClosing -> if (links.isNotEmpty()) links.removeAt(links.lastIndex)

                // line break inside a block
                name == "br" -> if (kind != null) {
                    current.append('\n')
                    lastWasSpace = true
                }

                name == "img" && !isClosing -> addImage(tag)

                // every other tag (div, span, ...) is ignored
                else -> Unit
            }
        }

        // MARK: - Images

        private fun addImage(tag: HtmlTag) {
            // squarespace puts the real url in data-src or data-image; skip inline data: images
            val raw = tag.attribute("data-src") ?: tag.attribute("data-image") ?: tag.attribute("src")
            if (raw == null || raw.startsWith("data:")) return
            val url = resolve(HtmlEntities.decode(raw)) ?: return

            // the url without ?format=..., so different sizes of one image count once
            val key = url.substringBefore("?")
            if (!seenImages.add(key)) return
            // an image ends the text block before it
            flush()
            blocks.add(ArticleBlock.Image(url, caption = null))
        }

        // MARK: - Text

        fun handleText(text: String) {
            // ignore text inside <script>, <style> ...
            if (skipTag != null) return

            // decode entities and collapse spaces and new lines into single spaces, like a browser
            val collapsed = StringBuilder()
            for (character in HtmlEntities.decode(text)) {
                if (character.isWhitespace()) {
                    if (!lastWasSpace) {
                        collapsed.append(' ')
                        lastWasSpace = true
                    }
                } else {
                    collapsed.append(character)
                    lastWasSpace = false
                }
            }
            if (collapsed.isEmpty()) return
            // text outside any block starts a paragraph, unless it's only spaces
            if (kind == null) {
                if (collapsed.all { it.isWhitespace() }) return
                kind = BlockKind.Paragraph
            }

            // style the text with whatever bold, italic and link tags are open
            val start = current.length
            current.append(collapsed)
            val end = current.length
            // the innermost link that has a url
            val link = links.lastOrNull { it != null }
            runs.add(Run(start, end, boldDepth > 0, italicDepth > 0, link))
        }
    }
}
