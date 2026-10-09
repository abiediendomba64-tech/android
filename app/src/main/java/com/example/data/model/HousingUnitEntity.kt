package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "housing_units")
data class HousingUnitEntity(
    @PrimaryKey val id: String,
    val project: String,
    val unitCode: String,
    val block: String,
    val sitePosition: String,
    val businessModel: String,
    val landAreaM2: Double,
    val buildingAreaM2: Double,
    val salePrice: Double,
    val buyerName: String,
    val status: String,
    val notes: String,
    val createdAt: Long
)
