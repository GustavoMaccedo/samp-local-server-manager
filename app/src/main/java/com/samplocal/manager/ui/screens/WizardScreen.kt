package com.samplocal.manager.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.RocketLaunch
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.samplocal.manager.R
import com.samplocal.manager.ui.theme.Neon
import com.samplocal.manager.ui.theme.TextDim
import com.samplocal.manager.ui.theme.TextMain
import com.samplocal.manager.ui.theme.WizardBg
import com.samplocal.manager.ui.theme.WizardDesc
import com.samplocal.manager.ui.theme.WizardOnNeon
import com.samplocal.manager.ui.strings.Strings
import com.samplocal.manager.ui.theme.WizardSubtitle
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
fun SetupScreen(
    step: String,
    done: Int,
    total: Int,
    T: Strings,
    error: String? = null,
    onRetry: (() -> Unit)? = null
) {
    val failed = error != null

    val displayStep = when (step) {
        "x86" -> T.instX86
        "libs" -> T.instLibs
        "validating" -> T.instValidating
        "ready" -> T.instReady
        "verifying" -> T.instVerifying
        else -> step
    }
    val allDone = !failed && step == "ready"
    val activeIdx = if (allDone) SetupSteps.size else setupActiveIndex(step)
    val determinate = total > 0
    val fraction = if (determinate) (done.toFloat() / total.toFloat()).coerceIn(0f, 1f) else 0f
    val animatedFraction by animateFloatAsState(fraction, tween(400), label = "setupProgress")
    val percent = "${(fraction * 100).roundToInt()}%"

    Box(
        Modifier
            .fillMaxSize()
            .background(WizardBg)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        WizardEdgeLines()
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
        ) {
            Spacer(Modifier.height(28.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(R.drawable.logo),
                    contentDescription = "SAMP Local Server Manager",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(
                        buildAnnotatedString {
                            withStyle(SpanStyle(color = Color.White)) { append("SAMP ") }
                            withStyle(SpanStyle(color = Neon)) { append("Local") }
                        },
                        fontSize = 27.sp,
                        fontWeight = FontWeight.ExtraBold,
                        lineHeight = 30.sp
                    )
                    Text(
                        "Server Manager",
                        color = WizardSubtitle,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Light,
                        letterSpacing = 1.sp
                    )
                }
            }
            Spacer(Modifier.height(26.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.Settings,
                    contentDescription = null,
                    tint = Neon,
                    modifier = Modifier.size(26.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    T.installTitle,
                    color = Color.White,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                T.installDesc,
                color = WizardDesc,
                fontSize = 13.5.sp,
                lineHeight = 20.sp
            )
            Spacer(Modifier.height(20.dp))

            Column(
                Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp, Neon.copy(alpha = 0.22f), RoundedCornerShape(18.dp)
                    )
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFF060A07))
                    .padding(vertical = 6.dp)
            ) {
                SetupSteps.forEachIndexed { i, s ->
                    val status = when {
                        failed && i == activeIdx -> SetupStatus.ERROR
                        i < activeIdx -> SetupStatus.DONE
                        i == activeIdx -> SetupStatus.ACTIVE
                        else -> SetupStatus.PENDING
                    }
                    SetupStepRow(title = s.title, subtitle = s.subtitle, icon = s.icon, status = status)
                    if (i < SetupSteps.lastIndex) {
                        HorizontalDivider(
                            color = Color.White.copy(alpha = 0.06f),
                            thickness = 1.dp,
                            modifier = Modifier.padding(horizontal = 18.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(22.dp))

            if (!failed) {
            if (determinate) {
                LinearProgressIndicator(
                    progress = { animatedFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(50)),
                    color = Neon,
                    trackColor = Color.White.copy(alpha = 0.10f)
                )
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        "$displayStep...",
                        color = Neon,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    Text(percent, color = Neon, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            } else if (!failed) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(50)),
                    color = Neon,
                    trackColor = Color.White.copy(alpha = 0.10f)
                )
                Spacer(Modifier.height(10.dp))
                Text(displayStep, color = Neon, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
            }
            if (failed) {
                Spacer(Modifier.height(14.dp))
                Column(
                    Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFFEF4444).copy(alpha = 0.08f))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.ErrorOutline,
                            contentDescription = null,
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            displayStep,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(error, color = WizardDesc, fontSize = 13.sp)
                    onRetry?.let {
                        Button(
                            onClick = it,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Neon,
                                contentColor = WizardOnNeon
                            ),
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(T.retry, fontWeight = FontWeight.Bold) }
                    }
                }
            }
            Spacer(Modifier.height(18.dp))

            Row(
                Modifier
                    .fillMaxWidth()
                    .border(1.dp, Neon.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.Transparent)
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Neon),
                    contentAlignment = Alignment.Center
                ) {
                    Text("i", color = WizardOnNeon, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        T.infoTitle,
                        color = Color.White,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        T.infoDesc,
                        color = WizardDesc,
                        fontSize = 13.sp
                    )
                }
            }
            Spacer(Modifier.height(28.dp))
        }
    }
}

