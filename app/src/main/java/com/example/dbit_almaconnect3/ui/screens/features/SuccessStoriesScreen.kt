package com.example.dbit_almaconnect3.ui.screens.features

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ThumbUp
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
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Image
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.dbit_almaconnect3.ui.theme.*
import com.example.dbit_almaconnect3.ui.theme.BlueBrandColor

data class SuccessStory(
    val id: String,
    val title: String,
    val content: String,
    val created: String,
    val updated: String,
    val author: String,
    val authorName: String = "",
    val authorRole: String = "",
    val Likes: Int = 0,
    val likedBy: List<String> = emptyList(),
    val attachments: List<String> = emptyList()
) {
    companion object {
        fun fromJson(json: JSONObject): SuccessStory {
            Log.d("SuccessStory", "📄 Parsing JSON: ${json.toString(2)}")

            // Get the raw ID value directly from the JSON
            val rawId = json.optString("id", "")
            Log.d("SuccessStory", "🔑 Raw ID from JSON: '$rawId'")

            // Don't modify the ID in any way - use it exactly as provided by PocketBase
            if (rawId.isBlank()) {
                Log.e("SuccessStory", "❌ Missing or empty ID in story JSON")
            } else {
                Log.d("SuccessStory", "✅ Found valid story ID: '$rawId'")
            }

            val authorObject = json.optJSONObject("expand")?.optJSONObject("author")
            Log.d("SuccessStory", "👤 Author object: ${authorObject?.toString(2)}")

            // Handle attachments field which can be a string or empty
            val attachmentsList = mutableListOf<String>()
            val attachmentValue = json.optString("attachments", "")
            if (attachmentValue.isNotEmpty()) {
                attachmentsList.add(attachmentValue)
                Log.d("SuccessStory", "📎 Found attachment: $attachmentValue")
            }

            // Parse liked_by field if it exists
            val likedByList = mutableListOf<String>()
            val likedByValue = json.optString("liked_by", "")
            if (likedByValue.isNotEmpty()) {
                try {
                    val likedByArray = JSONArray(likedByValue)
                    for (i in 0 until likedByArray.length()) {
                        likedByList.add(likedByArray.getString(i))
                    }
                    Log.d("SuccessStory", "👍 Liked by users: $likedByList")
                } catch (e: Exception) {
                    // If it's not a JSON array, try adding it as a single string
                    likedByList.add(likedByValue)
                    Log.d("SuccessStory", "👍 Liked by single user: $likedByValue")
                }
            }

            return SuccessStory(
                id = rawId, // Use the raw ID directly
                title = json.optString("title", ""),
                content = json.optString("content", ""),
                created = json.optString("created", ""),
                updated = json.optString("updated", ""),
                author = json.optString("author", ""),
                authorName = authorObject?.optString("username", "") ?: "",
                authorRole = authorObject?.optString("role", "") ?: "",
                Likes = json.optInt("Likes", 0),
                likedBy = likedByList,
                attachments = attachmentsList
            ).also {
                Log.d("SuccessStory", "✅ Created story object: id=${it.id}, title=${it.title}, author=${it.authorName}, likes=${it.Likes}, likedBy=${it.likedBy}")
            }
        }
    }
}

class SuccessStoriesViewModel {
    private val client = OkHttpClient()

    fun getSharedPreferences(context: Context): android.content.SharedPreferences? {
        return try {
            context.getSharedPreferences("auth", Context.MODE_PRIVATE)
        } catch (e: Exception) {
            Log.e("SuccessStories", "Error getting shared preferences", e)
            null
        }
    }

