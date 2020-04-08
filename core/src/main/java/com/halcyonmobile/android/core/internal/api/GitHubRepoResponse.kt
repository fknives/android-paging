package com.halcyonmobile.android.core.internal.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class GitHubRepoResponse(
    @Json(name = "node_id")
    val nodeId: String,
    @Json(name = "name")
    val name: String,
    @Json(name = "private")
    val isPrivate: Boolean,
    @Json(name = "html_url")
    val htmlUrl: String?,
    @Json(name = "description")
    val description: String?,
    @Json(name = "watchers_count")
    val numberOfWatchers: Int?
)