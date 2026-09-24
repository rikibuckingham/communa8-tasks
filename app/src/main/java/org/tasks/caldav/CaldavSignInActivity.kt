package org.tasks.caldav

import android.annotation.SuppressLint
import android.app.Activity
import android.net.Uri
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebViewDatabase
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
import androidx.compose.ui.viewinterop.AndroidView
import dagger.hilt.android.AndroidEntryPoint
import org.tasks.R
import org.tasks.compose.components.SymbolIcon
import org.tasks.data.entity.CaldavAccount
import org.tasks.preferences.fragments.CaldavAccountSettingsHiltViewModel
import org.tasks.themes.TasksIcons
import org.tasks.themes.TasksSettingsTheme
import org.tasks.themes.Theme
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.Locale
import javax.inject.Inject

private enum class Communa8LoginStage {
    LOGIN,
    SAVING,
    ERROR,
}

@AndroidEntryPoint
class CaldavSignInActivity : ComponentActivity() {

    @Inject lateinit var theme: Theme

    private val viewModel: CaldavAccountSettingsHiltViewModel by viewModels()

    private var stage by mutableStateOf(Communa8LoginStage.LOGIN)
    private var flowError by mutableStateOf<String?>(null)
    private var loginWebView: WebView? = null

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
                        flowError = accountState.snackbar
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
                    when (stage) {
                        Communa8LoginStage.LOGIN -> {
                            Communa8LoginWebView(
                                modifier = Modifier
                                    .padding(padding)
                                    .fillMaxSize(),
                            )
                        }

                        Communa8LoginStage.SAVING -> {
                            Communa8StatusScreen(
                                modifier = Modifier.padding(padding),
                                saving = true,
                                error = null,
                                onRetry = ::retryLogin,
                            )
                        }

                        Communa8LoginStage.ERROR -> {
                            Communa8StatusScreen(
                                modifier = Modifier.padding(padding),
                                saving = false,
                                error = flowError ?: accountState.snackbar,
                                onRetry = ::retryLogin,
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        destroyLoginWebView()
        super.onDestroy()
    }

    private fun retryLogin() {
        flowError = null
        viewModel.dismissSnackbar()
        stage = Communa8LoginStage.LOGIN
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Composable
    private fun Communa8LoginWebView(
        modifier: Modifier = Modifier,
    ) {
        AndroidView(
            modifier = modifier,
            factory = { context ->
                WebView(context).also { webView ->
                    loginWebView = webView

                    webView.settings.javaScriptEnabled = true
                    webView.settings.domStorageEnabled = true
                    webView.settings.saveFormData = false
                    webView.settings.userAgentString = USER_AGENT

                    val cookieManager = CookieManager.getInstance()
                    cookieManager.setAcceptCookie(true)
                    cookieManager.setAcceptThirdPartyCookies(webView, false)

                    webView.webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(
                            view: WebView?,
                            request: WebResourceRequest?,
                        ): Boolean = handlePossibleLoginCallback(request?.url?.toString())

                        @Deprecated("Deprecated in Android")
                        override fun shouldOverrideUrlLoading(
                            view: WebView?,
                            url: String?,
                        ): Boolean = handlePossibleLoginCallback(url)
                    }

                    // Login Flow must start with a clean, one-time browser session.
                    cookieManager.removeAllCookies {
                        cookieManager.flush()
                        if (stage == Communa8LoginStage.LOGIN && loginWebView === webView) {
                            val headers = mapOf(
                                "OCS-APIREQUEST" to "true",
                                "Accept-Language" to Locale.getDefault().toLanguageTag(),
                            )
                            webView.loadUrl(COMMUNA8_LOGIN_URL, headers)
                        }
                    }
                }
            },
        )
    }

    private fun handlePossibleLoginCallback(url: String?): Boolean {
        if (url.isNullOrBlank() || !url.startsWith(LOGIN_CALLBACK_PREFIX, ignoreCase = true)) {
            return false
        }

        try {
            val credentials = parseLoginCallback(url)
            val normalizedServer = credentials.server.withHttpsIfMissing()
            val serverUri = Uri.parse(normalizedServer)

            if (
                !serverUri.scheme.equals("https", ignoreCase = true) ||
                !serverUri.host.equals(COMMUNA8_HOST, ignoreCase = true)
            ) {
                throw IllegalArgumentException(getString(R.string.communa8_login_untrusted_server))
            }

            stage = Communa8LoginStage.SAVING
            destroyLoginWebView()

            viewModel.setUrl(normalizedServer.toNextcloudDavUrl())
            viewModel.setUsername(credentials.loginName)
            viewModel.setPassword(credentials.appPassword)
            viewModel.setServerType(CaldavAccount.SERVER_NEXTCLOUD)
            viewModel.save {
                setResult(Activity.RESULT_OK)
                finish()
            }
        } catch (e: Exception) {
            destroyLoginWebView()
            flowError = e.message ?: getString(R.string.communa8_login_failed)
            stage = Communa8LoginStage.ERROR
        }

        return true
    }

    private fun parseLoginCallback(url: String): LoginCredentials {
        val payload = url.substringAfter(LOGIN_CALLBACK_PREFIX, missingDelimiterValue = "")
        val match = LOGIN_CALLBACK_REGEX.matchEntire(payload)
            ?: throw IllegalArgumentException(getString(R.string.communa8_login_bad_callback))

        return LoginCredentials(
            server = decodeLoginValue(match.groupValues[1]),
            loginName = decodeLoginValue(match.groupValues[2]),
            appPassword = decodeLoginValue(match.groupValues[3]),
        )
    }

    private fun decodeLoginValue(value: String): String =
        URLDecoder.decode(value, StandardCharsets.UTF_8.name())

    private fun destroyLoginWebView() {
        val webView = loginWebView ?: return
        loginWebView = null

        runCatching {
            webView.stopLoading()
            webView.loadUrl("about:blank")
            webView.clearHistory()
            webView.clearCache(true)
            webView.removeAllViews()
            webView.destroy()
        }

        runCatching {
            WebViewDatabase.getInstance(this).clearFormData()
            CookieManager.getInstance().removeAllCookies(null)
            CookieManager.getInstance().flush()
        }
    }

    private data class LoginCredentials(
        val server: String,
        val loginName: String,
        val appPassword: String,
    )

    private fun String.withHttpsIfMissing(): String =
        if (contains("://")) this else "https://$this"

    private fun String.toNextcloudDavUrl(): String =
        trimEnd('/') + "/remote.php/dav"

    companion object {
        private const val COMMUNA8_HOST = "app.communa8.org"
        private const val COMMUNA8_LOGIN_URL =
            "https://app.communa8.org/index.php/login/flow"
        private const val USER_AGENT = "Communa8 Tasks Android"
        private const val LOGIN_CALLBACK_PREFIX = "nc://login/"
        private val LOGIN_CALLBACK_REGEX =
            Regex("^server:(.*?)&user:(.*?)&password:(.*)$")
    }
}

@Composable
private fun Communa8StatusScreen(
    modifier: Modifier = Modifier,
    saving: Boolean,
    error: String?,
    onRetry: () -> Unit,
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
            if (saving) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = stringResource(R.string.communa8_login_saving),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            } else {
                Text(
                    text = error ?: stringResource(R.string.communa8_login_failed),
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = onRetry,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.communa8_try_again))
                }
            }
        }
    }
}
