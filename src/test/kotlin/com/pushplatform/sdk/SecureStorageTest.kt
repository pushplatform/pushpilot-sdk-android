package com.pushplatform.sdk

import android.content.Context
import android.content.SharedPreferences
import com.pushplatform.sdk.core.SecureStorage
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.*
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21, 23, 26, 33])
class SecureStorageTest {

    private lateinit var context: Context
    private lateinit var secureStorage: SecureStorage

    @Before
    fun setup() {
        context = RuntimeEnvironment.getApplication()
        secureStorage = SecureStorage(context)
    }

    @Test
    fun testPutString_savesValue() {
        val result = secureStorage.putString("test_key", "test_value")

        assertTrue(result)
    }

    @Test
    fun testGetString_retrievesSavedValue() {
        secureStorage.putString("test_key", "test_value")

        val value = secureStorage.getString("test_key")

        assertEquals("test_value", value)
    }

    @Test
    fun testGetString_returnsNullForNonExistentKey() {
        val value = secureStorage.getString("non_existent_key")

        assertNull(value)
    }

    @Test
    fun testRemove_deletesValue() {
        secureStorage.putString("test_key", "test_value")

        val result = secureStorage.remove("test_key")

        assertTrue(result)
        assertNull(secureStorage.getString("test_key"))
    }

    @Test
    fun testContains_returnsTrueForExistingKey() {
        secureStorage.putString("test_key", "test_value")

        val exists = secureStorage.contains("test_key")

        assertTrue(exists)
    }

    @Test
    fun testContains_returnsFalseForNonExistentKey() {
        val exists = secureStorage.contains("non_existent_key")

        assertFalse(exists)
    }

    @Test
    fun testOverwrite_replacesExistingValue() {
        secureStorage.putString("test_key", "value1")
        secureStorage.putString("test_key", "value2")

        val value = secureStorage.getString("test_key")

        assertEquals("value2", value)
    }

    @Test
    fun testClear_removesAllValues() {
        secureStorage.putString("key1", "value1")
        secureStorage.putString("key2", "value2")

        val result = secureStorage.clear()

        assertTrue(result)
        assertNull(secureStorage.getString("key1"))
        assertNull(secureStorage.getString("key2"))
    }

    @Test
    fun testPersistence_valuesSurviveReinitialization() {
        secureStorage.putString("persist_key", "persist_value")

        val newStorage = SecureStorage(context)
        val value = newStorage.getString("persist_key")

        assertEquals("persist_value", value)
    }

    @Test
    fun testMultipleKeys_independentStorage() {
        secureStorage.putString("key1", "value1")
        secureStorage.putString("key2", "value2")
        secureStorage.putString("key3", "value3")

        assertEquals("value1", secureStorage.getString("key1"))
        assertEquals("value2", secureStorage.getString("key2"))
        assertEquals("value3", secureStorage.getString("key3"))
    }
}
