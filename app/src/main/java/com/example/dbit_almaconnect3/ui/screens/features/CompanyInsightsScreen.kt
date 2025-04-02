package com.example.dbit_almaconnect3.ui.screens.features

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Sort
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
import com.example.dbit_almaconnect3.App
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

data class CompanyInsight(
    val id: String,
    val title: String,
    val content: String,
    val companyName: String,
    val created: String,
    val updated: String,
    val authorId: String,
    val authorName: String,
    val authorRole: String
)

data class CompanyInsightComment(
    val id: String,
    val text: String,
    val created: String,
    val insight: String,
    val authorId: String,
    val authorName: String,
    val authorRole: String
)

class CompanyInsightsViewModel {
    private val client = OkHttpClient()

    fun getSharedPreferences(context: Context): android.content.SharedPreferences? {
        return try {
            context.getSharedPreferences("auth", Context.MODE_PRIVATE)
        } catch (e: Exception) {
            Log.e("CompanyInsights", "Error getting shared preferences", e)
            null
        }
    }

    fun createCompanyInsight(
        context: Context,
        title: String,
        content: String,
        companyName: String,
        email: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val sharedPrefs = getSharedPreferences(context) ?: return
        val userId = sharedPrefs.getString("userId", "") ?: ""
        val userName = sharedPrefs.getString("name", "") ?: ""
        val userRole = sharedPrefs.getString("role", "") ?: ""
        
        if (userId.isEmpty() || userName.isEmpty() || userRole.isEmpty()) {
            onError("Missing user information")
            return
        }
        
        val json = JSONObject().apply {
            put("title", title)
            put("content", content)
            put("company_name", companyName)
            put("author_id", userId)
            put("author_name", userName)
            put("author_role", userRole)
        }
        
        val request = Request.Builder()
            .url("http://129.154.249.30:8091/api/collections/company_insights/records")
            .post(json.toString().toRequestBody("application/json".toMediaType()))
            .build()
        
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                onError("Error creating insight: ${e.message}")
            }
            
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    onSuccess()
                } else {
                    onError("Error creating insight: ${response.body?.string()}")
                }
            }
        })
    }

    fun updateCompanyInsight(
        context: Context,
        id: String,
        title: String,
        content: String,
        companyName: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val sharedPrefs = getSharedPreferences(context) ?: return
        val userId = sharedPrefs.getString("userId", "") ?: ""
        
        if (userId.isEmpty()) {
            onError("Missing user ID")
            return
        }
        
        val getRequest = Request.Builder()
            .url("http://129.154.249.30:8091/api/collections/company_insights/records/$id")
            .build()
        
        client.newCall(getRequest).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                onError("Error fetching insight: ${e.message}")
            }
            
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    try {
                        val jsonObject = JSONObject(response.body?.string() ?: "")
                        val authorId = jsonObject.getString("author_id")
                        
                        if (authorId == userId || sharedPrefs.getString("role", "") == "college_admin") {
                            val json = JSONObject().apply {
                                put("title", title)
                                put("content", content)
                                put("company_name", companyName)
                            }
                            
                            val updateRequest = Request.Builder()
                                .url("http://129.154.249.30:8091/api/collections/company_insights/records/$id")
                                .patch(json.toString().toRequestBody("application/json".toMediaType()))
                                .build()
                            
                            client.newCall(updateRequest).enqueue(object : Callback {
                                override fun onFailure(call: Call, e: IOException) {
                                    onError("Error updating insight: ${e.message}")
                                }
                                
                                override fun onResponse(call: Call, response: Response) {
                                    if (response.isSuccessful) {
                                        onSuccess()
                                    } else {
                                        onError("Error updating insight: ${response.body?.string()}")
                                    }
                                }
                            })
                        } else {
                            onError("Not authorized to update this insight")
                        }
                    } catch (e: Exception) {
                        onError("Error parsing insight response: ${e.message}")
                    }
                } else {
                    onError("Error fetching insight: ${response.body?.string()}")
                }
            }
        })
    }

    fun deleteCompanyInsight(
        context: Context,
        id: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val sharedPrefs = getSharedPreferences(context) ?: return
        val userId = sharedPrefs.getString("userId", "") ?: ""
        val token = sharedPrefs.getString("token", "") ?: ""
        
        if (userId.isEmpty()) {
            kotlinx.coroutines.MainScope().launch {
                onError("Missing user ID")
            }
            return
        }
        
        val getRequest = Request.Builder()
            .url("http://129.154.249.30:8091/api/collections/company_insights/records/$id")
            .addHeader("Authorization", "Bearer $token")
            .build()
        
        client.newCall(getRequest).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                kotlinx.coroutines.MainScope().launch {
                    onError("Error fetching insight: ${e.message}")
                }
            }
            
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    try {
                        val jsonObject = JSONObject(response.body?.string() ?: "")
                        val authorId = jsonObject.getString("author_id")
                        
                        if (authorId == userId || sharedPrefs.getString("role", "") == "college_admin") {
                            val deleteRequest = Request.Builder()
                                .url("http://129.154.249.30:8091/api/collections/company_insights/records/$id")
                                .addHeader("Authorization", "Bearer $token")
                                .delete()
                                .build()
                            
                            client.newCall(deleteRequest).enqueue(object : Callback {
                                override fun onFailure(call: Call, e: IOException) {
                                    kotlinx.coroutines.MainScope().launch {
                                        onError("Error deleting insight: ${e.message}")
                                    }
                                }
                                
                                override fun onResponse(call: Call, response: Response) {
                                    kotlinx.coroutines.MainScope().launch {
                                        if (response.isSuccessful) {
                                            onSuccess()
                                        } else {
                                            onError("Error deleting insight: ${response.body?.string()}")
                                        }
                                    }
                                }
                            })
                        } else {
                            kotlinx.coroutines.MainScope().launch {
                                onError("Not authorized to delete this insight")
                            }
                        }
                    } catch (e: Exception) {
                        kotlinx.coroutines.MainScope().launch {
                            onError("Error parsing insight response: ${e.message}")
                        }
                    }
                } else {
                    kotlinx.coroutines.MainScope().launch {
                        onError("Error fetching insight: ${response.body?.string()}")
                    }
                }
            }
        })
    }

    fun fetchCompanyInsightComments(
        insightId: String,
        onSuccess: (List<CompanyInsightComment>) -> Unit,
        onError: (String) -> Unit,
        context: Context
    ) {
        val sharedPrefs = getSharedPreferences(context) ?: return
        val token = sharedPrefs.getString("token", "") ?: ""
        
        Log.d("CompanyInsights", "Fetching comments for insight: $insightId with token: $token") // Debug log
        
        val request = Request.Builder()
            .url("http://129.154.249.30:8091/api/collections/CompanyInsightComments/records?filter=(insight='$insightId')")
            .addHeader("Authorization", "Bearer $token")
            .get()
            .build()
        
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("CompanyInsights", "Error fetching comments", e)
                kotlinx.coroutines.MainScope().launch {
                    onError("Network error: ${e.message}")
                    onSuccess(emptyList())
                }
            }
            
            override fun onResponse(call: Call, response: Response) {
                try {
                    val responseBody = response.body?.string()
                    Log.d("CompanyInsights", "Comments response: $responseBody") // Debug log
                    
                    if (response.isSuccessful) {
                        val jsonObject = JSONObject(responseBody)
                        val jsonArray = jsonObject.getJSONArray("items")
                        val comments = mutableListOf<CompanyInsightComment>()
                        
                        for (i in 0 until jsonArray.length()) {
                            val commentObject = jsonArray.getJSONObject(i)
                            try {
                                comments.add(
                                    CompanyInsightComment(
                                        id = commentObject.getString("id"),
                                        text = commentObject.optString("text", ""),
                                        created = commentObject.optString("created", ""),
                                        insight = commentObject.optString("insight", ""),
                                        authorId = commentObject.optString("author_id", ""),
                                        authorName = commentObject.optString("author_name", ""),
                                        authorRole = commentObject.optString("author_role", "")
                                    )
                                )
                            } catch (e: Exception) {
                                Log.e("CompanyInsights", "Error parsing comment: ${e.message}")
                                continue
                            }
                        }
                        
                        kotlinx.coroutines.MainScope().launch {
                            onSuccess(comments)
                        }
                    } else {
                        Log.e("CompanyInsights", "Error fetching comments: ${response.code} - $responseBody")
                        kotlinx.coroutines.MainScope().launch {
                            onError("Error fetching comments: ${response.code}")
                            onSuccess(emptyList())
                        }
                    }
                } catch (e: Exception) {
                    Log.e("CompanyInsights", "Error parsing comments response", e)
                    kotlinx.coroutines.MainScope().launch {
                        onError("Error parsing response: ${e.message}")
                        onSuccess(emptyList())
                    }
                }
            }
        })
    }

    fun createCompanyInsightComment(
        context: Context,
        insightId: String,
        text: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val sharedPrefs = getSharedPreferences(context) ?: return
        val userId = sharedPrefs.getString("userId", "") ?: ""
        val userName = sharedPrefs.getString("name", "") ?: ""
        val userRole = sharedPrefs.getString("role", "") ?: ""
        val token = sharedPrefs.getString("token", "") ?: ""
        
        if (userId.isEmpty() || userName.isEmpty() || userRole.isEmpty()) {
            kotlinx.coroutines.MainScope().launch {
                onError("Missing user information")
            }
            return
        }
        
        val json = JSONObject().apply {
            put("text", text)
            put("insight", insightId)
            put("author_id", userId)
            put("author_name", userName)
            put("author_role", userRole)
        }

        Log.d("CompanyInsights", "Creating comment with data: ${json.toString()}") // Debug log
        
        val request = Request.Builder()
            .url("http://129.154.249.30:8091/api/collections/CompanyInsightComments/records")
            .addHeader("Authorization", "Bearer $token")
            .post(json.toString().toRequestBody("application/json".toMediaType()))
            .build()
        
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("CompanyInsights", "Error creating comment", e)
                kotlinx.coroutines.MainScope().launch {
                    onError("Error creating comment: ${e.message}")
                }
            }
            
            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                Log.d("CompanyInsights", "Create comment response: $responseBody") // Debug log
                
                kotlinx.coroutines.MainScope().launch {
                    if (response.isSuccessful) {
                        onSuccess()
                    } else {
                        onError("Error creating comment: $responseBody")
                    }
                }
            }
        })
    }

    fun deleteCompanyInsightComment(
        context: Context,
        commentId: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val sharedPrefs = getSharedPreferences(context) ?: return
        val token = sharedPrefs.getString("token", "") ?: ""
        
        val request = Request.Builder()
            .url("http://129.154.249.30:8091/api/collections/CompanyInsightComments/records/$commentId")
            .addHeader("Authorization", "Bearer $token")
            .delete()
            .build()
        
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                kotlinx.coroutines.MainScope().launch {
                    onError("Error deleting comment: ${e.message}")
                }
            }
            
            override fun onResponse(call: Call, response: Response) {
                kotlinx.coroutines.MainScope().launch {
                    if (response.isSuccessful) {
                        onSuccess()
                    } else {
                        val responseBody = response.body?.string() ?: ""
                        onError("Error deleting comment: $responseBody")
                    }
                }
            }
        })
    }

    fun flagCompanyInsightComment(
        context: Context,
        commentId: String,
        reason: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val sharedPrefs = getSharedPreferences(context) ?: return
        val token = sharedPrefs.getString("token", "") ?: ""
        
        val json = JSONObject().apply {
            put("is_flagged", true)
            put("flag_reason", reason)
            put("flagged_at", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))
        }
        
        val request = Request.Builder()
            .url("http://129.154.249.30:8091/api/collections/CompanyInsightComments/records/$commentId")
            .addHeader("Authorization", "Bearer $token")
            .patch(json.toString().toRequestBody("application/json".toMediaType()))
            .build()
        
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                kotlinx.coroutines.MainScope().launch {
                    onError("Error flagging comment: ${e.message}")
                }
            }
            
            override fun onResponse(call: Call, response: Response) {
                kotlinx.coroutines.MainScope().launch {
                    if (response.isSuccessful) {
                        onSuccess()
                    } else {
                        val responseBody = response.body?.string() ?: ""
                        onError("Error flagging comment: $responseBody")
                    }
                }
            }
        })
    }
}

