package com.matias.pomodoro.ui.screens

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.matias.pomodoro.R
import com.matias.pomodoro.ui.theme.LocalPomodoroColors

private data class TutorialSlide(
    @param:DrawableRes val illustrationRes: Int,
    @param:StringRes val titleRes: Int,
    @param:StringRes val descriptionRes: Int
)

@Composable
fun TutorialOverlay(
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalPomodoroColors.current
    val slides = remember { tutorialSlides() }
    var currentPage by remember { mutableIntStateOf(0) }
    val isLastPage = currentPage == slides.lastIndex
    val pageDescription = stringResource(
        R.string.tutorial_page_indicator,
        currentPage + 1,
        slides.size
    )
    val floatTransition = rememberInfiniteTransition(label = "tutorial_tomato_float")
    val illustrationOffset by floatTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "tutorial_tomato_offset"
    )

    Dialog(
        onDismissRequest = onFinish,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            modifier = modifier.fillMaxSize(),
            color = colors.background
        ) {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val compactLayout = maxHeight < 680.dp
                val illustrationSize = if (compactLayout) 156.dp else 210.dp

                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .graphicsLayer(translationX = 72f, translationY = -72f)
                        .size(220.dp)
                        .clip(CircleShape)
                        .background(colors.primary.copy(alpha = 0.10f))
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .graphicsLayer(translationX = -80f, translationY = 80f)
                        .size(190.dp)
                        .clip(CircleShape)
                        .background(colors.accent.copy(alpha = 0.08f))
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(WindowInsets.statusBars.asPaddingValues())
                        .padding(WindowInsets.navigationBars.asPaddingValues())
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.app_name),
                            color = colors.primary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        TextButton(onClick = onFinish) {
                            Text(
                                text = stringResource(R.string.tutorial_skip),
                                color = colors.muted,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(Modifier.height(if (compactLayout) 6.dp else 14.dp))

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        colors = CardDefaults.cardColors(
                            containerColor = colors.surface.copy(alpha = 0.96f)
                        ),
                        shape = RoundedCornerShape(34.dp)
                    ) {
                        AnimatedContent(
                            targetState = currentPage,
                            transitionSpec = {
                                val direction = if (targetState > initialState) 1 else -1
                                (
                                    slideInHorizontally(tween(360)) { it * direction } +
                                        fadeIn(tween(260))
                                    ).togetherWith(
                                    slideOutHorizontally(tween(320)) { -it * direction } +
                                        fadeOut(tween(220))
                                )
                            },
                            label = "tutorial_page",
                            modifier = Modifier.fillMaxSize()
                        ) { page ->
                            val slide = slides[page]
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                                    .padding(
                                        horizontal = if (compactLayout) 22.dp else 28.dp,
                                        vertical = if (compactLayout) 16.dp else 26.dp
                                    ),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(illustrationSize)
                                        .clip(RoundedCornerShape(30.dp))
                                        .background(colors.primary.copy(alpha = 0.09f))
                                        .border(
                                            width = 1.dp,
                                            color = colors.primary.copy(alpha = 0.16f),
                                            shape = RoundedCornerShape(30.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Image(
                                        painter = painterResource(slide.illustrationRes),
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(illustrationSize * 0.86f)
                                            .graphicsLayer(translationY = illustrationOffset)
                                    )
                                }

                                Spacer(Modifier.height(if (compactLayout) 16.dp else 24.dp))

                                Text(
                                    text = stringResource(slide.titleRes),
                                    color = colors.onBackground,
                                    fontSize = if (compactLayout) 24.sp else 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    lineHeight = if (compactLayout) 29.sp else 34.sp
                                )

                                Spacer(Modifier.height(10.dp))

                                Text(
                                    text = stringResource(slide.descriptionRes),
                                    color = colors.muted,
                                    fontSize = if (compactLayout) 15.sp else 16.sp,
                                    textAlign = TextAlign.Center,
                                    lineHeight = if (compactLayout) 21.sp else 23.sp
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.semantics {
                            contentDescription = pageDescription
                        },
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        slides.indices.forEach { index ->
                            Box(
                                modifier = Modifier
                                    .size(
                                        width = if (index == currentPage) 24.dp else 8.dp,
                                        height = 8.dp
                                    )
                                    .clip(CircleShape)
                                    .background(
                                        if (index == currentPage) {
                                            colors.primary
                                        } else {
                                            colors.primary.copy(alpha = 0.25f)
                                        }
                                    )
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (isLastPage) {
                                onFinish()
                            } else {
                                currentPage++
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.primary,
                            contentColor = colors.onPrimary
                        ),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Text(
                            text = stringResource(
                                if (isLastPage) {
                                    R.string.tutorial_start
                                } else {
                                    R.string.tutorial_next
                                }
                            ),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

private fun tutorialSlides(): List<TutorialSlide> = listOf(
    TutorialSlide(
        illustrationRes = R.drawable.ic_tomato_welcome,
        titleRes = R.string.tutorial_welcome_title,
        descriptionRes = R.string.tutorial_welcome_description
    ),
    TutorialSlide(
        illustrationRes = R.drawable.ic_tomato_focus,
        titleRes = R.string.tutorial_focus_title,
        descriptionRes = R.string.tutorial_focus_description
    ),
    TutorialSlide(
        illustrationRes = R.drawable.ic_tomato_short_break,
        titleRes = R.string.tutorial_short_break_title,
        descriptionRes = R.string.tutorial_short_break_description
    ),
    TutorialSlide(
        illustrationRes = R.drawable.ic_tomato_cycle,
        titleRes = R.string.tutorial_cycle_title,
        descriptionRes = R.string.tutorial_cycle_description
    ),
    TutorialSlide(
        illustrationRes = R.drawable.ic_tomato_settings,
        titleRes = R.string.tutorial_settings_title,
        descriptionRes = R.string.tutorial_settings_description
    ),
    TutorialSlide(
        illustrationRes = R.drawable.ic_tomato_stats,
        titleRes = R.string.tutorial_stats_title,
        descriptionRes = R.string.tutorial_stats_description
    )
)
