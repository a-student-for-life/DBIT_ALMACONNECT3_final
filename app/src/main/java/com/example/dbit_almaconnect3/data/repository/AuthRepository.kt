package com.example.dbit_almaconnect3.data.repository

import android.content.Context
import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import com.example.dbit_almaconnect3.App

class AuthRepository {

    private val pocketBaseUrl = "http://129.154.249.30:8091"
    private val flarumUrl = "http://129.154.249.30:8080"
    private val flarumApiKey = "9bf5f86b94d5873bf57808689182723dc93c6fdc"
    private val client = OkHttpClient()

    // Helper function to validate email domain
    private fun isValidDBITEmail(email: String, role: String): Boolean {
        return when (role) {
            "student" -> email.endsWith("@dbit.in", ignoreCase = true)
            else -> true // Allow any email domain for alumni and college admin
        }
    }

    // Sign-up for both PocketBase and Flarum
    fun signupUser(
        email: String,
        password: String,
        role: String,
        onSuccess: (String, String) -> Unit, // (email, role)
        onError: (String) -> Unit
    ) {
        // Validate email domain based on role
        if (!isValidDBITEmail(email, role)) {
            onError("Students must use @dbit.in email addresses")
            return
        }

        val pbUrl = "$pocketBaseUrl/api/collections/users/records"
        // Use the base username for both student and alumni.
        val baseUsername = email.split("@")[0].replace("[^a-zA-Z0-9_]".toRegex(), "_")
        val username = baseUsername

        val pbJson = JSONObject().apply {
            put("email", email)
            put("password", password)
            put("passwordConfirm", password)
            put("username", username)
            put("role", role)
        }
        // Replaced RequestBody.create(...) with toRequestBody(...)
        val pbBody = pbJson.toString().toRequestBody("application/json".toMediaTypeOrNull())

        val pbRequest = Request.Builder()
            .url(pbUrl)
            .post(pbBody)
            .header("Content-Type", "application/json")
            .build()

        client.newCall(pbRequest).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                onError("PocketBase Signup Failed: ${e.message}")
            }

