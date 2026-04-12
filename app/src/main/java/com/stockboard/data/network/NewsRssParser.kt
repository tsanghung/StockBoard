package com.stockboard.data.network

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.text.SimpleDateFormat
import java.time.OffsetDateTime
import java.util.Locale

/**
 * 支援 RSS 2.0（pubDate：RFC 2822）與 Atom（published：ISO 8601）格式。
 * 命名空間處理關閉，透過 substringAfterLast(":") 統一取 local name。
 */
object NewsRssParser {

    data class ParsedItem(
        val title: String,
        val link: String,
        val publishTimeMs: Long,
        val imageUrl: String?
    )

    fun parse(xml: String): List<ParsedItem> {
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(xml.reader())

        val items = mutableListOf<ParsedItem>()
        var inItem = false
        var title = ""
        var link = ""
        var pubDate = ""
        var imageUrl: String? = null

        // 標準 Android 官方做法：用 TEXT 事件累積文字，完全不呼叫 nextText()，
        // 無任何 parser 推進狀態不一致的風險。
        val currentText = StringBuilder()
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            // 去除命名空間前綴，例如 "media:content" → "content"
            val tag = parser.name?.substringAfterLast(":") ?: ""

            when (event) {
                XmlPullParser.START_TAG -> {
                    currentText.clear()
                    when (tag) {
                        "item", "entry" -> {
                            inItem = true
                            title = ""; link = ""; pubDate = ""; imageUrl = null
                        }
                        // Atom <link href="..."/>：屬性在 START_TAG 時讀取
                        "link" -> if (inItem) {
                            val href = parser.getAttributeValue(null, "href")
                            if (!href.isNullOrBlank()) link = href
                        }
                        // <media:content url="..."/> 或 <media:thumbnail url="..."/>
                        "content", "thumbnail" -> if (inItem && imageUrl == null) {
                            imageUrl = parser.getAttributeValue(null, "url")
                        }
                        // <enclosure url="..." type="image/jpeg"/>
                        "enclosure" -> if (inItem && imageUrl == null) {
                            val type = parser.getAttributeValue(null, "type") ?: ""
                            if (type.startsWith("image")) {
                                imageUrl = parser.getAttributeValue(null, "url")
                            }
                        }
                    }
                }
                // 累積文字節點（CDATA 與一般文字均透過此事件送出）
                XmlPullParser.TEXT -> currentText.append(parser.text ?: "")
                XmlPullParser.END_TAG -> when (tag) {
                    "title" -> if (inItem && title.isEmpty()) {
                        title = currentText.toString().trim()
                    }
                    // RSS <link> 文字節點；若已由 href 屬性填入則跳過
                    "link" -> if (inItem && link.isEmpty()) {
                        link = currentText.toString().trim()
                    }
                    "pubDate", "published", "updated" -> if (inItem && pubDate.isEmpty()) {
                        pubDate = currentText.toString().trim()
                    }
                    "item", "entry" -> {
                        if (inItem && title.isNotBlank() && link.isNotBlank()) {
                            items.add(ParsedItem(title, link, parseDate(pubDate), imageUrl))
                        }
                        inItem = false
                    }
                }
            }
            event = parser.next()
        }
        return items
    }

    private fun parseDate(dateStr: String): Long {
        if (dateStr.isBlank()) return System.currentTimeMillis()
        return try {
            if (dateStr.contains("T")) {
                // ISO 8601（Atom）：需 API 26+，StockBoard minSdk = 26
                OffsetDateTime.parse(dateStr).toInstant().toEpochMilli()
            } else {
                // RFC 2822（RSS）
                SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH)
                    .parse(dateStr)?.time ?: System.currentTimeMillis()
            }
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }
}
