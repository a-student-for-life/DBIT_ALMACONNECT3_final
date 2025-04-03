package com.example.dbit_almaconnect3.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DrawerState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun DrawerContent(
    navController: NavController,
    drawerState: DrawerState,
    scope: CoroutineScope,
    onLogout: () -> Unit  // New callback for logout action
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x55000000))
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Menu",
            fontSize = 30.sp,
            modifier = Modifier.padding(bottom = 16.dp),
            color = Color.White
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 55.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            DrawerItem("🏠 Home", "home", navController, drawerState, scope)
            DrawerItem("📣 Announcements", "announcements", navController, drawerState, scope)
            DrawerItem("🏢 Company Insights", "company_insights", navController, drawerState, scope)
            DrawerItem("💼 Job Portal", "jobs", navController, drawerState, scope)
            DrawerItem("🎉 Events & Reunions", "events", navController, drawerState, scope)
            DrawerItem("👨‍🏫 Mentorship", "mentorship", navController, drawerState, scope)
            DrawerItem("📝 Success Stories/Blogs", "success_stories", navController, drawerState, scope)
        }
        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = {
                onLogout()
                scope.launch { drawerState.close() }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
        ) {
            Text(text = "Logout", color = Color.White, fontSize = 18.sp)
        }
    }
}

@Composable
fun DrawerItem(
    title: String,
    route: String,
    navController: NavController,
    drawerState: DrawerState,
    scope: CoroutineScope
) {
    Button(
        onClick = {
            if (navController.currentDestination?.route != route) {
                navController.navigate(route) {
                    launchSingleTop = true
                }
            }
            scope.launch { drawerState.close() }
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF005B4F))
    ) {
        Text(text = title, color = Color.White, fontSize = 18.sp)
    }
}

