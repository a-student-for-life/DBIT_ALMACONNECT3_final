package com.example.dbit_almaconnect3.ui.screens.features

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
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
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

@Composable
fun StudentJobPortalScreen(email: String, navController: NavController) {
    val viewModel: JobPortalViewModel = viewModel()
    val jobs by viewModel.jobs.collectAsState()
    var showApplyDialog by remember { mutableStateOf(false) }
    var selectedJob by remember { mutableStateOf<JobPostingResponse?>(null) }
    var resumeFile by remember { mutableStateOf<File?>(null) }

    // Use LocalContext.current for a proper Context
    val context = LocalContext.current

    // Launch file picker for PDFs
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            resumeFile = getFileFromUri(context, uri)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.fetchJobs()
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(text = "Welcome, $email", fontSize = 20.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Available Jobs/Internships:", fontSize = 18.sp)
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn {
            items(jobs) { job ->
                JobPostingCardForStudent(job, navController) {
                    selectedJob = job
                    showApplyDialog = true
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }

    if (showApplyDialog && selectedJob != null) {
        AlertDialog(
            onDismissRequest = {
                showApplyDialog = false
                resumeFile = null
            },
            title = { Text("Apply to ${selectedJob!!.title}") },
            text = {
                Column {
                    // Button to pick a resume file
                    Button(onClick = { filePickerLauncher.launch("application/pdf") }) {
                        Text(
                            text = if (resumeFile != null)
                                "Resume Selected: ${resumeFile!!.name}"
                            else "Choose Resume File"
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    resumeFile?.let { file ->
                        viewModel.applyForJob(selectedJob!!.id, email, file) {
                            showApplyDialog = false
                            resumeFile = null
                            Toast.makeText(
                                context,
                                "Application submitted successfully!",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }) {
                    Text("Apply")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showApplyDialog = false
                    resumeFile = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }
}

fun getFileFromUri(context: Context, uri: Uri): File? {
    var fileName = "resume.pdf"
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (cursor.moveToFirst() && nameIndex != -1) {
            fileName = cursor.getString(nameIndex)
        }
    }
    val tempFile = File(context.cacheDir, fileName)
    context.contentResolver.openInputStream(uri)?.use { inputStream ->
        FileOutputStream(tempFile).use { output ->
            inputStream.copyTo(output)
        }
    }
    return if (tempFile.exists()) tempFile else null
}

@Composable
fun JobPostingCardForStudent(job: JobPostingResponse, navController: NavController, onApply: () -> Unit) {
    val viewModel: JobPortalViewModel = viewModel()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.Gray)
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Text(
                text = job.title,
                fontSize = 20.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )

            if (job.status) {
                androidx.compose.material3.Card(
                    colors = androidx.compose.material3.CardDefaults.cardColors(
                        containerColor = Color(0xFF4CAF50)
                    ),
                    modifier = Modifier.padding(4.dp)
                ) {
                    Text(
                        text = "✓ Verified",
                        color = Color.White,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        Text(
            text = "Company: ${job.company}",
            fontSize = 16.sp,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = job.description,
            fontSize = 16.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Posted: ${DateUtils.formatDate(job.postedAt)}",
            fontSize = 14.sp,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(
                onClick = onApply,
                modifier = Modifier.weight(1f)
            ) {
                Text("Apply Now")
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = {
                    if (job.discussionLink != null) {
                        val encodedLink = Uri.encode(job.discussionLink)
                        navController.navigate("discussion/$encodedLink")
                    } else {
                        Toast.makeText(context, "No discussion available for this job", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Open Discussion")
            }
        }
    }
}
