package fyp.project.datingapp.database

import androidx.room.*
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import fyp.project.datingapp.database.appView.dao.MessageDao
import fyp.project.datingapp.database.appView.dao.DiscoveryDao
import fyp.project.datingapp.database.appView.dao.IncomingLikesDao
import fyp.project.datingapp.database.appView.dao.MailboxDao
import fyp.project.datingapp.database.appView.entities.ConversationEntity
import fyp.project.datingapp.database.appView.entities.IncomingLikeEntity
import fyp.project.datingapp.database.appView.entities.MailboxEntity
import fyp.project.datingapp.database.appView.entities.MessageEntity
import fyp.project.datingapp.database.appView.entities.PeerProfileEntity
import fyp.project.datingapp.database.pds.dao.BlobDao
import fyp.project.datingapp.database.pds.dao.CommitDao
import fyp.project.datingapp.database.pds.dao.ProfileDao
import fyp.project.datingapp.database.pds.dao.RecordDao
import fyp.project.datingapp.database.pds.entities.BlobEntity
import fyp.project.datingapp.database.pds.entities.CommitEntity
import fyp.project.datingapp.database.pds.entities.RecordEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

@Database(
    entities = [
        RecordEntity::class,
        CommitEntity::class,
        BlobEntity::class,
        PeerProfileEntity::class,
        IncomingLikeEntity::class,
        ConversationEntity::class,
        MessageEntity::class,
        MailboxEntity::class,
        AuthSettingsEntity::class,
    ],
    version = 6,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        // v3 adds relay-cache columns to peer_profiles
        // (ownerDid, bodyCiphertext, bodyNonce, cachedAt, lastServedAt,
        //  expiresAt, sessionInteractionToken) plus two indices. All new
        // columns are nullable or have SQL defaults, so Room handles the
        // migration without an AutoMigrationSpec.
        AutoMigration(from = 2, to = 3),
        // v4 adds the nullable `signature` column to `records` (per-record P-256
        // signature for standalone verification on fetch). Nullable ⇒ no spec.
        AutoMigration(from = 3, to = 4),
        // v5 adds the new `messages` table (Part 5 / M3). A brand-new table needs
        // no AutoMigrationSpec.
        AutoMigration(from = 4, to = 5),
        // v6 adds the new `mailbox` table (M5 persistence): cacheHolders now store
        // queued mail durably so it survives a restart. Brand-new table ⇒ no spec.
        AutoMigration(from = 5, to = 6),
    ]
)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun recordDao(): RecordDao
    abstract fun commitDao(): CommitDao
    abstract fun blobDao(): BlobDao
    abstract fun profileDao(): ProfileDao
    abstract fun authSettingsDao(): AuthSettingsDao

    abstract fun discoveryDao(): DiscoveryDao
    abstract fun incomingLikeDao(): IncomingLikesDao
    abstract fun conversationDao(): MessageDao
    abstract fun mailboxDao(): MailboxDao
}

@Suppress("KotlinNoActualForExpect")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}

fun getRoomDatabase(
    builder: RoomDatabase.Builder<AppDatabase>
): AppDatabase {
    return builder
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
}
