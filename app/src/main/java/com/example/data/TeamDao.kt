package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TeamDao {
    @Query("SELECT * FROM teams WHERE creatorUsername = :username ORDER BY createdAt DESC")
    fun getTeamsFlow(username: String): Flow<List<TeamEntity>>

    @Query("SELECT * FROM teams WHERE creatorUsername = :username ORDER BY createdAt DESC")
    suspend fun getTeams(username: String): List<TeamEntity>

    @Query("SELECT * FROM team_members WHERE teamId = :teamId ORDER BY invitedAt DESC")
    fun getTeamMembersFlow(teamId: Int): Flow<List<TeamMemberEntity>>

    @Query("SELECT * FROM team_members WHERE teamId = :teamId ORDER BY invitedAt DESC")
    suspend fun getTeamMembers(teamId: Int): List<TeamMemberEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTeam(team: TeamEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTeamMember(member: TeamMemberEntity): Long

    @Query("DELETE FROM teams WHERE id = :teamId")
    suspend fun deleteTeam(teamId: Int)

    @Query("DELETE FROM team_members WHERE id = :memberId")
    suspend fun deleteTeamMember(memberId: Int)

    @Query("UPDATE team_members SET status = :status WHERE id = :memberId")
    suspend fun updateMemberStatus(memberId: Int, status: String)

    @Query("SELECT * FROM tasks WHERE teamId = :teamId AND isDeleted = 0")
    fun getTeamTasksFlow(teamId: Int): Flow<List<TaskEntity>>
}
