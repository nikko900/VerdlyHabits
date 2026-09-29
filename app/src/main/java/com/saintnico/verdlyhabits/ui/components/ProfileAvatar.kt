package com.saintnico.verdlyhabits.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.saintnico.verdlyhabits.ui.components.avatar.VerdlyAvatarFallback
import java.io.File

/**
 * Reliable avatar loader for Firebase HTTPS URLs, file:// paths, and content URIs.
 * Falls back to a stable hashed Lottie avatar when [userId] is set, otherwise index 0.
 */
@Composable
fun ProfileAvatar(
    photoUri: String?,
    modifier: Modifier = Modifier,
    size: Dp = 72.dp,
    contentDescription: String? = null,
    userId: String? = null,
    @Suppress("UNUSED_PARAMETER") fallbackTint: Color = Color.Unspecified,
    fallbackBackground: Color = Color.Unspecified,
) {
    val context = LocalContext.current
    val model = remember(photoUri) { avatarModel(photoUri) }
    val scheme = androidx.compose.material3.MaterialTheme.colorScheme
    val resolvedFallbackBg = if (fallbackBackground == Color.Unspecified) {
        scheme.surfaceVariant.copy(alpha = 0.55f)
    } else {
        fallbackBackground
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(resolvedFallbackBg),
        contentAlignment = Alignment.Center,
    ) {
        if (model != null) {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(context)
                    .data(model)
                    .crossfade(true)
                    .memoryCacheKey(photoUri)
                    .diskCacheKey(photoUri)
                    .build(),
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                loading = {
                    VerdlyAvatarFallback(
                        userId = userId,
                        modifier = Modifier.fillMaxSize(),
                        size = size,
                    )
                },
                error = {
                    VerdlyAvatarFallback(
                        userId = userId,
                        modifier = Modifier.fillMaxSize(),
                        size = size,
                    )
                },
            )
        } else {
            VerdlyAvatarFallback(
                userId = userId,
                modifier = Modifier.fillMaxSize(),
                size = size * 0.88f,
            )
        }
    }
}

private fun avatarModel(photoUri: String?): Any? {
    if (photoUri.isNullOrBlank()) return null
    val trimmed = photoUri.trim()
    return when {
        trimmed.startsWith("http://") || trimmed.startsWith("https://") -> trimmed
        trimmed.startsWith("file:") -> {
            val path = android.net.Uri.parse(trimmed).path
            if (path != null && File(path).exists()) File(path) else null
        }
        trimmed.startsWith("content:") -> trimmed
        File(trimmed).exists() -> File(trimmed)
        else -> trimmed
    }
}
