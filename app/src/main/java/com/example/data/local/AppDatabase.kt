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

@Database(entities = [VpnServer::class], version = 1, exportSchema = false)
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
                // Pre-populate required example entries with clearly marked placeholder values
                // Per requirements: Do not hard-code fake working IPs or pretend they are functional.
                val defaultServers = listOf(
                    VpnServer(
                        id = 1L,
                        name = "Singapore Cloud Gateway",
                        country = "Singapore",
                        countryCode = "SG",
                        host = "sg-node1.placeholder.internal",
                        port = 51820,
                        protocol = "WireGuard",
                        publicKey = "INSERT_SG_WIREGUARD_PUBLIC_KEY_BASE64",
                        clientIp = "10.10.1.2",
                        dns = "1.1.1.1, 8.8.8.8",
                        mtu = 1420,
                        isCustom = false,
                        notes = "Example placeholder: Edit host to your real Singapore server IP/domain and insert public key."
                    ),
                    VpnServer(
                        id = 2L,
                        name = "United States East",
                        country = "United States",
                        countryCode = "US",
                        host = "us-east.placeholder.internal",
                        port = 51820,
                        protocol = "WireGuard",
                        publicKey = "INSERT_US_WIREGUARD_PUBLIC_KEY_BASE64",
                        clientIp = "10.10.2.2",
                        dns = "1.1.1.1, 8.8.8.8",
                        mtu = 1420,
                        isCustom = false,
                        notes = "Example placeholder: Edit host to your real US server IP/domain and insert public key."
                    ),
                    VpnServer(
                        id = 3L,
                        name = "Germany Frankfurt",
                        country = "Germany",
                        countryCode = "DE",
                        host = "de-fra.placeholder.internal",
                        port = 51820,
                        protocol = "WireGuard",
                        publicKey = "INSERT_DE_WIREGUARD_PUBLIC_KEY_BASE64",
                        clientIp = "10.10.3.2",
                        dns = "1.1.1.1, 9.9.9.9",
                        mtu = 1420,
                        isCustom = false,
                        notes = "Example placeholder: Edit host to your real Germany server IP/domain."
                    ),
                    VpnServer(
                        id = 4L,
                        name = "Netherlands Amsterdam",
                        country = "Netherlands",
                        countryCode = "NL",
                        host = "nl-ams.placeholder.internal",
                        port = 51820,
                        protocol = "WireGuard",
                        publicKey = "INSERT_NL_WIREGUARD_PUBLIC_KEY_BASE64",
                        clientIp = "10.10.4.2",
                        dns = "1.1.1.1, 8.8.8.8",
                        mtu = 1420,
                        isCustom = false,
                        notes = "Example placeholder: Edit host to your real Netherlands server IP/domain."
                    ),
                    VpnServer(
                        id = 5L,
                        name = "United Kingdom London",
                        country = "United Kingdom",
                        countryCode = "GB",
                        host = "uk-lon.placeholder.internal",
                        port = 51820,
                        protocol = "WireGuard",
                        publicKey = "INSERT_UK_WIREGUARD_PUBLIC_KEY_BASE64",
                        clientIp = "10.10.5.2",
                        dns = "1.1.1.1, 8.8.8.8",
                        mtu = 1420,
                        isCustom = false,
                        notes = "Example placeholder: Edit host to your real UK server IP/domain."
                    )
                )
                dao.insertServers(defaultServers)
            }
        }
    }
}
