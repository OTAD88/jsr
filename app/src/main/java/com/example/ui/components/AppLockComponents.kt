package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.AppLockMethod
import com.example.security.AppLockManager
import com.example.ui.theme.InkBlack
import com.example.ui.theme.LocalJesrPalette
import com.example.ui.theme.LocalJesrStrings
import com.example.ui.theme.PureWhitePill
import com.example.ui.theme.TerracottaAccent
import kotlin.math.hypot

@Composable
fun PatternLockGrid(
    onPatternComplete: (List<Int>) -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false
) {
    val palette = LocalJesrPalette.current
    val selectedDots = remember { mutableStateListOf<Int>() }
    var currentDragOffset by remember { mutableStateOf<Offset?>(null) }
    var boxSize by remember { mutableStateOf(IntSize.Zero) }

    fun dotCenter(index: Int, w: Float, h: Float): Offset {
        val row = index / 3
        val col = index % 3
        val stepX = w / 3f
        val stepY = h / 3f
        return Offset(col * stepX + stepX / 2f, row * stepY + stepY / 2f)
    }

    fun hitTestDot(pos: Offset): Int? {
        val w = boxSize.width.toFloat()
        val h = boxSize.height.toFloat()
        if (w <= 0f || h <= 0f) return null
        val threshold = (minOf(w, h) / 6.2f)
        for (idx in 0 until 9) {
            val c = dotCenter(idx, w, h)
            if (hypot(pos.x - c.x, pos.y - c.y) <= threshold) {
                return idx
            }
        }
        return null
    }

    val lineColor = if (isError) TerracottaAccent else palette.accent

    Box(
        modifier = modifier
            .size(230.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(palette.elevatedSurface)
            .border(1.dp, if (isError) TerracottaAccent else palette.border, RoundedCornerShape(22.dp))
            .onSizeChanged { boxSize = it }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        selectedDots.clear()
                        currentDragOffset = offset
                        hitTestDot(offset)?.let { selectedDots.add(it) }
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        currentDragOffset = change.position
                        hitTestDot(change.position)?.let { hit ->
                            if (!selectedDots.contains(hit)) {
                                selectedDots.add(hit)
                            }
                        }
                    },
                    onDragEnd = {
                        currentDragOffset = null
                        if (selectedDots.isNotEmpty()) {
                            onPatternComplete(selectedDots.toList())
                            selectedDots.clear()
                        }
                    },
                    onDragCancel = {
                        currentDragOffset = null
                        selectedDots.clear()
                    }
                )
            }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    hitTestDot(offset)?.let { hit ->
                        if (!selectedDots.contains(hit)) {
                            selectedDots.add(hit)
                        }
                        if (selectedDots.size >= 4) {
                            onPatternComplete(selectedDots.toList())
                            selectedDots.clear()
                        }
                    }
                }
            }
            .testTag("pattern_lock_grid")
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Draw connecting lines
            if (selectedDots.size > 1) {
                for (i in 0 until selectedDots.size - 1) {
                    val p1 = dotCenter(selectedDots[i], w, h)
                    val p2 = dotCenter(selectedDots[i + 1], w, h)
                    drawLine(
                        color = lineColor,
                        start = p1,
                        end = p2,
                        strokeWidth = 4.5.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }
            val lastDot = selectedDots.lastOrNull()
            val dragPos = currentDragOffset
            if (lastDot != null && dragPos != null) {
                drawLine(
                    color = lineColor.copy(alpha = 0.65f),
                    start = dotCenter(lastDot, w, h),
                    end = dragPos,
                    strokeWidth = 3.5.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            // Draw 9 grid nodes
            for (idx in 0 until 9) {
                val center = dotCenter(idx, w, h)
                val isSelected = selectedDots.contains(idx)
                drawCircle(
                    color = if (isSelected) lineColor.copy(alpha = 0.22f) else palette.pillSurface,
                    radius = if (isSelected) 18.dp.toPx() else 12.dp.toPx(),
                    center = center
                )
                drawCircle(
                    color = if (isSelected) lineColor else palette.mutedText.copy(alpha = 0.7f),
                    radius = if (isSelected) 7.5.dp.toPx() else 5.5.dp.toPx(),
                    center = center
                )
            }
        }
    }
}

@Composable
fun AppLockOverlay(
    method: AppLockMethod,
    hasFallbackSecret: Boolean,
    onVerifySecret: (String) -> Boolean,
    onBiometricSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val palette = LocalJesrPalette.current
    val strings = LocalJesrStrings.current

    var pinOrPass by remember(method) { mutableStateOf("") }
    var errorMsg by remember(method) { mutableStateOf<String?>(null) }

    LaunchedEffect(method) {
        if (method == AppLockMethod.BIOMETRIC && AppLockManager.isBiometricHardwareSupported(context)) {
            AppLockManager.showBiometricPrompt(
                context = context,
                title = strings.appLockTitle,
                subtitle = strings.appLockBiometricPromptSub,
                negativeButtonText = strings.cancel,
                onSuccess = onBiometricSuccess,
                onError = { msg -> errorMsg = msg.ifBlank { null } }
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("app_lock_overlay")
    ) {
        EditorialSunlightBackdrop(modifier = Modifier.fillMaxSize(), isMeshActive = true)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            EditorialFloatingCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = palette.cardSurface,
                contentPadding = 24.dp
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(58.dp)
                            .clip(CircleShape)
                            .background(palette.accent.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (method) {
                                AppLockMethod.BIOMETRIC -> Icons.Default.Fingerprint
                                AppLockMethod.PATTERN -> Icons.Default.Gesture
                                AppLockMethod.PIN -> Icons.Default.Pin
                                else -> Icons.Default.Lock
                            },
                            contentDescription = strings.appLockTitle,
                            tint = palette.accent,
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    Text(
                        text = strings.appLockUnlockHeader,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = palette.inkPrimary,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = when (method) {
                            AppLockMethod.BIOMETRIC -> strings.appLockBioHint
                            AppLockMethod.PIN -> strings.appLockPinHint
                            AppLockMethod.PASSWORD -> strings.appLockPasswordHint
                            AppLockMethod.PATTERN -> strings.appLockPatternHint
                            AppLockMethod.NONE -> ""
                        },
                        fontSize = 12.5.sp,
                        color = palette.mutedText,
                        textAlign = TextAlign.Center
                    )

                    when (method) {
                        AppLockMethod.BIOMETRIC -> {
                            val canBio = remember { AppLockManager.isBiometricHardwareSupported(context) }
                            Button(
                                onClick = {
                                    if (canBio) {
                                        AppLockManager.showBiometricPrompt(
                                            context = context,
                                            title = strings.appLockTitle,
                                            subtitle = strings.appLockBiometricPromptSub,
                                            negativeButtonText = strings.cancel,
                                            onSuccess = onBiometricSuccess,
                                            onError = { msg -> errorMsg = msg }
                                        )
                                    } else {
                                        // Emulator / non-biometric test environment safe unlock
                                        onBiometricSuccess()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = palette.accent,
                                    contentColor = if (palette.accent.luminance() > 0.55f) InkBlack else PureWhitePill
                                ),
                                shape = RoundedCornerShape(50),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("lock_biometric_unlock_btn")
                            ) {
                                Icon(Icons.Default.Fingerprint, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (canBio) strings.appLockUseFingerprintBtn else strings.appLockContinueBtn,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            if (hasFallbackSecret) {
                                OutlinedTextField(
                                    value = pinOrPass,
                                    onValueChange = { pinOrPass = it; errorMsg = null },
                                    label = { Text(strings.appLockBackupSecretLabel) },
                                    singleLine = true,
                                    visualTransformation = PasswordVisualTransformation(),
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = palette.inkPrimary,
                                        unfocusedTextColor = palette.inkPrimary
                                    ),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                Button(
                                    onClick = {
                                        if (!onVerifySecret(pinOrPass)) {
                                            errorMsg = strings.appLockWrongSecret
                                        }
                                    },
                                    shape = RoundedCornerShape(50),
                                    modifier = Modifier.fillMaxWidth().height(46.dp)
                                ) {
                                    Text(strings.unlockBtn, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        AppLockMethod.PIN -> {
                            // Dots display
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.padding(vertical = 4.dp)
                            ) {
                                val slots = maxOf(4, pinOrPass.length.coerceAtMost(8))
                                for (i in 0 until slots) {
                                    val filled = i < pinOrPass.length
                                    Box(
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clip(CircleShape)
                                            .background(if (filled) palette.accent else palette.pillSurface)
                                            .border(1.dp, palette.border, CircleShape)
                                    )
                                }
                            }
                            NumericKeypad(
                                onDigit = { d ->
                                    if (pinOrPass.length < 8) {
                                        pinOrPass += d
                                        errorMsg = null
                                        if (pinOrPass.length >= 4 && onVerifySecret(pinOrPass)) {
                                            errorMsg = null
                                        }
                                    }
                                },
                                onBackspace = {
                                    if (pinOrPass.isNotEmpty()) pinOrPass = pinOrPass.dropLast(1)
                                }
                            )
                            Button(
                                onClick = {
                                    if (!onVerifySecret(pinOrPass)) {
                                        errorMsg = strings.appLockWrongSecret
                                        pinOrPass = ""
                                    }
                                },
                                enabled = pinOrPass.length >= 4,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = palette.inkPrimary,
                                    contentColor = palette.elevatedSurface
                                ),
                                shape = RoundedCornerShape(50),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("lock_pin_submit_btn")
                            ) {
                                Icon(Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(strings.unlockBtn, fontWeight = FontWeight.Bold)
                            }
                        }

                        AppLockMethod.PASSWORD -> {
                            OutlinedTextField(
                                value = pinOrPass,
                                onValueChange = { pinOrPass = it; errorMsg = null },
                                label = { Text(strings.appLockEnterPasswordLabel) },
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("lock_password_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = palette.inkPrimary,
                                    unfocusedTextColor = palette.inkPrimary,
                                    focusedBorderColor = palette.accent,
                                    unfocusedBorderColor = palette.border
                                ),
                                shape = RoundedCornerShape(16.dp)
                            )
                            Button(
                                onClick = {
                                    if (!onVerifySecret(pinOrPass)) {
                                        errorMsg = strings.appLockWrongSecret
                                    }
                                },
                                enabled = pinOrPass.isNotEmpty(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = palette.inkPrimary,
                                    contentColor = palette.elevatedSurface
                                ),
                                shape = RoundedCornerShape(50),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("lock_password_submit_btn")
                            ) {
                                Icon(Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(strings.unlockBtn, fontWeight = FontWeight.Bold)
                            }
                        }

                        AppLockMethod.PATTERN -> {
                            PatternLockGrid(
                                isError = errorMsg != null,
                                onPatternComplete = { dots ->
                                    if (dots.size < 4) {
                                        errorMsg = strings.appLockPatternTooShort
                                    } else {
                                        val encoded = AppLockManager.encodePattern(dots)
                                        if (!onVerifySecret(encoded)) {
                                            errorMsg = strings.appLockWrongSecret
                                        }
                                    }
                                }
                            )
                        }

                        AppLockMethod.NONE -> Unit
                    }

                    if (!errorMsg.isNullOrBlank()) {
                        Text(
                            text = errorMsg.orEmpty(),
                            color = TerracottaAccent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NumericKeypad(
    onDigit: (String) -> Unit,
    onBackspace: () -> Unit
) {
    val palette = LocalJesrPalette.current
    val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("", "0", "DEL")
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                row.forEach { key ->
                    when (key) {
                        "" -> Spacer(modifier = Modifier.size(56.dp))
                        "DEL" -> IconButton(
                            onClick = onBackspace,
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(palette.pillSurface)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Backspace, contentDescription = null, tint = palette.iconColor)
                        }
                        else -> Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(palette.elevatedSurface)
                                .border(0.8.dp, palette.border, CircleShape)
                                .clickable { onDigit(key) }
                                .testTag("pin_key_$key"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = key,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = palette.inkPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AppLockSetupDialog(
    currentMethod: AppLockMethod,
    onSelectNone: () -> Unit,
    onSetBiometric: () -> Unit,
    onSetPin: (String) -> Unit,
    onSetPassword: (String) -> Unit,
    onSetPattern: (List<Int>) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val palette = LocalJesrPalette.current
    val strings = LocalJesrStrings.current

    var pickedMethod by remember { mutableStateOf(currentMethod) }
    var draftInput by remember { mutableStateOf("") }
    var draftConfirm by remember { mutableStateOf("") }
    var firstPattern by remember { mutableStateOf<List<Int>?>(null) }
    var statusError by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            EditorialFloatingCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("app_lock_setup_dialog"),
                backgroundColor = palette.cardSurface,
                contentPadding = 20.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = strings.appLockTitle,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = palette.inkPrimary
                    )
                    Text(
                        text = strings.appLockSubtitle,
                        fontSize = 12.sp,
                        color = palette.mutedText
                    )

                    val options = listOf(
                        Triple(AppLockMethod.NONE, strings.lockMethodNone, Icons.Default.LockOpen),
                        Triple(AppLockMethod.BIOMETRIC, strings.lockMethodBiometric, Icons.Default.Fingerprint),
                        Triple(AppLockMethod.PIN, strings.lockMethodPin, Icons.Default.Pin),
                        Triple(AppLockMethod.PASSWORD, strings.lockMethodPassword, Icons.Default.Password),
                        Triple(AppLockMethod.PATTERN, strings.lockMethodPattern, Icons.Default.Gesture)
                    )

                    options.forEach { (method, label, icon) ->
                        val selected = pickedMethod == method
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (selected) palette.accent.copy(alpha = 0.14f) else palette.elevatedSurface)
                                .border(
                                    width = if (selected) 1.3.dp else 0.7.dp,
                                    color = if (selected) palette.accent else palette.border,
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clickable {
                                    pickedMethod = method
                                    draftInput = ""
                                    draftConfirm = ""
                                    firstPattern = null
                                    statusError = null
                                }
                                .testTag("lock_option_${method.name.lowercase()}")
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(icon, contentDescription = null, tint = if (selected) palette.accent else palette.iconColor, modifier = Modifier.size(20.dp))
                                Text(label, fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = palette.inkPrimary)
                            }
                            if (selected) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = palette.accent, modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    when (pickedMethod) {
                        AppLockMethod.NONE -> Unit

                        AppLockMethod.BIOMETRIC -> {
                            val hasFp = remember { AppLockManager.hasFingerprintFeature(context) }
                            val enrolled = remember { AppLockManager.isBiometricHardwareSupported(context) }
                            Text(
                                text = when {
                                    enrolled -> strings.appLockBioReadyMsg
                                    hasFp -> strings.appLockBioEnrollHint
                                    else -> strings.appLockBioReadyMsg
                                },
                                fontSize = 12.sp,
                                color = palette.inkSecondary
                            )
                        }

                        AppLockMethod.PIN -> {
                            OutlinedTextField(
                                value = draftInput,
                                onValueChange = { if (it.length <= 8 && it.all(Char::isDigit)) { draftInput = it; statusError = null } },
                                label = { Text(strings.appLockNewPinLabel) },
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("setup_pin_input"),
                                shape = RoundedCornerShape(16.dp)
                            )
                            OutlinedTextField(
                                value = draftConfirm,
                                onValueChange = { if (it.length <= 8 && it.all(Char::isDigit)) { draftConfirm = it; statusError = null } },
                                label = { Text(strings.appLockConfirmLabel) },
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("setup_pin_confirm_input"),
                                shape = RoundedCornerShape(16.dp)
                            )
                        }

                        AppLockMethod.PASSWORD -> {
                            OutlinedTextField(
                                value = draftInput,
                                onValueChange = { if (it.length <= 32) { draftInput = it; statusError = null } },
                                label = { Text(strings.appLockNewPassLabel) },
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("setup_password_input"),
                                shape = RoundedCornerShape(16.dp)
                            )
                            OutlinedTextField(
                                value = draftConfirm,
                                onValueChange = { if (it.length <= 32) { draftConfirm = it; statusError = null } },
                                label = { Text(strings.appLockConfirmLabel) },
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("setup_password_confirm_input"),
                                shape = RoundedCornerShape(16.dp)
                            )
                        }

                        AppLockMethod.PATTERN -> {
                            Text(
                                text = if (firstPattern == null) strings.appLockDrawPatternHint else strings.appLockDrawPatternConfirmHint,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = palette.accent,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                PatternLockGrid(
                                    isError = statusError != null,
                                    onPatternComplete = { dots ->
                                        if (dots.size < 4) {
                                            statusError = strings.appLockPatternTooShort
                                        } else if (firstPattern == null) {
                                            firstPattern = dots
                                            statusError = null
                                        } else if (firstPattern == dots) {
                                            onSetPattern(dots)
                                            onDismiss()
                                        } else {
                                            firstPattern = null
                                            statusError = strings.appLockPatternMismatch
                                        }
                                    }
                                )
                            }
                        }
                    }

                    if (!statusError.isNullOrBlank()) {
                        Text(
                            text = statusError.orEmpty(),
                            color = TerracottaAccent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (pickedMethod != AppLockMethod.PATTERN) {
                            Button(
                                onClick = {
                                    when (pickedMethod) {
                                        AppLockMethod.NONE -> {
                                            onSelectNone()
                                            onDismiss()
                                        }
                                        AppLockMethod.BIOMETRIC -> {
                                            onSetBiometric()
                                            onDismiss()
                                        }
                                        AppLockMethod.PIN -> {
                                            if (draftInput.length < 4) {
                                                statusError = strings.appLockPinTooShort
                                            } else if (draftInput != draftConfirm) {
                                                statusError = strings.appLockSecretMismatch
                                            } else {
                                                onSetPin(draftInput)
                                                onDismiss()
                                            }
                                        }
                                        AppLockMethod.PASSWORD -> {
                                            if (draftInput.length < 3) {
                                                statusError = strings.appLockPassTooShort
                                            } else if (draftInput != draftConfirm) {
                                                statusError = strings.appLockSecretMismatch
                                            } else {
                                                onSetPassword(draftInput)
                                                onDismiss()
                                            }
                                        }
                                        AppLockMethod.PATTERN -> Unit
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = palette.inkPrimary,
                                    contentColor = palette.elevatedSurface
                                ),
                                shape = RoundedCornerShape(50),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("app_lock_save_btn")
                            ) {
                                Text(strings.save, fontWeight = FontWeight.Bold)
                            }
                        }

                        TextButton(
                            onClick = onDismiss,
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("app_lock_cancel_btn")
                        ) {
                            Text(strings.cancel, fontWeight = FontWeight.Bold, color = palette.inkPrimary)
                        }
                    }
                }
            }
        }
    }
}
