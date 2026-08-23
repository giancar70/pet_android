package com.petapp.android.features.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.petapp.android.core.model.Pet

private val AvatarBackground = Color(0xFFE3E3E3)
private val StatusDotGreen = Color(0xFF4CAF50)

@Composable
fun PetAvatar(
    pet: Pet?,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    showStatusDot: Boolean = true,
) {
    Box(modifier = modifier.size(size)) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(AvatarBackground),
            contentAlignment = Alignment.Center,
        ) {
            val imageUrl = pet?.image
            if (imageUrl != null) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(size)
                        .clip(CircleShape),
                )
            } else {
                Icon(
                    Icons.Filled.Pets,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(size / 2),
                )
            }
        }
        if (showStatusDot) {
            Box(
                modifier = Modifier
                    .size(size / 4.5f)
                    .align(Alignment.BottomEnd)
                    .clip(CircleShape)
                    .background(Color.White),
            )
            Box(
                modifier = Modifier
                    .size(size / 6f)
                    .align(Alignment.BottomEnd)
                    .clip(CircleShape)
                    .background(StatusDotGreen),
            )
        }
    }
}