    fun createSuccessStory(
        context: Context,
        title: String,
        content: String,
        attachments: List<String>,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val sharedPrefs = getSharedPreferences(context) ?: return
        val userId = sharedPrefs.getString("userId", "") ?: ""
        val token = sharedPrefs.getString("token", "") ?: ""

        if (userId.isEmpty()) {
            onError("Missing user information")
            return
        }

        Log.d("SuccessStories", "Creating new story with attachments: $attachments")

        // Create multipart request if we have attachments
        if (attachments.isNotEmpty()) {
            try {
                val attachmentName = attachments.first()
                Log.d("SuccessStories", "📎 Creating story with attachment: $attachmentName")

                // Get the file from cache if it exists
                val attachmentFile = File(context.cacheDir, attachmentName)

                // Check and log if file exists and its size
                Log.d("SuccessStories", "📂 File exists: ${attachmentFile.exists()}, size: ${attachmentFile.length()} bytes at path: ${attachmentFile.absolutePath}")

                if (attachmentFile.exists()) {
                    // We have the file in cache, create a multipart request
                    val requestBody = MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("title", title)
                        .addFormDataPart("content", content)
                        .addFormDataPart("author", userId)
                        .addFormDataPart("Likes", "0")

                    // Determine content type based on file extension
                    val fileExtension = attachmentName.substringAfterLast('.', "")
                    val contentType = when(fileExtension.lowercase()) {
                        "jpg", "jpeg" -> "image/jpeg"
                        "png" -> "image/png"
                        "pdf" -> "application/pdf"
                        "txt" -> "text/plain"
                        "doc", "docx" -> "application/msword"
                        else -> "application/octet-stream"
                    }

                    // CRITICAL CHANGE: The field name must be "attachments" exactly as defined in PocketBase
                    // and we need to send the actual file, not just a reference to it
                    requestBody.addFormDataPart(
                        "attachments", // This must match the field name in PocketBase
                        attachmentName,
                        attachmentFile.asRequestBody(contentType.toMediaType())
                    )

                    val request = Request.Builder()
                        .url("http://129.154.249.30:8091/api/collections/success_stories/records")
                        .addHeader("Authorization", "Bearer $token")
                        .post(requestBody.build())
                        .build()

                    Log.d("SuccessStories", "📤 Sending multipart request with attachment: $attachmentName (${attachmentFile.length()} bytes)")

                    client.newCall(request).enqueue(object : Callback {
                        override fun onFailure(call: Call, e: IOException) {
                            Log.e("SuccessStories", "❌ Error creating story with attachment: ${e.message}", e)
                            kotlinx.coroutines.MainScope().launch {
                                onError("Error creating story: ${e.message}")
                            }
                        }

                        override fun onResponse(call: Call, response: Response) {
                            val responseBody = response.body?.string()
                            Log.d("SuccessStories", "📥 Create with attachment response: ${response.code} - $responseBody")

                            kotlinx.coroutines.MainScope().launch {
                                if (response.isSuccessful && responseBody != null) {
                                    onSuccess()
                                } else {
                                    // If multipart fails, try without the file
                                    Log.d("SuccessStories", "⚠️ Multipart request failed, trying JSON only")
                                    createWithJson(context, title, content, userId, token, onSuccess, onError)
                                }
                            }

                            response.body?.close()
                        }
                    })
                } else {
                    // File doesn't exist in cache - log warning and fallback to JSON without attachments
                    Log.w("SuccessStories", "⚠️ File doesn't exist in cache: ${attachmentFile.absolutePath}")

                    // Don't include attachment since file doesn't exist
                    val json = JSONObject().apply {
                        put("title", title)
                        put("content", content)
                        put("author", userId)
                        put("Likes", 0)
                        put("attachments", "")
                    }

                    createWithJsonRequest(json, token, onSuccess, onError)
                }
            } catch (e: Exception) {
                Log.e("SuccessStories", "❌ Error processing attachment: ${e.message}", e)
                // Fallback to JSON-only request if there's an error with the file
                createWithJson(context, title, content, userId, token, onSuccess, onError)
            }
        } else {
            // No attachments, use JSON
            createWithJson(context, title, content, userId, token, onSuccess, onError)
        }
    }

