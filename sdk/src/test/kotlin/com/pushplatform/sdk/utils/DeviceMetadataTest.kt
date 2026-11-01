package com.pushplatform.sdk.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class DeviceMetadataTest {
    @Test fun detectsKnownEmulatorSignals() {
        assertEquals("emulator", DeviceMetadata.classify("generic/sdk_gphone/emulator", "sdk_gphone64_arm64", "Google", "google", "emu64a", "ranchu"))
        assertEquals("emulator", DeviceMetadata.classify("test", "Virtual", "Genymotion", "generic", "virtual", "vbox86"))
    }
    @Test fun reportsPhysicalOnlyWithAvailableDeviceProperties() {
        assertEquals("physical", DeviceMetadata.classify("samsung/a/release-keys", "SM-A546B", "samsung", "samsung", "a54x", "s5e8835"))
    }
    @Test fun missingOrUnknownPropertiesAreUnknown() {
        assertEquals("unknown", DeviceMetadata.classify(null, null, null, null, null, null))
        assertEquals("unknown", DeviceMetadata.classify("unknown", "phone", "maker", "brand", "device", "hardware"))
    }
}
