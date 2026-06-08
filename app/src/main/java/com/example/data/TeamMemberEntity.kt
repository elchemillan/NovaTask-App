package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "team_members")
data class TeamMemberEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val teamId: Int,
    val email: String,
    val status: String = "INVITED", // INVITED, JOINED
    val invitedAt: Long = System.currentTimeMillis()
)
