/*
 * Copyright (C) 2021 Thibault B.
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
package com.github.thibaultbee.streampack.app.ui.main

import android.Manifest
import android.app.Application
import android.media.AudioFormat
import android.media.MediaFormat
import android.view.Surface
import androidx.annotation.RequiresPermission
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.github.thibaultbee.streampack.CaptureSrtLiveStream
import com.github.thibaultbee.streampack.app.configuration.Configuration
import com.github.thibaultbee.streampack.data.AudioConfig
import com.github.thibaultbee.streampack.data.VideoConfig
import com.github.thibaultbee.streampack.listeners.OnErrorListener
import com.github.thibaultbee.streampack.muxers.ts.data.ServiceInfo
import com.github.thibaultbee.streampack.utils.Logger

class PreviewViewModel(application: Application) : AndroidViewModel(application) {
    private val logger = Logger()

    private val configuration = Configuration(getApplication())

    private val tsServiceInfo = ServiceInfo(
        ServiceInfo.ServiceType.DIGITAL_TV,
        0x4698,
        "MyService",
        "MyProvider"
    )

    lateinit var captureLiveStream: CaptureSrtLiveStream

    val cameraId: String
        get() = captureLiveStream.videoSource.cameraId

    val error = MutableLiveData<String>()

    @RequiresPermission(allOf = [Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA])
    fun buildStreamer() {
        val videoConfig =
            VideoConfig(
                mimeType = MediaFormat.MIMETYPE_VIDEO_AVC,
                startBitrate = configuration.video.bitrate * 1000, // to b/s
                resolution = configuration.video.resolution,
                fps = 30
            )

        val audioConfig = AudioConfig(
            mimeType = MediaFormat.MIMETYPE_AUDIO_AAC,
            startBitrate = 128000,
            sampleRate = 48000,
            channelConfig = AudioFormat.CHANNEL_IN_STEREO,
            audioByteFormat = AudioFormat.ENCODING_PCM_16BIT
        )

        try {
            captureLiveStream =
                CaptureSrtLiveStream(getApplication(), tsServiceInfo, logger)

            captureLiveStream.onErrorListener = object : OnErrorListener {
                override fun onError(source: String, message: String) {
                    error.postValue("$source: $message")
                }
            }

            captureLiveStream.configure(audioConfig, videoConfig)
        } catch (e: Exception) {
            error.postValue("Failed to create CaptureLiveStream: ${e.message ?: "Unknown error"}")
        }

    }

    @RequiresPermission(allOf = [Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA])
    fun startCapture(previewSurface: Surface) {
        try {
            captureLiveStream.startCapture(previewSurface)
        } catch (e: Exception) {
            error.postValue("startCapture: ${e.message ?: "Unknown error"}")
        }
    }

    fun stopCapture() {
        captureLiveStream.stopCapture()
    }

    fun startStream() {
        try {
            captureLiveStream.connect(configuration.connection.ip, configuration.connection.port)
            captureLiveStream.startStream()
        } catch (e: Exception) {
            captureLiveStream.stopStream()
            error.postValue("startStream: ${e.message ?: "Unknown error"}")
        }
    }

    @RequiresPermission(allOf = [Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA])
    fun stopStream() {
        captureLiveStream.stopStream()
        captureLiveStream.disconnect()
    }

    @RequiresPermission(Manifest.permission.CAMERA)
    fun toggleVideoSource() {
        if (captureLiveStream.videoSource.cameraId == "0") {
            captureLiveStream.changeVideoSource("1")
        } else {
            captureLiveStream.changeVideoSource("0")
        }
    }

    override fun onCleared() {
        super.onCleared()
        captureLiveStream.release()
    }
}
