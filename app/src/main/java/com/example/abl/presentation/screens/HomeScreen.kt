package com.example.abl.presentation.screens

import android.content.ClipData
import android.content.ClipDescription
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.draganddrop.dragAndDropSource
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.DragAndDropTransferData
import androidx.compose.ui.draganddrop.mimeTypes
import androidx.compose.ui.draganddrop.toAndroidDragEvent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen() {
    val context = LocalContext.current
    val totalPages = 3
    val pagerState = rememberPagerState{ totalPages }
    val lazyGridState = rememberLazyGridState()
    var dragBoxIndex by remember { mutableIntStateOf(0) }

    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize(),
    ) { page ->
        Box(
            modifier = Modifier
                .padding(10.dp)
                .fillMaxSize()
                .background(Color.Gray),
            contentAlignment = Alignment.Center
        ) {
            LazyVerticalGrid(
                state = lazyGridState,
                columns = GridCells.Fixed(4),
                modifier = Modifier
                    .fillMaxSize(),
                userScrollEnabled = false
            ) {
                items(32) { index ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .fillMaxWidth()
                            .padding(10.dp)
                            .aspectRatio(1f)
                            .background(Color.Blue)
                            .dragAndDropTarget(
                                shouldStartDragAndDrop = { event ->
                                    event
                                        .mimeTypes()
                                        .contains(ClipDescription.MIMETYPE_TEXT_PLAIN)
                                },
                                target = remember {
                                    object: DragAndDropTarget {
                                        override fun onDrop(event: DragAndDropEvent): Boolean {
                                            val text = event.toAndroidDragEvent()
                                                .clipData?.getItemAt(0)?.text
                                            println("Drag data was $text")
                                            dragBoxIndex = index
                                            return true
                                        }

                                    }
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.animation.AnimatedVisibility(
                            visible = index == dragBoxIndex,
                            enter = scaleIn() + fadeIn(),
                            exit = scaleOut() + fadeOut()
                        ) {
                            Text(
                                text = "App",
                                fontSize = 30.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .dragAndDropSource {
                                        detectTapGestures(
                                            onLongPress = {
                                                startTransfer(
                                                    DragAndDropTransferData(
                                                        clipData = ClipData.newPlainText("text", "Drag me!")
                                                    )
                                                )
                                            }
                                        )
                                    }
                            )
                        }
                    }
                }
            }
        }
    }
}
