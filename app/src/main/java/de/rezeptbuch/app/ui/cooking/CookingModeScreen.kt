package de.rezeptbuch.app.ui.cooking

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.os.SystemClock
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.rezeptbuch.app.R
import de.rezeptbuch.app.domain.model.CookingStep
import de.rezeptbuch.app.domain.portion.formatTimer
import de.rezeptbuch.app.service.timer.CookingTimer
import de.rezeptbuch.app.ui.components.IngredientRows
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun CookingModeScreen(
    onClose: () -> Unit,
    viewModel: CookingModeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    KeepScreenOnAndImmersive()

    val recipe = state.recipe
    if (recipe == null || recipe.steps.isEmpty()) {
        Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
        return
    }

    val pagerState = rememberPagerState(pageCount = { recipe.steps.size })
    val scope = rememberCoroutineScope()
    var ingredientsOpen by rememberSaveable { mutableStateOf(false) }

    // Sekündlicher Takt für die Countdown-Anzeige
    val now by produceState(SystemClock.elapsedRealtime()) {
        while (true) {
            value = SystemClock.elapsedRealtime()
            delay(250)
        }
    }

    // Benachrichtigungsrecht (Android 13+) vor dem ersten Timer anfragen
    var pendingTimerStep by rememberSaveable { mutableStateOf<Int?>(null) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        pendingTimerStep?.let(viewModel::startTimer)
        pendingTimerStep = null
    }
    val startTimer: (Int) -> Unit = { step ->
        if (Build.VERSION.SDK_INT >= 33) {
            pendingTimerStep = step
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            viewModel.startTimer(step)
        }
    }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            // --- Kopfzeile mit Fortschritt
            Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose) { Icon(Icons.Filled.Close, stringResource(R.string.cooking_mode_exit)) }
                Text(
                    recipe.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    stringResource(R.string.step_progress, pagerState.currentPage + 1, recipe.steps.size),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(end = 12.dp),
                )
            }
            LinearProgressIndicator(
                progress = { (pagerState.currentPage + 1f) / recipe.steps.size },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            )

            // --- Laufende Timer anderer Schritte
            val otherTimers = state.timers.filterKeys { it != pagerState.currentPage }.values.sortedBy { it.stepIndex }
            if (otherTimers.isNotEmpty()) {
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    otherTimers.forEach { timer ->
                        AssistChip(
                            onClick = { scope.launch { pagerState.animateScrollToPage(timer.stepIndex) } },
                            label = {
                                Text(
                                    stringResource(R.string.step_number, timer.stepIndex + 1) + " · " +
                                        if (timer.finished) stringResource(R.string.timer_done_short)
                                        else formatTimer((timer.remainingMs(now) + 999) / 1000),
                                )
                            },
                            leadingIcon = { Icon(Icons.Outlined.Timer, null, Modifier.size(18.dp)) },
                        )
                    }
                }
            }

            // --- Ein Schritt pro Seite, Wischen zum Blättern
            HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { page ->
                StepPage(
                    index = page,
                    step = recipe.steps[page],
                    timer = state.timers[page],
                    now = now,
                    onStartTimer = { startTimer(page) },
                    onCancelTimer = { viewModel.cancelTimer(page) },
                )
            }

            // --- Ausklappbares Zutaten-Panel
            HorizontalDivider()
            TextButton(
                onClick = { ingredientsOpen = !ingredientsOpen },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    stringResource(R.string.ingredients_for) + " " +
                        pluralStringResource(R.plurals.servings, state.servings, state.servings),
                    style = MaterialTheme.typography.titleMedium,
                )
                Icon(if (ingredientsOpen) Icons.Filled.ExpandMore else Icons.Filled.ExpandLess, null)
            }
            AnimatedVisibility(ingredientsOpen, enter = expandVertically(), exit = shrinkVertically()) {
                IngredientRows(
                    state.ingredients,
                    textStyle = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .heightIn(max = 260.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }

            // --- Vor/Zurück
            Row(
                Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) } },
                    enabled = pagerState.currentPage > 0,
                    modifier = Modifier.weight(1f).heightIn(min = 56.dp),
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    Text(stringResource(R.string.previous), Modifier.padding(start = 8.dp), fontSize = 18.sp)
                }
                val isLast = pagerState.currentPage == recipe.steps.lastIndex
                Button(
                    onClick = {
                        if (isLast) onClose()
                        else scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    },
                    modifier = Modifier.weight(1f).heightIn(min = 56.dp),
                ) {
                    Text(stringResource(if (isLast) R.string.finish else R.string.next), fontSize = 18.sp)
                    Icon(
                        if (isLast) Icons.Filled.Check else Icons.AutoMirrored.Filled.ArrowForward,
                        null,
                        Modifier.padding(start = 8.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun StepPage(
    index: Int,
    step: CookingStep,
    timer: CookingTimer?,
    now: Long,
    onStartTimer: () -> Unit,
    onCancelTimer: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Text(
            stringResource(R.string.step_number, index + 1),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            step.text,
            fontSize = 30.sp,
            lineHeight = 40.sp,
            style = MaterialTheme.typography.headlineMedium,
        )
        step.timerSeconds?.let { seconds -> TimerCard(seconds, timer, now, onStartTimer, onCancelTimer) }
        Spacer(Modifier.size(8.dp))
    }
}

@Composable
private fun TimerCard(
    durationSeconds: Int,
    timer: CookingTimer?,
    now: Long,
    onStart: () -> Unit,
    onCancel: () -> Unit,
) {
    val finished = timer?.finished == true
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (finished) MaterialTheme.colorScheme.tertiaryContainer
            else MaterialTheme.colorScheme.primaryContainer,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when {
                timer == null -> {
                    Text(formatTimer(durationSeconds.toLong()), fontSize = 48.sp, fontWeight = FontWeight.Bold)
                    FilledTonalButton(onClick = onStart, modifier = Modifier.heightIn(min = 56.dp)) {
                        Icon(Icons.Outlined.Timer, null)
                        Text(stringResource(R.string.timer_start), Modifier.padding(start = 8.dp), fontSize = 18.sp)
                    }
                }
                finished -> {
                    Text(stringResource(R.string.timer_done), fontSize = 36.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    FilledTonalButton(onClick = onCancel, modifier = Modifier.heightIn(min = 56.dp)) {
                        Text(stringResource(R.string.timer_stop), fontSize = 18.sp)
                    }
                }
                else -> {
                    val remaining = timer.remainingMs(now)
                    Text(formatTimer((remaining + 999) / 1000), fontSize = 56.sp, fontWeight = FontWeight.Bold)
                    LinearProgressIndicator(
                        progress = { 1f - remaining.toFloat() / timer.durationMs },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedButton(onClick = onCancel) { Text(stringResource(R.string.timer_cancel)) }
                }
            }
        }
    }
}

/** Hält den Bildschirm an und blendet die Systemleisten aus, solange der Kochmodus sichtbar ist. */
@Composable
private fun KeepScreenOnAndImmersive() {
    val view = LocalView.current
    DisposableEffect(view) {
        view.keepScreenOn = true
        val window = view.context.findActivity()?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        controller?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller?.hide(WindowInsetsCompat.Type.systemBars())
        onDispose {
            view.keepScreenOn = false
            controller?.show(WindowInsetsCompat.Type.systemBars())
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
