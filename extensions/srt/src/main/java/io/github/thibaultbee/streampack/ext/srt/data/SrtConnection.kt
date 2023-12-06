/*
 * Copyright (C) 2023 Thibault B.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.thibaultbee.streampack.ext.srt.data

import android.net.Uri
import java.security.InvalidParameterException


/**
 * SRT connection parameters
 *
 * @param host server ip
 * @param port server port
 * @param streamId SRT stream ID
 * @param passPhrase SRT passPhrase
 */
data class SrtConnection(
    val host: String,
    val port: Int,
    val streamId: String? = null,
    val passPhrase: String? = null
) {
    init {
        require(host.isNotBlank()) { "Invalid host $host" }
        require(
            host.startsWith(SRT_PREFIX).not()
        ) { "Invalid host $host: must not start with prefix srt://" }
        require(port > 0) { "Invalid port $port" }
        require(port < 65536) { "Invalid port $port" }
    }

    companion object {
        private const val SRT_SCHEME = "srt"
        private const val SRT_PREFIX = "$SRT_SCHEME://"

        private const val STREAM_ID_QUERY_PARAMETER = "streamid"
        private const val PASS_PHRASE_QUERY_PARAMETER = "passphrase"

        private val queryParameterList = listOf(
            STREAM_ID_QUERY_PARAMETER,
            PASS_PHRASE_QUERY_PARAMETER
        )

        /**
         * Creates a SRT connection from an URL
         *
         * @param url server url (syntax: srt://host:port?streamid=streamId&passphrase=passPhrase)
         * @return SRT connection
         */
        fun fromUrl(url: String): SrtConnection {
            val uri = Uri.parse(url)
            if (uri.scheme != SRT_SCHEME) {
                throw InvalidParameterException("URL $url is not an srt URL")
            }
            val host = uri.host
                ?: throw InvalidParameterException("Failed to parse URL $url: unknown host")
            val port = uri.port
            val streamId = uri.getQueryParameter(STREAM_ID_QUERY_PARAMETER)
            val passPhrase = uri.getQueryParameter(PASS_PHRASE_QUERY_PARAMETER)
            val unknownParameters =
                uri.queryParameterNames.find { queryParameterList.contains(it).not() }
            if (unknownParameters != null) {
                throw InvalidParameterException("Failed to parse URL $url: unknown parameter(s): $unknownParameters")
            }
            return SrtConnection(host, port, streamId, passPhrase)
        }

        /**
         * Creates a SRT connection from an URL and given parameters.
         * Query parameters are ignored.
         *
         * @param url server url (syntax: srt://host:port)
         * @param streamId SRT stream ID
         * @param passPhrase SRT passPhrase
         * @return SRT connection
         */
        fun fromUrlAndParameters(
            url: String,
            streamId: String? = null,
            passPhrase: String? = null
        ): SrtConnection {
            val uri = Uri.parse(url)
            if (uri.scheme != SRT_SCHEME) {
                throw InvalidParameterException("URL $url is not an srt URL")
            }
            val host = uri.host
                ?: throw InvalidParameterException("Failed to parse URL $url: unknown host")
            val port = uri.port

            return SrtConnection(host, port, streamId, passPhrase)
        }
    }
}