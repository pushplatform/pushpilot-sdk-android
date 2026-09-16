package com.pushplatform.sdk

import com.pushplatform.sdk.core.InstallationManager
import com.pushplatform.sdk.core.SecureStorage
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*
import java.util.UUID

class InstallationManagerTest {

    private lateinit var mockStorage: SecureStorage
    private lateinit var installationManager: InstallationManager

    @Before
    fun setup() {
        mockStorage = mock()
        installationManager = InstallationManager(mockStorage)
    }

    @Test
    fun testGetOrCreateInstallationId_createsNewIdWhenNotExists() {
        whenever(mockStorage.getString("installation_id")).thenReturn(null)
        whenever(mockStorage.putString(any(), any())).thenReturn(true)

        val id = installationManager.getOrCreateInstallationId()

        assertNotNull(id)
        assertTrue(isValidUUID(id))
        verify(mockStorage).putString(eq("installation_id"), any())
    }

    @Test
    fun testGetOrCreateInstallationId_returnsExistingId() {
        val existingId = UUID.randomUUID().toString()
        whenever(mockStorage.getString("installation_id")).thenReturn(existingId)

        val id = installationManager.getOrCreateInstallationId()

        assertEquals(existingId, id)
        verify(mockStorage, never()).putString(any(), any())
    }

    @Test
    fun testGetOrCreateInstallationId_generatesValidUUIDv4() {
        whenever(mockStorage.getString("installation_id")).thenReturn(null)
        whenever(mockStorage.putString(any(), any())).thenReturn(true)

        val id = installationManager.getOrCreateInstallationId()

        assertTrue(isValidUUID(id))
        assertTrue(id.contains("-"))
        assertEquals(36, id.length)
    }

    @Test
    fun testGetOrCreateInstallationId_persistsNewId() {
        whenever(mockStorage.getString("installation_id")).thenReturn(null)
        whenever(mockStorage.putString(any(), any())).thenReturn(true)

        val id = installationManager.getOrCreateInstallationId()

        verify(mockStorage).putString("installation_id", id)
    }

    @Test
    fun testGetOrCreateInstallationId_savesOnlyOnce() {
        whenever(mockStorage.getString("installation_id"))
            .thenReturn(null)
        whenever(mockStorage.putString(any(), any())).thenReturn(true)

        val id1 = installationManager.getOrCreateInstallationId()

        // Second call should return the same ID that was just created
        whenever(mockStorage.getString("installation_id"))
            .thenReturn(id1)
        val id2 = installationManager.getOrCreateInstallationId()

        assertEquals(id1, id2)
        verify(mockStorage, times(1)).putString(eq("installation_id"), eq(id1))
    }

    @Test
    fun testGetOrCreateInstallationId_continuesWhenSaveFails() {
        whenever(mockStorage.getString("installation_id")).thenReturn(null)
        whenever(mockStorage.putString(any(), any())).thenReturn(false)

        val id = installationManager.getOrCreateInstallationId()

        assertNotNull(id)
        assertTrue(isValidUUID(id))
    }

    @Test
    fun testGetInstallationId_returnsNullWhenNotSet() {
        whenever(mockStorage.getString("installation_id")).thenReturn(null)

        val id = installationManager.getInstallationId()

        assertNull(id)
    }

    @Test
    fun testGetInstallationId_returnsExistingId() {
        val existingId = UUID.randomUUID().toString()
        whenever(mockStorage.getString("installation_id")).thenReturn(existingId)

        val id = installationManager.getInstallationId()

        assertEquals(existingId, id)
    }

    @Test
    fun testClearInstallationId_removesId() {
        whenever(mockStorage.remove("installation_id")).thenReturn(true)

        val result = installationManager.clearInstallationId()

        assertTrue(result)
        verify(mockStorage).remove("installation_id")
    }

    @Test
    fun testClearInstallationId_returnsFalseOnFailure() {
        whenever(mockStorage.remove("installation_id")).thenReturn(false)

        val result = installationManager.clearInstallationId()

        assertFalse(result)
    }

    @Test
    fun testMultipleInstallationManagers_shareStorage() {
        val sharedId = UUID.randomUUID().toString()
        whenever(mockStorage.getString("installation_id")).thenReturn(sharedId)

        val manager1 = InstallationManager(mockStorage)
        val manager2 = InstallationManager(mockStorage)

        assertEquals(sharedId, manager1.getInstallationId())
        assertEquals(sharedId, manager2.getInstallationId())
    }

    @Test
    fun testGetOrCreateInstallationId_generatesUniqueIds() {
        whenever(mockStorage.getString("installation_id")).thenReturn(null)
        whenever(mockStorage.putString(any(), any())).thenReturn(true)

        val id1 = installationManager.getOrCreateInstallationId()

        val manager2 = InstallationManager(mockStorage)
        val id2 = manager2.getOrCreateInstallationId()

        assertNotEquals(id1, id2)
    }

    @Test
    fun testGetOrCreateInstallationId_usesCorrectStorageKey() {
        whenever(mockStorage.getString("installation_id")).thenReturn(null)
        whenever(mockStorage.putString(any(), any())).thenReturn(true)

        installationManager.getOrCreateInstallationId()

        verify(mockStorage).getString("installation_id")
        verify(mockStorage).putString(eq("installation_id"), any())
    }

    private fun isValidUUID(uuid: String): Boolean {
        return try {
            UUID.fromString(uuid)
            true
        } catch (e: IllegalArgumentException) {
            false
        }
    }
}
