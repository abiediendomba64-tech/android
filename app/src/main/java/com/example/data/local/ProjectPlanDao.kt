package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.ProjectPlanEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectPlanDao {
    @Query("SELECT * FROM project_plans ORDER BY planDate ASC, createdAt ASC")
    fun getAllPlans(): Flow<List<ProjectPlanEntity>>

    @Query("SELECT * FROM project_plans WHERE planDate >= :startDate AND planDate <= :endDate ORDER BY planDate ASC")
    fun getPlansByPeriod(startDate: String, endDate: String): Flow<List<ProjectPlanEntity>>

    @Query("SELECT * FROM project_plans WHERE id = :id LIMIT 1")
    suspend fun getPlanById(id: String): ProjectPlanEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPlan(plan: ProjectPlanEntity)

    @Update
    suspend fun updatePlan(plan: ProjectPlanEntity)

    @Query("DELETE FROM project_plans WHERE id = :id")
    suspend fun deletePlan(id: String)
}
