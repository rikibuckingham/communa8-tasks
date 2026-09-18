package org.tasks.caldav

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.tasks.R
import org.tasks.compose.components.SymbolIcon
import org.tasks.data.entity.CaldavAccount
import org.tasks.preferences.fragments.CaldavAccountSettingsHiltViewModel
import org.tasks.themes.TasksIcons
import org.tasks.themes.TasksSettingsTheme
import org.tasks.themes.Theme
import java.io.IOException
import javax.inject.Inject

private enum class Communa8LoginStage {
    READY,
    STARTING,
    WAITING,
    SAVING,
    ERROR,
}

@AndroidEntryPoint
class CaldavSignInActivity : ComponentActivity() {

    @Inject lateinit var theme: Theme

    private val viewModel: CaldavAccountSettingsHiltViewModel by viewModels()
    private val httpClient = OkHttpClient()
    private var loginJob: Job? = null

    private var stage by mutableStateOf(Communa8LoginStage.READY)
    private var flowError by mutableStateOf<String?>(null)

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            TasksSettingsTheme(
                theme = theme.themeBase.index,
                primary = theme.themeColor.primaryColor,
            ) {
                val accountState by viewModel.state.collectAsState()

                LaunchedEffect(accountState.snackbar) {
                    if (accountState.snackbar != null && stage == Communa8LoginStage.SAVING) {
                        stage = Communa8LoginStage.ERROR
                    }
                }

                Scaffold(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    topBar = {
                        TopAppBar(
                            navigationIcon = {
                                IconButton(onClick = { finish() }) {
                                    SymbolIcon(
                                        name = TasksIcons.ARROW_BACK,
                                        contentDescription = stringResource(R.string.back),
                                    )
                                }
                            },
                            title = { Text(stringResource(R.string.communa8_connect_title)) },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                            ),
                        )
                    },
                ) { padding ->
                    Communa8LoginScreen(
                        modifier = Modifier.padding(padding),
                        stage = stage,
                        error = flowError ?: accountState.snackbar,
                        onStart = ::startLogin,
                        onCancel = { finish() },
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        loginJob?.cancel()
        httpClient.dispatcher.executorService.shutdown()
        httpClient.connectionPool.evictAll()
        super.onDestroy()
    }

    private fun startLogin() {
        if (loginJob?.isActive == true) return

        flowError = null
        viewModel.dismissSnackbar()
        loginJob = lifecycleScope.launch {
            try {
                stage = Communa8LoginStage.STARTING
                val session = withContext(Dispatchers.IO) { createLoginSession() }

                try {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(session.loginUrl)))
                } catch (e: ActivityNotFoundException) {
                    throw IOException(getString(R.string.communa8_no_browser), e)
                }

                stage = Communa8LoginStage.WAITING
                val credentials = pollForCredentials(session)

                stage = Communa8LoginStage.SAVING
                viewModel.setUrl(credentials.server)
                viewModel.setUsername(credentials.loginName)
                viewModel.setPassword(credentials.appPassword)
                viewModel.setServerType(CaldavAccount.SERVER_NEXTCLOUD)
                viewModel.save {
                    setResult(Activity.RESULT_OK)
                    finish()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                flowError = e.message ?: getString(R.string.communa8_login_failed)
                stage = Communa8LoginStage.ERROR
            }
        }
    }

    private fun createLoginSession(): LoginSession {
        val request = Request.Builder()
            .url("$COMMUNA8_SERVER/index.php/login/v2")
            .header("User-Agent", USER_AGENT)
            .post(FormBody.Builder().build())
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException(
                    getString(R.string.communa8_login_http_error, response.code)
                )
            }

            val body = response.body.string()
            val json = JSONObject(body)
            val poll = json.getJSONObject("poll")
            return LoginSession(
                loginUrl = json.getString("login"),
                token = poll.getString("token"),
                pollEndpoint = poll.getString("endpoint"),
            )
        }
    }

    private suspend fun pollForCredentials(session: LoginSession): LoginCredentials =
        withContext(Dispatchers.IO) {
            repeat(POLL_ATTEMPTS) {
                val body = FormBody.Builder()
                    .add("token", session.token)
                    .build()
                val request = Request.Builder()
                    .url(session.pollEndpoint)
                    .header("User-Agent", USER_AGENT)
                    .post(body)
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    when (response.code) {
                        200 -> {
                            val json = JSONObject(response.body.string())
                            return@withContext LoginCredentials(
                                server = json.getString("server"),
                                loginName = json.getString("loginName"),
                                appPassword = json.getString("appPassword"),
                            )
                        }
                        404 -> Unit
                        else -> throw IOException(
                            getString(R.string.communa8_login_http_error, response.code)
                        )
                    }
                }

                delay(POLL_INTERVAL_MS)
            }

            throw IOException(getString(R.string.communa8_login_timed_out))
        }

    private data class LoginSession(
        val loginUrl: String,
        val token: String,
        val pollEndpoint: String,
    )

    private data class LoginCredentials(
        val server: String,
        val loginName: String,
        val appPassword: String,
    )

    companion object {
        private const val COMMUNA8_SERVER = "https://app.communa8.org"
        private const val USER_AGENT = "Communa8 Tasks Android"
        private const val POLL_INTERVAL_MS = 1_000L
        private const val POLL_ATTEMPTS = 20 * 60
    }
}

@Composable
private fun Communa8LoginScreen(
    modifier: Modifier = Modifier,
    stage: Communa8LoginStage,
    error: String?,
    onStart: () -> Unit,
    onCancel: () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 480.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.communa8_sign_in),
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(12.dp))

            when (stage) {
                Communa8LoginStage.READY -> {
                    Text(
                        text = stringResource(R.string.communa8_login_description),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(28.dp))
                    Button(
                        onClick = onStart,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.communa8_sign_in))
                    }
                }

                Communa8LoginStage.STARTING,
                Communa8LoginStage.WAITING,
                Communa8LoginStage.SAVING -> {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = stringResource(
                            when (stage) {
                                Communa8LoginStage.STARTING ->
                                    R.string.communa8_login_starting
                                Communa8LoginStage.WAITING ->
                                    R.string.communa8_login_waiting
                                else ->
                                    R.string.communa8_login_saving
                            }
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    TextButton(onClick = onCancel) {
                        Text(stringResource(R.string.cancel))
                    }
                }

                Communa8LoginStage.ERROR -> {
                    Text(
                        text = error ?: stringResource(R.string.communa8_login_failed),
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = onStart,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.communa8_try_again))
                    }
                }
            }
        }
    }
}
