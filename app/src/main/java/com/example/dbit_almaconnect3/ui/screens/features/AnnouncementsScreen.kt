package com.example.dbit_almaconnect3.ui.screens.features

import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.text.SimpleDateFormat
import java.util.*
import com.example.dbit_almaconnect3.utils.DateUtils
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.content.Context
import com.example.dbit_almaconnect3.App
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import coil.size.Size
import androidx.compose.ui.text.style.TextAlign

data class Announcement(
    val id: String,
    val title: String,
    val content: String,
    val created: String,
    val updated: String,
    val pinned: Boolean = false,
    val attachments: List<String>? = null
)

data class Comment(
    val id: String,
    val text: String,
    val author: String,
    val authorName: String,
    val created: String,
    val announcementId: String
)

@Composable
fun StudentAnnouncementsScreen(email: String, navController: NavController) {
    var announcements by remember { mutableStateOf<List<Announcement>>(emptyList()) }
    var sortByNewest by remember { mutableStateOf(true) }
    val context = LocalContext.current
    
    LaunchedEffect(key1 = Unit) {
        fetchAnnouncements { fetchedAnnouncements ->
            announcements = fetchedAnnouncements
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Announcements",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            
            // Sort button
            IconButton(onClick = { sortByNewest = !sortByNewest }) {
                Icon(
                    Icons.Default.Sort,
                    contentDescription = "Sort",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        
        if (announcements.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            // Display sorting option text
            Text(
                text = "Showing ${if (sortByNewest) "newest" else "oldest"} first",
                fontSize = 12.sp,
                color = Color.Gray,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                textAlign = TextAlign.End
            )
            
            // Sort announcements based on user selection
            val sortedAnnouncements = if (sortByNewest) {
                announcements.sortedByDescending { it.created }
            } else {
                announcements.sortedBy { it.created }
            }
            
            LazyColumn {
                items(sortedAnnouncements) { announcement ->
                    AnnouncementItem(announcement = announcement)
                }
            }
        }
    }
}

@Composable
fun AlumniAnnouncementsScreen(email: String, navController: NavController) {
    var announcements by remember { mutableStateOf<List<Announcement>>(emptyList()) }
    var sortByNewest by remember { mutableStateOf(true) }
    val context = LocalContext.current
    
    LaunchedEffect(key1 = Unit) {
        fetchAnnouncements { fetchedAnnouncements ->
            announcements = fetchedAnnouncements
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Announcements",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            
            // Sort button
            IconButton(onClick = { sortByNewest = !sortByNewest }) {
                Icon(
                    Icons.Default.Sort,
                    contentDescription = "Sort",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        
        if (announcements.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            // Display sorting option text
            Text(
                text = "Showing ${if (sortByNewest) "newest" else "oldest"} first",
                fontSize = 12.sp,
                color = Color.Gray,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                textAlign = TextAlign.End
            )
            
            // Sort announcements based on user selection
            val sortedAnnouncements = if (sortByNewest) {
                announcements.sortedByDescending { it.created }
            } else {
                announcements.sortedBy { it.created }
            }
            
            LazyColumn {
                items(sortedAnnouncements) { announcement ->
                    AnnouncementItem(announcement = announcement)
                }
            }
        }
    }
}

@Composable
fun CollegeAdminAnnouncementsScreen(email: String, navController: NavController) {
    var announcements by remember { mutableStateOf<List<Announcement>>(emptyList()) }
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedAnnouncement by remember { mutableStateOf<Announcement?>(null) }
    var isRefreshing by remember { mutableStateOf(false) }
    var toastMessage by remember { mutableStateOf<String?>(null) }
    var sortByNewest by remember { mutableStateOf(true) }
    var managementModeAnnouncementId by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    
    // Save admin role in SharedPreferences to ensure it's available
    LaunchedEffect(Unit) {
        val sharedPrefs = App.instance.getSharedPreferences("auth", Context.MODE_PRIVATE)
        val editor = sharedPrefs.edit()
        
        // Check current role
        val currentRole = sharedPrefs.getString("role", "") ?: ""
        Log.d("AdminScreen", "Current role in SharedPreferences: '$currentRole'")
        
        // If role is not set or different, update it
        if (currentRole.isEmpty() || !currentRole.equals("collegeadmin", ignoreCase = true)) {
            Log.d("AdminScreen", "Setting role to 'collegeadmin' in SharedPreferences")
            editor.putString("role", "collegeadmin")
            editor.putString("email", email)
            editor.apply()
        }
        
        // Verify it was set
        val updatedRole = sharedPrefs.getString("role", "")
        Log.d("AdminScreen", "Updated role in SharedPreferences: '$updatedRole'")
    }
    
    // Show toast message when it changes
    LaunchedEffect(toastMessage) {
        toastMessage?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            toastMessage = null
        }
    }
    
    fun refreshAnnouncements() {
        isRefreshing = true
        fetchAnnouncements { fetchedAnnouncements ->
            MainScope().launch(Dispatchers.Main) {
                announcements = fetchedAnnouncements
                isRefreshing = false
            }
        }
    }
    
    LaunchedEffect(key1 = Unit) {
        refreshAnnouncements()
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Manage Announcements",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Sort Button
                IconButton(onClick = { sortByNewest = !sortByNewest }) {
                    Icon(
                        Icons.Default.Sort,
                        contentDescription = "Sort ${if (sortByNewest) "Newest" else "Oldest"} first",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                
                // Add Button
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Add Announcement")
                }
            }
        }
        
        if (isRefreshing) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (announcements.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "No announcements yet. Click + to add one.")
            }
        } else {
            // Sort announcements based on user selection
            val sortedAnnouncements = if (sortByNewest) {
                announcements.sortedByDescending { it.created }
            } else {
                announcements.sortedBy { it.created }
            }
            
            LazyColumn {
                items(sortedAnnouncements) { announcement ->
                    // Show the regular announcement item with comments functionality
                    Column {
                        // Admin management buttons
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp),
                            horizontalArrangement = Arrangement.End
                        ) {
                            IconButton(onClick = { selectedAnnouncement = announcement }) {
                                Icon(
                                    Icons.Filled.Edit,
                                    contentDescription = "Edit",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            
                            IconButton(
                                onClick = {
                                    MainScope().launch {
                                        deleteAnnouncement(announcement.id) { success, message ->
                                            MainScope().launch(Dispatchers.Main) {
                                                if (success) {
                                                    toastMessage = "Announcement deleted"
                                                    refreshAnnouncements()
                                                } else {
                                                    toastMessage = "Failed to delete: $message"
                                                }
                                            }
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    Icons.Filled.Delete,
                                    contentDescription = "Delete",
                                    tint = Color.Red
                                )
                            }
                        }
                        
                        // The regular announcement item with comments
                        AnnouncementItem(announcement = announcement)
                    }
                }
            }
        }
    }
    
    if (showAddDialog) {
        AnnouncementDialog(
            announcement = null,
            onDismiss = { showAddDialog = false },
            onSave = { title, content, attachments, pinned ->
                MainScope().launch {
                    createAnnouncement(title, content, attachments, pinned) { success, message ->
                        MainScope().launch(Dispatchers.Main) {
                            if (success) {
                                toastMessage = "Announcement created"
                                refreshAnnouncements()
                            } else {
                                toastMessage = "Failed to create: $message"
                            }
                            showAddDialog = false
                        }
                    }
                }
            }
        )
    }
    
    if (selectedAnnouncement != null) {
        AnnouncementDialog(
            announcement = selectedAnnouncement,
            onDismiss = { selectedAnnouncement = null },
            onSave = { title, content, attachments, pinned ->
                MainScope().launch {
                    updateAnnouncement(selectedAnnouncement!!.id, title, content, attachments, pinned) { success, message ->
                        MainScope().launch(Dispatchers.Main) {
                            if (success) {
                                toastMessage = "Announcement updated"
                                refreshAnnouncements()
                            } else {
                                toastMessage = "Failed to update: $message"
                            }
                            selectedAnnouncement = null
                        }
                    }
                }
            }
        )
    }
}

@Composable
fun AnnouncementDialog(
    announcement: Announcement?,
    onDismiss: () -> Unit,
    onSave: (title: String, content: String, attachments: List<Uri>?, pinned: Boolean) -> Unit
) {
    var title by remember { mutableStateOf(announcement?.title ?: "") }
    var content by remember { mutableStateOf(announcement?.content ?: "") }
    var pinned by remember { mutableStateOf(announcement?.pinned ?: false) }
    var attachments by remember { mutableStateOf<List<Uri>>(emptyList()) }
    
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        attachments = uris
    }
    
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = if (announcement == null) "Create Announcement" else "Edit Announcement",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )
                
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Content") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                )
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = pinned,
                        onCheckedChange = { pinned = it }
                    )
                    Text("Pin this announcement")
                }
                
                Button(
                    onClick = { launcher.launch("*/*") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Text("Add Attachments")
                }
                
                if (attachments.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Text(
                            text = "Selected Attachments:",
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        attachments.forEach { uri ->
                            Text(
                                text = uri.lastPathSegment ?: "Unknown file",
                                fontSize = 12.sp,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }
                }
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onSave(title, content, attachments, pinned) },
                        enabled = title.isNotBlank() && content.isNotBlank()
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

@Composable
fun AnnouncementsList(announcements: List<Announcement>) {
    var sortByNewest by remember { mutableStateOf(true) }
    
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Sort by: ${if (sortByNewest) "Newest" else "Oldest"}",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.primary
            )
            IconButton(onClick = { sortByNewest = !sortByNewest }) {
                Icon(
                    imageVector = Icons.Default.Sort,
                    contentDescription = "Sort",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        
        val sortedAnnouncements = if (sortByNewest) {
            announcements.sortedByDescending { it.created }
        } else {
            announcements.sortedBy { it.created }
        }
        
        LazyColumn {
            items(sortedAnnouncements) { announcement ->
                AnnouncementItem(announcement = announcement)
            }
        }
    }
}

@Composable
fun AnnouncementItem(announcement: Announcement) {
    var comments by remember { mutableStateOf<List<Comment>>(emptyList()) }
    var showComments by remember { mutableStateOf(false) }
    var newCommentText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var refreshTrigger by remember { mutableStateOf(0) }
    val context = LocalContext.current
    
    // Get user role from SharedPreferences
    val sharedPrefs = App.instance.getSharedPreferences("auth", Context.MODE_PRIVATE)
    val userRole = sharedPrefs.getString("role", "") ?: ""
    val userEmail = sharedPrefs.getString("email", "") ?: ""
    
    // Check if user is a college admin - check for both "college" and "collegeadmin"
    val isCollegeAdmin = userRole.equals("college", ignoreCase = true) || 
                        userRole.equals("collegeadmin", ignoreCase = true) ||
                        userRole.equals("admin", ignoreCase = true)
    
    // Function to refresh comments
    fun refreshComments() {
        isLoading = true
        fetchComments(announcement.id) { fetchedComments ->
            comments = fetchedComments
            isLoading = false
        }
    }

    LaunchedEffect(announcement.id, showComments, refreshTrigger) {
        if (showComments) {
            refreshComments()
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Special indicator for college admins
            if (isCollegeAdmin) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Admin View: ID ${announcement.id}",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontStyle = FontStyle.Italic
                    )
                    
                    if (showComments) {
                        Text(
                            text = "${comments.size} comments",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            
            if (announcement.pinned) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Icon(
                        imageVector = Icons.Default.PushPin,
                        contentDescription = "Pinned",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            Text(
                text = announcement.title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Text(
                text = announcement.content,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            // Enhanced attachment display with better previews
            if (!announcement.attachments.isNullOrEmpty()) {
                Text(
                    text = "Attachments:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
                
                announcement.attachments.forEach { attachment ->
                    val isImage = attachment.lowercase().endsWith(".jpg") || 
                                 attachment.lowercase().endsWith(".jpeg") || 
                                 attachment.lowercase().endsWith(".png") || 
                                 attachment.lowercase().endsWith(".gif")
                    
                    val isPdf = attachment.lowercase().endsWith(".pdf")
                    val isDoc = attachment.lowercase().endsWith(".doc") || 
                               attachment.lowercase().endsWith(".docx")
                    
                    val fileName = attachment.substringAfterLast("/")
                    val context = LocalContext.current
                    
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable {
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    data = Uri.parse(attachment)
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                context.startActivity(intent)
                            },
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = when {
                                        isImage -> Icons.Default.Image
                                        isPdf -> Icons.Default.PictureAsPdf
                                        isDoc -> Icons.Default.Description
                                        else -> Icons.Default.InsertDriveFile
                                    },
                                    contentDescription = "Attachment type",
                                    modifier = Modifier.size(24.dp),
                                    tint = when {
                                        isImage -> Color(0xFF4CAF50) // Green
                                        isPdf -> Color(0xFFF44336)   // Red
                                        isDoc -> Color(0xFF2196F3)   // Blue
                                        else -> Color.Gray
                                    }
                                )
                                
                                Spacer(modifier = Modifier.width(8.dp))
                                
                                Text(
                                    text = fileName,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            
                            // Show image preview for image files
                            if (isImage) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Image(
                                    painter = rememberAsyncImagePainter(
                                        ImageRequest.Builder(LocalContext.current)
                                            .data(data = attachment)
                                            .size(Size.ORIGINAL)
                                            .build()
                                    ),
                                    contentDescription = "Attachment preview",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }
                }
            }
            
            Text(
                text = "Posted: ${DateUtils.formatDateTime(announcement.created)}",
                fontSize = 12.sp,
                color = Color.Gray
            )

            // Comments section
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showComments = !showComments },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (showComments) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (showComments) "Hide comments" else "Show comments",
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = if (showComments) "Hide comments" else "Show comments",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(start = 4.dp)
                )
                
                // For admins, show comment count
                if (isCollegeAdmin && !showComments) {
                    Spacer(modifier = Modifier.weight(1f))
                    
                    // Get comment count without showing comments
                    LaunchedEffect(announcement.id) {
                        fetchCommentCount(announcement.id) { count ->
                            comments = List(count) { Comment("", "", "", "", "", "") } // Just for count
                        }
                    }
                    
                    Text(
                        text = "${comments.size} comments",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
            
            if (showComments) {
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    ) {
                        if (comments.isEmpty()) {
                            Text(
                                text = "No comments yet. Be the first to comment!",
                                fontSize = 14.sp,
                                color = Color.Gray,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        } else {
                            comments.forEach { comment ->
                                CommentItem(
                                    comment = comment,
                                    onCommentDeleted = {
                                        refreshTrigger += 1
                                    }
                                )
                            }
                        }
                        
                        // Allow all users (including college admins) to add comments
                        OutlinedTextField(
                            value = newCommentText,
                            onValueChange = { newCommentText = it },
                            placeholder = { Text("Add a comment...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                        )
                        
                        Button(
                            onClick = {
                                if (newCommentText.isNotBlank()) {
                                    val username = if (isCollegeAdmin) {
                                        "College Admin"  // Set display name for college admins
                                    } else {
                                        userEmail.substringBefore("@")
                                    }
                                    
                                    postComment(
                                        announcementId = announcement.id,
                                        text = newCommentText,
                                        author = userEmail,
                                        authorName = username
                                    ) { success, message ->
                                        MainScope().launch(Dispatchers.Main) {
                                            if (success) {
                                                refreshComments()
                                                newCommentText = ""
                                            } else {
                                                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                }
                            },
                            enabled = newCommentText.isNotBlank(),
                            modifier = Modifier
                                .align(Alignment.End)
                                .padding(top = 8.dp)
                        ) {
                            Text("Post")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CommentItem(
    comment: Comment,
    onCommentDeleted: () -> Unit
) {
    val context = LocalContext.current
    var showDeleteConfirm by remember { mutableStateOf(false) }
    
    // Get current user information
    val sharedPrefs = context.getSharedPreferences("auth", Context.MODE_PRIVATE)
    val currentUserEmail = sharedPrefs.getString("email", "") ?: ""
    val userRole = sharedPrefs.getString("role", "") ?: ""
    
    // Determine permissions - allow both authors and admins to delete
    val isAuthor = comment.author.equals(currentUserEmail, ignoreCase = true)
    val isAdmin = userRole.equals("admin", ignoreCase = true)
    val isCollegeAdmin = userRole.equals("college", ignoreCase = true) || 
                        userRole.equals("collegeadmin", ignoreCase = true)
    val canDelete = isAuthor || isAdmin || isCollegeAdmin
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = comment.text,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    
                    Text(
                        text = "By ${comment.authorName} on ${DateUtils.formatDateTime(comment.created)}",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        fontStyle = FontStyle.Italic
                    )
                }
                
                if (canDelete) {
                    IconButton(
                        onClick = { showDeleteConfirm = true }
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
    
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Comment?") },
            text = { Text("Are you sure you want to delete this comment?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val mainScope = MainScope()
                        mainScope.launch {
                            deleteComment(comment.id) { success, message ->
                                mainScope.launch(Dispatchers.Main) {
                                    if (success) {
                                        Toast.makeText(context, "Comment deleted", Toast.LENGTH_SHORT).show()
                                        onCommentDeleted()
                                        showDeleteConfirm = false
                                    } else {
                                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun AnnouncementItemWithActions(
    announcement: Announcement,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = announcement.title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(
                            Icons.Filled.Edit,
                            contentDescription = "Edit",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = "Delete",
                            tint = Color.Red
                        )
                    }
                }
            }
            
            Text(
                text = announcement.content,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = "Posted: ${DateUtils.formatDateTime(announcement.created)}",
                fontSize = 12.sp,
                color = Color.Gray
            )
            if (announcement.updated != announcement.created) {
                Text(
                    text = "Updated: ${DateUtils.formatDateTime(announcement.updated)}",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

fun fetchAnnouncements(callback: (List<Announcement>) -> Unit) {
    val client = OkHttpClient()
    val token = App.instance.getSharedPreferences("auth", Context.MODE_PRIVATE).getString("token", "") ?: ""
    val baseUrl = "http://129.154.249.30:8091"
    val collectionId = "Announcements" // Collection name for announcements
    
    val request = Request.Builder()
        .url("$baseUrl/api/collections/Announcements/records")
        .addHeader("Authorization", "Bearer $token")
        .build()
    
    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            Log.e("AnnouncementsScreen", "Failed to fetch announcements: ${e.message}", e)
            when {
                e is UnknownHostException -> callback(emptyList()) // No internet connection
                e is SocketTimeoutException -> callback(emptyList()) // Request timed out
                else -> callback(emptyList())
            }
        }
        
        override fun onResponse(call: Call, response: Response) {
            if (response.isSuccessful) {
                val responseData = response.body?.string()
                try {
                    val jsonObject = JSONObject(responseData)
                    val itemsArray = jsonObject.getJSONArray("items")
                    val announcements = mutableListOf<Announcement>()
                    
                    for (i in 0 until itemsArray.length()) {
                        val item = itemsArray.getJSONObject(i)
                        val recordId = item.getString("id")
                        
                        // Process attachments to create full URLs
                        val attachments = item.optJSONArray("attachments")?.let { attachmentsArray ->
                            (0 until attachmentsArray.length()).map { index ->
                                val fileName = attachmentsArray.getString(index)
                                // Construct the full PocketBase file URL
                                "$baseUrl/api/files/$collectionId/$recordId/$fileName"
                            }
                        }
                        
                        announcements.add(
                            Announcement(
                                id = recordId,
                                title = item.getString("title"),
                                content = item.getString("content"),
                                created = item.getString("created"),
                                updated = item.getString("updated"),
                                pinned = item.optBoolean("pinned", false),
                                attachments = attachments
                            )
                        )
                    }
                    
                    callback(announcements)
                } catch (e: Exception) {
                    Log.e("AnnouncementsScreen", "Error parsing announcements: ${e.message}", e)
                    callback(emptyList())
                }
            } else {
                handleErrorResponse(response) { errorMessage ->
                    Log.e("AnnouncementsScreen", "Failed to fetch announcements: $errorMessage")
                    callback(emptyList())
                }
            }
        }
    })
}

fun createAnnouncement(
    title: String,
    content: String,
    attachments: List<Uri>?,
    pinned: Boolean,
    callback: (Boolean, String) -> Unit
) {
    val client = OkHttpClient()
    val token = App.instance.getSharedPreferences("auth", Context.MODE_PRIVATE).getString("token", "") ?: ""
    
    try {
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("title", title)
            .addFormDataPart("content", content)
            .addFormDataPart("pinned", pinned.toString())
            .addFormDataPart("datetime", DateUtils.getCurrentISODateTime())
        
        // Add attachments if any
        attachments?.forEach { uri ->
            val context = App.instance.applicationContext
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val fileName = uri.lastPathSegment ?: "attachment_${System.currentTimeMillis()}"
                val bytes = inputStream.readBytes()
                
                // Check file size (10MB limit)
                if (bytes.size > 10 * 1024 * 1024) {
                    throw IOException("File size exceeds 10MB limit: $fileName")
                }
                
                requestBody.addFormDataPart(
                    "attachments",
                    fileName,
                    bytes.toRequestBody("application/octet-stream".toMediaType())
                )
            }
        }
        
        val request = Request.Builder()
            .url("http://129.154.249.30:8091/api/collections/Announcements/records")
            .addHeader("Authorization", "Bearer $token")
            .post(requestBody.build())
            .build()
        
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("AnnouncementsScreen", "Failed to create announcement: ${e.message}", e)
                val errorMessage = when (e) {
                    is UnknownHostException -> "No internet connection"
                    is SocketTimeoutException -> "Request timed out"
                    else -> e.message ?: "Network error"
                }
                callback(false, errorMessage)
            }
            
            override fun onResponse(call: Call, response: Response) {
                handleResponse(response, "create") { success, message ->
                    callback(success, message)
                }
            }
        })
    } catch (e: IOException) {
        Log.e("AnnouncementsScreen", "Error processing attachments: ${e.message}", e)
        callback(false, e.message ?: "Error processing attachments")
    }
}

private fun handleResponse(response: Response, operation: String, callback: (Boolean, String) -> Unit) {
    val responseBody = response.body?.string() ?: ""
    if (response.isSuccessful) {
        Log.d("AnnouncementsScreen", "Operation $operation successful: $responseBody")
        callback(true, "Success")
    } else {
        Log.e("AnnouncementsScreen", "Operation $operation failed: ${response.code}, Error: $responseBody")
        val errorMessage = try {
            val jsonObject = JSONObject(responseBody)
            when (response.code) {
                401 -> "Authentication required"
                403 -> "Not authorized"
                413 -> "File size too large"
                422 -> jsonObject.optString("message", "Validation error")
                else -> jsonObject.optString("message", "Error ${response.code}")
            }
        } catch (e: Exception) {
            "Error ${response.code}"
        }
        callback(false, errorMessage)
    }
}

fun updateAnnouncement(
    id: String,
    title: String,
    content: String,
    attachments: List<Uri>?,
    pinned: Boolean,
    callback: (Boolean, String) -> Unit
) {
    val client = OkHttpClient()
    val token = App.instance.getSharedPreferences("auth", Context.MODE_PRIVATE).getString("token", "") ?: ""
    
    try {
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("title", title)
            .addFormDataPart("content", content)
            .addFormDataPart("pinned", pinned.toString())
            .addFormDataPart("datetime", DateUtils.getCurrentISODateTime())
        
        // Add attachments if any
        attachments?.forEach { uri ->
            val context = App.instance.applicationContext
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val fileName = uri.lastPathSegment ?: "attachment_${System.currentTimeMillis()}"
                val bytes = inputStream.readBytes()
                
                // Check file size (10MB limit)
                if (bytes.size > 10 * 1024 * 1024) {
                    throw IOException("File size exceeds 10MB limit: $fileName")
                }
                
                requestBody.addFormDataPart(
                    "attachments",
                    fileName,
                    bytes.toRequestBody("application/octet-stream".toMediaType())
                )
            }
        }
        
        val request = Request.Builder()
            .url("http://129.154.249.30:8091/api/collections/Announcements/records/$id")
            .addHeader("Authorization", "Bearer $token")
            .patch(requestBody.build())
            .build()
        
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("AnnouncementsScreen", "Failed to update announcement: ${e.message}", e)
                val errorMessage = when (e) {
                    is UnknownHostException -> "No internet connection"
                    is SocketTimeoutException -> "Request timed out"
                    else -> e.message ?: "Network error"
                }
                callback(false, errorMessage)
            }
            
            override fun onResponse(call: Call, response: Response) {
                handleResponse(response, "update") { success, message ->
                    callback(success, message)
                }
            }
        })
    } catch (e: IOException) {
        Log.e("AnnouncementsScreen", "Error processing attachments: ${e.message}", e)
        callback(false, e.message ?: "Error processing attachments")
    }
}

fun deleteAnnouncement(id: String, callback: (Boolean, String) -> Unit) {
    val client = OkHttpClient()
    val token = App.instance.getSharedPreferences("auth", Context.MODE_PRIVATE).getString("token", "") ?: ""
    
    val request = Request.Builder()
        .url("http://129.154.249.30:8091/api/collections/Announcements/records/$id")
        .addHeader("Authorization", "Bearer $token")
        .delete()
        .build()
    
    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            Log.e("AnnouncementsScreen", "Failed to delete announcement: ${e.message}", e)
            val errorMessage = when (e) {
                is UnknownHostException -> "No internet connection"
                is SocketTimeoutException -> "Request timed out"
                else -> e.message ?: "Network error"
            }
            callback(false, errorMessage)
        }
        
        override fun onResponse(call: Call, response: Response) {
            handleResponse(response, "delete") { success, message ->
                callback(success, message)
            }
        }
    })
}

private fun handleErrorResponse(response: Response, callback: (String) -> Unit) {
    val responseBody = response.body?.string() ?: ""
    Log.e("AnnouncementsScreen", "Error response: ${response.code}, Body: $responseBody")
    
    val errorMessage = try {
        val jsonObject = JSONObject(responseBody)
        when (response.code) {
            401 -> "Authentication required"
            403 -> "Not authorized"
            413 -> "File size too large"
            422 -> jsonObject.optString("message", "Validation error")
            else -> jsonObject.optString("message", "Error ${response.code}")
        }
    } catch (e: Exception) {
        "Error ${response.code}"
    }
    
    callback(errorMessage)
}

fun fetchComments(announcementId: String, callback: (List<Comment>) -> Unit) {
    val client = OkHttpClient()
    val sharedPrefs = App.instance.getSharedPreferences("auth", Context.MODE_PRIVATE)
    val token = sharedPrefs.getString("token", "") ?: ""
    val currentUserEmail = sharedPrefs.getString("email", "") ?: ""
    val userRole = sharedPrefs.getString("role", "") ?: ""
    
    // Log the user role for debugging
    val isCollegeAdmin = userRole.equals("college", ignoreCase = true) || 
                         userRole.equals("collegeadmin", ignoreCase = true) ||
                         userRole.equals("admin", ignoreCase = true)
    
    Log.d("CommentDebug", "Fetching comments for announcement: $announcementId")
    Log.d("CommentDebug", "Current user: $currentUserEmail, role: '$userRole', isCollegeAdmin: $isCollegeAdmin")
    
    val request = Request.Builder()
        .url("http://129.154.249.30:8091/api/collections/Comments/records?filter=(announcement='$announcementId')")
        .addHeader("Authorization", "Bearer $token")
        .build()
    
    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            Log.e("CommentDebug", "Network error fetching comments: ${e.message}")
            callback(emptyList())
        }
        
        override fun onResponse(call: Call, response: Response) {
            if (response.isSuccessful) {
                val responseData = response.body?.string()
                Log.d("CommentDebug", "Comment list response (${response.code}): $responseData")
                
                try {
                    val jsonObject = JSONObject(responseData)
                    val itemsArray = jsonObject.getJSONArray("items")
                    val comments = mutableListOf<Comment>()
                    
                    Log.d("CommentDebug", "Found ${itemsArray.length()} comments")
                    
                    for (i in 0 until itemsArray.length()) {
                        val item = itemsArray.getJSONObject(i)
                        
                        // Extract essential fields
                        val commentId = item.optString("id", "unknown")
                        val text = item.optString("text", "")
                        val created = item.optString("created", "")
                        val anncmtId = item.optString("announcement", "")
                        
                        // Critical: Extract and validate author email
                        val authorEmail = item.optString("author", "").takeIf { 
                            it.isNotBlank() && it.contains("@") 
                        } ?: "unknown@example.com"
                        
                        // Extract or generate display name
                        val authorName = item.optString("authorName", "").takeIf { 
                            it.isNotBlank() && it != "Anonymous" 
                        } ?: authorEmail.substringBefore("@")
                        
                        // Log detailed info about this comment
                        Log.d("CommentDebug", "Comment $commentId: author=$authorEmail, name=$authorName, text=${text.take(20)}...")
                        
                        // Check if this comment belongs to current user
                        val isCurrentUserComment = authorEmail.equals(currentUserEmail, ignoreCase = true)
                        if (isCurrentUserComment) {
                            Log.d("CommentDebug", "Comment $commentId belongs to current user")
                        }
                        
                        // Create and add the comment object
                        comments.add(
                            Comment(
                                id = commentId,
                                text = text,
                                author = authorEmail,
                                authorName = authorName,
                                created = created,
                                announcementId = anncmtId
                            )
                        )
                    }
                    
                    // Sort by creation date, newest first
                    comments.sortByDescending { it.created }
                    
                    // Send comments back to the UI
                    callback(comments)
                    
                } catch (e: Exception) {
                    Log.e("CommentDebug", "Error parsing comments: ${e.message}")
                    e.printStackTrace()
                    callback(emptyList())
                }
            } else {
                val errorBody = response.body?.string() ?: "No error body"
                Log.e("CommentDebug", "Failed to fetch comments: ${response.code}, Error: $errorBody")
                callback(emptyList())
            }
        }
    })
}

// Separate function to get the current user's email - more reliable
fun getCurrentUserEmail(): String {
    val sharedPrefs = App.instance.getSharedPreferences("auth", Context.MODE_PRIVATE)
    return sharedPrefs.getString("email", "") ?: ""
}

fun postComment(
    announcementId: String,
    text: String,
    author: String,
    authorName: String,
    callback: (Boolean, String) -> Unit
) {
    val client = OkHttpClient()
    
    // Get a fresh token directly when posting
    val sharedPrefs = App.instance.getSharedPreferences("auth", Context.MODE_PRIVATE)
    val token = sharedPrefs.getString("token", "") ?: ""
    
    // 1. Validate inputs - ensure we have a valid author email
    if (author.isBlank() || !author.contains("@")) {
        Log.e("CommentDebug", "CRITICAL ERROR: Invalid author email: $author")
        callback(false, "Invalid user email")
        return
    }
    
    // 2. Prepare the display name
    val displayName = if (authorName.isBlank()) author.substringBefore("@") else authorName
    
    Log.d("CommentDebug", "Creating comment with author: $author, name: $displayName")
    
    // 3. Create the comment JSON
    val commentData = JSONObject().apply {
        put("text", text)
        put("announcement", announcementId) 
        put("author", author) // CRITICAL: This must be the user's email
        put("authorName", displayName)
    }
    
    val jsonString = commentData.toString()
    Log.d("CommentDebug", "Comment JSON: $jsonString")
    
    // 4. Create and send the request
    val requestBody = jsonString.toRequestBody("application/json".toMediaType())
    
    val request = Request.Builder()
        .url("http://129.154.249.30:8091/api/collections/Comments/records")
        .addHeader("Authorization", "Bearer $token")
        .addHeader("Content-Type", "application/json")
        .post(requestBody)
        .build()
    
    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            Log.e("CommentDebug", "Network error posting comment: ${e.message}")
            callback(false, "Network error: ${e.message}")
        }
        
        override fun onResponse(call: Call, response: Response) {
            val responseBody = response.body?.string() ?: ""
            Log.d("CommentDebug", "PocketBase response (${response.code}): $responseBody")
            
            if (response.isSuccessful) {
                try {
                    // Parse the response to verify saved data
                    val jsonResponse = JSONObject(responseBody)
                    val savedAuthor = jsonResponse.optString("author", "")
                    val savedName = jsonResponse.optString("authorName", "")
                    val commentId = jsonResponse.optString("id", "")
                    
                    Log.d("CommentDebug", "SUCCESS - Comment #$commentId saved with author: $savedAuthor")
                    
                    // Critical validation - did PocketBase save the correct author?
                    if (!author.equals(savedAuthor, ignoreCase = true)) {
                        Log.e("CommentDebug", "ERROR: PocketBase changed author from $author to $savedAuthor")
                    }
                    
                    callback(true, "Comment posted successfully")
                } catch (e: Exception) {
                    Log.e("CommentDebug", "Error parsing response: ${e.message}")
                    callback(true, "Comment posted, but couldn't verify details")
                }
            } else {
                // Error handling
                try {
                    val jsonObject = JSONObject(responseBody)
                    val errorMessage = jsonObject.optString("message", "Unknown error")
                    Log.e("CommentDebug", "PocketBase error: $errorMessage")
                    callback(false, "Error: $errorMessage")
                } catch (e: Exception) {
                    Log.e("CommentDebug", "Error ${response.code}: ${e.message}")
                    callback(false, "Error ${response.code}")
                }
            }
        }
    })
}

fun deleteComment(commentId: String, callback: (Boolean, String) -> Unit) {
    val client = OkHttpClient()
    val token = App.instance.getSharedPreferences("auth", Context.MODE_PRIVATE).getString("token", "") ?: ""
    
    val request = Request.Builder()
        .url("http://129.154.249.30:8091/api/collections/Comments/records/$commentId")
        .addHeader("Authorization", "Bearer $token")
        .delete()
        .build()
    
    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            Log.e("AnnouncementsScreen", "Failed to delete comment: ${e.message}", e)
            val errorMessage = when (e) {
                is UnknownHostException -> "No internet connection"
                is SocketTimeoutException -> "Request timed out"
                else -> e.message ?: "Network error"
            }
            callback(false, errorMessage)
        }
        
        override fun onResponse(call: Call, response: Response) {
            val responseBody = response.body?.string() ?: ""
            if (response.isSuccessful) {
                Log.d("AnnouncementsScreen", "Comment deleted successfully")
                callback(true, "Success")
            } else {
                Log.e("AnnouncementsScreen", "Failed to delete comment: ${response.code}, Error: $responseBody")
                val errorMessage = try {
                    val jsonObject = JSONObject(responseBody)
                    when (response.code) {
                        401 -> "Authentication required"
                        403 -> "Not authorized"
                        404 -> "Comment not found"
                        else -> jsonObject.optString("message", "Error ${response.code}")
                    }
                } catch (e: Exception) {
                    "Error ${response.code}"
                }
                callback(false, errorMessage)
            }
        }
    })
}

// Add a function to just get comment count for admins without loading all comments
fun fetchCommentCount(announcementId: String, callback: (Int) -> Unit) {
    val client = OkHttpClient()
    val token = App.instance.getSharedPreferences("auth", Context.MODE_PRIVATE).getString("token", "") ?: ""
    
    val request = Request.Builder()
        .url("http://129.154.249.30:8091/api/collections/Comments/records?filter=(announcement='$announcementId')&fields=id")
        .addHeader("Authorization", "Bearer $token")
        .build()
    
    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            Log.e("CommentDebug", "Failed to fetch comment count: ${e.message}")
            callback(0)
        }
        
        override fun onResponse(call: Call, response: Response) {
            if (response.isSuccessful) {
                try {
                    val responseData = response.body?.string()
                    val jsonObject = JSONObject(responseData)
                    val count = jsonObject.optInt("totalItems", 0)
                    callback(count)
                } catch (e: Exception) {
                    Log.e("CommentDebug", "Error parsing comment count: ${e.message}")
                    callback(0)
                }
            } else {
                callback(0)
            }
        }
    })
}