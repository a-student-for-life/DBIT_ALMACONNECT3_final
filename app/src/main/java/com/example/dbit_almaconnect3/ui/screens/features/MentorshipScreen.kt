package com.example.dbit_almaconnect3.ui.screens.features

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
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
import kotlinx.coroutines.launch
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

// Helper extension to convert Color to hex.
fun Color.toHex(): String {
    return String.format("#%06X", 0xFFFFFF and this.toArgb())
}

// Display a tag as a rounded colored card.
@Composable
fun TagItem(tag: Tag, onClick: () -> Unit) {
    val backgroundColor = try {
        Color(android.graphics.Color.parseColor(tag.color))
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

// Improved color picker with two horizontal rows.
@Composable
fun ColorPickerRows(
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
                ColorSwatch(color = color, selected = (color == selectedColor), onClick = { onColorSelected(color) })
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Additional Shades", fontSize = 14.sp)
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(additionalShades) { color ->
                ColorSwatch(color = color, selected = (color == selectedColor), onClick = { onColorSelected(color) })
            }
        }
    }
}

// A single circular color swatch.
@Composable
fun ColorSwatch(
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

@Composable
fun MentorshipScreen(email: String, navController: NavController) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val flarumTagRepository = remember { FlarumTagRepository() }

    var tagName by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(Color(0xFFFFFFFF)) }
    var tagList by remember { mutableStateOf<List<Tag>>(emptyList()) }

    // Define primary and additional colors.
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

    // Fetch all tags when the screen loads.
    LaunchedEffect(Unit) {
        flarumTagRepository.getTags()?.let { allTags ->
            // Filter to only show tags under the primary mentorship tag.
            val primaryId = flarumTagRepository.getPrimaryMentorshipTagId()
            tagList = if (primaryId != null) {
                allTags.filter { it.parentId == primaryId }
            } else {
                emptyList()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(text = "Mentorship for $email", fontSize = 24.sp)
        Spacer(modifier = Modifier.height(16.dp))

        Text(text = "Existing Tags:", fontSize = 20.sp)
        Spacer(modifier = Modifier.height(8.dp))
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            items(tagList) { tag ->
                TagItem(tag = tag) {
                    val tagUrl = "http://129.154.249.30:8080/t/${tag.slug}"
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(tagUrl))
                    context.startActivity(intent)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = tagName,
            onValueChange = { tagName = it },
            label = { Text("New Tag Name") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        Text(text = "Select Tag Color:", fontSize = 16.sp)
        Spacer(modifier = Modifier.height(8.dp))
        ColorPickerRows(
            primaryColors = primaryColors,
            additionalShades = additionalShades,
            selectedColor = selectedColor,
            onColorSelected = { selectedColor = it }
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                coroutineScope.launch {
                    val parentId = flarumTagRepository.getPrimaryMentorshipTagId()
                    val newTag = flarumTagRepository.createTag(tagName, selectedColor.toHex(), parentId)
                    if (newTag != null) {
                        Toast.makeText(context, "Tag '${newTag.name}' created", Toast.LENGTH_SHORT).show()
                        // Refresh the tag list.
                        flarumTagRepository.getTags()?.let { allTags ->
                            val primaryId = flarumTagRepository.getPrimaryMentorshipTagId()
                            tagList = if (primaryId != null) {
                                allTags.filter { it.parentId == primaryId }
                            } else {
                                emptyList()
                            }
                        }
                        tagName = ""
                    } else {
                        Toast.makeText(context, "Failed to create tag", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Create Tag")
        }
    }
}

// Wrappers to retain existing file names.
@Composable
fun StudentMentorshipScreen(email: String, navController: NavController) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val flarumTagRepository = remember { FlarumTagRepository() }

    var tagName by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(Color(0xFFFFFFFF)) }
    var tagList by remember { mutableStateOf<List<Tag>>(emptyList()) }

    // Define primary and additional colors.
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

    // Fetch all tags when the screen loads.
    LaunchedEffect(Unit) {
        val allTags = flarumTagRepository.getTags() ?: return@LaunchedEffect
        val primaryId = flarumTagRepository.getPrimaryMentorshipTagId()
        if (primaryId != null) {
            // Get student-created programs
            val studentPrograms = allTags.filter { 
                it.parentId == primaryId && 
                !it.name.startsWith("[OFFICIAL]") 
            }
            
            // Get all official programs
            val officialPrograms = allTags.filter { tag ->
                tag.parentId == primaryId && 
                tag.name.startsWith("[OFFICIAL]")
            }
            
            // Combine both lists
            tagList = officialPrograms + studentPrograms
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(text = "Student Mentorship Programs", fontSize = 24.sp)
        Spacer(modifier = Modifier.height(16.dp))

        Text(text = "Available Programs:", fontSize = 20.sp)
        Spacer(modifier = Modifier.height(8.dp))
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            items(tagList) { tag ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    TagItem(tag = tag) {
                        val tagUrl = "http://129.154.249.30:8080/t/${tag.slug}"
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(tagUrl))
                        context.startActivity(intent)
                    }
                    
                    // Show "OFFICIAL" label if it's an official program
                    if (tag.name.startsWith("[OFFICIAL]")) {
                        Text(
                            text = "OFFICIAL COLLEGE PROGRAM",
                            color = Color.Blue,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 8.dp, top = 2.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(text = "Create New Program:", fontSize = 20.sp)
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = tagName,
            onValueChange = { tagName = it },
            label = { Text("Program Topic/Name") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        Text(text = "Select Program Color:", fontSize = 16.sp)
        Spacer(modifier = Modifier.height(8.dp))
        ColorPickerRows(
            primaryColors = primaryColors,
            additionalShades = additionalShades,
            selectedColor = selectedColor,
            onColorSelected = { selectedColor = it }
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                coroutineScope.launch {
                    val parentId = flarumTagRepository.getPrimaryMentorshipTagId()
                    val newTag = flarumTagRepository.createTag(tagName, selectedColor.toHexString(), parentId)
                    if (newTag != null) {
                        Toast.makeText(context, "Program '${newTag.name}' created", Toast.LENGTH_SHORT).show()
                        // Refresh the tag list
                        val updatedTags = flarumTagRepository.getTags()
                        if (updatedTags != null && parentId != null) {
                            // Get student-created programs
                            val studentPrograms = updatedTags.filter { 
                                it.parentId == parentId && 
                                !it.name.startsWith("[OFFICIAL]") 
                            }
                            
                            // Get official programs marked for students
                            val officialPrograms = updatedTags.filter { tag ->
                                tag.parentId == parentId && 
                                tag.name.startsWith("[OFFICIAL]") &&
                                (tag.name.contains("[STUDENTS]") || tag.name.contains("[ALL]"))
                            }
                            
                            // Update the combined list
                            tagList = officialPrograms + studentPrograms
                        }
                        tagName = ""
                    } else {
                        Toast.makeText(context, "Failed to create program", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Create Program")
        }
    }
}

@Composable
fun AlumniMentorshipScreen(email: String, navController: NavController) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val flarumTagRepository = remember { FlarumTagRepository() }

    var tagName by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(Color(0xFFFFFFFF)) }
    var tagList by remember { mutableStateOf<List<Tag>>(emptyList()) }

    // Define primary and additional colors.
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

    // Fetch all tags when the screen loads.
    LaunchedEffect(Unit) {
        val allTags = flarumTagRepository.getTags() ?: return@LaunchedEffect
        val primaryId = flarumTagRepository.getPrimaryMentorshipTagId()
        if (primaryId != null) {
            // Get alumni-created programs
            val alumniPrograms = allTags.filter { 
                it.parentId == primaryId && 
                !it.name.startsWith("[OFFICIAL]") 
            }
            
            // Get all official programs
            val officialPrograms = allTags.filter { tag ->
                tag.parentId == primaryId && 
                tag.name.startsWith("[OFFICIAL]")
            }
            
            // Combine both lists
            tagList = officialPrograms + alumniPrograms
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(text = "Alumni Mentorship Programs", fontSize = 24.sp)
        Spacer(modifier = Modifier.height(16.dp))

        Text(text = "Available Programs:", fontSize = 20.sp)
        Spacer(modifier = Modifier.height(8.dp))
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            items(tagList) { tag ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    TagItem(tag = tag) {
                        val tagUrl = "http://129.154.249.30:8080/t/${tag.slug}"
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(tagUrl))
                        context.startActivity(intent)
                    }
                    
                    // Show "OFFICIAL" label if it's an official program
                    if (tag.name.startsWith("[OFFICIAL]")) {
                        Text(
                            text = "OFFICIAL COLLEGE PROGRAM",
                            color = Color.Blue,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 8.dp, top = 2.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(text = "Create New Program:", fontSize = 20.sp)
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = tagName,
            onValueChange = { tagName = it },
            label = { Text("Program Topic/Name") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        Text(text = "Select Program Color:", fontSize = 16.sp)
        Spacer(modifier = Modifier.height(8.dp))
        ColorPickerRows(
            primaryColors = primaryColors,
            additionalShades = additionalShades,
            selectedColor = selectedColor,
            onColorSelected = { selectedColor = it }
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                coroutineScope.launch {
                    val parentId = flarumTagRepository.getPrimaryMentorshipTagId()
                    val newTag = flarumTagRepository.createTag(tagName, selectedColor.toHexString(), parentId)
                    if (newTag != null) {
                        Toast.makeText(context, "Program '${newTag.name}' created", Toast.LENGTH_SHORT).show()
                        // Refresh the tag list
                        val updatedTags = flarumTagRepository.getTags()
                        if (updatedTags != null && parentId != null) {
                            // Get alumni-created programs
                            val alumniPrograms = updatedTags.filter { 
                                it.parentId == parentId && 
                                !it.name.startsWith("[OFFICIAL]") 
                            }
                            
                            // Get official programs marked for alumni
                            val officialPrograms = updatedTags.filter { tag ->
                                tag.parentId == parentId && 
                                tag.name.startsWith("[OFFICIAL]") &&
                                (tag.name.contains("[ALUMNI]") || tag.name.contains("[ALL]"))
                            }
                            
                            // Update the combined list
                            tagList = officialPrograms + alumniPrograms
                        }
                        tagName = ""
                    } else {
                        Toast.makeText(context, "Failed to create program", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Create Program")
        }
    }
}

@Composable
fun CollegeAdminMentorshipScreen(email: String, navController: NavController) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val flarumTagRepository = remember { FlarumTagRepository() }
    
    // Separate lists for official and regular programs
    var officialPrograms by remember { mutableStateOf<List<Tag>>(emptyList()) }
    var regularPrograms by remember { mutableStateOf<List<Tag>>(emptyList()) }
    
    var newProgramName by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(Color(0xFF005B4F)) } // College theme color
    var showCreateDialog by remember { mutableStateOf(false) }
    
    // Define primary and additional colors for the program themes
    val primaryColors = listOf(
        Color(0xFF005B4F), // College theme color
        Color(0xFF2E7D32), // Green
        Color(0xFF1565C0), // Blue
        Color(0xFF6A1B9A), // Purple
        Color(0xFFC62828), // Red
        Color(0xFFFF8F00)  // Orange
    )
    
    // Fetch all tags when the screen loads.
    LaunchedEffect(Unit) {
        flarumTagRepository.getTags()?.let { allTags ->
            // Filter to only show tags under the primary mentorship tag.
            val primaryId = flarumTagRepository.getPrimaryMentorshipTagId()
            if (primaryId != null) {
                val allPrograms = allTags.filter { it.parentId == primaryId }
                // Separate into official and regular programs
                officialPrograms = allPrograms.filter { it.name.startsWith("[OFFICIAL]") }
                regularPrograms = allPrograms.filter { !it.name.startsWith("[OFFICIAL]") }
            }
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Mentorship Programs",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        Text(
            text = "View and manage mentorship programs at the college",
            fontSize = 16.sp,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        
        // Create New Program button
        Button(
            onClick = { showCreateDialog = true },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Text("Create New Official Program")
        }
        
        // Official Programs Card
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
                    text = "Official Mentorship Programs",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                if (officialPrograms.isEmpty()) {
                    Text(
                        text = "No official programs found. Create your first official program!",
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                    ) {
                        items(officialPrograms) { program ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                TagItem(tag = program) {
                                    val tagUrl = "http://129.154.249.30:8080/t/${program.slug}"
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(tagUrl))
                                    context.startActivity(intent)
                                }
                                
                                Text(
                                    text = "COLLEGE OFFICIAL PROGRAM",
                                    color = Color.Blue,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(start = 8.dp, top = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
        
        // Regular Programs Card
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
                    text = "Other Mentorship Programs",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                if (regularPrograms.isEmpty()) {
                    Text(
                        text = "No other mentorship programs found.",
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                    ) {
                        items(regularPrograms) { program ->
                            TagItem(tag = program) {
                                val tagUrl = "http://129.154.249.30:8080/t/${program.slug}"
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(tagUrl))
                                context.startActivity(intent)
                            }
                        }
                    }
                }
            }
        }
    }
    
    // Dialog to create a new program
    if (showCreateDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { 
                showCreateDialog = false
            },
            title = { Text("Create New Official Mentorship Program") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newProgramName,
                        onValueChange = { newProgramName = it },
                        label = { Text("Program Topic/Name") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text("Select Program Theme Color:")
                    
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        items(primaryColors) { color ->
                            ColorSwatch(
                                color = color, 
                                selected = color == selectedColor,
                                onClick = { selectedColor = color }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newProgramName.isNotEmpty()) {
                            coroutineScope.launch {
                                // Add [OFFICIAL] prefix to program name
                                val officialProgramName = "[OFFICIAL] $newProgramName"
                                val parentId = flarumTagRepository.getPrimaryMentorshipTagId()
                                val newTag = flarumTagRepository.createTag(
                                    officialProgramName, 
                                    selectedColor.toHex(), 
                                    parentId
                                )
                                
                                if (newTag != null) {
                                    Toast.makeText(
                                        context, 
                                        "Official Program '${newTag.name}' created", 
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    
                                    // Refresh the tag list
                                    flarumTagRepository.getTags()?.let { allTags ->
                                        val primaryId = flarumTagRepository.getPrimaryMentorshipTagId()
                                        if (primaryId != null) {
                                            val allPrograms = allTags.filter { it.parentId == primaryId }
                                            // Separate into official and regular programs
                                            officialPrograms = allPrograms.filter { it.name.startsWith("[OFFICIAL]") }
                                            regularPrograms = allPrograms.filter { !it.name.startsWith("[OFFICIAL]") }
                                        }
                                    }
                                    
                                    // Reset fields
                                    newProgramName = ""
                                    showCreateDialog = false
                                } else {
                                    Toast.makeText(
                                        context, 
                                        "Failed to create program", 
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        } else {
                            Toast.makeText(
                                context, 
                                "Program name cannot be empty", 
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                ) {
                    Text("Create Program")
                }
            },
            dismissButton = {
                Button(onClick = { showCreateDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