@Composable
fun StudentCompanyInsightsScreen(email: String, navController: NavController) {
    val viewModel = remember { CompanyInsightsViewModel() }
    var insights by remember { mutableStateOf<List<CompanyInsight>>(emptyList()) }
    var sortByNewest by remember { mutableStateOf(true) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    LaunchedEffect(key1 = Unit) {
        fetchCompanyInsights({ fetchedInsights ->
            insights = fetchedInsights
            if (fetchedInsights.isEmpty()) {
                Toast.makeText(context, "No insights found", Toast.LENGTH_SHORT).show()
            }
        }, { error ->
            Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
        }, context)
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
                text = "Company Insights",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            
            IconButton(onClick = { sortByNewest = !sortByNewest }) {
                Icon(
                    Icons.Default.Sort,
                    contentDescription = "Sort",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        
        if (insights.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Text(
                text = "Showing ${if (sortByNewest) "newest" else "oldest"} first",
                fontSize = 12.sp,
                color = Color.Gray,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )
            
            val sortedInsights = if (sortByNewest) {
                insights.sortedByDescending { it.created }
            } else {
                insights.sortedBy { it.created }
            }
            
            LazyColumn {
                items(sortedInsights) { insight ->
                    CompanyInsightItem(
                        insight = insight,
                        viewModel = viewModel,
                        onInsightUpdated = {
                            fetchCompanyInsights({ fetchedInsights ->
                                insights = fetchedInsights
                            }, { error ->
                                Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                            }, context)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun AlumniCompanyInsightsScreen(email: String, navController: NavController) {
    val viewModel = remember { CompanyInsightsViewModel() }
    var insights by remember { mutableStateOf<List<CompanyInsight>>(emptyList()) }
    var showAddDialog by remember { mutableStateOf(false) }
    var sortByNewest by remember { mutableStateOf(true) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    LaunchedEffect(key1 = Unit) {
        fetchCompanyInsights({ fetchedInsights ->
            insights = fetchedInsights
        }, { error ->
            Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
        }, context)
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
                text = "Company Insights",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            
            Row {
                IconButton(onClick = { sortByNewest = !sortByNewest }) {
                    Icon(
                        Icons.Default.Sort,
                        contentDescription = "Sort",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                
                IconButton(onClick = { showAddDialog = true }) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Add Insight",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        
        if (insights.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Text(
                text = "Showing ${if (sortByNewest) "newest" else "oldest"} first",
                fontSize = 12.sp,
                color = Color.Gray,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )
            
            val sortedInsights = if (sortByNewest) {
                insights.sortedByDescending { it.created }
            } else {
                insights.sortedBy { it.created }
            }
            
            LazyColumn {
                items(sortedInsights) { insight ->
                    CompanyInsightItem(
                        insight = insight,
                        viewModel = viewModel,
                        onInsightUpdated = {
                            fetchCompanyInsights({ fetchedInsights ->
                                insights = fetchedInsights
                            }, { error ->
                                Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                            }, context)
                        }
                    )
                }
            }
        }
    }
    
    if (showAddDialog) {
        AddEditCompanyInsightDialog(
            insight = null,
            onDismiss = { showAddDialog = false },
            onSave = { title, content, companyName ->
                viewModel.createCompanyInsight(
                    context = context,
                    title = title,
                    content = content,
                    companyName = companyName,
                    email = email,
                    onSuccess = {
                        showAddDialog = false
                        fetchCompanyInsights({ fetchedInsights ->
                            insights = fetchedInsights
                        }, { error ->
                            Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                        }, context)
                    },
                    onError = { error ->
                        Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                    }
                )
            }
        )
    }
}

@Composable
fun CollegeAdminCompanyInsightsScreen(email: String, navController: NavController) {
    val viewModel = remember { CompanyInsightsViewModel() }
    var insights by remember { mutableStateOf<List<CompanyInsight>>(emptyList()) }
    var sortByNewest by remember { mutableStateOf(true) }
    val context = LocalContext.current
    
    LaunchedEffect(key1 = Unit) {
        fetchCompanyInsights({ fetchedInsights ->
            insights = fetchedInsights
        }, { error ->
            Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
        }, context)
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
                text = "Company Insights",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            
            IconButton(onClick = { sortByNewest = !sortByNewest }) {
                Icon(
                    Icons.Default.Sort,
                    contentDescription = "Sort",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        
        if (insights.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Text(
                text = "Showing ${if (sortByNewest) "newest" else "oldest"} first",
                fontSize = 12.sp,
                color = Color.Gray,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )
            
            val sortedInsights = if (sortByNewest) {
                insights.sortedByDescending { it.created }
            } else {
                insights.sortedBy { it.created }
            }
            
            LazyColumn {
                items(sortedInsights) { insight ->
                    CompanyInsightItem(
                        insight = insight,
                        viewModel = viewModel,
                        onInsightUpdated = {
                            fetchCompanyInsights({ fetchedInsights ->
                                insights = fetchedInsights
                            }, { error ->
                                Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                            }, context)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun CompanyInsightItem(
    insight: CompanyInsight,
    viewModel: CompanyInsightsViewModel,
    onInsightUpdated: () -> Unit
) {
    val context = LocalContext.current
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var showComments by remember { mutableStateOf(false) }
    var showAddCommentDialog by remember { mutableStateOf(false) }
    var comments by remember { mutableStateOf<List<CompanyInsightComment>>(emptyList()) }
    
    LaunchedEffect(showComments) {
        if (showComments) {
            viewModel.fetchCompanyInsightComments(
                insightId = insight.id,
                onSuccess = { fetchedComments ->
                    comments = fetchedComments
                },
                onError = { error ->
                    Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                },
                context = context
            )
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = insight.companyName,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Row {
                    IconButton(onClick = { showEditDialog = true }) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    IconButton(onClick = { showDeleteConfirmation = true }) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            
            Text(
                text = insight.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            
            Text(
                text = insight.content,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Text(
                text = "Posted by ${insight.authorName} on ${formatDate(insight.created)}",
                fontSize = 12.sp,
                color = Color.Gray,
                fontStyle = FontStyle.Italic
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Comments section toggle button
            TextButton(
                onClick = { showComments = !showComments },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (showComments) "Hide Comments" else "Show Comments")
            }
            
            if (showComments) {
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Comments (${comments.size})",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    
                    TextButton(onClick = { showAddCommentDialog = true }) {
                        Text("Add Comment")
                    }
                }
                
                if (comments.isEmpty()) {
                    Text(
                        text = "No comments yet",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    comments.forEach { comment ->
                        CommentItem(
                            comment = comment,
                            viewModel = viewModel,
                            onCommentDeleted = {
                                // Refresh comments after deletion
                                viewModel.fetchCompanyInsightComments(
                                    insightId = insight.id,
                                    onSuccess = { fetchedComments ->
                                        comments = fetchedComments
                                    },
                                    onError = { error ->
                                        Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                                    },
                                    context = context
                                )
                            }
                        )
                    }
                }
            }
        }
    }
    
    if (showEditDialog) {
        AddEditCompanyInsightDialog(
            insight = insight,
            onDismiss = { showEditDialog = false },
            onSave = { title, content, companyName ->
                viewModel.updateCompanyInsight(
                    context = context,
                    id = insight.id,
                    title = title,
                    content = content,
                    companyName = companyName,
                    onSuccess = {
                        showEditDialog = false
                        onInsightUpdated()
                    },
                    onError = { error ->
                        Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                    }
                )
            }
        )
    }
    
    if (showDeleteConfirmation) {
        DeleteConfirmationDialog(
            onDismiss = { showDeleteConfirmation = false },
            onConfirm = {
                viewModel.deleteCompanyInsight(
                    context = context,
                    id = insight.id,
                    onSuccess = {
                        showDeleteConfirmation = false
                        onInsightUpdated()
                    },
                    onError = { error ->
                        Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                    }
                )
            }
        )
    }
    
    if (showAddCommentDialog) {
        AddCommentDialog(
            onDismiss = { showAddCommentDialog = false },
            onSave = { text ->
                viewModel.createCompanyInsightComment(
                    context = context,
                    insightId = insight.id,
                    text = text,
                    onSuccess = {
                        showAddCommentDialog = false
                        // Refresh comments after adding a new one
                        viewModel.fetchCompanyInsightComments(
                            insightId = insight.id,
                            onSuccess = { fetchedComments ->
                                comments = fetchedComments
                            },
                            onError = { error ->
                                Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                            },
                            context = context
                        )
                    },
                    onError = { error ->
                        Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                    }
                )
            }
        )
    }
}

@Composable
fun CommentItem(
    comment: CompanyInsightComment,
    viewModel: CompanyInsightsViewModel,
    onCommentDeleted: () -> Unit
) {
    val context = LocalContext.current
    var showDeleteConfirm by remember { mutableStateOf(false) }
    
    // Get current user information
    val sharedPrefs = context.getSharedPreferences("auth", Context.MODE_PRIVATE)
    val currentUserId = sharedPrefs.getString("userId", "") ?: ""
    val userRole = sharedPrefs.getString("role", "") ?: ""
    
    // Determine permissions
    val isAuthor = comment.authorId == currentUserId
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
                        text = "By ${comment.authorName} on ${formatDate(comment.created)}",
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
                        viewModel.deleteCompanyInsightComment(
                            context = context,
                            commentId = comment.id,
                            onSuccess = {
                                Toast.makeText(context, "Comment deleted", Toast.LENGTH_SHORT).show()
                                onCommentDeleted()
                                showDeleteConfirm = false
                            },
                            onError = { error ->
                                Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                            }
                        )
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
fun AddEditCompanyInsightDialog(
    insight: CompanyInsight? = null,
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    var title by remember { mutableStateOf(insight?.title ?: "") }
    var content by remember { mutableStateOf(insight?.content ?: "") }
    var companyName by remember { mutableStateOf(insight?.companyName ?: "") }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = if (insight == null) "Add Company Insight" else "Edit Company Insight",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                OutlinedTextField(
                    value = companyName,
                    onValueChange = { companyName = it },
                    label = { Text("Company Name") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
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
                        .padding(bottom = 16.dp),
                    minLines = 4
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    
                    TextButton(
                        onClick = {
                            if (title.isNotBlank() && content.isNotBlank() && companyName.isNotBlank()) {
                                onSave(title, content, companyName)
                            }
                        }
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

@Composable
fun AddCommentDialog(
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var commentText by remember { mutableStateOf("") }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Add Comment",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                OutlinedTextField(
                    value = commentText,
                    onValueChange = { commentText = it },
                    label = { Text("Comment") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    minLines = 3
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    
                    TextButton(
                        onClick = {
                            if (commentText.isNotBlank()) {
                                onSave(commentText)
                            }
                        }
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

@Composable
fun DeleteConfirmationDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Are you sure you want to delete this insight?",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    
                    TextButton(
                        onClick = {
                            onConfirm()
                        }
                    ) {
                        Text("Delete")
                    }
                }
            }
        }
    }
}

private fun formatDate(dateString: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSSSS", Locale.getDefault())
        val outputFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
        val date = inputFormat.parse(dateString)
        outputFormat.format(date!!)
    } catch (e: Exception) {
        dateString
    }
}

private fun fetchCompanyInsights(
    onSuccess: (List<CompanyInsight>) -> Unit,
    onError: (String) -> Unit,
    context: Context
) {
    val client = OkHttpClient()
    val sharedPrefs = context.getSharedPreferences("auth", Context.MODE_PRIVATE)
    val token = sharedPrefs.getString("token", "") ?: ""
    
    Log.d("CompanyInsights", "Token for insights: $token") // Debug log
    
    val request = Request.Builder()
        .url("http://129.154.249.30:8091/api/collections/company_insights/records")
        .addHeader("Authorization", "Bearer $token")
        .get()
        .build()
    
    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            Log.e("CompanyInsights", "Error fetching insights", e)
            kotlinx.coroutines.MainScope().launch {
                onError("Network error: ${e.message}")
                onSuccess(emptyList())
            }
        }
        
        override fun onResponse(call: Call, response: Response) {
            try {
                val responseBody = response.body?.string()
                Log.d("CompanyInsights", "Insights response: $responseBody") // Debug log

                if (response.isSuccessful) {
                    val jsonObject = JSONObject(responseBody)
                    val jsonArray = jsonObject.getJSONArray("items")
                    val insights = mutableListOf<CompanyInsight>()
                    
                    for (i in 0 until jsonArray.length()) {
                        val insightObject = jsonArray.getJSONObject(i)
                        try {
                            insights.add(
                                CompanyInsight(
                                    id = insightObject.getString("id"),
                                    title = insightObject.optString("title", ""),
                                    content = insightObject.optString("content", ""),
                                    companyName = insightObject.optString("company_name", ""),
                                    created = insightObject.optString("created", ""),
                                    updated = insightObject.optString("updated", ""),
                                    authorId = insightObject.optString("author_id", ""),
                                    authorName = insightObject.optString("author_name", ""),
                                    authorRole = insightObject.optString("author_role", "")
                                )
                            )
                        } catch (e: Exception) {
                            Log.e("CompanyInsights", "Error parsing insight: ${e.message}")
                            continue
                        }
                    }
                    
                    kotlinx.coroutines.MainScope().launch {
                        onSuccess(insights)
                    }
                } else {
                    Log.e("CompanyInsights", "Error fetching insights: ${response.code} - $responseBody")
                    kotlinx.coroutines.MainScope().launch {
                        onError("Error fetching insights: ${response.code}")
                        onSuccess(emptyList())
                    }
                }
            } catch (e: Exception) {
                Log.e("CompanyInsights", "Error parsing insights response", e)
                kotlinx.coroutines.MainScope().launch {
                    onError("Error parsing response: ${e.message}")
                    onSuccess(emptyList())
                }
            }
        }
    })
}
