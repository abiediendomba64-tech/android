package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.HousingUnitEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HousingUnitDao {
    @Query("SELECT * FROM housing_units ORDER BY project COLLATE NOCASE ASC, block COLLATE NOCASE ASC, unitCode COLLATE NOCASE ASC")
    fun getAllUnits(): Flow<List<HousingUnitEntity>>

    @Query("SELECT * FROM housing_units WHERE project = :project COLLATE NOCASE ORDER BY block COLLATE NOCASE ASC, unitCode COLLATE NOCASE ASC")
    fun getUnitsForProject(project: String): Flow<List<HousingUnitEntity>>

    @Query("SELECT * FROM housing_units WHERE id = :id LIMIT 1")
    suspend fun getUnitById(id: String): HousingUnitEntity?

    @Query("SELECT COUNT(*) FROM housing_units WHERE project = :project COLLATE NOCASE AND unitCode = :unitCode COLLATE NOCASE")
    suspend fun countCodeInProject(project: String, unitCode: String): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertUnit(unit: HousingUnitEntity)

    @Update
    suspend fun updateUnit(unit: HousingUnitEntity)
}
