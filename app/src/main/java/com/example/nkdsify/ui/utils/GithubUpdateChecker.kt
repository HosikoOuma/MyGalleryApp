package com.example.nkdsify.ui.utils

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path

data class GithubAsset(
    val name: String,
    val browser_download_url: String
)

data class GithubRelease(
    val tag_name: String,
    val assets: List<GithubAsset>,
    val body: String
)

interface GithubApiService {
    @GET("repos/{owner}/{repo}/releases/latest")
    suspend fun getLatestRelease(@Path("owner") owner: String, @Path("repo") repo: String): GithubRelease
}

object GithubUpdateChecker {
    private const val GITHUB_API_URL = "https://api.github.com/"

    private val retrofit = Retrofit.Builder()
        .baseUrl(GITHUB_API_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val service = retrofit.create(GithubApiService::class.java)

    suspend fun getLatestRelease(owner: String, repo: String): GithubRelease? {
        return try {
            service.getLatestRelease(owner, repo)
        } catch (e: Exception) {
            null
        }
    }
}