package com.example.dbit_almaconnect3.ui.screens.features

// Use an alias for android.graphics.Color to avoid conflicts with Compose's Color
import android.graphics.Color as AndroidColor
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.dbit_almaconnect3.data.repository.FlarumTagRepository
import com.example.dbit_almaconnect3.data.repository.Tag
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import kotlinx.coroutines.launch
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.layout.ContentScale
import com.example.dbit_almaconnect3.R
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.MaterialTheme

// --------------------- HELPER EXTENSION ---------------------
// Extension to convert Compose Color to a hex string.
fun Color.toHexString(): String {
    return String.format("#%06X", 0xFFFFFF and this.toArgb())
}

// --------------------- CUSTOM COLOR SWATCH ---------------------
@Composable
fun CustomColorSwatch(
    color: Color,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(color)
            .clickable { onClick() }
            .border(
                width = if (selected) 4.dp else 2.dp,
                color = if (selected) Color.Black else Color.Gray,
                shape = CircleShape
            )
    )
}

// --------------------- CUSTOM COLOR PICKER ROWS ---------------------
@Composable
fun CustomColorPickerRows(
    primaryColors: List<Color>,
    additionalShades: List<Color>,
    selectedColor: Color,
    onColorSelected: (Color) -> Unit
) {
    Column {
        Text(text = "Primary Colors", fontSize = 14.sp)
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(primaryColors) { color ->
                CustomColorSwatch(
                    color = color,
                    selected = (color == selectedColor),
                    onClick = { onColorSelected(color) }
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Additional Shades", fontSize = 14.sp)
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(additionalShades) { color ->
                CustomColorSwatch(
                    color = color,
                    selected = (color == selectedColor),
                    onClick = { onColorSelected(color) }
                )
            }
        }
    }
}

// --------------------- TAG CARD (formerly TagItem) ---------------------
@Composable
fun TagCard(
    tag: Tag,
    onClick: () -> Unit
) {
    // Use a default color if tag.color is null or empty.
    val colorString = if (tag.color.isNullOrEmpty()) "#D3D3D3" else tag.color
    val backgroundColor = try {
        Color(AndroidColor.parseColor(colorString))
    } catch (e: Exception) {
        Color.LightGray
    }
    Card(
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(12.dp),
                spotColor = backgroundColor.copy(alpha = 0.5f)
            )
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = tag.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                
                // Add a subtle indicator icon
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(Color.White.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "→",
                        color = Color.White,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

// --------------------- ALUMNI EVENTS / REUNIONS SCREEN ---------------------
@Composable
fun AlumniEventsScreen(email: String, navController: NavController) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val flarumTagRepository = remember { FlarumTagRepository() }

    var graduationYear by remember { mutableStateOf("") }
    var discussionTitle by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(Color(0xFF005B4F)) }
    var tagList by remember { mutableStateOf<List<Tag>>(emptyList()) }
    var showCreateDialog by remember { mutableStateOf(false) }

    val primaryColors = listOf(
        Color(0xFF005B4F), // College theme color
        Color(0xFF2E7D32), // Forest Green
        Color(0xFF1565C0), // Royal Blue
        Color(0xFF6A1B9A), // Deep Purple
        Color(0xFFC62828), // Deep Red
        Color(0xFF8D6E63)  // Brown
    )
    val additionalShades = listOf(
        Color(0xFF78909C), // Blue Gray
        Color(0xFF546E7A), // Steel Blue
        Color(0xFF37474F), // Dark Slate
        Color(0xFF004D40), // Teal
        Color(0xFF006064), // Cyan
        Color(0xFF01579B)  // Light Blue
    )

    // Load both alumni-created events and admin events marked for alumni
    LaunchedEffect(Unit) {
        val allTags = flarumTagRepository.getTags() ?: return@LaunchedEffect
        
        // Get alumni-created events from alumni-reunions category
        val alumniParentId = allTags.firstOrNull { it.slug == "alumni-reunions" }?.id
        val alumniEvents = if (alumniParentId != null) {
            allTags.filter { it.parentId == alumniParentId }
        } else {
            emptyList()
        }
        
        // Get admin events marked for alumni from events category
        val eventsParentId = allTags.firstOrNull { it.slug == "events" }?.id
        val adminEvents = if (eventsParentId != null) {
            allTags.filter { tag ->
                tag.parentId == eventsParentId && 
                (tag.name.contains("[ALUMNI]") || tag.name.contains("[ALL]"))
            }
        } else {
            emptyList()
        }
        
        // Combine both lists
        tagList = alumniEvents + adminEvents
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5)) // Fallback background color
    ) {
        // Background Image (commented out until images are added)

        Image(
            painter = painterResource(id = R.drawable.student_events_bg),
            contentDescription = "Alumni Events Background",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            colorFilter = ColorFilter.tint(
                color = Color.Black.copy(alpha = 0.3f),
                blendMode = BlendMode.Darken
            )
        )


        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header with gradient background
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    )
                    .padding(16.dp)
            ) {
                Column {
                    Text(
                        text = "Alumni Events & Reunions",
                        style = MaterialTheme.typography.headlineLarge,
                        color = Color.White,
                        modifier = Modifier.shadow(4.dp, shape = RoundedCornerShape(4.dp))
                    )
                    Text(
                        text = "Connect with fellow alumni and celebrate together",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Create New Event button
            Button(
                onClick = { showCreateDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF005B4F)
                )
            ) {
                Text("Create New Event")
            }

            // List of events
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                items(tagList) { tag ->
                    val colorStr = if (tag.color.isNullOrEmpty()) "#005B4F" else tag.color
                    val backgroundColor = try {
                        Color(AndroidColor.parseColor(colorStr))
                    } catch (e: Exception) {
                        Color(0xFF005B4F)
                    }
                    
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable {
                                val tagUrl = "http://129.154.249.30:8080/t/${tag.slug}"
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(tagUrl))
                                context.startActivity(intent)
                            }
                    ) {
                        Box(
                            modifier = Modifier
                                .background(backgroundColor)
                                .padding(16.dp)
                        ) {
                            Column {
                                Text(
                                    text = tag.name,
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                
                                if (tag.name.contains("[OFFICIAL]")) {
                                    Text(
                                        text = "OFFICIAL COLLEGE EVENT",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(top = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Create Event Dialog
    if (showCreateDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Create New Event") },
            text = {
                Column {
                    OutlinedTextField(
                        value = graduationYear,
                        onValueChange = { graduationYear = it },
                        label = { Text("Graduation Year (e.g., 2015)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = discussionTitle,
                        onValueChange = { discussionTitle = it },
                        label = { Text("Discussion Title (optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Select Event Color:")
                    Spacer(modifier = Modifier.height(8.dp))
                    CustomColorPickerRows(
                        primaryColors = primaryColors,
                        additionalShades = additionalShades,
                        selectedColor = selectedColor,
                        onColorSelected = { selectedColor = it }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            val allTags = flarumTagRepository.getTags()
                            val parentId = allTags?.firstOrNull { it.slug == "alumni-reunions" }?.id
                            if (parentId == null) {
                                Toast.makeText(context, "Alumni parent tag missing. Create it first.", Toast.LENGTH_SHORT).show()
                                return@launch
                            }
                            
                            val finalTitle = if (graduationYear.isNotBlank()) {
                                "Alumni Reunion $graduationYear"
                            } else if (discussionTitle.isNotBlank()) {
                                discussionTitle
                            } else {
                                ""
                            }
                            
                            if (finalTitle.isBlank()) {
                                Toast.makeText(context, "Please enter a graduation year or a discussion title.", Toast.LENGTH_SHORT).show()
                                return@launch
                            }
                            
                            val newTag = flarumTagRepository.createTag(
                                name = finalTitle,
                                color = selectedColor.toHexString(),
                                parentTagId = parentId
                            )
                            
                            if (newTag != null) {
                                Toast.makeText(context, "Event '${newTag.name}' created", Toast.LENGTH_SHORT).show()
                                val updatedAllTags = flarumTagRepository.getTags()
                                if (updatedAllTags != null) {
                                    val alumniEvents = updatedAllTags.filter { it.parentId == parentId }
                                    val eventsParentId = updatedAllTags.firstOrNull { it.slug == "events" }?.id
                                    val adminEvents = if (eventsParentId != null) {
                                        updatedAllTags.filter { tag ->
                                            tag.parentId == eventsParentId && 
                                            (tag.name.contains("[ALUMNI]") || tag.name.contains("[ALL]"))
                                        }
                                    } else {
                                        emptyList()
                                    }
                                    tagList = alumniEvents + adminEvents
                                }
                                graduationYear = ""
                                discussionTitle = ""
                                showCreateDialog = false
                            } else {
                                Toast.makeText(context, "Failed to create event", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF005B4F)
                    )
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                Button(
                    onClick = { showCreateDialog = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Gray
                    )
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun StudentEventsScreen(email: String, navController: NavController) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val flarumTagRepository = remember { FlarumTagRepository() }

    var eventName by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(Color(0xFF005B4F)) }
    var tagList by remember { mutableStateOf<List<Tag>>(emptyList()) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var selectedYear by remember { mutableStateOf("FE") }
    var expanded by remember { mutableStateOf(false) }
    val yearOptions = listOf("FE", "SE", "TE", "BE")

    val primaryColors = listOf(
        Color(0xFF005B4F), // College theme color
        Color(0xFF2E7D32), // Forest Green
        Color(0xFF1565C0), // Royal Blue
        Color(0xFF6A1B9A), // Deep Purple
        Color(0xFFC62828), // Deep Red
        Color(0xFF8D6E63)  // Brown
    )
    val additionalShades = listOf(
        Color(0xFF78909C), // Blue Gray
        Color(0xFF546E7A), // Steel Blue
        Color(0xFF37474F), // Dark Slate
        Color(0xFF004D40), // Teal
        Color(0xFF006064), // Cyan
        Color(0xFF01579B)  // Light Blue
    )

    // Load both student-created events and admin events marked for students
    LaunchedEffect(Unit) {
        val allTags = flarumTagRepository.getTags() ?: return@LaunchedEffect
        
        // Get student-created events from student-events category
        val studentParentId = allTags.firstOrNull { it.slug == "student-events" }?.id
        val studentEvents = if (studentParentId != null) {
            allTags.filter { it.parentId == studentParentId }
        } else {
            emptyList()
        }
        
        // Get admin events marked for students from events category
        val eventsParentId = allTags.firstOrNull { it.slug == "events" }?.id
        val adminEvents = if (eventsParentId != null) {
            allTags.filter { tag ->
                tag.parentId == eventsParentId && 
                (tag.name.contains("[STUDENTS]") || tag.name.contains("[ALL]"))
            }
        } else {
            emptyList()
        }
        
        // Combine both lists
        tagList = studentEvents + adminEvents
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5)) // Fallback background color
    ) {
        // Background Image
        Image(
            painter = painterResource(id = R.drawable.student_events_bg),
            contentDescription = "Student Events Background",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            colorFilter = ColorFilter.tint(
                color = Color.Black.copy(alpha = 0.3f),
                blendMode = BlendMode.Darken
            )
        )
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header with gradient background
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    )
                    .padding(16.dp)
            ) {
                Column {
                    Text(
                        text = "Student Events",
                        style = MaterialTheme.typography.headlineLarge,
                        color = Color.White,
                        modifier = Modifier.shadow(4.dp, shape = RoundedCornerShape(4.dp))
                    )
                    Text(
                        text = "Discover and organize campus activities",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Create New Event button
            Button(
                onClick = { showCreateDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF005B4F)
                )
            ) {
                Text("Create New Event")
            }

            // List of events
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                items(tagList) { tag ->
                    val colorStr = if (tag.color.isNullOrEmpty()) "#005B4F" else tag.color
                    val backgroundColor = try {
                        Color(AndroidColor.parseColor(colorStr))
                    } catch (e: Exception) {
                        Color(0xFF005B4F)
                    }
                    
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable {
                                val tagUrl = "http://129.154.249.30:8080/t/${tag.slug}"
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(tagUrl))
                                context.startActivity(intent)
                            }
                    ) {
                        Box(
                            modifier = Modifier
                                .background(backgroundColor)
                                .padding(16.dp)
                        ) {
                            Column {
                                Text(
                                    text = tag.name,
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                
                                if (tag.name.contains("[OFFICIAL]")) {
                                    Text(
                                        text = "OFFICIAL COLLEGE EVENT",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(top = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Create Event Dialog
    if (showCreateDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Create New Event") },
            text = {
                Column {
                    // Year selection dropdown
                    Text("Select Your Year:")
                    Spacer(modifier = Modifier.height(8.dp))
                    Box {
                        Button(
                            onClick = { expanded = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF005B4F)
                            )
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(selectedYear)
                                Icon(
                                    if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                    contentDescription = null
                                )
                            }
                        }
                        
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            yearOptions.forEach { year ->
                                DropdownMenuItem(
                                    text = { Text(year) },
                                    onClick = {
                                        selectedYear = year
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    OutlinedTextField(
                        value = eventName,
                        onValueChange = { eventName = it },
                        label = { Text("Event Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text("Select Event Color:")
                    Spacer(modifier = Modifier.height(8.dp))
                    CustomColorPickerRows(
                        primaryColors = primaryColors,
                        additionalShades = additionalShades,
                        selectedColor = selectedColor,
                        onColorSelected = { selectedColor = it }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            val allTags = flarumTagRepository.getTags()
                            val parentId = allTags?.firstOrNull { it.slug == "student-events" }?.id
                            if (parentId == null) {
                                Toast.makeText(context, "Student parent tag missing. Create it first.", Toast.LENGTH_SHORT).show()
                                return@launch
                            }

                            if (eventName.isNotBlank()) {
                                val finalTitle = "$selectedYear - $eventName"
                                val newTag = flarumTagRepository.createTag(
                                    name = finalTitle,
                                    color = selectedColor.toHexString(),
                                    parentTagId = parentId
                                )

                                if (newTag != null) {
                                    Toast.makeText(context, "Event '${newTag.name}' created", Toast.LENGTH_SHORT).show()
                                    val updatedAllTags = flarumTagRepository.getTags()
                                    if (updatedAllTags != null) {
                                        val studentEvents = updatedAllTags.filter { it.parentId == parentId }
                                        val eventsParentId = updatedAllTags.firstOrNull { it.slug == "events" }?.id
                                        val adminEvents = if (eventsParentId != null) {
                                            updatedAllTags.filter { tag ->
                                                tag.parentId == eventsParentId && 
                                                (tag.name.contains("[STUDENTS]") || tag.name.contains("[ALL]"))
                                            }
                                        } else {
                                            emptyList()
                                        }
                                        tagList = studentEvents + adminEvents
                                    }
                                    eventName = ""
                                    showCreateDialog = false
                                } else {
                                    Toast.makeText(context, "Failed to create event", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                Toast.makeText(context, "Please enter an event name.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF005B4F)
                    )
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                Button(
                    onClick = { showCreateDialog = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Gray
                    )
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun CollegeAdminEventsScreen(email: String, navController: NavController) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val flarumTagRepository = remember { FlarumTagRepository() }
    
    var collegeEventsList by remember { mutableStateOf<List<Tag>>(emptyList()) }
    var alumniEventsList by remember { mutableStateOf<List<Tag>>(emptyList()) }
    var studentEventsList by remember { mutableStateOf<List<Tag>>(emptyList()) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var newEventTitle by remember { mutableStateOf("") }
    var newEventDate by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(Color(0xFF005B4F)) }
    var isOfficialEvent by remember { mutableStateOf(true) }
    var isForStudents by remember { mutableStateOf(false) }
    var isForAlumni by remember { mutableStateOf(false) }
    
    val primaryColors = listOf(
        Color(0xFF005B4F), // College theme color
        Color(0xFF2E7D32), // Forest Green
        Color(0xFF1565C0), // Royal Blue
        Color(0xFF6A1B9A), // Deep Purple
        Color(0xFFC62828), // Deep Red
        Color(0xFF8D6E63), // Brown
        Color(0xFF455A64), // Slate Blue
        Color(0xFF0277BD), // Ocean Blue
        Color(0xFF2E7D32), // Emerald Green
        Color(0xFF4527A0)  // Indigo
    )
    
    val additionalShades = listOf(
        Color(0xFF78909C), // Blue Gray
        Color(0xFF546E7A), // Steel Blue
        Color(0xFF37474F), // Dark Slate
        Color(0xFF004D40), // Teal
        Color(0xFF006064), // Cyan
        Color(0xFF01579B), // Light Blue
        Color(0xFF311B92), // Deep Indigo
        Color(0xFF4A148C), // Purple
        Color(0xFF880E4F), // Deep Pink
        Color(0xFFBF360C)  // Deep Orange
    )

    // Fetch all types of events when screen loads
    LaunchedEffect(Unit) {
        val allTags = flarumTagRepository.getTags() ?: return@LaunchedEffect
        
        // Get college events
        val eventsParentId = allTags.firstOrNull { it.slug == "events" }?.id
        if (eventsParentId != null) {
            collegeEventsList = allTags.filter { it.parentId == eventsParentId }
        }
        
        // Get alumni events
        val alumniParentId = allTags.firstOrNull { it.slug == "alumni-reunions" }?.id
        if (alumniParentId != null) {
            alumniEventsList = allTags.filter { it.parentId == alumniParentId }
        }
        
        // Get ONLY student-created events
        val studentParentId = allTags.firstOrNull { it.slug == "student-events" }?.id
        studentEventsList = if (studentParentId != null) {
            allTags.filter { it.parentId == studentParentId }
        } else {
            emptyList()
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        // Background Image
        Image(
            painter = painterResource(id = R.drawable.student_events_bg),
            contentDescription = "Admin Events Background",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            colorFilter = ColorFilter.tint(
                color = Color.Black.copy(alpha = 0.3f),
                blendMode = BlendMode.Darken
            )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header with gradient background
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    )
                    .padding(16.dp)
            ) {
                Column {
                    Text(
                        text = "Events & Reunions Management",
                        style = MaterialTheme.typography.headlineLarge,
                        color = Color.White,
                        modifier = Modifier.shadow(4.dp, shape = RoundedCornerShape(4.dp))
                    )
                    Text(
                        text = "Organize, promote, and manage campus events and alumni reunions",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Create New Event button
            Button(
                onClick = { showCreateDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF005B4F)
                )
            ) {
                Text("Create New Official Event")
            }

            // Main content with three sections
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                // College events section
                Card(
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(vertical = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "College Official Events",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        
                        if (collegeEventsList.isEmpty()) {
                            Text(
                                text = "No official events found. Create your first event!",
                                modifier = Modifier.padding(vertical = 16.dp)
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(collegeEventsList) { event ->
                                    val colorStr = if (event.color.isNullOrEmpty()) "#005B4F" else event.color
                                    val backgroundColor = try {
                                        Color(AndroidColor.parseColor(colorStr))
                                    } catch (e: Exception) {
                                        Color(0xFF005B4F)
                                    }
                                    
                                    Card(
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                            .clickable {
                                                val tagUrl = "http://129.154.249.30:8080/t/${event.slug}"
                                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(tagUrl))
                                                context.startActivity(intent)
                                            }
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .background(backgroundColor)
                                                .padding(16.dp)
                                        ) {
                                            Column {
                                                Text(
                                                    text = event.name,
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 18.sp
                                                )
                                                
                                                val dateRegex = "\\(([^)]+)\\)".toRegex()
                                                val matchResult = dateRegex.find(event.name)
                                                if (matchResult != null) {
                                                    Text(
                                                        text = matchResult.groupValues[1],
                                                        color = Color.White,
                                                        fontSize = 14.sp,
                                                        modifier = Modifier.padding(top = 8.dp)
                                                    )
                                                }
                                                
                                                if (event.name.contains("[OFFICIAL]")) {
                                                    Text(
                                                        text = "OFFICIAL COLLEGE EVENT",
                                                        color = Color.White,
                                                        fontSize = 12.sp,
                                                        modifier = Modifier.padding(top = 8.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                
                // Alumni events section
                Card(
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(vertical = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Alumni Reunion Events",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        
                        if (alumniEventsList.isEmpty()) {
                            Text(
                                text = "No alumni events found.",
                                modifier = Modifier.padding(vertical = 16.dp)
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(alumniEventsList) { event ->
                                    val colorStr = if (event.color.isNullOrEmpty()) "#1565C0" else event.color
                                    val backgroundColor = try {
                                        Color(AndroidColor.parseColor(colorStr))
                                    } catch (e: Exception) {
                                        Color(0xFF1565C0)
                                    }
                                    
                                    Card(
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                            .clickable {
                                                val tagUrl = "http://129.154.249.30:8080/t/${event.slug}"
                                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(tagUrl))
                                                context.startActivity(intent)
                                            }
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .background(backgroundColor)
                                                .padding(16.dp)
                                        ) {
                                            Column {
                                                Text(
                                                    text = event.name,
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 18.sp
                                                )
                                                
                                                Text(
                                                    text = "Alumni Event",
                                                    color = Color.White,
                                                    fontSize = 12.sp,
                                                    modifier = Modifier.padding(top = 8.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                
                // Student events section
                Card(
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(vertical = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Student Events",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        
                        if (studentEventsList.isEmpty()) {
                            Text(
                                text = "No student events found.",
                                modifier = Modifier.padding(vertical = 16.dp)
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(studentEventsList) { event ->
                                    val colorStr = if (event.color.isNullOrEmpty()) "#2E7D32" else event.color
                                    val backgroundColor = try {
                                        Color(AndroidColor.parseColor(colorStr))
                                    } catch (e: Exception) {
                                        Color(0xFF2E7D32)
                                    }
                                    
                                    Card(
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                            .clickable {
                                                val tagUrl = "http://129.154.249.30:8080/t/${event.slug}"
                                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(tagUrl))
                                                context.startActivity(intent)
                                            }
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .background(backgroundColor)
                                                .padding(16.dp)
                                        ) {
                                            Column {
                                                Text(
                                                    text = event.name,
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 18.sp
                                                )
                                                
                                                // Show the year if it's in the event name
                                                if (event.name.contains("FE") || event.name.contains("SE") || 
                                                    event.name.contains("TE") || event.name.contains("BE")) {
                                                    Text(
                                                        text = event.name.substringBefore(" - "),
                                                        color = Color.White,
                                                        fontSize = 14.sp,
                                                        modifier = Modifier.padding(top = 4.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            // Add some bottom padding
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
    
    // Create Event Dialog
    if (showCreateDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { 
                showCreateDialog = false
                isForStudents = false
                isForAlumni = false
            },
            title = { Text("Create New Event") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newEventTitle,
                        onValueChange = { newEventTitle = it },
                        label = { Text("Event Title") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    )
                    
                    OutlinedTextField(
                        value = newEventDate,
                        onValueChange = { newEventDate = it },
                        label = { Text("Event Date (e.g., May 15, 2023)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text("Select Event Theme Color:")
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Primary Colors
                    Text("Primary Colors", fontSize = 14.sp)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        items(primaryColors) { color ->
                            CustomColorSwatch(
                                color = color,
                                selected = color == selectedColor,
                                onClick = { selectedColor = color }
                            )
                        }
                    }
                    
                    // Additional Shades
                    Text("Additional Shades", fontSize = 14.sp)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        items(additionalShades) { color ->
                            CustomColorSwatch(
                                color = color,
                                selected = color == selectedColor,
                                onClick = { selectedColor = color }
                            )
                        }
                    }
                    
                    // Target audience selection
                    Text(
                        text = "Target Audience:",
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Checkbox(
                            checked = isForStudents,
                            onCheckedChange = { isForStudents = it }
                        )
                        Text(
                            text = "Students",
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Checkbox(
                            checked = isForAlumni,
                            onCheckedChange = { isForAlumni = it }
                        )
                        Text(
                            text = "Alumni",
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                    
                    // Checkbox for official event
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        Checkbox(
                            checked = isOfficialEvent,
                            onCheckedChange = { isOfficialEvent = it }
                        )
                        Text(
                            text = "Mark as Official College Event",
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            if (newEventTitle.isNotBlank()) {
                                // Create audience tag
                                val audienceTag = when {
                                    isForStudents && isForAlumni -> "[ALL]"
                                    isForStudents -> "[STUDENTS]"
                                    isForAlumni -> "[ALUMNI]"
                                    else -> "[ALL]" // Default to all if none selected
                                }
                                
                                val fullEventTitle = if (newEventDate.isNotEmpty()) {
                                    val prefix = if (isOfficialEvent) "[OFFICIAL] " else ""
                                    "$prefix$audienceTag $newEventTitle ($newEventDate)"
                                } else {
                                    val prefix = if (isOfficialEvent) "[OFFICIAL] " else ""
                                    "$prefix$audienceTag $newEventTitle"
                                }
                                
                                // Get the parent ID for the events category
                                val allTags = flarumTagRepository.getTags() ?: return@launch
                                val eventsParentId = allTags.firstOrNull { it.slug == "events" }?.id
                                
                                if (eventsParentId != null) {
                                    val newEvent = flarumTagRepository.createTag(
                                        fullEventTitle,
                                        selectedColor.toHexString(),
                                        eventsParentId
                                    )
                                    
                                    if (newEvent != null) {
                                        Toast.makeText(
                                            context,
                                            "Event '${newEvent.name}' created successfully",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        
                                        // Refresh events list
                                        flarumTagRepository.getTags()?.let { updatedTags ->
                                            collegeEventsList = updatedTags.filter { it.parentId == eventsParentId }
                                        }
                                        
                                        // Reset fields and close dialog
                                        newEventTitle = ""
                                        newEventDate = ""
                                        isForStudents = false
                                        isForAlumni = false
                                        showCreateDialog = false
                                    } else {
                                        Toast.makeText(
                                            context,
                                            "Failed to create event",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                } else {
                                    Toast.makeText(
                                        context,
                                        "Events category not found",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            } else {
                                Toast.makeText(
                                    context,
                                    "Event title cannot be empty",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF005B4F)
                    )
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                Button(
                    onClick = { 
                        showCreateDialog = false
                        isForStudents = false
                        isForAlumni = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Gray
                    )
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun EventCard(
    title: String,
    date: String,
    backgroundColor: Color,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .width(220.dp)
            .height(160.dp)
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(12.dp),
                spotColor = backgroundColor.copy(alpha = 0.5f)
            )
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = title,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = date,
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 14.sp
                    )
                }
                
                // Add a subtle divider and status indicator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "View Details",
                            color = Color.White,
                            fontSize = 12.sp
                        )
                    }
                    
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(Color.White.copy(alpha = 0.6f), CircleShape)
                    )
                }
            }
        }
    }
}


