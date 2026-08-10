package com.example.data.db

import kotlinx.coroutines.flow.Flow

class RecapProjectRepository(private val dao: RecapProjectDao) {
    val allProjects: Flow<List<RecapProjectEntity>> = dao.getAllProjects()

    suspend fun getProjectById(id: String): RecapProjectEntity? = dao.getProjectById(id)

    suspend fun saveProject(project: RecapProjectEntity) = dao.insertProject(project)

    suspend fun updateProject(project: RecapProjectEntity) = dao.updateProject(project)

    suspend fun deleteProject(id: String) = dao.deleteProjectById(id)
}
