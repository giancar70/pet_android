package com.petapp.android.features.auth

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val FieldGreen = Color(0xFF406E5F)
private val FieldBorder = Color(0xFFEFEFF4)
/// Form-field color spec from CU02 (§5): labels/entered text, placeholders, and error
/// text all need more contrast than the defaults previously in use.
private val TextDark = Color(0xFF333333)
private val PlaceholderGray = Color(0xFF8A8A8A)
private val AuxGray = Color(0xFF666666)
val AuthFieldErrorRed = Color(0xFFC62828)

@Composable
fun AuthTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    leadingIcon: ImageVector,
    placeholder: String,
    modifier: Modifier = Modifier,
    isPassword: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    /// Shown immediately below the field in red when non-null, and clears the error
    /// (red) border styling when null -- CU02 §1: every validation error appears right
    /// under the field it belongs to, not in one shared banner.
    errorMessage: String? = null,
    /// Static helper text shown below the field regardless of error state (e.g. the
    /// password's "Mínimo 8 caracteres." requirement, CU02 §3).
    caption: String? = null,
    onFocusLost: (() -> Unit)? = null,
) {
    var passwordVisible by remember { mutableStateOf(false) }
    var wasFocused by remember { mutableStateOf(false) }
    Column(modifier = modifier) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = TextDark,
        )
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = PlaceholderGray) },
            textStyle = androidx.compose.ui.text.TextStyle(color = TextDark),
            leadingIcon = { Icon(leadingIcon, contentDescription = null, tint = FieldGreen) },
            trailingIcon = if (isPassword) {
                {
                    val icon = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(icon, contentDescription = null, tint = PlaceholderGray)
                    }
                }
            } else null,
            visualTransformation = if (isPassword && !passwordVisible) {
                PasswordVisualTransformation()
            } else {
                VisualTransformation.None
            },
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            singleLine = true,
            isError = errorMessage != null,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = FieldBorder,
                focusedBorderColor = FieldGreen,
                errorBorderColor = AuthFieldErrorRed,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { state ->
                    if (state.isFocused) {
                        wasFocused = true
                    } else if (wasFocused) {
                        onFocusLost?.invoke()
                    }
                },
        )
        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = errorMessage, fontSize = 12.sp, color = AuthFieldErrorRed)
        }
        if (caption != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = caption, fontSize = 11.sp, color = AuxGray)
        }
    }
}
