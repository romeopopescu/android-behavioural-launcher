package com.example.abl.presentation.screens.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    onNavigateToUsageStats: () -> Unit
) {
        Button(
            onClick = onNavigateToUsageStats
        ) {
            Text("App Usage")
        }
//    val context = LocalContext.current
//    val totalPages = 3
//    val pagerState = rememberPagerState{ totalPages }
//    val lazyGridState = rememberLazyGridState()
//    var dragBoxIndex by remember { mutableIntStateOf(0) }

//    HorizontalPager(
//        state = pagerState,
//        modifier = Modifier.fillMaxSize(),
//    ) { page ->
//        Box(
//            modifier = Modifier
//                .padding(10.dp)
//                .fillMaxSize()
//                .background(Color.Gray),
//            contentAlignment = Alignment.Center
//        ) {
//            LazyVerticalGrid(
//                state = lazyGridState,
//                columns = GridCells.Fixed(4),
//                modifier = Modifier
//                    .fillMaxSize(),
//                userScrollEnabled = false
//            ) {
//                items(32) { index ->
//                    Box(
//                        modifier = Modifier
//                            .fillMaxSize()
//                            .fillMaxWidth()
//                            .padding(10.dp)
//                            .aspectRatio(1f)
//                            .background(Color.Blue)
//                            .dragAndDropTarget(
//                                shouldStartDragAndDrop = { event ->
//                                    event
//                                        .mimeTypes()
//                                        .contains(ClipDescription.MIMETYPE_TEXT_PLAIN)
//                                },
//                                target = remember {
//                                    object: DragAndDropTarget {
//                                        override fun onDrop(event: DragAndDropEvent): Boolean {
//                                            val text = event.toAndroidDragEvent()
//                                                .clipData?.getItemAt(0)?.text
//                                            println("Drag data was $text")
//                                            dragBoxIndex = index
//                                            return true
//                                        }
//
//                                    }
//                                }
//                            ),
//                        contentAlignment = Alignment.Center
//                    ) {
//                        androidx.compose.animation.AnimatedVisibility(
//                            visible = index == dragBoxIndex,
//                            enter = scaleIn() + fadeIn(),
//                            exit = scaleOut() + fadeOut()
//                        ) {
//                            Text(
//                                text = "App",
//                                fontSize = 30.sp,
//                                color = Color.White,
//                                fontWeight = FontWeight.Bold,
//                                modifier = Modifier
//                                    .dragAndDropSource {
//                                        detectTapGestures(
//                                            onLongPress = {
//                                                startTransfer(
//                                                    DragAndDropTransferData(
//                                                        clipData = ClipData.newPlainText("text", "Drag me!")
//                                                    )
//                                                )
//                                            }
//                                        )
//                                    }
//                            )
//                        }
//                    }
//                }
//            }
//        }
//    }
}
