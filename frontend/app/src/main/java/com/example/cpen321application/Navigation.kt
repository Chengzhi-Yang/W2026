package com.example.cpen321application

import android.graphics.Point
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.time.Duration.Companion.milliseconds
import androidx.core.graphics.toColorInt
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive


private open class Screen(val route: String) {
    object Landing : Screen("landing")
    object Util : Screen("util")
    object Canvas : Screen("canvas")
    object Timer : Screen("timer")
    object Surprise : Screen("surprise")
}

@Serializable
data class PixelUpdate(
    val x: Int,
    val y: Int,
    val color: String
)

@Serializable
data class UserResponse(
    val firstName: String,
    val lastName: String
)

@Serializable
data class InfoResponse(
    val time: String? = null,
    val serverIp: String? = null,
    val clientIp: String? = null,
    val timeZone: String? = null
)

suspend fun fetchRandomXkcd(
    client: OkHttpClient): Pair<String, String> = withContext(Dispatchers.IO) {

        var result: Pair<String, String>? = null

        while (result == null) {
            val randomImageNum = (1..3299).random()
            val url = "https://xkcd.com/$randomImageNum/info.0.json"
            try {
                val response = client.newCall(Request.Builder().url(url).build()).execute()
                val jsonString = response.body?.string() ?: ""
                val json = Json.parseToJsonElement(jsonString).jsonObject
                val originalTitle = json["safe_title"]?.jsonPrimitive?.content ?: ""
                val imgUrl = json["img"]?.jsonPrimitive?.content ?: ""

                if (originalTitle.isNotBlank() && imgUrl.isNotBlank()) {
                    result = imgUrl to originalTitle.lowercase().replace(" ", "_")
                }
            } catch (e: Exception) {
                delay(500.milliseconds)
                e.printStackTrace()
            }
        }
        result
}


@Composable
fun Navigation(apiBaseUrl: String, modifier: Modifier) {
    val navController = rememberNavController()

    var loggedInUserName by remember { mutableStateOf("Guest User") }

    var imageUrl  by remember { mutableStateOf<String?>(null) }
    var comicTitle by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    val client = remember { OkHttpClient() }

    val triggerPreload = {
        imageUrl = null
        scope.launch {
            val data = fetchRandomXkcd(client)

            data.let { pair ->
                imageUrl = pair.first
                comicTitle = pair.second
            }
        }
    }

    NavHost(navController = navController, startDestination = Screen.Landing.route, modifier = modifier){

        composable(Screen.Landing.route){
            LandingPage(
                navController = navController,
                modifier = modifier,

                onLoginSuccess = { name -> loggedInUserName = name }
            )
        }

        composable(Screen.Util.route){
            UtilScreen(
                navController = navController,
                apiBaseUrl = apiBaseUrl,
                modifier = modifier,
                userName = loggedInUserName
            )
        }

        composable(Screen.Canvas.route){
            CanvasScreen(
                apiBaseUrl = apiBaseUrl,
                navController = navController,
                modifier = modifier,
            )
        }

        composable(Screen.Timer.route){
            TimerScreen(
                navController = navController,
                modifier = modifier,

                onStartTimer = triggerPreload,
                onEnterScreen = triggerPreload
            )
        }

        composable(Screen.Surprise.route){
            SurpriseScreen(
                imageUrl = imageUrl,
                navController = navController,
                modifier = modifier,
            )
        }


    }
}

@Composable
fun BoxScope.HomeButton(navController: NavController) {
    SmallFloatingActionButton(
        modifier = Modifier
            .padding(16.dp)
            .align(Alignment.BottomEnd), // Standard position
        onClick = {
            // Using popUpTo ensures we clear the backstack when going home
            navController.navigate(Screen.Landing.route) {
                popUpTo(Screen.Landing.route) { inclusive = true }
            }
        },
    ) {
        Icon(Icons.Filled.Home, contentDescription = "Home")
    }
}


