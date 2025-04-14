@file:OptIn(ExperimentalFoundationApi::class)

package com.example.abl

import android.content.ClipData
import android.content.ClipDescription
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.draganddrop.dragAndDropSource
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.DragAndDropTransferData
import androidx.compose.ui.draganddrop.mimeTypes
import androidx.compose.ui.draganddrop.toAndroidDragEvent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.abl.presentation.viewmodel.AppInformationTest
import com.example.abl.presentation.viewmodel.LauncherViewModel
import com.example.abl.presentation.viewmodel.SearchViewModel
import com.example.abl.presentation.theme.AndroidLauncherForBehavouralProfileTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            AndroidLauncherForBehavouralProfileTheme {
                val viewModel = hiltViewModel<LauncherViewModel>()
                LauncherScreen()

            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AppDrawer(
    lazyListState: LazyListState,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = { onDismiss() },
        sheetState = sheetState,
        modifier = Modifier
            .fillMaxSize(),
        shape = RectangleShape,
        windowInsets = WindowInsets(0, 0, 0, 0),
        dragHandle = null
    ) {
        val viewModel: SearchViewModel = viewModel(factory =
            SearchViewModelFactory(context)
        )
        val searchText by viewModel.searchText.collectAsState()
        val apps by viewModel.apps.collectAsState()
        viewModel.deleteSearch()

        Spacer(modifier = Modifier.height(32.dp))
        LazyColumn (
            state = lazyListState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ){
            stickyHeader {
                Box (
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    Row {
                        TextField(
                            value = searchText,
                            onValueChange = viewModel::onSearchTextChange,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    RoundedCornerShape(8.dp)
                                ),
                            placeholder = { Text("Search") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "search icon"
                                )
                            },
                            trailingIcon = {
                                if (searchText.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.deleteSearch() }) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "clear search"
                                        )
                                    }
                                }
                            },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                unfocusedContainerColor = Color.Transparent,
                                focusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                disabledIndicatorColor = Color.Transparent
                            )
                        )
//                        val intent = Intent()
//                        Button(
//                            onClick = context.startActivity(intent)
//                        ) { }
                    }

                }

            }
            items(
                items = apps,
                key = {
                    it.id
                }
            ) { app ->
                Row (
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .clickable {
                            launchApp(context, app.packageName)
                            scope.launch { sheetState.hide() }.invokeOnCompletion {
                                if (!sheetState.isVisible) {
                                    onDismiss()
                                }
                            }
                        }
                ) {
                    Image(
                        bitmap = app.icon.toBitmap().asImageBitmap(),
                        contentDescription = app.name,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = app.name,
                    )
                }
            }
        }
    }

}

fun launchApp(context: Context, packageName: String) {
    val packageManager = context.packageManager
    val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
    if (launchIntent != null) {
        context.startActivity(launchIntent)
    }
}

@Composable
fun LauncherScreen(modifier: Modifier = Modifier) {
    var showAppDrawer by remember { mutableStateOf(false) }
    val lazyListState = rememberLazyListState()

    Box (
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures { _, dragAmount ->
                    if (!showAppDrawer && dragAmount.y < -50) {
                        showAppDrawer = true
                    }
                }
            }
    ) {
        HomeScreen()
        if (showAppDrawer) {
            AppDrawer(
                lazyListState = lazyListState,
                onDismiss = { showAppDrawer = false }
            )
        }
    }
}

//fun getCurrentWallPaper(context: Context): Bitmap {
//    val wallpaperManager = WallpaperManager.getInstance(context)
//    val wallpaperDrawable = wallpaperManager.drawable
//    val wallpaper = (wallpaperDrawable as BitmapDrawable).bitmap
//    return wallpaper
//}

@Composable
fun HomeScreen() {
    val context = LocalContext.current
//    val wallpaper = remember { getCurrentWallPaper(context) }
//
//    Box(
//        modifier = Modifier
//            .fillMaxSize(),
//        contentAlignment = Alignment.Center
//    ) {
//        wallpaper.let { bitmap ->
//            Image(
//                bitmap = bitmap.asImageBitmap(),
//                contentDescription = "Wallpaper",
//                modifier = Modifier.fillMaxSize(),
//                contentScale = ContentScale.Crop
//            )
//            Text(
//                text = "HomeScreen",
//                color = MaterialTheme.colorScheme.primary
//            )
//        }

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

@Composable
fun HomeScreenApp(app: AppInformationTest, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.padding(10.dp)
    ) {
        Image(
            bitmap = app.icon.toBitmap().asImageBitmap(),
            contentDescription = app.name
        )
        Text(
            text = app.name,
            fontSize = 18.sp,
        )
    }
}

@Preview(showBackground = true)
@Composable
fun Preview() {
    AndroidLauncherForBehavouralProfileTheme {
        HomeScreen()
    }
}