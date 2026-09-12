package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.VpnServer
import kotlinx.coroutines.flow.Flow

@Dao
interface VpnServerDao {
    @Query("SELECT * FROM vpn_servers ORDER BY isCustom DESC, id ASC")
    fun getAllServers(): Flow<List<VpnServer>>

    @Query("SELECT * FROM vpn_servers WHERE id = :id LIMIT 1")
    fun getServerById(id: Long): Flow<VpnServer?>

    @Query("SELECT * FROM vpn_servers WHERE id = :id LIMIT 1")
    suspend fun getServerByIdSync(id: Long): VpnServer?

    @Query("SELECT COUNT(*) FROM vpn_servers")
    suspend fun getServerCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServer(server: VpnServer): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServers(servers: List<VpnServer>)

    @Update
    suspend fun updateServer(server: VpnServer)

    @Delete
    suspend fun deleteServer(server: VpnServer)

    @Query("UPDATE vpn_servers SET pingMs = :pingMs WHERE id = :id")
    suspend fun updatePing(id: Long, pingMs: Long?)
}
