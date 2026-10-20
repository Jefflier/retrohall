package com.retrohall.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.retrohall.app.AppGraph
import com.retrohall.app.data.Game
import com.retrohall.app.ui.theme.brandColor
import com.retrohall.core.Platform

@Composable
fun rememberCover(game: Game): ImageBitmap? {
    val loader = AppGraph.covers
    // produceState keeps its value across key changes, so always assign the freshly loaded cover.
    val cover by produceState(loader.cached(game)?.asImageBitmap(), loader.key(game)) {
        value = loader.load(game)?.asImageBitmap()
    }
    return cover
}

/** Box art, or a coloured placeholder with the platform name when no art is available. */
@Composable
fun GameCover(game: Game, modifier: Modifier = Modifier) {
    val cover = rememberCover(game)
    Box(modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh)) {
        if (cover != null) {
            Image(
                bitmap = cover,
                contentDescription = game.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            CoverPlaceholder(game)
        }
    }
}

@Composable
private fun CoverPlaceholder(game: Game) {
    val color = game.platform.brandColor
    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.linearGradient(listOf(color, color.copy(alpha = 0.55f)))),
    ) {
        Text(
            text = game.platform.shortName,
            color = Color.White.copy(alpha = 0.85f),
            fontWeight = FontWeight.Black,
            fontSize = 13.sp,
            modifier = Modifier.padding(8.dp),
        )
        Text(
            text = game.title,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 10.dp),
        )
    }
}

@Composable
fun GameCard(game: Game, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
    ) {
        Box {
            GameCover(
                game,
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.78f)
                    .clip(RoundedCornerShape(12.dp)),
            )
            if (game.favorite) {
                Icon(
                    Icons.Filled.Favorite,
                    contentDescription = "已收藏",
                    tint = Color(0xFFFF5370),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(22.dp)
                        .background(Color.Black.copy(alpha = 0.35f), CircleShape)
                        .padding(3.dp),
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            game.title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 2.dp),
        )
        PlatformTag(game.platform, Modifier.padding(start = 2.dp, top = 2.dp, bottom = 4.dp))
    }
}

@Composable
fun PlatformTag(platform: Platform, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(8.dp)
                .background(platform.brandColor, CircleShape),
        )
        Spacer(Modifier.width(4.dp))
        Text(
            platform.shortName,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun PlatformCard(platform: Platform, count: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = platform.brandColor, contentColor = Color.White),
        modifier = modifier
            .width(118.dp)
            .height(84.dp),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(platform.shortName, fontWeight = FontWeight.Black, fontSize = 22.sp)
            Column {
                Text(platform.chineseName, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("$count 款", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.8f))
            }
        }
    }
}
