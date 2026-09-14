package com.example.cpen321application

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.time.Duration.Companion.milliseconds


private open class Screen(val route: String) {
    object Landing : Screen("landing")
    object Util : Screen("util")
    object Canvas : Screen("canvas")
    object Timer : Screen("timer")
}

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
                navController = navController,
                apiBaseUrl = apiBaseUrl,
                modifier = modifier
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

}


@Composable
fun CanvasScreen(apiBaseUrl: String,
               navController: NavController,
               modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center) {
        Text(
            text = "Canvas Screen"
        )

    }
}

@Composable
fun TimerScreen(apiBaseUrl: String,
               navController: NavController,
               modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center) {
        Text(
            text = "Timer Screen"
        )

    }
}



@Composable
fun LandingPage(apiBaseUrl: String,
                navController: NavController,
                modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center) {


        Button(
            modifier = modifier
                .align(Alignment.CenterHorizontally),
            onClick = {
                navController.navigate(Screen.Util.route)
            }) {
            Text(text = "Util Screen")

        }

        Button(
            modifier = modifier
                .align(Alignment.CenterHorizontally),
            onClick = {
                navController.navigate(Screen.Canvas.route)
            }) {
            Text(text = "Canvas")

        }

        Button(
            modifier = modifier
                .align(Alignment.CenterHorizontally),
            onClick = {
                navController.navigate(Screen.Timer.route)
            }) {
            Text(text = "Timer")

        }
    }


}