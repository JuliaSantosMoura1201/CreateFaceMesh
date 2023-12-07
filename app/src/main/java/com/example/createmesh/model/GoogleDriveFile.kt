package com.example.createmesh.model

import java.io.Serializable

data class GoogleDriveFile(
    val kind: String,
    val mimeType: String,
    val id: String,
    val name: String,
    val parentFolderName: String
): Serializable