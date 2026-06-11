package com.example.ui.components

import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun VideoPlayer(
    url: String,
    modifier: Modifier = Modifier,
    isMuted: Boolean = false,
    showControls: Boolean = true
) {
    var isWebViewLoading by remember { mutableStateOf(true) }

    // Generates the HTML5 player source code with embedded hls.js support
    val htmlContent = remember(url, showControls) {
        """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
            <style>
                body, html {
                    margin: 0;
                    padding: 0;
                    width: 100%;
                    height: 100%;
                    overflow: hidden;
                    background-color: #000000;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                }
                video {
                    width: 100%;
                    height: 100%;
                    object-fit: contain;
                    outline: none;
                }
            </style>
            <!-- Include robust hls.js for handling live TS / M3U8 streams directly in Android WebView and chromium runtime -->
            <script src="https://cdn.jsdelivr.net/npm/hls.js@1.4.0/dist/hls.min.js"></script>
        </head>
        <body>
            <video id="video" ${if (showControls) "controls" else ""} autoplay playsinline></video>
            <script>
                var video = document.getElementById('video');
                var streamUrl = '$url';
                
                function initPlayer() {
                    if (hlsIsSupportedForUrl(streamUrl)) {
                        var hls = new Hls({
                            maxMaxBufferLength: 10,
                            enableWorker: true,
                            lowLatencyMode: true
                        });
                        hls.loadSource(streamUrl);
                        hls.attachMedia(video);
                        hls.on(Hls.Events.MANIFEST_PARSED, function() {
                            video.play().catch(function(e) {
                                console.log("Play failed: " + e);
                            });
                        });
                        hls.on(Hls.Events.ERROR, function(event, data) {
                            if (data.fatal) {
                                switch(data.type) {
                                    case Hls.ErrorTypes.NETWORK_ERROR:
                                        hls.startLoad();
                                        break;
                                    case Hls.ErrorTypes.MEDIA_ERROR:
                                        hls.recoverMediaError();
                                        break;
                                    default:
                                        break;
                                }
                            }
                        });
                    } else {
                        // Direct source fallback for standard MP4, WebM or native HLS if WebView allows it
                        video.src = streamUrl;
                        video.play().catch(function(e) {
                            console.log("Fallback play failed: " + e);
                        });
                    }
                }

                function hlsIsSupportedForUrl(url) {
                    return Hls.isSupported() && (url.toLowerCase().indexOf('.m3u8') !== -1 || url.toLowerCase().indexOf('stream') !== -1);
                }

                initPlayer();
            </script>
        </body>
        </html>
        """.trimIndent()
    }

    Box(
        modifier = modifier.background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        mediaPlaybackRequiresUserGesture = false
                        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        useWideViewPort = true
                        loadWithOverviewMode = true
                    }
                    webChromeClient = WebChromeClient()
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            isWebViewLoading = false
                        }
                    }
                    
                    val encodedHtml = android.util.Base64.encodeToString(
                        htmlContent.toByteArray(),
                        android.util.Base64.NO_PADDING
                    )
                    loadData(encodedHtml, "text/html", "base64")
                    tag = url
                }
            },
            update = { webView ->
                if (webView.tag != url) {
                    webView.tag = url
                    isWebViewLoading = true
                    val encodedHtml = android.util.Base64.encodeToString(
                        htmlContent.toByteArray(),
                        android.util.Base64.NO_PADDING
                    )
                    webView.loadData(encodedHtml, "text/html", "base64")
                }
                // Directly and efficiently control muted state through evaluation without reloading the page
                webView.evaluateJavascript("document.getElementById('video').muted = $isMuted;", null)
            },
            modifier = Modifier.fillMaxSize()
        )

        if (isWebViewLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(40.dp),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 3.dp
                )
            }
        }
    }
}