            override fun onResponse(call: okhttp3.Call, response: Response) {
                val responseBodyString = response.body?.string()
                if (response.isSuccessful) {
                    // Pass the same username to Flarum.
                    signupFlarumUser(email, password, role, username, onSuccess, onError)
                } else {
                    onError("PocketBase Signup failed: $responseBodyString")
                }
            }
        })
    }

    // Flarum sign-up with username passed in.
    private fun signupFlarumUser(
        email: String,
        password: String,
        role: String,
        username: String,
        onSuccess: (String, String) -> Unit,
        onError: (String) -> Unit
    ) {
        val flUrl = "$flarumUrl/api/users"
        val flJson = JSONObject().apply {
            put("data", JSONObject().apply {
                put("type", "users")
                put("attributes", JSONObject().apply {
                    put("username", username) // Use the same username generated earlier.
                    put("email", email)
                    put("password", password)
                    put("isEmailConfirmed", true)
                })
            })
        }
        // Replaced RequestBody.create(...) with toRequestBody(...)
        val flBody = flJson.toString().toRequestBody("application/json".toMediaTypeOrNull())

        val flRequest = Request.Builder()
            .url(flUrl)
            .post(flBody)
            .header("Content-Type", "application/json")
            .header("Authorization", "Token $flarumApiKey")
            .build()

        client.newCall(flRequest).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                onError("Flarum Signup Failed: ${e.message}")
            }

            override fun onResponse(call: okhttp3.Call, response: Response) {
                if (response.isSuccessful) {
                    onSuccess(email, role)
                } else {
                    val errorBody = response.body?.string()
                    Log.e("FlarumSignup", "Signup error: $errorBody")
                    onError("Flarum Signup failed: $errorBody")
                }
            }
        })
    }

    // Login to PocketBase and then to Flarum
    fun loginUser(
        email: String,
        password: String,
        onSuccess: (String, String) -> Unit, // (email, role)
        onError: (String) -> Unit
    ) {
        // Get the role from the email prefix to determine validation
        val role = if (email.endsWith("@dbit.in", ignoreCase = true)) "student" else "alumni"
        
        // Validate email domain based on role
        if (!isValidDBITEmail(email, role)) {
            onError("Students must use @dbit.in email addresses")
            return
        }

        val pbUrl = "$pocketBaseUrl/api/collections/users/auth-with-password"
        val pbJson = JSONObject().apply {
            put("identity", email)
            put("password", password)
        }
        // Replaced RequestBody.create(...) with toRequestBody(...)
        val pbBody = pbJson.toString().toRequestBody("application/json".toMediaTypeOrNull())

        val pbRequest = Request.Builder()
            .url(pbUrl)
            .post(pbBody)
            .header("Content-Type", "application/json")
            .build()

        client.newCall(pbRequest).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                onError("PocketBase Login Failed: ${e.message}")
            }

            override fun onResponse(call: okhttp3.Call, response: Response) {
                val responseBodyString = response.body?.string()
                try {
                    val jsonResponse = JSONObject(responseBodyString ?: "")
                    val token = jsonResponse.optString("token")
                    // Extract role from the PocketBase record.
                    val recordObj = jsonResponse.getJSONObject("record")
                    val role = recordObj.optString("role")
                    val userId = recordObj.optString("id", "")
                    val username = recordObj.optString("username", email.substringBefore("@"))

                    if (token.isNotEmpty()) {
                        // Save authentication details to SharedPreferences
                        saveAuthDetails(email, token, role, username, userId)
                        
                        // Log authentication success
                        Log.d("AuthRepository", "Login successful for $email with role $role")
                        Log.d("AuthRepository", "Saved auth token: ${token.take(10)}...")
                        
                        // Pass the role from PocketBase into loginFlarum
                        loginFlarum(email, password, role, onSuccess, onError)
                    } else {
                        onError("Login failed: Missing token")
                    }
                } catch (e: Exception) {
                    onError("Login Parsing Failed: ${e.message}")
                }
            }
        })
    }

    // Save authentication details to SharedPreferences
    private fun saveAuthDetails(email: String, token: String, role: String, name: String, userId: String) {
        try {
            val sharedPrefs = App.instance.getSharedPreferences("auth", Context.MODE_PRIVATE)
            val editor = sharedPrefs.edit()
            
            // Save all relevant user information
            editor.putString("email", email)
            editor.putString("token", token)
            editor.putString("role", role)
            editor.putString("name", name)
            editor.putString("userId", userId)
            
            // Save the edit time for debugging
            editor.putLong("lastLogin", System.currentTimeMillis())
            
            // Commit changes synchronously to ensure they're saved
            val success = editor.commit()
            
            if (success) {
                Log.d("AuthRepository", "Auth details saved successfully for $email")
            } else {
                Log.e("AuthRepository", "Failed to save auth details for $email")
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error saving auth details: ${e.message}")
        }
    }

    // Helper method to clear auth details on logout
    private fun clearAuthDetails() {
        try {
            val sharedPrefs = App.instance.getSharedPreferences("auth", Context.MODE_PRIVATE)
            val editor = sharedPrefs.edit()
            editor.clear()
            editor.commit()
            Log.d("AuthRepository", "Auth details cleared on logout")
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error clearing auth details: ${e.message}")
        }
    }

    // Updated Flarum Login function to accept the role from PocketBase.
    private fun loginFlarum(
        email: String,
        password: String,
        role: String,
        onSuccess: (String, String) -> Unit,
        onError: (String) -> Unit
    ) {
        val flUrl = "$flarumUrl/api/token"
        val flJson = JSONObject().apply {
            put("identification", email)
            put("password", password)
            put("remember", 1)
        }
        // Replaced RequestBody.create(...) with toRequestBody(...)
        val flBody = flJson.toString().toRequestBody("application/json".toMediaTypeOrNull())

        val flRequest = Request.Builder()
            .url(flUrl)
            .post(flBody)
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .build()

        client.newCall(flRequest).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                onError("Flarum Login Failed: ${e.message}")
            }

            override fun onResponse(call: okhttp3.Call, response: Response) {
                if (response.isSuccessful) {
                    // Pass along the actual role retrieved from PocketBase.
                    onSuccess(email, role)
                } else {
                    onError("Flarum Login Failed: ${response.body?.string()}")
                }
            }
        })
    }

    // Legacy Flarum Login (if needed for backward compatibility)
    private fun loginFlarum(
        email: String,
        password: String,
        onSuccess: (String, String) -> Unit,
        onError: (String) -> Unit
    ) {
        val flUrl = "$flarumUrl/api/token"
        val flJson = JSONObject().apply {
            put("identification", email)
            put("password", password)
            put("remember", 1)
        }
        // Replaced RequestBody.create(...) with toRequestBody(...)
        val flBody = flJson.toString().toRequestBody("application/json".toMediaTypeOrNull())

        val flRequest = Request.Builder()
            .url(flUrl)
            .post(flBody)
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .build()

        client.newCall(flRequest).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                onError("Flarum Login Failed: ${e.message}")
            }

            override fun onResponse(call: okhttp3.Call, response: Response) {
                if (response.isSuccessful) {
                    onSuccess(email, "Authenticated")
                } else {
                    onError("Flarum Login Failed: ${response.body?.string()}")
                }
            }
        })
    }

    // Logout from both PocketBase and Flarum
    fun logoutUser(
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            // Clear local auth data first
            clearAuthDetails()
            
            // PocketBase does not require an explicit logout for token-based systems.
            val flUrl = "$flarumUrl/api/token"
            val flRequest = Request.Builder()
                .url(flUrl)
                .delete()
                .header("Authorization", "Token $flarumApiKey")
                .build()

            client.newCall(flRequest).enqueue(object : okhttp3.Callback {
                override fun onFailure(call: okhttp3.Call, e: IOException) {
                    onError("Logout Failed: ${e.message}")
                }

                override fun onResponse(call: okhttp3.Call, response: Response) {
                    if (response.isSuccessful) {
                        onSuccess()
                    } else {
                        onError("Flarum Logout Failed: ${response.body?.string()}")
                    }
                }
            })
        } catch (e: Exception) {
            onError("Logout Exception: ${e.message}")
        }
    }
}