private enum class SetupStatus { DONE, ACTIVE, PENDING, ERROR }

private data class SetupStepInfo(val title: String, val subtitle: String, val icon: ImageVector)

private val SetupSteps = listOf(
    SetupStepInfo("Verificando dispositivo", "Arquitetura e permissões", Icons.Outlined.Smartphone),
    SetupStepInfo("Detectando ARM64", "Compatibilidade do sistema", Icons.Outlined.Memory),
    SetupStepInfo("Preparando runtime", "Ambiente de execução", Icons.Outlined.Schedule),
    SetupStepInfo("Instalando compatibilidade x86", "Bibliotecas e dependências", Icons.Outlined.Download),
    SetupStepInfo("Preparando bibliotecas", "Arquivos de suporte", Icons.Outlined.MenuBook),
    SetupStepInfo("Validando runtime", "Testes de integridade", Icons.Outlined.Settings),
    SetupStepInfo("Finalizando instalação", "Preparando o ambiente", Icons.Outlined.VerifiedUser)
)

private fun setupActiveIndex(label: String): Int {
    when (label) {
        "verifying" -> return 0
        "x86" -> return 3
        "libs" -> return 4
        "validating" -> return 5
        "ready" -> return 6
    }
    val l = label.lowercase()
    return when {
        "pronto" in l || "finalizando" in l -> 6
        "valid" in l -> 5
        "bibliotecas do servidor" in l || ("preparando bibliotecas" in l) -> 4
        "x86" in l || "compatibilidade" in l -> 3
        "preparando runtime" in l || "ambiente de execução" in l || "execucao" in l -> 2
        "arm64" in l || "detectando" in l -> 1
        else -> 0
    }
}

@Composable
private fun SetupStepRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    status: SetupStatus
) {
    val dim = status == SetupStatus.PENDING
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (status == SetupStatus.ACTIVE) Neon.copy(alpha = 0.10f)
                else Color.Transparent
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        when (status) {
            SetupStatus.DONE -> Icon(
                Icons.Filled.CheckCircle, contentDescription = null,
                tint = Neon, modifier = Modifier.size(24.dp)
            )
            SetupStatus.ACTIVE -> CircularProgressIndicator(
                strokeWidth = 2.5.dp, color = Neon, modifier = Modifier.size(24.dp)
            )
            SetupStatus.ERROR -> Icon(
                Icons.Outlined.ErrorOutline, contentDescription = null,
                tint = Color(0xFFEF4444), modifier = Modifier.size(24.dp)
            )
            SetupStatus.PENDING -> Icon(
                Icons.Outlined.RadioButtonUnchecked, contentDescription = null,
                tint = Color.White.copy(alpha = 0.22f), modifier = Modifier.size(24.dp)
            )
        }
        Spacer(Modifier.width(12.dp))

        Box(
            Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF0B120D))
                .border(
                    1.dp,
                    if (dim) Color.White.copy(alpha = 0.10f) else Neon.copy(alpha = 0.45f),
                    RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon, contentDescription = null,
                tint = if (dim) WizardDesc else Neon,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(Modifier.width(12.dp))

        Column(Modifier.weight(1f)) {
            Text(
                title,
                color = if (dim) WizardDesc else Color.White,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(subtitle, color = WizardDesc, fontSize = 12.sp)
        }
    }
}

private data class Feature(val icon: ImageVector, val label: String)