fun DrawScope.pixel(point: Point, color: Color) {
    val pixelSize = 60f

    drawRect(
        topLeft = Offset((point.x + 1) * pixelSize, (point.y + 1) * pixelSize),
        color = color,
        size = Size(pixelSize, pixelSize)
    )

}

@Composable
fun UtilScreen(
    apiBaseUrl: String,
    navController: NavController,
    modifier: Modifier = Modifier,
    userName: String
) {
    var statusText by remember { mutableStateOf("Checking backend...") }
    var fullName by remember { mutableStateOf("Loading...") }
    var clientIp by remember { mutableStateOf("Loading...") }
    var serverIp by remember { mutableStateOf("Loading...") }
    var serverTime by remember { mutableStateOf("Loading...") }

    // Helper to format GMT offset (e.g., +00:00)
    fun formatOffset(instant: Instant, zoneId: ZoneId): String {
        val offset = zoneId.rules.getOffset(instant)
        return offset.id.let { if (it == "Z") "+00:00" else it }
    }

    LaunchedEffect(apiBaseUrl) {
        // 1. Health
        launch { statusText = fetchHealthStatus(apiBaseUrl) }

        // 2. Name
        launch {
            try {
                val json = fetchName(apiBaseUrl)
                val user = Json.decodeFromString<UserResponse>(json)
                fullName = "${user.firstName} ${user.lastName}"
            } catch (t: Throwable) {
                fullName = "Error"
                t.printStackTrace()
            }
        }

        // 3. IP
        launch {
            try {
                val json = fetchIp(apiBaseUrl)
                val info = Json.decodeFromString<InfoResponse>(json)
                serverIp = info.serverIp ?: "Unknown"
                clientIp = info.clientIp ?: "Unknown"
            } catch (t: Throwable) {
                serverIp = "Error"; clientIp = "Error"
                t.printStackTrace()
            }
        }

        // 4. Time
        launch {
            try {
                val json = fetchTime(apiBaseUrl)
                val info = Json.decodeFromString<InfoResponse>(json)
                val iso = info.time
                val tz = info.timeZone
                if (iso != null && tz != null) {
                    val inst = Instant.parse(iso)
                    val zid = ZoneId.of(tz)
                    val local = inst.atZone(zid).toLocalTime().truncatedTo(ChronoUnit.SECONDS)
                    serverTime = "$local GMT ${formatOffset(inst, zid)}"
                } else { serverTime = "Unknown" }
            } catch (t: Throwable) {
                serverTime = "Error"
                t.printStackTrace()
            }
        }
    }

    Box(modifier = modifier.fillMaxSize().padding(10.dp)) {
        Column {
            Text(text = statusText, modifier = Modifier.padding(bottom = 10.dp))
            Text(text = "Name: $fullName")
            Text(text = "Logged in user: $userName")
            Text(text = "Client IP: $clientIp")
            Text(text = "Server IP: $serverIp")
            Text(text = "Server Time: $serverTime")

            // Fixed Client Time Logic
            val now = Instant.now()
            val sysZone = ZoneId.systemDefault()
            val clientLocalTime = LocalTime.now().truncatedTo(ChronoUnit.SECONDS)
            Text(text = "Client time: $clientLocalTime GMT ${formatOffset(now, sysZone)}")
        }
        HomeButton(navController = navController)
    }
}


