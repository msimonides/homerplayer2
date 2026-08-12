/*
 * MIT License
 *
 * Copyright (c) 2026 Marcin Simonides
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.studio4plus.homerplayer2.daisyonline

import android.util.Xml
import com.studio4plus.homerplayer2.net.NetworkClient
import com.studio4plus.homerplayer2.net.NetworkResult
import org.koin.core.annotation.Factory
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlSerializer
import timber.log.Timber
import java.io.IOException
import java.io.StringReader
import java.io.StringWriter

data class DaisyOnlineContentMetadata(
    val title: String,
    val author: String,
)

data class DaisyOnlineContentResource(
    val uri: String,
    val mimeType: String?,
    val size: Long?,
)

sealed interface DaisyOnlineSoapResult<out T> {
    data class Success<T>(val value: T) : DaisyOnlineSoapResult<T>
    data object NoActiveSession : DaisyOnlineSoapResult<Nothing>
    data class Error(val message: String) : DaisyOnlineSoapResult<Nothing>
}

/**
 * SOAP client for the DAISY Online protocol.
 * Maintains session cookies in memory; create one instance per service session.
 */
@Factory
open class DaisyOnlineSoapClient(
    private val networkClient: NetworkClient,
) {
    private var sessionCookies: Map<String, String> = emptyMap()

    open val hasSession: Boolean get() = sessionCookies.isNotEmpty()

    /**
     * Logs on to the service and stores the session cookies.
     * Returns true if logOn succeeded.
     */
    open suspend fun logOn(url: String, username: String, password: String): DaisyOnlineSoapResult<Boolean> {
        val body = buildLogOnBody(username, password)
        val result = postSoap(url, "/logOn", body)
        return when (result) {
            is DaisyOnlineSoapResult.Success -> {
                val (xml, headers) = result.value
                sessionCookies = extractCookiesFromHeaders(headers)
                val loggedOn = parseLogOnResult(xml)
                Timber.i("DaisyOnline logOn result: $loggedOn, cookies: ${sessionCookies.keys}")
                DaisyOnlineSoapResult.Success(loggedOn)
            }
            is DaisyOnlineSoapResult.NoActiveSession -> result
            is DaisyOnlineSoapResult.Error -> result
        }
    }

    /**
     * Clears the in-memory session cookies (does not call logOff on server).
     */
    open fun clearSession() {
        sessionCookies = emptyMap()
    }

    /**
     * Returns the list of content IDs issued to the user.
     */
    open suspend fun getIssuedContentList(url: String): DaisyOnlineSoapResult<List<String>> {
        val result = postSoap(url, "/getContentList", SOAP_ISSUED_CONTENT)
        return when (result) {
            is DaisyOnlineSoapResult.Success -> {
                val (xml, _) = result.value
                DaisyOnlineSoapResult.Success(parseContentListIds(xml))
            }
            is DaisyOnlineSoapResult.NoActiveSession -> result
            is DaisyOnlineSoapResult.Error -> result
        }
    }

    /**
     * Returns the metadata (title, author) for a single content item.
     */
    open suspend fun getContentMetadata(url: String, contentId: String): DaisyOnlineSoapResult<DaisyOnlineContentMetadata> {
        val body = buildGetContentMetadataBody(contentId)
        val result = postSoap(url, "/getContentMetadata", body)
        return when (result) {
            is DaisyOnlineSoapResult.Success -> {
                val (xml, _) = result.value
                val metadata = parseContentMetadata(xml)
                if (metadata != null) {
                    DaisyOnlineSoapResult.Success(metadata)
                } else {
                    DaisyOnlineSoapResult.Error("Failed to parse metadata for $contentId")
                }
            }
            is DaisyOnlineSoapResult.NoActiveSession -> result
            is DaisyOnlineSoapResult.Error -> result
        }
    }

    /**
     * Returns the list of downloadable resources for a content item.
     */
    open suspend fun getContentResources(url: String, contentId: String): DaisyOnlineSoapResult<List<DaisyOnlineContentResource>> {
        val body = buildGetContentResourcesBody(contentId)
        val result = postSoap(url, "/getContentResources", body)
        return when (result) {
            is DaisyOnlineSoapResult.Success -> {
                val (xml, _) = result.value
                DaisyOnlineSoapResult.Success(parseContentResources(xml))
            }
            is DaisyOnlineSoapResult.NoActiveSession -> result
            is DaisyOnlineSoapResult.Error -> result
        }
    }

    // --- Private helpers ---

    private suspend fun postSoap(
        url: String,
        action: String,
        body: String,
    ): DaisyOnlineSoapResult<Pair<String, Map<String, List<String>>>> {
        val headers = buildMap {
            put("SOAPAction", action)
            if (sessionCookies.isNotEmpty()) {
                put("Cookie", sessionCookies.entries.joinToString("; ") { "${it.key}=${it.value}" })
            }
        }
        return when (val result = networkClient.postXml(url, body, headers)) {
            is NetworkResult.Success -> {
                DaisyOnlineSoapResult.Success(result.body to result.headers)
            }
            is NetworkResult.HttpError -> {
                DaisyOnlineSoapResult.Error("HTTP error ${result.code}")
            }
            is NetworkResult.Failure -> {
                throw IOException("Network failure: ${result.type}", result.cause)
            }
        }
    }

    private fun extractCookiesFromHeaders(headers: Map<String, List<String>>): Map<String, String> {
        val result = mutableMapOf<String, String>()
        // Headers are case-insensitive in HTTP; check both cases
        val setCookieValues = headers["Set-Cookie"] ?: headers["set-cookie"] ?: return emptyMap()
        for (cookieHeader in setCookieValues) {
            // Each Set-Cookie value is "name=value; attributes..."
            val nameValue = cookieHeader.substringBefore(";").trim()
            val eqIdx = nameValue.indexOf('=')
            if (eqIdx > 0) {
                val name = nameValue.substring(0, eqIdx).trim()
                val value = nameValue.substring(eqIdx + 1).trim()
                result[name] = value
            }
        }
        return result
    }

    private fun parseLogOnResult(xml: String): Boolean {
        val parser = Xml.newPullParser().apply {
            setInput(StringReader(xml))
            setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        }
        while (parser.eventType != XmlPullParser.END_DOCUMENT) {
            if (parser.eventType == XmlPullParser.START_TAG) {
                if (parser.name.split(":").last() == "logOnResult") {
                    return parser.nextText().trim().equals("true", ignoreCase = true)
                }
            }
            parser.next()
        }
        return false
    }

    private fun parseContentListIds(xml: String): List<String> {
        val parser = Xml.newPullParser().apply {
            setInput(StringReader(xml))
            setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        }
        return buildList {
            while (parser.eventType != XmlPullParser.END_DOCUMENT) {
                if (parser.eventType == XmlPullParser.START_TAG) {
                    if (parser.name.split(":").last() == "contentItem") {
                        parser.getAttributeValue(null, "id")
                            ?.takeIf { it.isNotBlank() }
                            ?.let { add(it) }
                    }
                }
                parser.next()
            }
        }
    }

    private fun parseContentMetadata(xml: String): DaisyOnlineContentMetadata? {
        val parser = Xml.newPullParser().apply {
            setInput(StringReader(xml))
            setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        }
        var title: String? = null
        var author: String? = null
        while (parser.eventType != XmlPullParser.END_DOCUMENT) {
            if (parser.eventType == XmlPullParser.START_TAG) {
                when (parser.name.split(":").last()) {
                    "title" -> title = parser.nextText().trim()
                    "creator" -> author = parser.nextText().trim()
                }
            }
            if (parser.eventType != XmlPullParser.END_DOCUMENT) parser.next()
        }
        return if (title != null && author != null) {
            DaisyOnlineContentMetadata(title = title, author = author)
        } else if (title != null) {
            DaisyOnlineContentMetadata(title = title, author = "")
        } else {
            null
        }
    }

    private fun parseContentResources(xml: String): List<DaisyOnlineContentResource> {
        val parser = Xml.newPullParser().apply {
            setInput(StringReader(xml))
            setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        }
        return buildList {
            while (parser.eventType != XmlPullParser.END_DOCUMENT) {
                if (parser.eventType == XmlPullParser.START_TAG) {
                    if (parser.name.split(":").last() == "resource") {
                        val uri = parser.getAttributeValue(null, "uri")
                        val mimeType = parser.getAttributeValue(null, "mimeType")
                        val sizeStr = parser.getAttributeValue(null, "size")
                        if (!uri.isNullOrBlank()) {
                            add(DaisyOnlineContentResource(
                                uri = uri,
                                mimeType = mimeType,
                                size = sizeStr?.toLongOrNull(),
                            ))
                        }
                    }
                }
                parser.next()
            }
        }
    }

    private fun buildLogOnBody(username: String, password: String): String {
        val xmlSerializer = Xml.newSerializer()
        val escapedUsername = xmlSerializer.escapeText(username)
        val escapedPassword = xmlSerializer.escapeText(password)
        return SOAP_LOGON.format(escapedUsername, escapedPassword)
    }

    private fun buildGetContentMetadataBody(contentId: String): String {
        val xmlSerializer = Xml.newSerializer()
        val escapedId = xmlSerializer.escapeText(contentId)
        return SOAP_GET_CONTENT_METADATA.format(escapedId)
    }

    private fun buildGetContentResourcesBody(contentId: String): String {
        val xmlSerializer = Xml.newSerializer()
        val escapedId = xmlSerializer.escapeText(contentId)
        return SOAP_GET_CONTENT_RESOURCES.format(escapedId)
    }

    private fun XmlSerializer.escapeText(text: String): String =
        StringWriter().use { writer ->
            setOutput(writer)
            text(text)
            flush()
            writer.toString()
        }
}

