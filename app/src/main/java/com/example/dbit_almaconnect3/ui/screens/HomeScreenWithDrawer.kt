package com.example.dbit_almaconnect3.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.dbit_almaconnect3.R
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreenWithDrawer(role: String, navController: NavController, onLogout: () -> Unit) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        // Pass onLogout to the drawer content.
        drawerContent = { DrawerContent(navController, drawerState, scope, onLogout = onLogout) }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("DBIT-AlmaConnect") },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    },
                    actions = {
                        // Logout action in the TopAppBar.
                        TextButton(onClick = onLogout) {
                            Text("Logout", color = Color.White)
                        }
                    }
                )
            },
            content = { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .background(Color.LightGray)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.graduation_photo),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Welcome! ($role)",
                            fontSize = 30.sp,
                            fontStyle = FontStyle.Italic,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            color = Color.White,
                            modifier = Modifier
                                .background(Color.Black.copy(alpha = 0.5f))
                                .padding(10.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        if (role.lowercase() == "alumni") {
                            Text(
                                text = "Alumni Home Features:",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Yellow
                            )
                        }else if(role.lowercase() == "collegeadmin"){
                            Text(
                                text = "Admin Home Features:",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Green
                            )
                        }else {
                            Text(
                                text = "Student Home Features:",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Cyan
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Box(
                            modifier = Modifier
                                .size(280.dp)
                                .clip(HexagonShape)
                                .background(Color(0xFF004d40).copy(alpha = 0.7f))
                                .padding(10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Stay Connected and Grow Together—Through Mentorship, Memories & More!",
                                fontSize = 24.sp,
                                fontStyle = FontStyle.Italic,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        )
    }
}

val HexagonShape = GenericShape { size, _ ->
    val width = size.width
    val height = size.height
    moveTo(width * 0.5f, 0f)
    lineTo(width, height * 0.25f)
    lineTo(width, height * 0.75f)
    lineTo(width * 0.5f, height)
    lineTo(0f, height * 0.75f)
    lineTo(0f, height * 0.25f)
    close()
}
