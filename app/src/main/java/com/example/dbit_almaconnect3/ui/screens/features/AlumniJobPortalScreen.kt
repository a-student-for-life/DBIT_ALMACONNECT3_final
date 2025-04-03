package com.example.dbit_almaconnect3.ui.screens.features

import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.dbit_almaconnect3.data.api.JobPostingResponse
import com.example.dbit_almaconnect3.utils.DateUtils
import com.example.dbit_almaconnect3.viewmodel.JobPortalViewModel

@Composable
fun AlumniJobPortalScreen(email: String, navController: NavController) {
    val viewModel: JobPortalViewModel = viewModel()
    val jobs by viewModel.jobs.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var jobTitle by remember { mutableStateOf("") }
    var jobDescription by remember { mutableStateOf("") }
    var company by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.fetchJobs()
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(text = "Welcome, $email", fontSize = 20.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = { showDialog = true }) {
            Text("Post a New Job/Internship")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Your Posted Jobs:", fontSize = 18.sp)
        Spacer(modifier = Modifier.height(8.dp))
        LazyColumn {
            items(jobs) { job ->
                if (job.postedBy == email) {
                    JobPostingCardForAlumni(job, navController)
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Post a New Job") },
            text = {
                Column {
                    OutlinedTextField(
                        value = jobTitle,
                        onValueChange = { jobTitle = it },
                        label = { Text("Job Title") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = company,
                        onValueChange = { company = it },
                        label = { Text("Company Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = jobDescription,
                        onValueChange = { jobDescription = it },
                        label = { Text("Job Description") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (jobTitle.isNotBlank() && company.isNotBlank()) {
                        viewModel.postJob(jobTitle, jobDescription, company, email) {
                            showDialog = false
                            jobTitle = ""
                            jobDescription = ""
                            company = ""
                        }
                    } else {
                        // Show error or handle empty fields
                    }
                }) {
                    Text("Post")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun JobPostingCardForAlumni(job: JobPostingResponse, navController: NavController) {
    // Access the same ViewModel or pass it in
    val viewModel: JobPortalViewModel = viewModel()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.Gray)
            .padding(8.dp)
    ) {
        Text(text = job.title, fontSize = 20.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
        Text(text = "Company: ${job.company}", fontSize = 16.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium)
        Text(text = job.description, fontSize = 16.sp)
        Spacer(modifier = Modifier.height(8.dp))

        // Show verification status
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = if (job.status) "✅ Verified by College" else "⏳ Pending Verification",
                color = if (job.status) Color.Green else Color.Gray,
                fontSize = 14.sp
            )

            Text(
                text = "Posted: ${DateUtils.formatDate(job.postedAt)}",
                fontSize = 14.sp,
                color = Color.Gray
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(
                onClick = {
                    // Navigate to see applications for this job
                    val encodedTitle = Uri.encode(job.title)
                    navController.navigate("jobApplications/$encodedTitle")
                }
            ) {
                Text("View Applications")
            }

            Spacer(modifier = Modifier.width(8.dp))

            TextButton(
                onClick = {
                    // Delete this job
                    viewModel.deleteJob(job.id, job.discussionLink) {
                        // Refresh happens in the ViewModel
                    }
                }
            ) {
                Text("Delete", color = Color.Red)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(
                onClick = {
                    if (job.discussionLink != null) {
                        val encodedLink = Uri.encode(job.discussionLink)
                        navController.navigate("discussion/$encodedLink") {
                            launchSingleTop = true
                        }
                    } else {
                        Toast.makeText(context, "No discussion available for this job", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Open Discussion")
            }
        }
    }
}
