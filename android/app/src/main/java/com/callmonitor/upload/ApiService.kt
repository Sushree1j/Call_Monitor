package com.callmonitor.upload

import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface ApiService {

    @Multipart
    @POST("/upload")
    suspend fun uploadRecording(
        @Part file: MultipartBody.Part,
        @Part("phone_number") phoneNumber: RequestBody,
        @Part("is_incoming") isIncoming: RequestBody,
        @Part("timestamp") timestamp: RequestBody,
        @Part("duration") duration: RequestBody
    ): Response<UploadResponse>
}

data class UploadResponse(
    val status: String,
    val file_id: String?,
    val message: String?
)
