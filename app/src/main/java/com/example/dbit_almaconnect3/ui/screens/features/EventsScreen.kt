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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import kotlinx.coroutines.launch
import androidx.compose.foundation.verticalScroll

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
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .background(backgroundColor)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = tag.name,
                fontSize = 18.sp,
                color = Color.White
            )
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
    var selectedColor by remember { mutableStateOf(Color(0xFFFFFFFF)) }
    var tagList by remember { mutableStateOf<List<Tag>>(emptyList()) }

    val primaryColors = listOf(
        Color(0xFFFF0000), // Red
        Color(0xFFFFA500), // Orange
        Color(0xFFFFFF00), // Yellow
        Color(0xFF008000), // Green
        Color(0xFF0000FF), // Blue
        Color(0xFF800080)  // Purple
    )
    val additionalShades = listOf(
        Color(0xFFFFC0CB), // Pink
        Color(0xFF808080), // Gray
        Color(0xFF000000), // Black
        Color(0xFFFFFFFF), // White
        Color(0xFFB22222), // Firebrick
        Color(0xFF8B4513), // SaddleBrown
        Color(0xFF2E8B57), // SeaGreen
        Color(0xFF4682B4), // SteelBlue
        Color(0xFFDAA520)  // Goldenrod
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

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // Title at the top
        Text(text = "Alumni Events & Reunions", fontSize = 24.sp)
        Spacer(modifier = Modifier.height(16.dp))

        // List of discussions fills the available space
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            items(tagList) { tag ->
                TagCard(tag = tag) {
                    val tagUrl = "http://129.154.249.30:8080/t/${tag.slug}"
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(tagUrl))
                    context.startActivity(intent)
                }
            }
        }

        // The creation UI is pinned at the bottom
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(text = "Create New Alumni Event", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            
            OutlinedTextField(
                value = graduationYear,
                onValueChange = { graduationYear = it },
                label = { Text("Enter Graduation Year (e.g., 2015)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = discussionTitle,
                onValueChange = { discussionTitle = it },
                label = { Text("Discussion Title (optional)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Select Tag Color:", fontSize = 16.sp)
            Spacer(modifier = Modifier.height(8.dp))
            CustomColorPickerRows(
                primaryColors = primaryColors,
                additionalShades = additionalShades,
                selectedColor = selectedColor,
                onColorSelected = { selectedColor = it }
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    coroutineScope.launch {
                        val allTags = flarumTagRepository.getTags()
                        val parentId = allTags?.firstOrNull { it.slug == "alumni-reunions" }?.id
                        if (parentId == null) {
                            Toast.makeText(context, "Alumni parent tag missing. Create it first.", Toast.LENGTH_SHORT).show()
                            return@launch
                        }
                        // If graduationYear is provided, use it to create the title with "Alumni Reunion" prefixed.
                        // Otherwise, use the discussionTitle.
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
                            // Refresh list
                            val updatedAllTags = flarumTagRepository.getTags()
                            if (updatedAllTags != null) {
                                // Get updated alumni events
                                val alumniEvents = updatedAllTags.filter { it.parentId == parentId }
                                
                                // Get admin events marked for alumni
                                val eventsParentId = updatedAllTags.firstOrNull { it.slug == "events" }?.id
                                val adminEvents = if (eventsParentId != null) {
                                    updatedAllTags.filter { tag ->
                                        tag.parentId == eventsParentId && 
                                        (tag.name.contains("[ALUMNI]") || tag.name.contains("[ALL]"))
                                    }
                                } else {
                                    emptyList()
                                }
                                
                                // Update the combined list
                                tagList = alumniEvents + adminEvents
                            }
                            graduationYear = ""
                            discussionTitle = ""
                        } else {
                            Toast.makeText(context, "Failed to create event", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Create Event")
            }
        }
    }
}

@Composable
fun StudentEventsScreen(email: String, navController: NavController) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val flarumTagRepository = remember { FlarumTagRepository() }

    // Dropdown state variables
    val yearOptions = listOf("FE", "SE", "TE", "BE")
    var expanded by remember { mutableStateOf(false) }
    var selectedYear by remember { mutableStateOf(yearOptions.first()) }

    // Event name input field
    var eventName by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(Color(0xFFFFFFFF)) }
    var tagList by remember { mutableStateOf<List<Tag>>(emptyList()) }

    val primaryColors = listOf(
        Color(0xFFFF0000),
        Color(0xFFFFA500),
        Color(0xFFFFFF00),
        Color(0xFF008000),
        Color(0xFF0000FF),
        Color(0xFF800080)
    )
    val additionalShades = listOf(
        Color(0xFFFFC0CB),
        Color(0xFF808080),
        Color(0xFF000000),
        Color(0xFFFFFFFF),
        Color(0xFFB22222),
        Color(0xFF8B4513),
        Color(0xFF2E8B57),
        Color(0xFF4682B4),
        Color(0xFFDAA520)
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

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // Screen Title
        Text(text = "Student Events", fontSize = 24.sp)
        Spacer(modifier = Modifier.height(16.dp))

        // List of discussions
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            items(tagList) { tag ->
                TagCard(tag = tag) {
                    val tagUrl = "http://129.154.249.30:8080/t/${tag.slug}"
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(tagUrl))
                    context.startActivity(intent)
                }
            }
        }

        // Creation UI (pinned at the bottom)
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(text = "Create New Student Event", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            // Year selection dropdown
            Text(text = "Select Your Year:", fontSize = 16.sp)
            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = { expanded = !expanded }
                ) {
                    Text(text = selectedYear)
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
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

            Spacer(modifier = Modifier.height(8.dp))
            
            // Event name input
            OutlinedTextField(
                value = eventName,
                onValueChange = { eventName = it },
                label = { Text("Event Name") },
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(8.dp))

            Text(text = "Select Tag Color:", fontSize = 16.sp)
            Spacer(modifier = Modifier.height(8.dp))
            CustomColorPickerRows(
                primaryColors = primaryColors,
                additionalShades = additionalShades,
                selectedColor = selectedColor,
                onColorSelected = { selectedColor = it }
            )

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    coroutineScope.launch {
                        val allTags = flarumTagRepository.getTags()
                        val parentId = allTags?.firstOrNull { it.slug == "student-events" }?.id
                        if (parentId == null) {
                            Toast.makeText(context, "Student parent tag missing. Create it first.", Toast.LENGTH_SHORT).show()
                            return@launch
                        }

                        // Generate final title as "FE - [Event Name]"
                        val finalTitle = if (eventName.isNotBlank()) {
                            "$selectedYear - $eventName"
                        } else {
                            Toast.makeText(context, "Please enter an event name.", Toast.LENGTH_SHORT).show()
                            return@launch
                        }

                        val newTag = flarumTagRepository.createTag(
                            name = finalTitle,
                            color = selectedColor.toHexString(),
                            parentTagId = parentId
                        )

                        if (newTag != null) {
                            Toast.makeText(context, "Event '${newTag.name}' created", Toast.LENGTH_SHORT).show()
                            // Refresh list
                            val updatedAllTags = flarumTagRepository.getTags()
                            if (updatedAllTags != null) {
                                // Get updated student events
                                val studentEvents = updatedAllTags.filter { it.parentId == parentId }
                                
                                // Get admin events marked for students
                                val eventsParentId = updatedAllTags.firstOrNull { it.slug == "events" }?.id
                                val adminEvents = if (eventsParentId != null) {
                                    updatedAllTags.filter { tag ->
                                        tag.parentId == eventsParentId && 
                                        (tag.name.contains("[STUDENTS]") || tag.name.contains("[ALL]"))
                                    }
                                } else {
                                    emptyList()
                                }
                                
                                // Update the combined list
                                tagList = studentEvents + adminEvents
                            }
                            eventName = ""
                        } else {
                            Toast.makeText(context, "Failed to create event", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Create Event")
            }
        }
    }
}

@Composable
fun CollegeAdminEventsScreen(email: String, navController: NavController) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val flarumTagRepository = remember { FlarumTagRepository() }
    
    // State for managing different event types
    var collegeEventsList by remember { mutableStateOf<List<Tag>>(emptyList()) }
    var alumniEventsList by remember { mutableStateOf<List<Tag>>(emptyList()) }
    var studentEventsList by remember { mutableStateOf<List<Tag>>(emptyList()) }
    
    // State for creating a new event
    var showCreateDialog by remember { mutableStateOf(false) }
    var newEventTitle by remember { mutableStateOf("") }
    var newEventDate by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(Color(0xFF005B4F)) }
    var isOfficialEvent by remember { mutableStateOf(true) }
    var isForStudents by remember { mutableStateOf(false) }
    var isForAlumni by remember { mutableStateOf(false) }
    
    // Define event theme colors
    val eventColors = listOf(
        Color(0xFF005B4F), // College theme color
        Color(0xFF2E7D32), // Green
        Color(0xFF1565C0), // Blue
        Color(0xFF6A1B9A), // Purple
        Color(0xFFC62828), // Red
        Color(0xFFFF8F00)  // Orange
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
        
        // Get student events
        val studentParentId = allTags.firstOrNull { it.slug == "student-events" }?.id
        if (studentParentId != null) {
            studentEventsList = allTags.filter { it.parentId == studentParentId }
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Events & Reunions Management",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        Text(
            text = "Organize, promote, and manage campus events and alumni reunions",
            fontSize = 16.sp,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        
        // Create New Event button
        Button(
            onClick = { showCreateDialog = true },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Text("Create New Official Event")
        }
        
        // College events section
        Card(
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                    ) {
                        items(collegeEventsList) { event ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                val colorStr = if (event.color.isNullOrEmpty()) "#005B4F" else event.color
                                val backgroundColor = try {
                                    Color(android.graphics.Color.parseColor(colorStr))
                                } catch (e: Exception) {
                                    Color(0xFF005B4F)
                                }
                                
                                Card(
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            val tagUrl = "http://129.154.249.30:8080/t/${event.slug}"
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(tagUrl))
                                            context.startActivity(intent)
                                        }
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .background(backgroundColor)
                                            .padding(16.dp)
                                    ) {
                                        Text(
                                            text = event.name,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 18.sp
                                        )
                                        
                                        // If the event name contains a date in parentheses, extract and show it
                                        val dateRegex = "\\(([^)]+)\\)".toRegex()
                                        val matchResult = dateRegex.find(event.name)
                                        if (matchResult != null) {
                                            Text(
                                                text = matchResult.groupValues[1],
                                                color = Color.White,
                                                fontSize = 14.sp
                                            )
                                        }
                                        
                                        // Show "OFFICIAL" label if it's an official college event
                                        if (event.name.startsWith("[OFFICIAL]")) {
                                            Text(
                                                text = "OFFICIAL COLLEGE EVENT",
                                                color = Color.Yellow,
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                    ) {
                        items(alumniEventsList) { event ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                val colorStr = if (event.color.isNullOrEmpty()) "#1565C0" else event.color
                                val backgroundColor = try {
                                    Color(android.graphics.Color.parseColor(colorStr))
                                } catch (e: Exception) {
                                    Color(0xFF1565C0)
                                }
                                
                                Card(
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            val tagUrl = "http://129.154.249.30:8080/t/${event.slug}"
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(tagUrl))
                                            context.startActivity(intent)
                                        }
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .background(backgroundColor)
                                            .padding(16.dp)
                                    ) {
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                    ) {
                        items(studentEventsList) { event ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                val colorStr = if (event.color.isNullOrEmpty()) "#2E7D32" else event.color
                                val backgroundColor = try {
                                    Color(android.graphics.Color.parseColor(colorStr))
                                } catch (e: Exception) {
                                    Color(0xFF2E7D32)
                                }
                                
                                Card(
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            val tagUrl = "http://129.154.249.30:8080/t/${event.slug}"
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(tagUrl))
                                            context.startActivity(intent)
                                        }
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .background(backgroundColor)
                                            .padding(16.dp)
                                    ) {
                                        Text(
                                            text = event.name,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 18.sp
                                        )
                                        
                                        Text(
                                            text = "Student Event",
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
    
    // Dialog to create a new event
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
                    
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        items(eventColors) { color ->
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
                    }
                ) {
                    Text("Create Event")
                }
            },
            dismissButton = {
                Button(onClick = { 
                    showCreateDialog = false
                    isForStudents = false
                    isForAlumni = false
                }) {
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
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .width(180.dp)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .background(backgroundColor)
                .padding(12.dp)
        ) {
            Text(
                text = title,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            
            Text(
                text = date,
                color = Color.White,
                fontSize = 14.sp
            )
        }
    }
}


