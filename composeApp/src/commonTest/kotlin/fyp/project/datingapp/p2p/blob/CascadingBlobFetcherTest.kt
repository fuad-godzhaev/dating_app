package fyp.project.datingapp.p2p.blob

import fyp.project.datingapp.database.pds.dao.BlobDao
import fyp.project.datingapp.database.pds.entities.BlobEntity
import fyp.project.datingapp.p2p.discovery.PeerContact
import fyp.project.datingapp.p2p.discovery.PeerDirectory
import fyp.project.datingapp.p2p.transport.wire.BlobFetchRequest
import fyp.project.datingapp.p2p.transport.wire.BlobFetchResponse
import fyp.project.datingapp.records.canonical.Cid
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class CascadingBlobFetcherTest {

    private val ownerDid = "did:key:zDnaembgSGUhZULN2Caob4HLJPaxBh92N7rtH21TErzqf8HQo"
    private val photoBytes = byteArrayOf(10, 20, 30, 40, 50, 60)
    private val photoCid = Cid.cidV1Raw(photoBytes)

    private class FakeBlobDao : BlobDao {
        val rows = mutableMapOf<String, BlobEntity>()
        override suspend fun upsertBlob(blob: BlobEntity) { rows[blob.cid] = blob }
        override suspend fun getBlob(cid: String): BlobEntity? = rows[cid]
        override suspend fun getAllBlobs(): List<BlobEntity> = rows.values.toList()
        override suspend fun deleteBlob(cid: String) { rows.remove(cid) }
    }

    private class FakeBlobStore : BlobStore {
        val files = mutableMapOf<String, ByteArray>()
        override suspend fun write(cid: String, bytes: ByteArray): String {
            val path = "file://$cid"
            files[path] = bytes
            return path
        }
        override suspend fun read(path: String): ByteArray? = files[path]
        override suspend fun exists(path: String): Boolean = files.containsKey(path)
    }

    private class FakeBlobStreamClient(var response: BlobFetchResponse?) : BlobStreamClient {
        var calls = 0
        override suspend fun request(contact: PeerContact, request: BlobFetchRequest): BlobFetchResponse? {
            calls++
            return response
        }
    }

    private fun directoryWithOwner() = PeerDirectory().apply {
        record(ownerDid, "12D3KooWowner", listOf("/ip4/10.0.0.2/tcp/4001/p2p/12D3KooWowner"))
    }

    @Test fun localHit_returnsPathWithoutNetwork() = runTest {
        val dao = FakeBlobDao()
        val store = FakeBlobStore()
        val path = store.write(photoCid, photoBytes)
        dao.upsertBlob(BlobEntity(cid = photoCid, mimeType = "image/jpeg", size = photoBytes.size.toLong(), filePath = path))
        val client = FakeBlobStreamClient(null)
        val fetcher = CascadingBlobFetcher(dao, store, directoryWithOwner(), client)

        assertEquals(path, fetcher.fetch(ownerDid, photoCid))
        assertEquals(0, client.calls, "local hit must not hit the network")
    }

    @Test fun ownerHit_verifiesPersistsAndReturnsPath() = runTest {
        val dao = FakeBlobDao()
        val store = FakeBlobStore()
        val client = FakeBlobStreamClient(
            BlobFetchResponse(cid = photoCid, bytes = photoBytes, mimeType = "image/png"),
        )
        val fetcher = CascadingBlobFetcher(dao, store, directoryWithOwner(), client)

        val path = fetcher.fetch(ownerDid, photoCid)
        assertNotNull(path)
        assertEquals(1, client.calls)
        // Persisted to both the DAO and the store, with the right mime type.
        val row = dao.getBlob(photoCid)
        assertNotNull(row)
        assertEquals("image/png", row.mimeType)
        assertEquals(photoBytes.size.toLong(), row.size)
        assertNotNull(store.read(path))
    }

    @Test fun integrityMismatch_rejectedAndNotPersisted() = runTest {
        val dao = FakeBlobDao()
        val store = FakeBlobStore()
        // Bytes don't hash to the requested CID -> reject.
        val client = FakeBlobStreamClient(
            BlobFetchResponse(cid = photoCid, bytes = byteArrayOf(99, 99, 99), mimeType = "image/jpeg"),
        )
        val fetcher = CascadingBlobFetcher(dao, store, directoryWithOwner(), client)

        assertNull(fetcher.fetch(ownerDid, photoCid))
        assertNull(dao.getBlob(photoCid), "a CID-mismatched blob must not be cached")
    }

    @Test fun noOwnerKnown_returnsNull() = runTest {
        val client = FakeBlobStreamClient(
            BlobFetchResponse(cid = photoCid, bytes = photoBytes, mimeType = "image/jpeg"),
        )
        val fetcher = CascadingBlobFetcher(FakeBlobDao(), FakeBlobStore(), PeerDirectory(), client)
        // No PeerDirectory entry -> owner step skipped; holder fallback deferred -> null.
        assertNull(fetcher.fetch(ownerDid, photoCid))
        assertEquals(0, client.calls)
    }
}