    private fun createWithJsonRequest(
        json: JSONObject,
        token: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        Log.d("SuccessStories", "📤 Creating story with JSON: $json")

        val request = Request.Builder()
            .url("http://129.154.249.30:8091/api/collections/success_stories/records")
            .addHeader("Authorization", "Bearer $token")
            .post(json.toString().toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("SuccessStories", "❌ Error creating story with JSON: ${e.message}", e)
                kotlinx.coroutines.MainScope().launch {
                    onError("Error creating story: ${e.message}")
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                Log.d("SuccessStories", "📥 JSON Create story response: ${response.code} - $responseBody")

                kotlinx.coroutines.MainScope().launch {
                    if (response.isSuccessful && responseBody != null) {
                        onSuccess()
                    } else {
                        onError("Error creating story: ${responseBody ?: response.code}")
                    }
                }

                response.body?.close()
            }
        })
    }

    private fun createWithJson(
        context: Context,
        title: String,
        content: String,
        userId: String,
        token: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val json = JSONObject().apply {
            put("title", title)
            put("content", content)
            put("author", userId)
            put("Likes", 0)
            // Don't include attachments in JSON fallback since the file doesn't exist in PocketBase
            put("attachments", "")
        }

        createWithJsonRequest(json, token, onSuccess, onError)
    }

    fun updateSuccessStory(
        context: Context,
        id: String,
        title: String,
        content: String,
        attachments: List<String>,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val sharedPrefs = getSharedPreferences(context) ?: return
        val userId = sharedPrefs.getString("userId", "") ?: ""
        val token = sharedPrefs.getString("token", "") ?: ""

        if (userId.isEmpty()) {
            onError("Missing user ID")
            return
        }

        Log.d("SuccessStories", "Updating story $id with attachments: $attachments")

        // For multiple attachments, we need to use multipart request
        if (attachments.isNotEmpty()) {
            try {
                // Check if we have any new attachments (those in the cache dir)
                val newAttachments = attachments.filter { attachment ->
                    val attachmentFile = File(context.cacheDir, attachment)
                    attachmentFile.exists()
                }

                // If we have new attachments, use multipart
                if (newAttachments.isNotEmpty()) {
                    val requestBody = MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("title", title)
                        .addFormDataPart("content", content)

                    // Add existing attachments as a string field
                    val existingAttachments = attachments.filterNot { newAttachments.contains(it) }
                    if (existingAttachments.isNotEmpty()) {
                        val existingAttachmentStr = existingAttachments.joinToString(",")
                        requestBody.addFormDataPart("existingAttachments", existingAttachmentStr)
                    }

                    // Add each new attachment as a file part
                    newAttachments.forEach { attachmentName ->
                        val attachmentFile = File(context.cacheDir, attachmentName)
                        if (attachmentFile.exists()) {
                            // Determine content type based on file extension
                            val fileExtension = attachmentName.substringAfterLast('.', "")
                            val contentType = when(fileExtension.lowercase()) {
                                "jpg", "jpeg" -> "image/jpeg"
                                "png" -> "image/png"
                                "pdf" -> "application/pdf"
                                "txt" -> "text/plain"
                                "doc", "docx" -> "application/msword"
                                else -> "application/octet-stream"
                            }

                            requestBody.addFormDataPart(
                                "attachments", // This must match the field name in PocketBase
                                attachmentName,
                                attachmentFile.asRequestBody(contentType.toMediaType())
                            )
                        }
                    }

                    val request = Request.Builder()
                        .url("http://129.154.249.30:8091/api/collections/success_stories/records/$id")
                        .addHeader("Authorization", "Bearer $token")
                        .patch(requestBody.build())
                        .build()

                    client.newCall(request).enqueue(object : Callback {
                        override fun onFailure(call: Call, e: IOException) {
                            Log.e("SuccessStories", "Error updating story: ${e.message}", e)
                            kotlinx.coroutines.MainScope().launch {
                                onError("Error updating story: ${e.message}")
                            }
                        }

                        override fun onResponse(call: Call, response: Response) {
                            val responseBody = response.body?.string()
                            Log.d("SuccessStories", "Update story response: ${response.code} - $responseBody")

                            kotlinx.coroutines.MainScope().launch {
                                if (response.isSuccessful) {
                                    onSuccess()
                                } else {
                                    onError("Error updating story: ${responseBody ?: response.code}")
                                }
                                response.body?.close()
                            }
                        }
                    })
                } else {
                    // No new attachments, just update with existing ones
                    updateWithJson(id, title, content, attachments, token, onSuccess, onError)
                }
            } catch (e: Exception) {
                Log.e("SuccessStories", "Error with attachments: ${e.message}", e)
                // Fallback to regular JSON update
                updateWithJson(id, title, content, attachments, token, onSuccess, onError)
            }
        } else {
            // No attachments at all, use simple JSON
            updateWithJson(id, title, content, emptyList(), token, onSuccess, onError)
        }
    }

    private fun updateWithJson(
        id: String,
        title: String,
        content: String,
        attachments: List<String>,
        token: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val json = JSONObject().apply {
            put("title", title)
            put("content", content)

            // Handle attachments as a comma-separated string
            if (attachments.isNotEmpty()) {
                // Join the attachments with commas for the API
                put("attachments", attachments.joinToString(","))
            } else {
                put("attachments", "")
            }
        }

        Log.d("SuccessStories", "Update story payload: $json")

        val request = Request.Builder()
            .url("http://129.154.249.30:8091/api/collections/success_stories/records/$id")
            .addHeader("Authorization", "Bearer $token")
            .patch(json.toString().toRequestBody("application/json".toMediaType()))
            .build()

        OkHttpClient().newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("SuccessStories", "Error updating story: ${e.message}", e)
                kotlinx.coroutines.MainScope().launch {
                    onError("Error updating story: ${e.message}")
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                Log.d("SuccessStories", "Update story response: ${response.code} - $responseBody")

                kotlinx.coroutines.MainScope().launch {
                    if (response.isSuccessful) {
                        onSuccess()
                    } else {
                        onError("Error updating story: ${responseBody ?: response.code}")
                    }
                    response.body?.close()
                }
            }
        })
    }

    fun deleteSuccessStory(
        context: Context,
        id: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val sharedPrefs = getSharedPreferences(context) ?: return
        val userId = sharedPrefs.getString("userId", "") ?: ""
        val token = sharedPrefs.getString("token", "") ?: ""

        if (userId.isEmpty()) {
            onError("Missing user ID")
            return
        }

        val request = Request.Builder()
            .url("http://129.154.249.30:8091/api/collections/success_stories/records/$id")
            .addHeader("Authorization", "Bearer $token")
            .delete()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                kotlinx.coroutines.MainScope().launch {
                    onError("Error deleting story: ${e.message}")
                }
            }

            override fun onResponse(call: Call, response: Response) {
                kotlinx.coroutines.MainScope().launch {
                    if (response.isSuccessful) {
                        onSuccess()
                    } else {
                        onError("Error deleting story: ${response.body?.string()}")
                    }
                }
            }
        })
    }

    fun updateLikes(
        context: Context,
        storyId: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val sharedPrefs = getSharedPreferences(context) ?: return
        val currentUserId = sharedPrefs.getString("userId", "") ?: ""
        val token = sharedPrefs.getString("token", "") ?: ""

        if (currentUserId.isEmpty()) {
            onError("You must be logged in to like posts")
            return
        }

        // Check if already liked in our local tracker
        if (LikesManager.isLiked(storyId)) {
            onError("You already liked this post")
            return
        }

        Log.d("SuccessStories", "🔍 Attempting to like story ID: $storyId")

        // First get the current story to check its current likes count
        val getRequest = Request.Builder()
            .url("http://129.154.249.30:8091/api/collections/success_stories/records/$storyId")
            .addHeader("Authorization", "Bearer $token")
            .get()
            .build()

        OkHttpClient().newCall(getRequest).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("SuccessStories", "❌ Network error getting story: ${e.message}")
                kotlinx.coroutines.MainScope().launch {
                    onError("Network error: ${e.message}")
                }
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    val responseBody = response.body?.string()
                    Log.d("SuccessStories", "📥 Get story response: ${response.code} - $responseBody")

                    if (response.isSuccessful && responseBody != null) {
                        val storyJson = JSONObject(responseBody)
                        val currentLikes = storyJson.optInt("Likes", 0)
                        val newLikes = currentLikes + 1

                        Log.d("SuccessStories", "👍 Incrementing likes from $currentLikes to $newLikes")

                        // Create a minimal JSON with ONLY the Likes field
                        val updateJson = JSONObject().apply {
                            put("Likes", newLikes)
                        }

                        // Send PATCH request to update only the likes count
                        val patchRequest = Request.Builder()
                            .url("http://129.154.249.30:8091/api/collections/success_stories/records/$storyId")
                            .addHeader("Authorization", "Bearer $token")
                            .patch(updateJson.toString().toRequestBody("application/json".toMediaType()))
                            .build()

                        Log.d("SuccessStories", "📤 Sending PATCH request to update likes: ${updateJson.toString()}")

                        OkHttpClient().newCall(patchRequest).enqueue(object : Callback {
                            override fun onFailure(call: Call, e: IOException) {
                                Log.e("SuccessStories", "❌ Network error updating likes: ${e.message}")
                                kotlinx.coroutines.MainScope().launch {
                                    onError("Network error: ${e.message}")
                                }
                            }

                            override fun onResponse(call: Call, patchResponse: Response) {
                                val patchBody = patchResponse.body?.string()
                                Log.d("SuccessStories", "📥 Patch response: ${patchResponse.code} - $patchBody")

                                kotlinx.coroutines.MainScope().launch {
                                    if (patchResponse.isSuccessful) {
                                        // Mark as liked in our local tracker
                                        LikesManager.like(storyId)
                                        Log.d("SuccessStories", "✅ Successfully updated likes")
                                        onSuccess()
                                    } else {
                                        Log.e("SuccessStories", "❌ Failed to update likes: ${patchResponse.code}")
                                        onError("Could not update likes: ${patchResponse.message}")
                                    }
                                }

                                patchResponse.body?.close()
                            }
                        })
                    } else {
                        Log.e("SuccessStories", "❌ Failed to get story: ${response.code}")
                        kotlinx.coroutines.MainScope().launch {
                            onError("Could not retrieve story: ${response.message}")
                        }
                    }
                } catch (e: Exception) {
                    Log.e("SuccessStories", "❌ Error processing response: ${e.message}")
                    kotlinx.coroutines.MainScope().launch {
                        onError("Error processing response: ${e.message}")
                    }
                } finally {
                    response.body?.close()
                }
            }
        })
    }
}

