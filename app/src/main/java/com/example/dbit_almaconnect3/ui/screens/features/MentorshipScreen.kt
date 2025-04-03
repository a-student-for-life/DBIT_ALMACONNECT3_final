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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.dbit_almaconnect3.data.repository.FlarumTagRepository
import com.example.dbit_almaconnect3.data.repository.Tag
import kotlinx.coroutines.launch
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.Badge
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.ButtonDefaults

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
fun StudentMentorshipScreen(email: String, navController: NavController) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val flarumTagRepository = remember { FlarumTagRepository() }

    var tagName by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(Color(0xFFFFFFFF)) }
    var tagList by remember { mutableStateOf<List<Tag>>(emptyList()) }
    var selectedTab by remember { mutableStateOf(0) } // 0 for All, 1 for Official, 2 for Non-Official
    var showCreateDialog by remember { mutableStateOf(false) }

    // Define primary and additional colors.
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
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Modern header with gradient background
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
                    text = "Student Mentorship Programs",
                    style = MaterialTheme.typography.headlineLarge,
                    color = Color.White,
                    modifier = Modifier.shadow(4.dp, shape = RoundedCornerShape(4.dp))
                )
                Text(
                    text = "Connect with mentors and explore programs",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White,
                    modifier = Modifier.shadow(4.dp, shape = RoundedCornerShape(4.dp))
                )
            }
        }

        // Tab layout for filtering programs
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("All Programs") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Official") }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text("Non-Official") }
            )
        }

        // Create New Program button
        Button(
            onClick = { showCreateDialog = true },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text("Create New Program")
        }

        // Filtered programs list
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            items(
                tagList.filter { tag ->
                    when (selectedTab) {
                        0 -> true // All programs
                        1 -> tag.name.startsWith("[OFFICIAL]") // Official programs
                        2 -> !tag.name.startsWith("[OFFICIAL]") // Non-official programs
                        else -> true
                    }
                }
            ) { tag ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .clickable {
                    val tagUrl = "http://129.154.249.30:8080/t/${tag.slug}"
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(tagUrl))
                    context.startActivity(intent)
                        },
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    shape = MaterialTheme.shapes.large
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                try {
                                    Color(android.graphics.Color.parseColor(tag.color))
                                } catch (e: Exception) {
                                    MaterialTheme.colorScheme.surface
                                }
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
                                Column {
                                    Text(
                                        text = tag.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.White
                                    )
                                    if (tag.name.startsWith("[OFFICIAL]")) {
                                        Badge(
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.padding(top = 4.dp)
                                        ) {
                                            Text("OFFICIAL PROGRAM")
                                        }
                                    }
                                }
                                Icon(
                                    Icons.Default.ArrowForward,
                                    contentDescription = "View Program",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Create Program Dialog
    if (showCreateDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { 
                Text(
                    "Create New Program",
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column {
        OutlinedTextField(
            value = tagName,
            onValueChange = { tagName = it },
                        label = { Text("Program Topic/Name") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        "Select Program Color:",
                        style = MaterialTheme.typography.titleMedium
                    )
                    
        Spacer(modifier = Modifier.height(8.dp))
        ColorPickerRows(
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
                        if (tagName.isNotEmpty()) {
                coroutineScope.launch {
                    val parentId = flarumTagRepository.getPrimaryMentorshipTagId()
                                val newTagName = "$tagName [by:$email]"
                                val newTag = flarumTagRepository.createTag(newTagName, selectedColor.toHexString(), parentId)
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
                                        
                                        // Get official programs
                                        val officialPrograms = updatedTags.filter { tag ->
                                            tag.parentId == parentId && 
                                            tag.name.startsWith("[OFFICIAL]")
                                        }
                                        
                                        // Update the combined list
                                        tagList = officialPrograms + studentPrograms
                                    }
                                    tagName = ""
                                    showCreateDialog = false
                            } else {
                                    Toast.makeText(context, "Failed to create program", Toast.LENGTH_SHORT).show()
                                }
                            }
                        } else {
                            Toast.makeText(context, "Program name cannot be empty", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("Create Program")
                }
            },
            dismissButton = {
                Button(
                    onClick = { showCreateDialog = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Text("Cancel")
                }
            }
        )
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
    var selectedTab by remember { mutableStateOf(0) } // 0 for All, 1 for Official, 2 for Non-Official
    var showCreateDialog by remember { mutableStateOf(false) }
    
    // Define primary and additional colors.
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
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Modern header with gradient background
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
                    text = "Alumni Mentorship Programs",
                    style = MaterialTheme.typography.headlineLarge,
                    color = Color.White,
                    modifier = Modifier.shadow(4.dp, shape = RoundedCornerShape(4.dp))
                )
        Text(
                    text = "Connect with students and share your experience",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White,
                    modifier = Modifier.shadow(4.dp, shape = RoundedCornerShape(4.dp))
                )
            }
        }

        // Tab layout for filtering programs
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("All Programs") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Official") }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text("Non-Official") }
            )
        }
        
        // Create New Program button
        Button(
            onClick = { showCreateDialog = true },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text("Create New Program")
        }

        // Filtered programs list
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            items(
                tagList.filter { tag ->
                    when (selectedTab) {
                        0 -> true // All programs
                        1 -> tag.name.startsWith("[OFFICIAL]") // Official programs
                        2 -> !tag.name.startsWith("[OFFICIAL]") // Non-official programs
                        else -> true
                    }
                }
            ) { tag ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .clickable {
                            val tagUrl = "http://129.154.249.30:8080/t/${tag.slug}"
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(tagUrl))
                            context.startActivity(intent)
                        },
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    shape = MaterialTheme.shapes.large
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                try {
                                    Color(android.graphics.Color.parseColor(tag.color))
                                } catch (e: Exception) {
                                    MaterialTheme.colorScheme.surface
                                }
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
                                Column {
                                    Text(
                                        text = tag.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.White
                                    )
                                    if (tag.name.startsWith("[OFFICIAL]")) {
                                        Badge(
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.padding(top = 4.dp)
                                        ) {
                                            Text("OFFICIAL PROGRAM")
                                        }
                                    }
                                }
                                Icon(
                                    Icons.Default.ArrowForward,
                                    contentDescription = "View Program",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Create Program Dialog
    if (showCreateDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { 
                Text(
                    "Create New Program",
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column {
                    OutlinedTextField(
                        value = tagName,
                        onValueChange = { tagName = it },
                        label = { Text("Program Topic/Name") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        "Select Program Color:",
                        style = MaterialTheme.typography.titleMedium
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    ColorPickerRows(
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
                        if (tagName.isNotEmpty()) {
                            coroutineScope.launch {
                                val parentId = flarumTagRepository.getPrimaryMentorshipTagId()
                                val newTagName = "$tagName [by:$email]"
                                val newTag = flarumTagRepository.createTag(newTagName, selectedColor.toHexString(), parentId)
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
                                        
                                        // Get official programs
                                        val officialPrograms = updatedTags.filter { tag ->
                                            tag.parentId == parentId && 
                                            tag.name.startsWith("[OFFICIAL]")
                                        }
                                        
                                        // Update the combined list
                                        tagList = officialPrograms + alumniPrograms
                                    }
                                    tagName = ""
                                    showCreateDialog = false
                                } else {
                                    Toast.makeText(context, "Failed to create program", Toast.LENGTH_SHORT).show()
                                }
                            }
                        } else {
                            Toast.makeText(context, "Program name cannot be empty", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("Create Program")
                }
            },
            dismissButton = {
                Button(
                    onClick = { showCreateDialog = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun CollegeAdminMentorshipScreen(email: String, navController: NavController) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val flarumTagRepository = remember { FlarumTagRepository() }
    
    var tagList by remember { mutableStateOf<List<Tag>>(emptyList()) }
    var selectedTab by remember { mutableStateOf(0) } // 0 for All, 1 for Official, 2 for Non-Official
    var newProgramName by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(Color(0xFF005B4F)) } // College theme color
    var showCreateDialog by remember { mutableStateOf(false) }
    
    // Define primary and additional colors for the program themes
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
    
    // Fetch all tags when the screen loads.
    LaunchedEffect(Unit) {
        val allTags = flarumTagRepository.getTags() ?: return@LaunchedEffect
        val primaryId = flarumTagRepository.getPrimaryMentorshipTagId()
        if (primaryId != null) {
            tagList = allTags.filter { it.parentId == primaryId }
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Modern header with gradient background
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
                    text = "Admin Mentorship Programs",
                    style = MaterialTheme.typography.headlineLarge,
                    color = Color.White,
                    modifier = Modifier.shadow(4.dp, shape = RoundedCornerShape(4.dp))
                )
                Text(
                    text = "Manage and create official mentorship programs",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White,
                    modifier = Modifier.shadow(4.dp, shape = RoundedCornerShape(4.dp))
                )
            }
        }

        // Tab layout for filtering programs
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("All Programs") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Official") }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text("Non-Official") }
            )
        }

        // Create New Program button
        Button(
            onClick = { showCreateDialog = true },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text("Create New Official Program")
        }

        // Filtered programs list
                    LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            items(
                tagList.filter { tag ->
                    when (selectedTab) {
                        0 -> true // All programs
                        1 -> tag.name.startsWith("[OFFICIAL]") // Official programs
                        2 -> !tag.name.startsWith("[OFFICIAL]") // Non-official programs
                        else -> true
                    }
                }
            ) { tag ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .clickable {
                            val tagUrl = "http://129.154.249.30:8080/t/${tag.slug}"
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(tagUrl))
                            context.startActivity(intent)
                        },
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    shape = MaterialTheme.shapes.large
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                try {
                                    Color(android.graphics.Color.parseColor(tag.color))
                                } catch (e: Exception) {
                                    MaterialTheme.colorScheme.surface
                                }
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
                                Column {
                                    Text(
                                        text = tag.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.White
                                    )
                                    if (tag.name.startsWith("[OFFICIAL]")) {
                                        Badge(
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.padding(top = 4.dp)
                                        ) {
                                            Text("OFFICIAL PROGRAM")
                                        }
                                    }
                                }
                                Icon(
                                    Icons.Default.ArrowForward,
                                    contentDescription = "View Program",
                                    tint = Color.White
                                )
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
            onDismissRequest = { showCreateDialog = false },
            title = { 
                Text(
                    "Create New Official Program",
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column {
                    OutlinedTextField(
                        value = newProgramName,
                        onValueChange = { newProgramName = it },
                        label = { Text("Program Topic/Name") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        "Select Program Theme Color:",
                        style = MaterialTheme.typography.titleMedium
                    )
                    
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
                                val officialProgramName = "[OFFICIAL] $newProgramName [ALL]"
                                val parentId = flarumTagRepository.getPrimaryMentorshipTagId()
                                val newTag = flarumTagRepository.createTag(
                                    officialProgramName, 
                                    selectedColor.toHexString(), 
                                    parentId
                                )
                                
                                if (newTag != null) {
                                    Toast.makeText(
                                        context, 
                                        "Official Program '${newTag.name}' created", 
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    
                                    // Refresh the tag list
                                    val updatedTags = flarumTagRepository.getTags()
                                    if (updatedTags != null && parentId != null) {
                                        tagList = updatedTags.filter { it.parentId == parentId }
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
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("Create Program")
                }
            },
            dismissButton = {
                Button(
                    onClick = { showCreateDialog = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}