@Composable
private fun wizardFeatures(T: Strings): List<Feature> = listOf(
    Feature(Icons.Outlined.Folder, T.featDirs),
    Feature(Icons.Outlined.Bolt, T.featRuntime),
    Feature(Icons.Outlined.Link, T.featDeps),
    Feature(Icons.Outlined.VerifiedUser, T.featArch)
)

@Composable
private fun WizardEdgeLines() {
    val c = Neon.copy(alpha = 0.28f)
    Canvas(Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val sw = 2.dp.toPx()

        drawLine(c, Offset(w * 0.02f, h * 0.115f), Offset(w * 0.22f, 0f), sw)
        drawLine(c, Offset(w * 0.02f, h * 0.15f), Offset(w * 0.30f, 0f), sw * 0.7f)

        drawLine(c, Offset(0f, h * 0.90f), Offset(w * 0.20f, h), sw * 0.7f)
        drawLine(c, Offset(0f, h * 0.925f), Offset(w * 0.16f, h), sw)

        drawLine(c, Offset(w, h * 0.865f), Offset(w * 0.80f, h), sw)
    }
}

@Composable
private fun WizardAppear(delayMs: Int, content: @Composable () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(delayMs.toLong())
        visible = true
    }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(450)) + slideInVertically(tween(450)) { it / 6 }
    ) { content() }
}

@Composable
fun WizardScreen(onStart: () -> Unit, T: Strings) {
    val features = wizardFeatures(T)
    Box(
        Modifier
            .fillMaxSize()
            .background(WizardBg)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        WizardEdgeLines()
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp)
        ) {
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Spacer(Modifier.height(24.dp))
                WizardAppear(0) {
                    Image(
                        painter = painterResource(R.drawable.logo),
                        contentDescription = "SAMP Local Server Manager",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth(0.68f)
                            .height(140.dp)
                    )
                }
                Spacer(Modifier.height(20.dp))
                WizardAppear(120) {
                    Text(
                        buildAnnotatedString {
                            withStyle(SpanStyle(color = Color.White)) { append("SAMP ") }
                            withStyle(SpanStyle(color = Neon)) { append("Local") }
                        },
                        fontSize = 44.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center,
                        lineHeight = 48.sp
                    )
                }
                WizardAppear(200) {
                    Text(
                        "Server Manager",
                        color = WizardSubtitle,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Light,
                        letterSpacing = 2.sp,
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(Modifier.height(18.dp))
                WizardAppear(280) {
                    Text(
                        T.wizardTagline,
                        color = WizardDesc,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 21.sp
                    )
                }
                Spacer(Modifier.height(30.dp))
                WizardAppear(360) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.Top
                    ) {
                    features.forEachIndexed { i, f ->
                            if (i > 0) {
                                VerticalDivider(
                                    color = Neon.copy(alpha = 0.18f),
                                    thickness = 1.dp,
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .padding(vertical = 6.dp)
                                )
                            }
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    f.icon,
                                    contentDescription = f.label,
                                    tint = Neon,
                                    modifier = Modifier.size(30.dp)
                                )
                                Text(
                                    f.label,
                                    color = TextMain,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
            WizardAppear(450) {
                val interaction = remember { MutableInteractionSource() }
                val pressed by interaction.collectIsPressedAsState()
                val scale by animateFloatAsState(
                    if (pressed) 0.97f else 1f, tween(120), label = "startPress"
                )
                Button(
                    onClick = onStart,
                    interactionSource = interaction,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Neon,
                        contentColor = WizardOnNeon
                    ),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .shadow(
                            elevation = 18.dp,
                            shape = RoundedCornerShape(50),
                            ambientColor = Neon.copy(alpha = 0.35f),
                            spotColor = Neon.copy(alpha = 0.35f)
                        )
                        .scale(scale)
                        .height(62.dp)
                ) {
                    Icon(
                        Icons.Outlined.RocketLaunch,
                        contentDescription = null,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(Modifier.width(14.dp))
                    Box(
                        Modifier
                            .width(1.dp)
                            .height(26.dp)
                            .background(WizardOnNeon.copy(alpha = 0.55f))
                    )
                    Spacer(Modifier.width(14.dp))
                    Text(
                        T.start,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                }
            }
            Spacer(Modifier.height(28.dp))
        }
    }
}
