package com.example.dbit_almaconnect3.data.repository

import android.util.Log
import com.example.dbit_almaconnect3.data.api.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.MultipartBody
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.io.File
import com.example.dbit_almaconnect3.data.api.RetrofitClient

// Define a data class to use for job updates
data class JobUpdateRequest(
    val status: Boolean? = null,
    val title: String? = null,
    val description: String? = null,
    val company: String? = null
)

class JobPortalRepository {

    // Use the RetrofitClient instance with the custom Gson configuration.
    private val service = RetrofitClient.instance.create(JobPortalService::class.java)

    // Flarum details
    private val flarumUrl = "http://129.154.249.30:8080"
    private val flarumApiKey = "9bf5f86b94d5873bf57808689182723dc93c6fdc"

    // OkHttp client for Flarum requests
    private val flarumClient = OkHttpClient()

    suspend fun getJobs(): List<JobPostingResponse>? {
        val response = service.getJobs()
        return if (response.isSuccessful) {
            response.body()?.items
        } else {
            Log.e("JobPortalRepository", "getJobs failed: code=${response.code()}, body=${response.errorBody()?.string()}")
            null
        }
    }

    suspend fun postJob(title: String, description: String, company: String, postedBy: String): JobPostingResponse? = withContext(Dispatchers.IO) {
        val discussionLink = createFlarumDiscussion(title, description)
        val postedAt = getCurrentTimestamp()
        if (discussionLink == null) {
            Log.e("Flarum", "Discussion creation failed or returned null.")
        }
        val request = JobPostingRequest(
            title = title,
            description = description,
            company = company,
            postedBy = postedBy,
            postedAt = postedAt,
            status = false, // Default is false until admin verifies
            discussionLink = discussionLink
        )
        val response = service.postJob(request)
        if (response.isSuccessful) response.body() else {
            Log.e("JobPortalRepository", "postJob failed: code=${response.code()}, body=${response.errorBody()?.string()}")
            null
        }
    }

    suspend fun applyForJob(jobId: String, applicant: String, resumeFile: File): ApplicationResponse? = withContext(Dispatchers.IO) {
        val jobRequest = jobId.toRequestBody("text/plain".toMediaTypeOrNull())
        val appliedByRequest = applicant.toRequestBody("text/plain".toMediaTypeOrNull())
        val appliedAt = getCurrentTimestamp().toRequestBody("text/plain".toMediaTypeOrNull())
        val status = "pending".toRequestBody("text/plain".toMediaTypeOrNull())

        val fileRequestBody = resumeFile.asRequestBody("application/pdf".toMediaTypeOrNull())
        val resumePart = MultipartBody.Part.createFormData("resume", resumeFile.name, fileRequestBody)

        val response = service.applyForJobMultipart(jobRequest, appliedByRequest, appliedAt, status, resumePart)
        return@withContext if (response.isSuccessful) {
            val appResponse = response.body()
            appResponse?.let { application ->
                val resumeList = application.resume
                if (resumeList != null && resumeList.isNotEmpty()) {
                    val firstResume = resumeList.first()
                    if (!firstResume.startsWith("http", ignoreCase = true)) {
                        val collectionId = "pbc_2689671926"
                        val folderId = "YOUR_FOLDER_ID" // Replace with actual folder ID
                        val baseUrl = "http://129.154.249.30:8091"
                        val fullUrl = "$baseUrl/api/files/$collectionId/$folderId/$firstResume"
                        return@withContext application.copy(resume = listOf(fullUrl))
                    }
                }
                application
            }
        } else {
            Log.e("JobPortalRepository", "applyForJob failed: code=${response.code()}, body=${response.errorBody()?.string()}")
            null
        }
    }

    suspend fun getApplicationsForJob(jobTitle: String): List<ApplicationResponse>? = withContext(Dispatchers.IO) {
        val filter = "job.title='$jobTitle'"
        val response = service.getApplicationsForJob(filter)
        return@withContext if (response.isSuccessful) {
            response.body()?.items
        } else {
            Log.e("JobPortalRepository", "getApplicationsForJob failed: code=${response.code()}, body=${response.errorBody()?.string()}")
            null
        }
    }