@Composable
fun CanvasScreen(
    apiBaseUrl: String,
    navController: NavController,
    modifier: Modifier = Modifier,
) {
    val pixels = remember { mutableStateMapOf<Pair<Int, Int>, Color>() }

    LaunchedEffect(apiBaseUrl) {
        val client = OkHttpClient()
        val host = apiBaseUrl.removePrefix("http://").removeSuffix(":3000").trimEnd('/')
        val wsUrl = "ws://$host:3000/ws/pixels"

        val request = Request.Builder().url(wsUrl).build()

        val listener = object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val update = Json.decodeFromString<PixelUpdate>(text)
                    // Update state (triggers Canvas redraw)
                    pixels[update.x to update.y] = Color(update.color.toColorInt())
                } catch (e: Exception) { e.printStackTrace() }
            }
        }

        val webSocket = client.newWebSocket(request, listener)
        try {
            awaitCancellation() // Keep alive while on screen
        } finally {
            webSocket.close(1000, "Done")
        }

    }

    Box(modifier = modifier.fillMaxSize()){

        Canvas(modifier = modifier
            .fillMaxSize()
            .align(Alignment.Center)
        ) {

            val canvasSize = 17 * 60

            for (i in 60.. canvasSize step 60) {
                drawLine(
                    start = Offset(i.toFloat(), 60f),
                    end = Offset(i.toFloat(), canvasSize.toFloat()),
                    color = Color.Black,
                    strokeWidth = 3f
                )
            }
            for (j in 60.. canvasSize step 60) {

                drawLine(
                    start = Offset(60f, j.toFloat()),
                    end = Offset(canvasSize.toFloat(), j.toFloat()),
                    color = Color.Black,
                    strokeWidth = 3f
                )
            }

            pixels.forEach { (coords, color) ->
                pixel(Point(coords.first, coords.second), color)
            }

        }

        Text(
            text = "Canvas Screen",
            modifier = Modifier
                .align(Alignment.TopCenter)
        )

        HomeButton(navController = navController)
    }
}