private const val SOAP_LOGON = """
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:dais="http://www.daisy.org/ns/daisy-online/">
   <soapenv:Header/>
   <soapenv:Body>
      <dais:logOn>
         <dais:username>%s</dais:username>
         <dais:password>%s</dais:password>
      </dais:logOn>
   </soapenv:Body>
</soapenv:Envelope>
"""

private const val SOAP_ISSUED_CONTENT = """
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:dais="http://www.daisy.org/ns/daisy-online/">
   <soapenv:Header/>
   <soapenv:Body>
      <dais:getContentList>
         <dais:id>issued</dais:id>
         <dais:firstItem>0</dais:firstItem>
         <dais:lastItem>-1</dais:lastItem>
      </dais:getContentList>
   </soapenv:Body>
</soapenv:Envelope>
"""

private const val SOAP_GET_CONTENT_METADATA = """
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:dais="http://www.daisy.org/ns/daisy-online/">
   <soapenv:Header/>
   <soapenv:Body>
      <dais:getContentMetadata>
         <dais:contentID>%s</dais:contentID>
      </dais:getContentMetadata>
   </soapenv:Body>
</soapenv:Envelope>
"""

private const val SOAP_GET_CONTENT_RESOURCES = """
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:dais="http://www.daisy.org/ns/daisy-online/">
   <soapenv:Header/>
   <soapenv:Body>
      <dais:getContentResources>
         <dais:contentID>%s</dais:contentID>
      </dais:getContentResources>
   </soapenv:Body>
</soapenv:Envelope>
"""
