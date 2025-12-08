package com.apiguave.onboarding_ui.login

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.apiguave.onboarding_ui.components.AnimatedLogo
import com.apiguave.core_ui.theme.Orange
import com.apiguave.core_ui.theme.Pink
import com.apiguave.core_ui.theme.TinderCloneComposeTheme
import com.apiguave.onboarding_ui.R

@Composable
fun LoginView(
    uiState: LoginViewState,
    onSignInClicked: (String, String) -> Unit,
    onRegisterClicked: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(Pink, Orange)
                )
            )
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(1.0f))

        AnimatedLogo(
            modifier = Modifier
                .fillMaxWidth(.4f)
                .padding(bottom = 32.dp),
            isAnimating = uiState is LoginViewState.Loading
        )

        Column(modifier = Modifier.weight(1.0f)) {
            Spacer(modifier = Modifier.weight(1f))

            TextField(
                value = email,
                onValueChange = { email = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(if (uiState is LoginViewState.Loading) 0f else 1f),
                label = { Text("Email") },
                singleLine = true,
                colors = TextFieldDefaults.textFieldColors(
                    textColor = Color.White,
                    backgroundColor = Color.White.copy(alpha = 0.2f),
                    focusedLabelColor = Color.White,
                    unfocusedLabelColor = Color.White.copy(alpha = 0.7f),
                    cursorColor = Color.White
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            TextField(
                value = password,
                onValueChange = { password = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(if (uiState is LoginViewState.Loading) 0f else 1f),
                label = { Text("Password") },
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                            contentDescription = if (passwordVisible) "Hide password" else "Show password",
                            tint = Color.White
                        )
                    }
                },
                colors = TextFieldDefaults.textFieldColors(
                    textColor = Color.White,
                    backgroundColor = Color.White.copy(alpha = 0.2f),
                    focusedLabelColor = Color.White,
                    unfocusedLabelColor = Color.White.copy(alpha = 0.7f),
                    cursorColor = Color.White
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        onSignInClicked(email, password)
                    }
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { onSignInClicked(email, password) },
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(if (uiState is LoginViewState.Loading) 0f else 1f),
                colors = ButtonDefaults.buttonColors(backgroundColor = Color.White),
                shape = RoundedCornerShape(50)
            ) {
                Text(
                    text = "SIGN IN",
                    color = Pink,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(
                onClick = onRegisterClicked,
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(if (uiState is LoginViewState.Loading) 0f else 1f)
            ) {
                Text(
                    text = "Don't have an account? Register",
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(44.dp))
        }
    }

    // Legacy Google Sign-In UI - COMMENTED OUT
    /*
    Column(modifier = Modifier.weight(1.0f)){
        Spacer(modifier = Modifier.weight(1f))

        OutlinedButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .alpha(if (uiState is LoginViewState.Loading) 0f else 1f),
            onClick = onSignInClicked,
            contentPadding = PaddingValues(
                start = 20.dp,
                top = 12.dp,
                end = 20.dp,
                bottom = 12.dp
            ),
            border = BorderStroke(2.dp, Color.White),
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.outlinedButtonColors(backgroundColor = Color.Transparent)
        ) {
            Box(Modifier.fillMaxWidth()){
               Image(
                   modifier = Modifier.align(Alignment.CenterStart),
                   painter = painterResource(id = R.drawable.google_icon),
                   contentDescription = null
               )
               Text(modifier = Modifier.align(Alignment.Center), textAlign = TextAlign.Center,text = stringResource(id = R.string.sign_in_with_google).uppercase(), color = Color.White)
            }
        }
        Spacer(modifier = Modifier.height(44.dp))
    }
    */
}

@Preview
@Composable
fun LoginViewPreview() {
    TinderCloneComposeTheme {
        LoginView(
            uiState = LoginViewState.Ready,
            onSignInClicked = { _, _ -> },
            onRegisterClicked = {}
        )
    }
}