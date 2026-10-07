package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.EmployeeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EmployeeDao {
    @Query("SELECT * FROM employees WHERE isActive = 1 ORDER BY name ASC")
    fun getAllActiveEmployees(): Flow<List<EmployeeEntity>>

    @Query("SELECT * FROM employees WHERE isActive = 1 ORDER BY name ASC")
    suspend fun getAllEmployeesList(): List<EmployeeEntity>

    @Query("SELECT * FROM employees ORDER BY name ASC")
    fun getAllEmployees(): Flow<List<EmployeeEntity>>

    @Query("SELECT * FROM employees WHERE id = :id LIMIT 1")
    suspend fun getEmployeeById(id: String): EmployeeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmployee(employee: EmployeeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmployees(employees: List<EmployeeEntity>)

    @Update
    suspend fun updateEmployee(employee: EmployeeEntity)

    @Query("DELETE FROM employees WHERE id = :id")
    suspend fun deleteEmployee(id: String)
}
