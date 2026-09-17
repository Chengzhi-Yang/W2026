package com.example.cpen321application

import android.graphics.Point
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
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


private open class Screen(val route: String) {
    object Landing : Screen("landing")
    object Util : Screen("util")
    object Canvas : Screen("canvas")
    object Timer : Screen("timer")
}

@Serializable
data class PixelUpdate(val x: Int, val y: Int, val color: String)

@Composable
fun Navigation(apiBaseUrl: String, modifier: Modifier) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Screen.Landing.route, modifier = modifier){

        composable(Screen.Landing.route){
            LandingPage(
                navController = navController,
                apiBaseUrl = apiBaseUrl,
                modifier = modifier
            )
        }

        composable(Screen.Util.route){
            UtilScreen(
                navController = navController,
                apiBaseUrl = apiBaseUrl,
                modifier = modifier
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
                apiBaseUrl = apiBaseUrl,
                modifier = modifier
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
fun UtilScreen(apiBaseUrl: String,
             navController: NavController,
             modifier: Modifier = Modifier) {
    var statusText by remember { mutableStateOf("Checking backend at $apiBaseUrl/health...") }
    var currentTime by remember { mutableStateOf(LocalTime.now().truncatedTo(ChronoUnit.SECONDS)) }


    LaunchedEffect(apiBaseUrl) {
        statusText = fetchHealthStatus(apiBaseUrl)
    }

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = LocalTime.now().truncatedTo(ChronoUnit.SECONDS)
            delay(1000.milliseconds)
        }
    }

    Box(modifier = modifier.fillMaxSize()){
        Column() {
            Text(
                text = statusText,
                modifier = modifier
                    .padding(16.dp)
            )

            Text(
                text = "Hello World",
                modifier = modifier
                    .align(Alignment.CenterHorizontally)
            )

            Text(
                text = currentTime.toString(),
                modifier = modifier
                    .align(Alignment.CenterHorizontally)
            )

            Text(
                text = "GMT ${ZoneId.of("America/Vancouver").rules.getOffset(Instant.now())}",
                modifier = modifier
                    .align(Alignment.CenterHorizontally)
            )

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
        val wsUrl = "ws://10.0.2.2:3000/ws/pixels"
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
fun TimerScreen(apiBaseUrl: String,
               navController: NavController,
               modifier: Modifier = Modifier) {

    Box(modifier = modifier.fillMaxSize()){
        Column(modifier = modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center) {
            Text(
                text = "Timer Screen"
            )

        }

        HomeButton(navController = navController)
    }
}



@Composable
fun LandingPage(apiBaseUrl: String,
                navController: NavController,
                modifier: Modifier = Modifier) {

    val buttonModifier = Modifier.width(200.dp)

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
                navController.navigate(Screen.Util.route)
            }) {
            Text(text = "Util Screen")

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