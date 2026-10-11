package com.bgradio.app.data.model

/** Public links of the project (GitHub repository of Dominix). */
object AppInfo {
    const val GITHUB_OWNER = "dominitriks"
    const val GITHUB_REPO = "dominixradio"

    const val REPO_URL = "https://github.com/$GITHUB_OWNER/$GITHUB_REPO"
    const val RELEASES_URL = "$REPO_URL/releases"
    const val LATEST_RELEASE_API = "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases/latest"
    const val NEW_ISSUE_URL = "$REPO_URL/issues/new"
    /** Release assets of the official repository — the only accepted source of update APKs. */
    const val APK_DOWNLOAD_PREFIX = "$RELEASES_URL/download/"
    const val PRIVACY_URL = "$REPO_URL/blob/main/PRIVACY.md"

    /**
     * Official download page used in shares and QR codes. Change it here if Dominix gets its own
     * website. (No per-station deep link exists, so links never point at a specific station.)
     */
    const val DOWNLOAD_URL = "$RELEASES_URL/latest"

    /** Support e-mail for problem reports. Not configured → the e-mail option is hidden. */
    val SUPPORT_EMAIL: String? = null
}
