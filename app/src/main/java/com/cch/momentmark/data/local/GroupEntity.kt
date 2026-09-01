package com.cch.momentmark.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "groups")
data class GroupEntity(
    @PrimaryKey val id: String,
    val name: String,
    val colorToken: String,
    val sortOrder: Int,
    val createdAt: Long,
    val updatedAt: Long,
)
