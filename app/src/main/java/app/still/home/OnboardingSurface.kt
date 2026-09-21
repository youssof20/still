package app.still.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import app.still.R
import app.still.ui.StillSpacing

@Composable
fun OnboardingSurface(
    modifier: Modifier = Modifier,
    onFinished: () -> Unit,
) {
    var page by remember { mutableIntStateOf(0) }
    val titles = listOf(
        R.string.onboarding_title_1,
        R.string.onboarding_title_2,
        R.string.onboarding_title_3,
    )
    val bodies = listOf(
        R.string.onboarding_body_1,
        R.string.onboarding_body_2,
        R.string.onboarding_body_3,
    )
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(StillSpacing.lg),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        TextButton(
            onClick = onFinished,
            modifier = Modifier.align(Alignment.End),
        ) {
            Text(stringResource(R.string.onboarding_skip))
        }
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(StillSpacing.md),
        ) {
            Text(
                text = stringResource(titles[page]),
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = stringResource(bodies[page]),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "${page + 1} / 3",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                onClick = {
                    if (page >= 2) onFinished() else page += 1
                },
            ) {
                Text(
                    stringResource(
                        if (page >= 2) R.string.onboarding_start else R.string.onboarding_next,
                    ),
                )
            }
        }
    }
}
