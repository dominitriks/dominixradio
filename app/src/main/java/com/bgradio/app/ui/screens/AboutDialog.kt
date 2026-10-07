package com.bgradio.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bgradio.app.BuildConfig
import com.bgradio.app.R

@Composable
fun AboutDialog(stationCount: Int, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Затвори") } },
        title = { Text("За приложението") },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(
                        painterResource(R.drawable.about_logo),
                        contentDescription = null,
                        modifier = Modifier.size(72.dp).clip(RoundedCornerShape(18.dp)),
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("DOMINIX", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "Версия: ${BuildConfig.VERSION_NAME}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Labeled("Създадено от:", "Dominantus")
                Labeled("Автор:", "Dominantus")

                Text(
                    "Dominix е разработено от Dominantus.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    "Авторските и имуществените права върху оригиналния код, дизайна, структурата и " +
                        "разработката на приложението принадлежат на Dominantus, освен ако за отделни " +
                        "компоненти не е посочено друго.",
                    style = MaterialTheme.typography.bodyMedium,
                )

                HorizontalDivider()
                Text("Радиостанции", style = MaterialTheme.typography.titleSmall)
                Text(
                    "Приложението съдържа препратки към $stationCount публично достъпни интернет потока на " +
                        "български радиостанции. Имената, логата, програмното съдържание и аудио потоците на " +
                        "радиостанциите са собственост на съответните им притежатели. Dominix не претендира " +
                        "за права върху тях и ги използва само за да покаже и възпроизведе съответното радио.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "Приложението използва библиотеки с отворен код (AndroidX, Jetpack Compose, Media3, Coil), " +
                        "разпространявани под съответните им лицензи (Apache 2.0).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                HorizontalDivider()
                Text(
                    "© 2026 Dominantus\nВсички права запазени.",
                    style = MaterialTheme.typography.labelMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
    )
}

@Composable
private fun Labeled(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
    }
}
