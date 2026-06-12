package com.initcn.powertools.feature.callblocker

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.initcn.powertools.feature.callblocker.data.CallBlockerPrefs
import com.initcn.powertools.feature.callblocker.data.CallRuleDao
import com.initcn.powertools.feature.callblocker.data.CallRuleEntity
import com.initcn.powertools.feature.callblocker.domain.CallEvaluator
import com.initcn.powertools.feature.callblocker.domain.CallResult
import com.initcn.powertools.feature.callblocker.domain.RuleType
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CallBlockerIntegrationTest {

    private lateinit var context: Context
    private lateinit var db: CallRuleDatabase
    private lateinit var dao: CallRuleDao
    private lateinit var prefs: CallBlockerPrefs
    private lateinit var evaluator: CallEvaluator

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()

        // 1. Spin up an isolated, in-memory database that wipes itself after the test
        db = Room.inMemoryDatabaseBuilder(
            context,
            CallRuleDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.callRuleDao() // Adjust to your actual DAO accessor

        // 2. Setup real SharedPreferences strictly for this test
        val sharedPrefs = context.getSharedPreferences("test_call_blocker_prefs", Context.MODE_PRIVATE)
        sharedPrefs.edit().clear().commit()
        prefs = CallBlockerPrefs(sharedPrefs)

        // 3. Initialize the core evaluator
        evaluator = CallEvaluator()
    }

    @After
    fun teardown() {
        db.close()
    }

    // --- E2E SCENARIOS ---

    @Test
    fun e2e_incomingCall_matchesExactBlocklist_isBlocked() = runBlocking {
        // Arrange: User adds a spam number to the blocklist
        val spamNumber = "8005551234"
        dao.insertRule(CallRuleEntity(pattern = spamNumber, ruleType = RuleType.BLOCKLIST_EXACT))

        // Act: A call comes in. The service pulls from the DAO and evaluates.
        val result = evaluator.evaluateCall(
            incomingNumber = spamNumber,
            isSavedContact = false,
            isHiddenNumber = false,
            whitelist = dao.getWhitelistSync(),
            exactBlocklist = dao.getExactBlocklistSync(),
            regexBlocklist = dao.getRegexBlocklistSync(),
            blockHiddenPref = prefs.isBlockHiddenEnabled(),
            blockUnsavedPref = prefs.isBlockUnsavedEnabled()
        )

        // Assert
        assertTrue("Call should be blocked by Exact Match", result is CallResult.Block)
    }

    @Test
    fun e2e_incomingCall_matchesWhitelist_overridesBlocklist_isAllowed() = runBlocking {
        // Arrange: A number is somehow caught in a regex blocklist, but specifically whitelisted
        val doctorNumber = "555-999-0000"
        dao.insertRule(CallRuleEntity(pattern = "555-.*", ruleType = RuleType.BLOCKLIST_REGEX))
        dao.insertRule(CallRuleEntity(pattern = doctorNumber, ruleType = RuleType.WHITELIST))

        // Act
        val result = evaluator.evaluateCall(
            incomingNumber = doctorNumber,
            isSavedContact = false,
            isHiddenNumber = false,
            whitelist = dao.getWhitelistSync(),
            exactBlocklist = dao.getExactBlocklistSync(),
            regexBlocklist = dao.getRegexBlocklistSync(),
            blockHiddenPref = prefs.isBlockHiddenEnabled(),
            blockUnsavedPref = prefs.isBlockUnsavedEnabled()
        )

        // Assert
        assertTrue("Whitelist MUST override any blocklist rules", result is CallResult.Allow)
    }

    @Test
    fun e2e_incomingCall_hiddenNumber_blockedWhenSettingEnabled() = runBlocking {
        // Arrange: User toggles "Block Hidden Numbers" in the UI
        prefs.setBlockHidden(true)
        val hiddenNumber = ""

        // Act
        val result = evaluator.evaluateCall(
            incomingNumber = hiddenNumber,
            isSavedContact = false,
            isHiddenNumber = true,
            whitelist = dao.getWhitelistSync(),
            exactBlocklist = dao.getExactBlocklistSync(),
            regexBlocklist = dao.getRegexBlocklistSync(),
            blockHiddenPref = prefs.isBlockHiddenEnabled(),
            blockUnsavedPref = prefs.isBlockUnsavedEnabled()
        )

        // Assert
        assertTrue("Hidden call should be blocked when preference is enabled", result is CallResult.Block)
    }

    @Test
    fun e2e_failsafe_unsavedContact_notBlockedIfPermissionRevoked() = runBlocking {
        // Arrange: User toggles "Block Unsaved Contacts" ON, but later revokes the permission via Android Settings
        prefs.setBlockUnsaved(true)
        val incomingNumber = "1234567890"

        // Simulate the failsafe logic from PowerCallScreeningService.kt
        val hasContactsPermission = false

        // Act
        val result = evaluator.evaluateCall(
            incomingNumber = incomingNumber,
            isSavedContact = false,
            isHiddenNumber = false,
            whitelist = dao.getWhitelistSync(),
            exactBlocklist = dao.getExactBlocklistSync(),
            regexBlocklist = dao.getRegexBlocklistSync(),
            blockHiddenPref = prefs.isBlockHiddenEnabled(),

            // This is the critical failsafe injection you built
            blockUnsavedPref = prefs.isBlockUnsavedEnabled() && hasContactsPermission
        )

        // Assert
        assertTrue(
            "CRITICAL: App must allow call if Contacts permission was revoked to prevent blocking all calls!",
            result is CallResult.Allow
        )
    }
}