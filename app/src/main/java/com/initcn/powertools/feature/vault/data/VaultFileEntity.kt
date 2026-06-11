package com.initcn.powertools.feature.vault.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "vault_metadata")
data class VaultFileEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String = UUID.randomUUID().toString(), // Strictly UUID now

    @ColumnInfo(name = "encryptedName")
    val encryptedName: String,

    @ColumnInfo(name = "isDirectory")
    val isDirectory: Boolean,

    @ColumnInfo(name = "parentPath")
    val parentPath: String,

    @ColumnInfo(name = "fileSize")
    val fileSize: Long = 0L,

    @ColumnInfo(name = "mimeType")
    val mimeType: String = "application/octet-stream",

    @ColumnInfo(name = "lastModified")
    val lastModified: Long = System.currentTimeMillis()
)