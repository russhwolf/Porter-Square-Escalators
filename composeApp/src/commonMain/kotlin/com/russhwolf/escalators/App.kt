package com.russhwolf.escalators

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun App(mbtaApiClient: MbtaApiClient) {
    val state = remember { mutableStateOf<UiState>(UiState.Init) }

    LaunchedEffect(Unit) {
        val escalatorResponse = mbtaApiClient.getPorterEscalatorStatus()
        state.value = UiState.Populated(escalatorResponse)
    }

    RootUi(state.value)
}

sealed interface UiState {
    data object Init : UiState
    data class Populated(val escalatorResponse: EscalatorResponse) : UiState
}

@Composable
fun RootUi(state: UiState) {
    MaterialTheme {
        Column(Modifier.fillMaxSize()) {
            Spacer(Modifier.height(16.dp))
            HeaderView(Modifier.fillMaxWidth().align(Alignment.CenterHorizontally))
            Spacer(Modifier.height(16.dp))
            Box(Modifier.weight(1f).fillMaxWidth()) {
                when (state) {
                    is UiState.Init -> LoadingView(Modifier.align(Alignment.Center))
                    is UiState.Populated -> when (state.escalatorResponse) {
                        is EscalatorResponse.Success -> PopulatedView(
                            state.escalatorResponse.escalators,
                            Modifier.align(Alignment.Center)
                        )

                        is EscalatorResponse.Failure -> ErrorView(Modifier.align(Alignment.Center))
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            FooterView(Modifier.fillMaxWidth())
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun HeaderView(modifier: Modifier = Modifier) {
    Text(
        "Do the Porter Square escalators work?",
        modifier,
        style = MaterialTheme.typography.titleLarge,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun FooterView(modifier: Modifier = Modifier) {
    // TODO
}


@Composable
fun PopulatedView(escalators: List<Escalator>, modifier: Modifier = Modifier) {
    Row(modifier) {
        escalators.forEach {
            EscalatorView(it, Modifier.weight(1f).fillMaxSize())
        }
    }
}

@Composable
fun EscalatorView(escalator: Escalator, modifier: Modifier = Modifier) {
    Column(modifier.padding(8.dp)) {
        Spacer(Modifier.height(48.dp))
        Text(
            "${escalator.id}: ${escalator.description}",
            Modifier.fillMaxWidth().align(Alignment.CenterHorizontally),
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "Direction: ${escalator.direction}",
            Modifier.fillMaxWidth().align(Alignment.CenterHorizontally),
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(16.dp))
        Text(
            escalator.status,
            Modifier.fillMaxWidth().align(Alignment.CenterHorizontally),
            style = MaterialTheme.typography.bodyMedium,
            color = if (escalator.isWorking) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.error
        )
    }
}

@Composable
fun ErrorView(modifier: Modifier = Modifier) {
    Text("An error occurred", modifier, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.error)
}

@Composable
fun LoadingView(modifier: Modifier = Modifier) {
    CircularProgressIndicator(modifier)
}
