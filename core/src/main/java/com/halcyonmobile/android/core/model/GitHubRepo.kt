package com.halcyonmobile.android.core.model

data class GitHubRepo(
    val nodeId: String,
    val name: String,
    val isPrivate: Boolean,
    val htmlUrl: String,
    val description: String,
    val numberOfWatchers: Int
)