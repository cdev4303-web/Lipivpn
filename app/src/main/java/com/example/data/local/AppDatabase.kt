package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.VpnServer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [VpnServer::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun vpnServerDao(): VpnServerDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "freeshield_vpn.db"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(AppDatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class AppDatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialServers(database.vpnServerDao())
                    }
                }
            }

            private suspend fun populateInitialServers(dao: VpnServerDao) {
                val defaultServers = listOf(
                    VpnServer(
                        id = 1L,
                        name = "Singapore",
                        country = "Singapore",
                        countryCode = "SG",
                        host = "",
                        port = 51820,
                        protocol = "WireGuard",
                        publicKey = "",
                        clientPrivateKey = "",
                        clientPublicKey = "",
                        clientIp = "10.0.0.2/32",
                        dns = "1.1.1.1, 8.8.8.8",
                        mtu = 1420,
                        isCustom = false,
                        notes = "Configure with your WireGuard endpoint host and keys to connect."
                    ),
                    VpnServer(
                        id = 2L,
                        name = "United States",
                        country = "United States",
                        countryCode = "US",
                        host = "",
                        port = 51820,
                        protocol = "WireGuard",
                        publicKey = "",
                        clientPrivateKey = "",
                        clientPublicKey = "",
                        clientIp = "10.0.0.2/32",
                        dns = "1.1.1.1, 8.8.8.8",
                        mtu = 1420,
                        isCustom = false,
                        notes = "Configure with your WireGuard endpoint host and keys to connect."
                    ),
                    VpnServer(
                        id = 3L,
                        name = "Germany",
                        country = "Germany",
                        countryCode = "DE",
                        host = "",
                        port = 51820,
                        protocol = "WireGuard",
                        publicKey = "",
                        clientPrivateKey = "",
                        clientPublicKey = "",
                        clientIp = "10.0.0.2/32",
                        dns = "1.1.1.1, 9.9.9.9",
                        mtu = 1420,
                        isCustom = false,
                        notes = "Configure with your WireGuard endpoint host and keys to connect."
                    ),
                    VpnServer(
                        id = 4L,
                        name = "Netherlands",
                        country = "Netherlands",
                        countryCode = "NL",
                        host = "",
                        port = 51820,
                        protocol = "WireGuard",
                        publicKey = "",
                        clientPrivateKey = "",
                        clientPublicKey = "",
                        clientIp = "10.0.0.2/32",
                        dns = "1.1.1.1, 8.8.8.8",
                        mtu = 1420,
                        isCustom = false,
                        notes = "Configure with your WireGuard endpoint host and keys to connect."
                    ),
                    VpnServer(
                        id = 5L,
                        name = "United Kingdom",
                        country = "United Kingdom",
                        countryCode = "GB",
                        host = "",
                        port = 51820,
                        protocol = "WireGuard",
                        publicKey = "",
                        clientPrivateKey = "",
                        clientPublicKey = "",
                        clientIp = "10.0.0.2/32",
                        dns = "1.1.1.1, 8.8.8.8",
                        mtu = 1420,
                        isCustom = false,
                        notes = "Configure with your WireGuard endpoint host and keys to connect."
                    )
                )
                dao.insertServers(defaultServers)
            }
        }
    }
}
