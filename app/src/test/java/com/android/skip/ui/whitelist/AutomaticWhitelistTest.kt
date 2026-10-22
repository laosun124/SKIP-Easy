package com.android.skip.ui.whitelist

import org.junit.Assert.*
import org.junit.Test

class AutomaticWhitelistTest {
    @Test fun androidPrefixRequiresPackageBoundary() {
        assertEquals(AutomaticWhitelist.Reason.ANDROID_PREFIX, AutomaticWhitelist.reason("com.android.settings"))
        assertNull(AutomaticWhitelist.reason("com.androidfake.app"))
        assertNull(AutomaticWhitelist.reason("org.example.com.android.settings"))
    }
    @Test fun knownOverseasPackagesAreRecognizedExactly() {
        for (pkg in listOf("com.whatsapp", "org.telegram.messenger", "com.bitwarden.mobile")) {
            assertEquals(AutomaticWhitelist.Reason.KNOWN_OVERSEAS_APP, AutomaticWhitelist.reason(pkg))
            assertNull(AutomaticWhitelist.reason("$pkg.clone"))
        }
    }
    @Test fun domesticAndUnknownAppsRemainOff() {
        for (pkg in listOf("com.tencent.mm", "com.taobao.taobao", "com.ss.android.ugc.aweme", "com.example.englishapp", "com.unknown.app")) {
            assertNull(AutomaticWhitelist.reason(pkg))
        }
    }
    @Test fun batchSelectsOnlyInstalledMatchesAndDeduplicates() {
        val selected = AutomaticWhitelist.select(listOf("com.whatsapp", "com.whatsapp", "com.android.settings", "com.tencent.mm"))
        assertEquals(setOf("com.whatsapp", "com.android.settings"), selected)
        assertFalse("com.google.android.gm" in selected)
    }
    @Test fun vendorFamiliesCoverNewProductsWithBoundaryProtection() {
        for (prefix in listOf("com.google", "com.microsoft", "com.apple", "com.adobe", "me.proton")) {
            assertNotNull(AutomaticWhitelist.reason("$prefix.newproduct"))
            assertNull(AutomaticWhitelist.reason("${prefix}fake.app"))
            assertNull(AutomaticWhitelist.reason("org.example.$prefix.app"))
        }
    }
    @Test fun acquiredAndSpecialPackageNamesAreIncluded() {
        for (pkg in listOf("com.azure.authenticator", "com.swiftkey.swiftkeyconfigurator", "tv.twitch.android.app", "com.linkedin.android", "com.apple.movetoios")) {
            assertNotNull(AutomaticWhitelist.reason(pkg))
        }
    }
    @Test fun emptyInstalledListProducesEmptySelection() {
        assertTrue(AutomaticWhitelist.select(emptyList()).isEmpty())
    }
}