// Add this class for local likes management
object LikesManager {
    // Map to track which stories have been liked by the current user
    private val likedStories = mutableMapOf<String, Boolean>()

    // Check if a story has been liked by the current user
    fun isLiked(storyId: String): Boolean {
        return likedStories[storyId] ?: false
    }

    // Mark a story as liked
    fun like(storyId: String) {
        likedStories[storyId] = true
    }

    // Clear likes (for logout)
    fun clear() {
        likedStories.clear()
    }
}

private fun fetchSuccessStories(
    onSuccess: (List<SuccessStory>) -> Unit,
    onError: (String) -> Unit,
    context: Context
) {
    val client = OkHttpClient()
    val sharedPrefs = context.getSharedPreferences("auth", Context.MODE_PRIVATE)
    val token = sharedPrefs.getString("token", "") ?: ""

    // Log the token length for debugging
    Log.d("SuccessStories", "⭐ Token length: ${token.length}")

    // Update to use correct expand field
    val apiUrl = "http://129.154.249.30:8091/api/collections/success_stories/records?expand=author"
    Log.d("SuccessStories", "⭐ Fetching stories from: $apiUrl")

    val request = Request.Builder()
        .url(apiUrl)
        .addHeader("Authorization", "Bearer $token")
        .get()
        .build()

    Log.d("SuccessStories", "📤 Sending request with headers: ${request.headers}")

    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            Log.e("SuccessStories", "❌ Network error: ${e.message}", e)
            kotlinx.coroutines.MainScope().launch {
                onError("Network error: ${e.message}")
            }
        }

        override fun onResponse(call: Call, response: Response) {
            try {
                val responseBody = response.body?.string()
                Log.d("SuccessStories", "📥 Response code: ${response.code}")
                Log.d("SuccessStories", "📥 Response headers: ${response.headers}")

                if (responseBody != null) {
                    Log.d("SuccessStories", "📥 Response body preview: ${responseBody.take(200)}")
                } else {
                    Log.e("SuccessStories", "❌ Empty response body")
                }

                if (response.isSuccessful && responseBody != null) {
                    try {
                        val jsonObject = JSONObject(responseBody)
                        val jsonArray = jsonObject.getJSONArray("items")
                        val stories = mutableListOf<SuccessStory>()

                        Log.d("SuccessStories", "✅ Found ${jsonArray.length()} stories")

                        // First log all the raw story IDs for debugging
                        val allIds = mutableListOf<String>()
                        for (i in 0 until jsonArray.length()) {
                            val storyObject = jsonArray.getJSONObject(i)
                            val rawId = storyObject.optString("id", "")
                            allIds.add(rawId)
                        }
                        Log.d("SuccessStories", "🔑 All story IDs in response: $allIds")

                        for (i in 0 until jsonArray.length()) {
                            val storyObject = jsonArray.getJSONObject(i)
                            try {
                                // Store the raw ID for debugging
                                val rawId = storyObject.optString("id", "")
                                Log.d("SuccessStories", "🔑 Processing story ID: '$rawId'")

                                // Log the raw story object for debugging
                                Log.d("SuccessStories", "📄 Processing story: ${storyObject.toString(2)}")

                                val story = SuccessStory.fromJson(storyObject)
                                Log.d("SuccessStories", "✅ Parsed story: id=${story.id}, title=${story.title}, author=${story.authorName}")
                                stories.add(story)
                            } catch (e: Exception) {
                                Log.e("SuccessStories", "❌ Error parsing story: ${e.message}")
                                e.printStackTrace()
                                continue
                            }
                        }

                        kotlinx.coroutines.MainScope().launch {
                            onSuccess(stories)
                        }
                    } catch (e: Exception) {
                        Log.e("SuccessStories", "❌ Error parsing JSON: ${e.message}")
                        e.printStackTrace()
                        kotlinx.coroutines.MainScope().launch {
                            onError("Error parsing data: ${e.message}")
                        }
                    }
                } else {
                    Log.e("SuccessStories", "❌ Error response: ${response.code} - $responseBody")
                    kotlinx.coroutines.MainScope().launch {
                        onError("Server error: ${response.code}")
                    }
                }
            } catch (e: Exception) {
                Log.e("SuccessStories", "❌ Unexpected error: ${e.message}")
                e.printStackTrace()
                kotlinx.coroutines.MainScope().launch {
                    onError("Unexpected error: ${e.message}")
                }
            } finally {
                response.body?.close()
            }
        }
    })
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

