package com.example.cpen321application

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.example.cpen321application.ui.theme.CPEN321ApplicationTheme
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.google.android.libraries.identity.googleid.GetGoogleIdOption


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CPEN321ApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Navigation(
                        apiBaseUrl = BuildConfig.API_BASE_URL,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}



suspend fun fetchHealthStatus(apiBaseUrl: String): String = withContext(Dispatchers.IO) {
    val healthUrl = "${apiBaseUrl.trimEnd('/')}/health"
    try {
        val connection = (URL(healthUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 5_000
            readTimeout = 5_000
        }

        when (val code = connection.responseCode) {
            HttpURLConnection.HTTP_OK -> {
                val body = connection.inputStream.bufferedReader().use { it.readText() }
                "Backend healthy ($healthUrl): $body"
            }
            else -> {
                val errorBody = connection.errorStream?.bufferedReader()?.use { it.readText() }
                "Backend error ($healthUrl): HTTP $code${errorBody?.let { " — $it" } ?: ""}"
            }
        }
    } catch (e: Exception) {
        "Backend unreachable ($healthUrl): ${e.message ?: e.javaClass.simpleName}"
    }
}

suspend fun fetchName(apiBaseUrl: String): String = withContext(Dispatchers.IO) {
    val nameUrl = "${apiBaseUrl.trimEnd('/')}/user"
    try {
        val connection = (URL(nameUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 5_000
            readTimeout = 5_000
        }

        when (val code = connection.responseCode) {
            HttpURLConnection.HTTP_OK -> {
                val body = connection.inputStream.bufferedReader().use { it.readText() }
                body
            }
            else -> {
                "Error ($nameUrl): HTTP $code"
            }
        }
    } catch (e: Exception) {
        "Backend unreachable ($nameUrl): ${e.message ?: e.javaClass.simpleName}"
    }
}

suspend fun fetchIp(apiBaseUrl: String): String = withContext(Dispatchers.IO) {
    val ipUrl = "${apiBaseUrl.trimEnd('/')}/ip"
    try {
        val connection = (URL(ipUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 5_000
            readTimeout = 5_000
        }

        when (val code = connection.responseCode) {
            HttpURLConnection.HTTP_OK -> {
                val body = connection.inputStream.bufferedReader().use { it.readText() }
                body
            }
            else -> {
                "Error ($ipUrl): HTTP $code"
            }
        }
    } catch (e: Exception) {
        "Backend unreachable ($ipUrl): ${e.message ?: e.javaClass.simpleName}"
    }
}

suspend fun fetchTime(apiBaseUrl: String): String = withContext(Dispatchers.IO) {
    val timeUrl = "${apiBaseUrl.trimEnd('/')}/time"
    try {
        val connection = (URL(timeUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 5_000
            readTimeout = 5_000
        }

        when (val code = connection.responseCode) {
            HttpURLConnection.HTTP_OK -> {
                val body = connection.inputStream.bufferedReader().use { it.readText() }
                body
            }
            else -> {
                "Error ($timeUrl): HTTP $code"
            }
        }
    } catch (e: Exception) {
        "Backend unreachable ($timeUrl): ${e.message ?: e.javaClass.simpleName}"
    }
}


suspend fun loginWithGoogle(context: Context): String? = withContext(Dispatchers.Main) {
    val credentialManager = CredentialManager.create(context)
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(BuildConfig.GOOGLE_CLIENT_ID)
            .build())
        .build()

    try {
        val result = credentialManager.getCredential(context, request)
        val credential = result.credential

        if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)

            // Return the name found inside the Google credential directly
            "${googleIdTokenCredential.givenName} ${googleIdTokenCredential.familyName}"
        } else null
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}