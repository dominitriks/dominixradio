package com.bgradio.app.data.model

/** Public links of the project (GitHub repository of Dominix). */
object AppInfo {
    const val GITHUB_OWNER = "dominitriks"
    const val GITHUB_REPO = "dominixradio"

    const val REPO_URL = "https://github.com/$GITHUB_OWNER/$GITHUB_REPO"
    const val RELEASES_URL = "$REPO_URL/releases"
    const val LATEST_RELEASE_API = "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases/latest"
    const val NEW_ISSUE_URL = "$REPO_URL/issues/new"
    const val PRIVACY_URL = "$REPO_URL/blob/main/PRIVACY.md"
}