@Composable
fun StudentSuccessStoriesScreen(email: String, navController: NavController) {
    val viewModel = remember { SuccessStoriesViewModel() }
    var stories by remember { mutableStateOf<List<SuccessStory>>(emptyList()) }
    var sortByNewest by remember { mutableStateOf(true) }
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("auth", Context.MODE_PRIVATE) }
    val currentUserId = sharedPrefs.getString("userId", "") ?: ""

    LaunchedEffect(key1 = Unit) {
        fetchSuccessStories({ fetchedStories ->
            stories = fetchedStories
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
                text = "Success Stories",
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

        if (stories.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("No success stories found")
            }
        } else {
            Text(
                text = "Showing ${if (sortByNewest) "newest" else "oldest"} first",
                fontSize = 12.sp,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            val sortedStories = if (sortByNewest) {
                stories.sortedByDescending { it.created }
            } else {
                stories.sortedBy { it.created }
            }

            LazyColumn {
                items(sortedStories) { story ->
                    SuccessStoryItem(
                        story = story,
                        viewModel = viewModel,
                        currentUserId = currentUserId,
                        onStoryUpdated = {
                            fetchSuccessStories({ fetchedStories ->
                                stories = fetchedStories
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
fun AlumniSuccessStoriesScreen(email: String, navController: NavController) {
    val viewModel = remember { SuccessStoriesViewModel() }
    var stories by remember { mutableStateOf<List<SuccessStory>>(emptyList()) }
    var showAddDialog by remember { mutableStateOf(false) }
    var sortByNewest by remember { mutableStateOf(true) }
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("auth", Context.MODE_PRIVATE) }
    val currentUserId = sharedPrefs.getString("userId", "") ?: ""

    LaunchedEffect(key1 = Unit) {
        fetchSuccessStories({ fetchedStories ->
            stories = fetchedStories
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
                text = "Success Stories",
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
                        contentDescription = "Add Story",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        if (stories.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("No success stories found")
            }
        } else {
            Text(
                text = "Showing ${if (sortByNewest) "newest" else "oldest"} first",
                fontSize = 12.sp,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            val sortedStories = if (sortByNewest) {
                stories.sortedByDescending { it.created }
            } else {
                stories.sortedBy { it.created }
            }

            LazyColumn {
                items(sortedStories) { story ->
                    SuccessStoryItem(
                        story = story,
                        viewModel = viewModel,
                        currentUserId = currentUserId,
                        onStoryUpdated = {
                            fetchSuccessStories({ fetchedStories ->
                                stories = fetchedStories
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
        AddSuccessStoryDialog(
            onDismiss = { showAddDialog = false },
            onSave = { title, content, attachments ->
                viewModel.createSuccessStory(
                    context = context,
                    title = title,
                    content = content,
                    attachments = attachments,
                    onSuccess = {
                        showAddDialog = false
                        fetchSuccessStories({ fetchedStories ->
                            stories = fetchedStories
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
fun CollegeAdminSuccessStoriesScreen(email: String, navController: NavController) {
    val viewModel = remember { SuccessStoriesViewModel() }
    var stories by remember { mutableStateOf<List<SuccessStory>>(emptyList()) }
    var sortByNewest by remember { mutableStateOf(true) }
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("auth", Context.MODE_PRIVATE) }
    val currentUserId = sharedPrefs.getString("userId", "") ?: ""

    LaunchedEffect(key1 = Unit) {
        fetchSuccessStories({ fetchedStories ->
            stories = fetchedStories
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
                text = "Success Stories",
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

        if (stories.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("No success stories found")
            }
        } else {
            Text(
                text = "Showing ${if (sortByNewest) "newest" else "oldest"} first",
                fontSize = 12.sp,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            val sortedStories = if (sortByNewest) {
                stories.sortedByDescending { it.created }
            } else {
                stories.sortedBy { it.created }
            }

            LazyColumn {
                items(sortedStories) { story ->
                    SuccessStoryItem(
                        story = story,
                        viewModel = viewModel,
                        currentUserId = currentUserId,
                        onStoryUpdated = {
                            fetchSuccessStories({ fetchedStories ->
                                stories = fetchedStories
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
fun SuccessStoryItem(
    story: SuccessStory,
    viewModel: SuccessStoriesViewModel,
    currentUserId: String,
    onStoryUpdated: () -> Unit
) {
    val context = LocalContext.current
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var isLikeInProgress by remember { mutableStateOf(false) }
    var likeCount by remember { mutableStateOf(story.Likes) }

    // Check if current user already liked this post (using local tracking)
    val alreadyLiked = remember(story.id) {
        LikesManager.isLiked(story.id)
    }

    // Debug log to check story ID and likes
    LaunchedEffect(key1 = story.id) {
        Log.d("SuccessStories", "⭐ Story ID: '${story.id}' with ${story.Likes} likes, author: ${story.author}")
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
            // Author info with profile-like display
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Profile photo placeholder (circular)
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary,
                            shape = androidx.compose.foundation.shape.CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = story.authorName.firstOrNull()?.uppercase() ?: "U",
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = story.authorName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = story.authorRole.capitalize(),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                if (story.author == currentUserId) {
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
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Story title
            Text(
                text = story.title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Story content
            Text(
                text = story.content,
                fontSize = 16.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Attachments
            if (story.attachments.isNotEmpty()) {
                Text(
                    text = "Attachments:",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )

                story.attachments.forEach { attachment ->
                    // Clean the attachment string to remove brackets and quotes
                    val cleanAttachment = attachment.replace("[", "")
                        .replace("]", "")
                        .replace("\"", "")
                        .trim()

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                            .clickable {
                                try {
                                    val attachmentUrl = "http://129.154.249.30:8091/api/files/success_stories/${story.id}/$cleanAttachment"
                                    Log.d("SuccessStories", "🔗 Opening attachment URL: $attachmentUrl")
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(attachmentUrl))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Log.e("SuccessStories", "❌ Error opening attachment: ${e.message}", e)
                                    Toast.makeText(context, "Cannot open attachment: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                    ) {
                        Icon(
                            imageVector = Icons.Default.AttachFile,
                            contentDescription = "Attachment",
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = cleanAttachment,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
            }

            // Like and date info
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = {
                            if (!isLikeInProgress && !alreadyLiked) {
                                isLikeInProgress = true
                                // Don't log the ID in a way that might add quotes
                                val exactId = story.id
                                Log.d("SuccessStories", "👍 Attempting to like story with ID: $exactId")
                                viewModel.updateLikes(
                                    context = context,
                                    storyId = exactId,
                                    onSuccess = {
                                        likeCount += 1
                                        isLikeInProgress = false
                                        Toast.makeText(context, "Successfully liked the post!", Toast.LENGTH_SHORT).show()
                                        onStoryUpdated()
                                    },
                                    onError = { error ->
                                        isLikeInProgress = false
                                        Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                                        Log.e("SuccessStories", "Like error: $error")
                                    }
                                )
                            } else if (alreadyLiked) {
                                Toast.makeText(context, "You already liked this post", Toast.LENGTH_SHORT).show()
                            } else if (story.author == currentUserId) {
                                Toast.makeText(context, "You can't like your own post", Toast.LENGTH_SHORT).show()
                            }
                        },
                        enabled = !isLikeInProgress && !alreadyLiked && story.author != currentUserId
                    ) {
                        if (isLikeInProgress) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                Icons.Default.ThumbUp,
                                contentDescription = "Like",
                                tint = BlueBrandColor
                            )
                        }
                    }
                    Text(
                        text = "$likeCount ${if (likeCount == 1) "like" else "likes"}",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }

                Text(
                    text = formatDate(story.created),
                    fontSize = 12.sp,
                    color = Color.Gray,
                    fontStyle = FontStyle.Italic
                )
            }
        }
    }

    if (showEditDialog) {
        EditSuccessStoryDialog(
            story = story,
            onDismiss = { showEditDialog = false },
            onSave = { title, content, attachments ->
                viewModel.updateSuccessStory(
                    context = context,
                    id = story.id,
                    title = title,
                    content = content,
                    attachments = attachments,
                    onSuccess = {
                        showEditDialog = false
                        onStoryUpdated()
                    },
                    onError = { error ->
                        Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                    }
                )
            }
        )
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete Story") },
            text = { Text("Are you sure you want to delete this story?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteSuccessStory(
                            context = context,
                            id = story.id,
                            onSuccess = {
                                showDeleteConfirmation = false
                                onStoryUpdated()
                            },
                            onError = { error ->
                                Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun EditSuccessStoryDialog(
    story: SuccessStory,
    onDismiss: () -> Unit,
    onSave: (String, String, List<String>) -> Unit
) {
    var title by remember { mutableStateOf(story.title) }
    var content by remember { mutableStateOf(story.content) }

    // Clean up the attachment names from PocketBase format
    var existingAttachments by remember {
        mutableStateOf(
            story.attachments.map { attachment ->
                attachment.replace("[", "")
                    .replace("]", "")
                    .replace("\"", "")
                    .trim()
            }
        )
    }

    var newAttachments by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var uploadedAttachments by remember { mutableStateOf<List<String>>(emptyList()) }

    val context = LocalContext.current
    var isUploading by remember { mutableStateOf(false) }
    var uploadProgress by remember { mutableStateOf(0f) }

    // File picker launcher
    val fileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            // Add the URI to our attachments list
            newAttachments = newAttachments + it
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Success Story") },
        text = {
            Column {
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
                        .height(120.dp),
                    maxLines = 5
                )

                // Attachment section
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Attachments",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Button(
                        onClick = {
                            // Launch file picker
                            fileLauncher.launch("*/*") // Accept any file type
                        },
                        modifier = Modifier.padding(start = 8.dp),
                        enabled = !isUploading,
                        colors = ButtonDefaults.buttonColors(containerColor = BlueBrandColor)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.AttachFile,
                                contentDescription = "Attach file",
                                modifier = Modifier.size(16.dp)
                            )
                            Text("Add File")
                        }
                    }
                }

                // Display existing attachments
                if (existingAttachments.isNotEmpty()) {
                    Text(
                        "Current attachments:",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .background(
                                Color.LightGray.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(8.dp)
                    ) {
                        existingAttachments.forEachIndexed { index, attachment ->
                            // Clean the attachment name if needed
                            val fileName = attachment.split("/").lastOrNull() ?: attachment
                            val fileIcon = getFileIcon(fileName)

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        fileIcon,
                                        contentDescription = "File",
                                        tint = BlueBrandColor,
                                        modifier = Modifier
                                            .size(20.dp)
                                            .padding(end = 8.dp)
                                    )
                                    Text(
                                        text = fileName,
                                        fontSize = 14.sp,
                                        maxLines = 1
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        existingAttachments = existingAttachments.toMutableList().apply {
                                            removeAt(index)
                                        }
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Remove attachment",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Display newly added attachments
                if (newAttachments.isNotEmpty()) {
                    Text(
                        "New attachments:",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .background(
                                BlueBrandColor.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(8.dp)
                    ) {
                        newAttachments.forEachIndexed { index, uri ->
                            val fileName = getFileName(context, uri) ?: "Unknown file"
                            val fileIcon = getFileIcon(fileName)

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        fileIcon,
                                        contentDescription = "File",
                                        tint = BlueBrandColor,
                                        modifier = Modifier
                                            .size(20.dp)
                                            .padding(end = 8.dp)
                                    )
                                    Text(
                                        text = fileName,
                                        fontSize = 14.sp,
                                        maxLines = 1
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        newAttachments = newAttachments.toMutableList().apply {
                                            removeAt(index)
                                        }
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Remove attachment",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                if (isUploading) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            progress = { uploadProgress },
                            color = BlueBrandColor
                        )
                        Text(
                            "Uploading files... ${(uploadProgress * 100).toInt()}%",
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (title.isBlank() || content.isBlank()) {
                        Toast.makeText(context, "Title and content are required", Toast.LENGTH_SHORT).show()
                        return@TextButton
                    }

                    // Only upload if there are new attachments
                    if (newAttachments.isNotEmpty()) {
                        isUploading = true
                        val totalFiles = newAttachments.size
                        var filesUploaded = 0

                        newAttachments.forEach { uri ->
                            uploadAttachment(
                                context = context,
                                uri = uri,
                                onSuccess = { fileUrl ->
                                    uploadedAttachments = uploadedAttachments + fileUrl
                                    filesUploaded++
                                    uploadProgress = filesUploaded.toFloat() / totalFiles

                                    // If all files are uploaded, save the story
                                    if (filesUploaded == totalFiles) {
                                        isUploading = false
                                        // Combine existing and new attachments
                                        val allAttachments = existingAttachments + uploadedAttachments
                                        onSave(title, content, allAttachments)
                                    }
                                },
                                onError = { error ->
                                    isUploading = false
                                    Toast.makeText(context, "Upload failed: $error", Toast.LENGTH_LONG).show()
                                }
                            )
                        }
                    } else {
                        // No new attachments, just save with existing ones
                        onSave(title, content, existingAttachments)
                    }
                },
                enabled = !isUploading,
                colors = ButtonDefaults.textButtonColors(contentColor = BlueBrandColor)
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isUploading,
                colors = ButtonDefaults.textButtonColors(contentColor = Color.Gray)
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun AddSuccessStoryDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, List<String>) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var attachments by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var uploadedAttachments by remember { mutableStateOf<List<String>>(emptyList()) }
    val context = LocalContext.current
    var isUploading by remember { mutableStateOf(false) }
    var uploadProgress by remember { mutableStateOf(0f) }

    // File picker launcher
    val fileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            // Add the URI to our attachments list
            attachments = attachments + it
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Share Your Success Story") },
        text = {
            Column {
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
                        .height(120.dp),
                    maxLines = 5
                )

                // Attachment section
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Attachments",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Button(
                        onClick = {
                            // Launch file picker
                            fileLauncher.launch("*/*") // Accept any file type
                        },
                        modifier = Modifier.padding(start = 8.dp),
                        enabled = !isUploading,
                        colors = ButtonDefaults.buttonColors(containerColor = BlueBrandColor)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.AttachFile,
                                contentDescription = "Attach file",
                                modifier = Modifier.size(16.dp)
                            )
                            Text("Attach File")
                        }
                    }
                }

                // Show selected files
                if (attachments.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .background(
                                Color.LightGray.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(8.dp)
                    ) {
                        attachments.forEachIndexed { index, uri ->
                            val fileName = getFileName(context, uri) ?: "Unknown file"
                            val fileIcon = getFileIcon(fileName)

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        fileIcon,
                                        contentDescription = "File",
                                        tint = BlueBrandColor,
                                        modifier = Modifier
                                            .size(20.dp)
                                            .padding(end = 8.dp)
                                    )
                                    Text(
                                        text = fileName,
                                        fontSize = 14.sp,
                                        maxLines = 1
                                    )
                                }

                                if (!isUploading) {
                                    IconButton(
                                        onClick = {
                                            attachments = attachments.toMutableList().apply {
                                                removeAt(index)
                                            }
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Remove attachment",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (isUploading) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            progress = { uploadProgress }
                        )
                        Text(
                            "Uploading files... ${(uploadProgress * 100).toInt()}%",
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (title.isBlank() || content.isBlank()) {
                        Toast.makeText(context, "Title and content are required", Toast.LENGTH_SHORT).show()
                        return@TextButton
                    }

                    // Only upload if there are attachments and they haven't been uploaded yet
                    if (attachments.isNotEmpty() && uploadedAttachments.isEmpty()) {
                        isUploading = true
                        val totalFiles = attachments.size
                        var filesUploaded = 0

                        attachments.forEach { uri ->
                            uploadAttachment(
                                context = context,
                                uri = uri,
                                onSuccess = { fileUrl ->
                                    uploadedAttachments = uploadedAttachments + fileUrl
                                    filesUploaded++
                                    uploadProgress = filesUploaded.toFloat() / totalFiles

                                    // If all files are uploaded, save the story
                                    if (filesUploaded == totalFiles) {
                                        isUploading = false
                                        onSave(title, content, uploadedAttachments)
                                    }
                                },
                                onError = { error ->
                                    isUploading = false
                                    Toast.makeText(context, "Upload failed: $error", Toast.LENGTH_LONG).show()
                                }
                            )
                        }
                    } else {
                        // No attachments or already uploaded, just save the story
                        onSave(title, content, uploadedAttachments)
                    }
                },
                enabled = !isUploading
            ) {
                Text("Share")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isUploading) {
                Text("Cancel")
            }
        }
    )
}

// Helper functions for file handling
fun getFileName(context: Context, uri: Uri): String? {
    var result: String? = null
    if (uri.scheme == "content") {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0) {
                    result = it.getString(nameIndex)
                }
            }
        }
    }
    if (result == null) {
        result = uri.path
        val cut = result?.lastIndexOf('/')
        if (cut != -1) {
            result = result?.substring(cut!! + 1)
        }
    }
    return result
}

fun getFileIcon(fileName: String): ImageVector {
    return when {
        fileName.endsWith(".pdf", ignoreCase = true) -> Icons.Default.PictureAsPdf
        fileName.endsWith(".jpg", ignoreCase = true) ||
                fileName.endsWith(".jpeg", ignoreCase = true) ||
                fileName.endsWith(".png", ignoreCase = true) ||
                fileName.endsWith(".gif", ignoreCase = true) -> Icons.Default.Image
        else -> Icons.Default.AttachFile
    }
}

// Simplified uploadAttachment function to directly use the form field name expected by PocketBase
fun uploadAttachment(
    context: Context,
    uri: Uri,
    onSuccess: (String) -> Unit,
    onError: (String) -> Unit
) {
    val token = context.getSharedPreferences("auth", Context.MODE_PRIVATE).getString("token", "") ?: ""

    try {
        // Get file name and type
        val contentResolver = context.contentResolver
        val fileName = getFileName(context, uri) ?: "file_${System.currentTimeMillis()}"
        val mimeType = contentResolver.getType(uri) ?: "application/octet-stream"

        // Generate a unique filename to avoid conflicts
        val uniqueFileName = "${System.currentTimeMillis()}_$fileName"

        Log.d("SuccessStories", "⭐ Uploading attachment: $uniqueFileName, type: $mimeType")

        // Read file content
        val inputStream = contentResolver.openInputStream(uri)
        if (inputStream == null) {
            onError("Could not open file")
            return
        }

        // Create temp file
        val tempFile = File(context.cacheDir, uniqueFileName)
        val outputStream = FileOutputStream(tempFile)
        inputStream.copyTo(outputStream)
        inputStream.close()
        outputStream.close()

        // Verify file exists and log details
        if (tempFile.exists()) {
            Log.d("SuccessStories", "✅ File saved to cache: ${tempFile.absolutePath}, size: ${tempFile.length()} bytes")
        } else {
            Log.e("SuccessStories", "❌ Failed to save file to cache: ${tempFile.absolutePath}")
        }

        // Return the filename - IMPORTANT: Don't delete the file, it needs to stay in cache
        // for the multipart request to use it
        kotlinx.coroutines.MainScope().launch {
            onSuccess(uniqueFileName)
            // DO NOT delete the file here - it needs to be available for the multipart request to use it
        }
    } catch (e: Exception) {
        Log.e("SuccessStories", "❌ Error preparing file: ${e.message}", e)
        onError("Error preparing file: ${e.message}")
    }
}
