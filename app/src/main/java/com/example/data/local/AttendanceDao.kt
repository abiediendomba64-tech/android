package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.AttendanceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AttendanceDao {
    @Query("SELECT * FROM attendances ORDER BY date DESC, timeIn DESC")
    fun getAllAttendances(): Flow<List<AttendanceEntity>>

    @Query("SELECT * FROM attendances WHERE date = :date ORDER BY timeIn ASC")
    fun getAttendancesByDate(date: String): Flow<List<AttendanceEntity>>

    @Query("SELECT * FROM attendances WHERE date LIKE :monthPrefix || '%' ORDER BY date DESC, employeeName ASC")
    fun getAttendancesByMonth(monthPrefix: String): Flow<List<AttendanceEntity>>

    @Query("SELECT * FROM attendances WHERE date LIKE :yearPrefix || '%' ORDER BY date DESC")
    fun getAttendancesByYear(yearPrefix: String): Flow<List<AttendanceEntity>>

    @Query("SELECT * FROM attendances WHERE date >= :startDate AND date <= :endDate ORDER BY date DESC")
    fun getAttendancesByPeriod(startDate: String, endDate: String): Flow<List<AttendanceEntity>>

    @Query("SELECT * FROM attendances WHERE employeeId = :empId AND date = :date LIMIT 1")
    suspend fun getAttendanceByEmployeeAndDate(empId: String, date: String): AttendanceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendance(attendance: AttendanceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendances(attendances: List<AttendanceEntity>)

    @Update
    suspend fun updateAttendance(attendance: AttendanceEntity)

    @Query("DELETE FROM attendances WHERE id = :id")
    suspend fun deleteAttendance(id: String)

    @Query("DELETE FROM attendances")
    suspend fun deleteAllAttendances()
}