    suspend fun deleteApplication(applicationId: String): Boolean = withContext(Dispatchers.IO) {
        val response = service.deleteApplication(applicationId)
        response.isSuccessful
    }

    suspend fun deleteJobAndDiscussion(jobId: String, discussionLink: String?): Boolean = withContext(Dispatchers.IO) {
        discussionLink?.let { link ->
            val regex = Regex("/d/(\\d+)")
            val match = regex.find(link)
            val discussionId = match?.groupValues?.getOrNull(1)
            if (!discussionId.isNullOrEmpty()) {
                val deleteRequest = Request.Builder()
                    .url("$flarumUrl/api/discussions/$discussionId")
                    .delete()
                    .header("Authorization", "Token $flarumApiKey")
                    .build()
                val deleteResponse = flarumClient.newCall(deleteRequest).execute()
                deleteResponse.use { r ->
                    if (!r.isSuccessful) {
                        Log.e("Flarum", "Failed to delete discussion: code=${r.code}, body=${r.body?.string()}")
                    }
                }
            }
        }
        val response = service.deleteJob(jobId)
        response.isSuccessful
    }

    // Helper function to get current datetime in ISO 8601 format.
    private fun getCurrentTimestamp(): String {
        return ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
    }

    // Helper to get all tags from Flarum.
    suspend fun getTags(): List<Tag>? = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("$flarumUrl/api/tags?page[limit]=100")
            .header("Authorization", "Token $flarumApiKey")
            .build()
        val response = flarumClient.newCall(request).execute()
        if (response.isSuccessful) {
            val responseBody = response.body?.string() ?: return@withContext null
            val jsonObj = JSONObject(responseBody)
            val dataArray = jsonObj.getJSONArray("data")
            val tags = mutableListOf<Tag>()
            for (i in 0 until dataArray.length()) {
                val tagObj = dataArray.getJSONObject(i)
                val id = tagObj.getString("id")
                val attributes = tagObj.getJSONObject("attributes")
                val name = attributes.getString("name")
                val slug = attributes.optString("slug", "")
                val color = attributes.optString("color", null)
                val parentId = tagObj.optJSONObject("relationships")
                    ?.optJSONObject("parent")
                    ?.optJSONObject("data")
                    ?.optString("id")
                tags.add(Tag(id, name, slug, color, parentId))
            }
            tags
        } else {
            Log.e("JobPortalRepository", "Failed to get tags: ${response.code}")
            null
        }
    }

    // Helper to get the primary "Job Internship Portal" tag's ID.
    // Primary tag name is "Job Internship Portal" and slug is "job-internship-portal".
    private suspend fun getPrimaryJobInternshipPortalTagId(): String? = withContext(Dispatchers.IO) {
        val tags = getTags()
        if (tags != null) {
            for (tag in tags) {
                if (tag.slug == "job-internship-portal") {
                    Log.d("JobPortalRepository", "Found primary tag ID: ${tag.id}")
                    return@withContext tag.id
                }
            }
        }
        Log.e("JobPortalRepository", "Primary tag 'Job Internship Portal' not found.")
        null
    }

    // Helper to create a secondary tag for a particular job.
    // This tag will have the job's title as its name and will be nested under the primary "Job Internship Portal" tag.
    private suspend fun createSecondaryTagForJob(jobTitle: String, color: String? = "#add8e6"): Tag? = withContext(Dispatchers.IO) {
        val parentTagId = getPrimaryJobInternshipPortalTagId()
        if (parentTagId == null) {
            Log.e("JobPortalRepository", "Primary tag 'Job Internship Portal' not found.")
            return@withContext null
        }
        val slug = jobTitle.lowercase().trim().replace("\\s+".toRegex(), "-")
        val payload = JSONObject().apply {
            put("data", JSONObject().apply {
                put("type", "tags")
                put("attributes", JSONObject().apply {
                    put("name", jobTitle)
                    put("slug", slug)
                    put("description", "")
                    put("color", color?.trim()?.lowercase() ?: "#ffffff")
                })
                put("relationships", JSONObject().apply {
                    put("parent", JSONObject().apply {
                        put("data", JSONObject().apply {
                            put("type", "tags")
                            put("id", parentTagId)
                        })
                    })
                })
            })
        }
        val body = payload.toString().toRequestBody("application/json".toMediaTypeOrNull())
        val request = Request.Builder()
            .url("$flarumUrl/api/tags")
            .post(body)
            .header("Content-Type", "application/json")
            .header("Authorization", "Token $flarumApiKey")
            .build()
        val response = flarumClient.newCall(request).execute()
        if (response.isSuccessful) {
            val responseBody = response.body?.string() ?: return@withContext null
            val dataObj = JSONObject(responseBody).getJSONObject("data")
            val id = dataObj.getString("id")
            val attributes = dataObj.getJSONObject("attributes")
            val tagName = attributes.getString("name")
            val tagSlug = attributes.optString("slug", slug)
            val tagColor = attributes.optString("color", "#ffffff")
            Tag(id, tagName, tagSlug, tagColor, parentTagId)
        } else {
            Log.e("JobPortalRepository", "Failed to create secondary tag: code=${response.code}, body=${response.body?.string()}")
            null
        }
    }

    // Updated discussion creation function.
    // 1. It creates a secondary tag dedicated to that job using the job title.
    // 2. It creates the discussion and attaches that secondary tag.
    // 3. It returns the URL for the secondary tag page, so the user is taken there.
    private suspend fun createFlarumDiscussion(title: String, content: String): String? = withContext(Dispatchers.IO) {
        // Ensure title is at least 3 characters
        val validTitle = if (title.length < 3) {
            title.padEnd(3, ' ')
        } else {
            title
        }

        // Create a secondary tag for this job.
        val secondaryTag = createSecondaryTagForJob(validTitle)
        if (secondaryTag == null) {
            Log.e("JobPortalRepository", "Failed to create secondary tag for job: $validTitle")
            return@withContext null
        }
        val json = JSONObject().apply {
            put("data", JSONObject().apply {
                put("type", "discussions")
                put("attributes", JSONObject().apply {
                    put("title", validTitle)
                    put("content", content)
                })
                // Attach the secondary tag.
                put("relationships", JSONObject().apply {
                    put("tags", JSONObject().apply {
                        put("data", JSONArray().apply {
                            put(JSONObject().apply {
                                put("type", "tags")
                                put("id", secondaryTag.id)
                            })
                        })
                    })
                })
            })
        }
        val body = json.toString().toRequestBody("application/json".toMediaTypeOrNull())
        val request = Request.Builder()
            .url("$flarumUrl/api/discussions")
            .post(body)
            .header("Content-Type", "application/json")
            .header("Authorization", "Token $flarumApiKey")
            .build()
        val response = flarumClient.newCall(request).execute()
        response.use { r ->
            if (r.isSuccessful) {
                // Return the secondary tag's page URL.
                "$flarumUrl/t/${secondaryTag.slug}"
            } else {
                Log.e("Flarum", "Discussion creation failed: code=${r.code}, body=${r.body?.string()}")
                null
            }
        }
    }

    suspend fun updateJob(jobId: String, updateData: Map<String, Any>): JobPostingResponse? = withContext(Dispatchers.IO) {
        // Convert Map to JobUpdateRequest
        val request = JobUpdateRequest(
            status = updateData["status"] as? Boolean
        )
        val response = service.updateJob(jobId, request)
        return@withContext if (response.isSuccessful) {
            response.body()
        } else {
            Log.e("JobPortalRepository", "updateJob failed: code=${response.code()}, body=${response.errorBody()?.string()}")
            null
        }
    }
}
