package com.example.dbit_almaconnect3.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.dbit_almaconnect3.ui.theme.*
import com.example.dbit_almaconnect3.viewmodel.AuthViewModel
import com.example.dbit_almaconnect3.ui.theme.BlueBrandColor

// Admin passcode constant - in a real app, this would be stored securely
private const val ADMIN_PASSCODE = "adminaccess123"

@Composable
fun SignUpScreen(
    viewModel: AuthViewModel = viewModel(),
    onSignUpSuccess: (String, String) -> Unit, // (email, role)
    onNavigateToLogin: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var selectedRole by remember { mutableStateOf("student") }  // Default role
    var adminPasscode by remember { mutableStateOf("") }  // Add passcode field
    var adminPasscodeVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Get the current color scheme from MaterialTheme
    val colorScheme = MaterialTheme.colorScheme
    val isDarkTheme = isSystemInDarkTheme()

    // signUpSuccess is a Pair<Boolean, String> where Boolean indicates success
    // and String holds the role on success or an error message on failure.
    val signUpSuccess by viewModel.signUpSuccess.collectAsStateWithLifecycle()

    LaunchedEffect(signUpSuccess) {
        signUpSuccess?.let { result ->
            isLoading = false
            if (result.first) {
                onSignUpSuccess(email, result.second)
            } else {
                val errorMsg = if (result.second.isNotEmpty()) result.second else "Sign Up Failed"
                Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = if (isDarkTheme) DarkCard else LightCard)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Create Account",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = BlueBrandColor
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Join our community",
                    fontSize = 14.sp,
                    color = if (isDarkTheme) DarkTextSecondary else LightTextSecondary
                )
                Spacer(modifier = Modifier.height(32.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = "Email Icon",
                            tint = BlueBrandColor
                        )
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    textStyle = TextStyle(color = if (isDarkTheme) DarkTextPrimary else LightTextPrimary),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedTextColor = if (isDarkTheme) DarkTextPrimary else LightTextPrimary,
                        focusedTextColor = if (isDarkTheme) DarkTextPrimary else LightTextPrimary,
                        cursorColor = BlueBrandColor,
                        unfocusedBorderColor = if (isDarkTheme) Color(0xFF444444) else Color(0xFFDDDDDD),
                        focusedBorderColor = BlueBrandColor
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Password Icon",
                            tint = BlueBrandColor
                        )
                    },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (passwordVisible) "Hide Password" else "Show Password",
                                tint = BlueBrandColor
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Next
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    textStyle = TextStyle(color = if (isDarkTheme) DarkTextPrimary else LightTextPrimary),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedTextColor = if (isDarkTheme) DarkTextPrimary else LightTextPrimary,
                        focusedTextColor = if (isDarkTheme) DarkTextPrimary else LightTextPrimary,
                        cursorColor = BlueBrandColor,
                        unfocusedBorderColor = if (isDarkTheme) Color(0xFF444444) else Color(0xFFDDDDDD),
                        focusedBorderColor = BlueBrandColor
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Role selection section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDarkTheme) Color(0xFF1A1A1A) else Color(0xFFF5F5F5)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Select Your Role",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isDarkTheme) DarkTextPrimary else LightTextPrimary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        // Student option
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedRole == "student",
                                onClick = { selectedRole = "student" },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = BlueBrandColor
                                )
                            )
                            Text(
                                text = "Student",
                                modifier = Modifier.padding(start = 8.dp),
                                color = if (isDarkTheme) DarkTextPrimary else LightTextPrimary
                            )
                        }

                        // Alumni option
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedRole == "alumni",
                                onClick = { selectedRole = "alumni" },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = BlueBrandColor
                                )
                            )
                            Text(
                                text = "Alumni",
                                modifier = Modifier.padding(start = 8.dp),
                                color = if (isDarkTheme) DarkTextPrimary else LightTextPrimary
                            )
                        }

                        // College Admin option
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedRole == "collegeadmin",
                                onClick = { selectedRole = "collegeadmin" },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = BlueBrandColor
                                )
                            )
                            Text(
                                text = "College Admin",
                                modifier = Modifier.padding(start = 8.dp),
                                color = if (isDarkTheme) DarkTextPrimary else LightTextPrimary
                            )
                        }
                    }
                }

                // Show admin passcode field when College Admin is selected
                if (selectedRole == "collegeadmin") {
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = adminPasscode,
                        onValueChange = { adminPasscode = it },
                        label = { Text("Admin Passcode") },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Passcode Icon",
                                tint = BlueBrandColor
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = { adminPasscodeVisible = !adminPasscodeVisible }) {
                                Icon(
                                    imageVector = if (adminPasscodeVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (adminPasscodeVisible) "Hide Passcode" else "Show Passcode",
                                    tint = BlueBrandColor
                                )
                            }
                        },
                        visualTransformation = if (adminPasscodeVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        textStyle = TextStyle(color = if (isDarkTheme) DarkTextPrimary else LightTextPrimary),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedTextColor = if (isDarkTheme) DarkTextPrimary else LightTextPrimary,
                            focusedTextColor = if (isDarkTheme) DarkTextPrimary else LightTextPrimary,
                            cursorColor = BlueBrandColor,
                            unfocusedBorderColor = if (isDarkTheme) Color(0xFF444444) else Color(0xFFDDDDDD),
                            focusedBorderColor = BlueBrandColor
                        )
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        if (email.isNotBlank() && password.isNotBlank()) {
                            // Validate admin passcode if trying to sign up as an admin
                            if (selectedRole == "collegeadmin" && adminPasscode != ADMIN_PASSCODE) {
                                Toast.makeText(context, "Invalid admin passcode", Toast.LENGTH_SHORT).show()
                            } else {
                                isLoading = true
                                viewModel.signUp(email, password, selectedRole)
                            }
                        } else {
                            Toast.makeText(context, "Enter valid details", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BlueBrandColor)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White
                        )
                    } else {
                        Text("Sign Up", fontSize = 16.sp)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(
                    onClick = onNavigateToLogin,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = BlueBrandColor
                    )
                ) {
                    Text(
                        text = "Already have an account? Login",
                        color = BlueBrandColor,
                        textAlign = TextAlign.Center,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewSignUp() {
    DBIT_ALMACONNECT3Theme {
        SignUpScreen(onSignUpSuccess = { _, _ -> }, onNavigateToLogin = {})
    }
}

@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PreviewSignUpDark() {
    DBIT_ALMACONNECT3Theme {
        SignUpScreen(onSignUpSuccess = { _, _ -> }, onNavigateToLogin = {})
    }
}