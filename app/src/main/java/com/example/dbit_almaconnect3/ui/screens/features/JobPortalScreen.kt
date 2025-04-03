package com.example.dbit_almaconnect3.ui.screens.features

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.dbit_almaconnect3.utils.DateUtils
import com.example.dbit_almaconnect3.viewmodel.JobPortalViewModel
import com.example.dbit_almaconnect3.data.api.JobPostingResponse
import androidx.compose.foundation.shape.RoundedCornerShape

@Composable
fun JobPortalScreen(email: String, role: String, navController: NavController) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (role.lowercase() == "alumni") {
            Text(text = "Alumni Job Portal for $email", fontSize = 24.sp)
        } else if (role.lowercase() == "collegeadmin") {
            CollegeAdminJobPortalScreen(email, navController)
        } else {
            Text(text = "Student Job & Internship Portal for $email", fontSize = 24.sp)
        }
    }
}

@Composable
fun CollegeAdminJobPortalScreen(email: String, navController: NavController) {
    val viewModel: JobPortalViewModel = viewModel()
    val jobs by viewModel.jobs.collectAsState()
    val context = LocalContext.current

    // Fetch jobs when screen loads
    LaunchedEffect(Unit) {
        viewModel.fetchJobs()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Job Portal Management",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Text(
            text = "Oversee job listings, verify opportunities, and manage collaborations with employers",
            fontSize = 16.sp,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Pending Job Verification Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Pending Job Verification",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                val pendingJobs = jobs.filter { !it.status }

                if (pendingJobs.isEmpty()) {
                    Text(
                        text = "No pending jobs to review",
                        fontSize = 14.sp,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                } else {
                    Text(
                        text = "Review and verify jobs posted by alumni",
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    pendingJobs.forEach { job ->
                        PendingJobCard(
                            job = job,
                            onVerify = {
                                viewModel.updateJobStatus(job.id, true) {
                                    Toast.makeText(
                                        context,
                                        "Job '${job.title}' has been verified",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }

        // Verified Jobs Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Verified Jobs",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                val verifiedJobs = jobs.filter { it.status }

                if (verifiedJobs.isEmpty()) {
                    Text(
                        text = "No verified jobs",
                        fontSize = 14.sp,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                } else {
                    Text(
                        text = "Jobs that have been reviewed and verified",
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    verifiedJobs.forEach { job ->
                        VerifiedJobCard(
                            job = job,
                            onRevoke = {
                                viewModel.updateJobStatus(job.id, false) {
                                    Toast.makeText(
                                        context,
                                        "Verification revoked for '${job.title}'",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }

        // Company Statistics Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Company Engagement",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Text(
                    text = "Companies actively recruiting from your institution",
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Group jobs by company and count them
                val companyStats = jobs
                    .groupBy { it.company }
                    .map { (company, jobList) -> company to jobList.size }
                    .sortedByDescending { it.second }

                if (companyStats.isEmpty()) {
                    Text(
                        text = "No companies have posted jobs yet",
                        fontSize = 14.sp
                    )
                } else {
                    Column {
                        companyStats.forEach { (company, count) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = company, fontWeight = FontWeight.Medium)
                                Text(text = "$count job postings")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PendingJobCard(job: JobPostingResponse, onVerify: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = job.title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFFA000)
                    ),
                    modifier = Modifier.padding(4.dp)
                ) {
                    Text(
                        text = "⏳ Pending",
                        color = Color.White,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Text(
                text = "Company: ${job.company}",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = job.description,
                fontSize = 14.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Posted by: ${job.postedBy}",
                fontSize = 12.sp,
                color = Color.Gray
            )

            Text(
                text = "Posted: ${DateUtils.formatDate(job.postedAt)}",
                fontSize = 12.sp,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onVerify,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4CAF50)
                )
            ) {
                Text("Verify Job")
            }
        }
    }
}

@Composable
fun VerifiedJobCard(job: JobPostingResponse, onRevoke: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1B5E20) // Darker green background that works better in dark mode
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = job.title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White // Make text white for better contrast on dark background
                )

                Card(
                    colors = CardDefaults.cardColors(
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

            Text(
                text = "Company: ${job.company}",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White // Make text white for better contrast on dark background
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = job.description,
                fontSize = 14.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                color = Color.White.copy(alpha = 0.87f) // Slightly dimmed white for better readability
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Posted by: ${job.postedBy}",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.6f) // Dimmed white for secondary text
            )

            Text(
                text = "Posted: ${DateUtils.formatDate(job.postedAt)}",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.6f) // Dimmed white for secondary text
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onRevoke,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFF44336)
                )
            ) {
                Text("Revoke Verification")
            }
        }
    }
}