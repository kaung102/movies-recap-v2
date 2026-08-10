package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RecapProjectDao {
    @Query("SELECT * FROM recap_projects ORDER BY updatedAt DESC")
    fun getAllProjects(): Flow<List<RecapProjectEntity>>

    @Query("SELECT * FROM recap_projects WHERE id = :id")
    suspend fun getProjectById(id: String): RecapProjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: RecapProjectEntity)

    @Update
    suspend fun updateProject(project: RecapProjectEntity)

    @Query("DELETE FROM recap_projects WHERE id = :id")
    suspend fun deleteProjectById(id: String)
}