@Composable
fun TimerScreen(
    navController: NavController,
    modifier: Modifier = Modifier,
    onStartTimer: () -> Job,
    onEnterScreen: () -> Job,
) {

    val context = LocalContext.current

    val hourListState = rememberLazyListState()
    val hourSnapFlingBehavior = rememberSnapFlingBehavior(lazyListState = hourListState)
    val minuteListState = rememberLazyListState()
    val minuteSnapFlingBehavior = rememberSnapFlingBehavior(lazyListState = minuteListState)
    val secondListState = rememberLazyListState()
    val secondSnapFlingBehavior = rememberSnapFlingBehavior(lazyListState = secondListState)


    val columnHeight = 120.dp
    val itemHeight = 25.dp
    val verticalPadding = (columnHeight - itemHeight) / 2

    var showCountDownPopUp by remember { mutableStateOf(false) }
    var timeRemaining by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        onEnterScreen()
    }


    if (showCountDownPopUp) {
        LaunchedEffect(timeRemaining) {
            while (timeRemaining > 0) {
                delay(1000.milliseconds)
                timeRemaining--
            }
            if (timeRemaining == 0) {
                delay(500.milliseconds)
                showCountDownPopUp = false
                navController.navigate(Screen.Surprise.route)
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()){
        Column(
            modifier = modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Timer Screen"
            )

            Row(
                modifier = modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically

            ){

                LazyColumn(
                    state = hourListState,
                    flingBehavior = hourSnapFlingBehavior,
                    modifier = Modifier
                        .height(columnHeight)
                        .padding(horizontal = 5.dp)
                        .width(20.dp),

                    contentPadding = PaddingValues(vertical = verticalPadding)
                    ) {
                    items(24) { index ->
                        Text(
                            modifier = Modifier.height(itemHeight),
                            text = "$index"
                        )
                    }
                }

                Text(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    text = "Hours : "
                )

                LazyColumn(
                    state = minuteListState,
                    flingBehavior = minuteSnapFlingBehavior,
                    modifier = Modifier
                        .height(columnHeight)
                        .padding(horizontal = 5.dp)
                        .width(20.dp),
                    contentPadding = PaddingValues(vertical = verticalPadding)
                ) {
                    items(60) { index ->
                        Text(
                            modifier = Modifier.height(itemHeight),
                            text = "$index"
                        )
                    }
                }

                Text(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    text = "Minutes : "
                )

                LazyColumn(
                    state = secondListState,
                    flingBehavior = secondSnapFlingBehavior,
                    modifier = Modifier
                        .height(columnHeight)
                        .padding(horizontal = 5.dp)
                        .width(20.dp),
                    contentPadding = PaddingValues(vertical = verticalPadding)
                ) {
                    items(60) { index ->
                        Text(
                            modifier = Modifier.height(itemHeight),
                            text = "$index"
                        )
                    }
                }

                Text(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    text = "Seconds "
                )
            }


            Button(
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 20.dp,
                ),
                shape = RoundedCornerShape(5.dp),
                modifier = Modifier
                    .width(120.dp)
                    .padding(10.dp),
                onClick = {
                    val hours = hourListState.firstVisibleItemIndex
                    val minutes = minuteListState.firstVisibleItemIndex
                    val seconds = secondListState.firstVisibleItemIndex

                    val totalSeconds = hours*3600 + minutes*60 + seconds

                    Toast.makeText(
                        context,
                        "Timer started for: $hours h :  $minutes m : ${seconds}s",
                        Toast.LENGTH_SHORT
                    ).show()

                    if (totalSeconds > 0) {
                        timeRemaining = totalSeconds
                        showCountDownPopUp = true
                    }

                    onStartTimer()

                }) {
                Text(text = "Start")
            }

        }

        if (showCountDownPopUp) {
            AlertDialog(
                onDismissRequest = { showCountDownPopUp = false },
                title = { Text("Time Remaining") },
                text = {
                    val hourRemaining = timeRemaining / 3600
                    val minuteRemaining = (timeRemaining % 3600) / 60
                    val secondRemaining = timeRemaining % 60

                    val timeRemainingFormatted = "$hourRemaining h : $minuteRemaining m : $secondRemaining s"

                    Text(
                        text = timeRemainingFormatted,
                        style = typography.headlineLarge
                    )
                },
                confirmButton = {
                    Button(onClick = { showCountDownPopUp = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        HomeButton(navController = navController)
    }
}


@Composable
fun SurpriseScreen(
    navController: NavController,
    modifier: Modifier = Modifier,
    imageUrl : String?
) {


    Box(modifier = modifier.fillMaxSize()){

        Text(
            text = "Loading...",
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 120.dp),
            style = typography.bodyLarge
        )

        AsyncImage(
            model = imageUrl,
            contentDescription = "Surprise",
            alignment = Alignment.TopCenter,
            modifier = modifier
                .fillMaxSize()
                .padding(top = 60.dp, start = 5.dp, end = 5.dp),

            contentScale = ContentScale.Fit
        )

        HomeButton(navController = navController)
    }

}


@Composable
fun LandingPage(
    navController: NavController,
    modifier: Modifier = Modifier,
    onLoginSuccess: (String) -> Unit
) {

    val buttonModifier = Modifier.width(200.dp)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Column(modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically)) {


        Button(
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 20.dp,
            ),
            shape = RoundedCornerShape(5.dp),
            modifier = buttonModifier,
            onClick = {
                scope.launch {
                    val name = loginWithGoogle(context)
                    if (name != null) {
                        onLoginSuccess(name) // Pass name back to Navigation
                        navController.navigate(Screen.Util.route)
                    }
                }
            }) {
            Text(text = "Login / Util Screen")

        }

        Button(
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 20.dp,
            ),
            shape = RoundedCornerShape(5.dp),
            modifier = buttonModifier,
            onClick = {
                navController.navigate(Screen.Canvas.route)
            }) {
            Text(text = "Canvas")

        }

        Button(
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 20.dp,
            ),
            shape = RoundedCornerShape(5.dp),
            modifier = buttonModifier,
            onClick = {
                navController.navigate(Screen.Timer.route)
            }) {
            Text(text = "Timer")

        }
    }


}