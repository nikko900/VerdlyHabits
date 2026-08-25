package com.saintnico.verdlyhabits.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import java.io.File

/**
 * Reliable avatar loader for Firebase HTTPS URLs, file:// paths, and content URIs.
 * Falls back to a person icon when the image is missing or fails to decode.
 */
@Composable
fun ProfileAvatar(
    photoUri: String?,
    modifier: Modifier = Modifier,
    size: Dp = 72.dp,
    contentDescription: String? = null,
    fallbackTint: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
    fallbackBackground: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
) {
    val context = LocalContext.current
    val model = remember(photoUri) { avatarModel(photoUri) }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(fallbackBackground),
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
                    Icon(
                        Icons.Rounded.Person,
                        contentDescription = null,
                        tint = fallbackTint.copy(alpha = 0.35f),
                        modifier = Modifier.size(size * 0.45f),
                    )
                },
                error = {
                    Icon(
                        Icons.Rounded.Person,
                        contentDescription = null,
                        tint = fallbackTint,
                        modifier = Modifier.size(size * 0.45f),
                    )
                },
            )
        } else {
            Icon(
                Icons.Rounded.Person,
                contentDescription = contentDescription,
                tint = fallbackTint,
                modifier = Modifier.size(size * 0.45f),
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
