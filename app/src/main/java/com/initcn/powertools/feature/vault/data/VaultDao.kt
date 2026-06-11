package com.initcn.powertools.feature.vault.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface VaultDao {

    @Query("SELECT * FROM vault_metadata WHERE parentPath = :parentPath")
    fun getFilesByParentPathFlow(parentPath: String): Flow<List<VaultFileEntity>>

    @Query("SELECT * FROM vault_metadata WHERE id = :id")
    suspend fun getFileById(id: String): VaultFileEntity?

    @Query("SELECT * FROM vault_metadata WHERE encryptedName = :encryptedName")
    suspend fun getFileByEncryptedName(encryptedName: String): VaultFileEntity?

    @Query("SELECT * FROM vault_metadata WHERE parentPath = :parentPath")
    suspend fun getFilesByParentPath(parentPath: String): List<VaultFileEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFile(file: VaultFileEntity)

    @Delete
    suspend fun deleteFile(file: VaultFileEntity)

    @Query("DELETE FROM vault_metadata")
    suspend fun deleteAll()
}