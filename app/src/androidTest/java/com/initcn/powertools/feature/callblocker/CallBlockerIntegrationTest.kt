package com.initcn.powertools.feature.callblocker

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.initcn.powertools.feature.callblocker.data.CallBlockerDatabase
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
    private lateinit var db: CallBlockerDatabase
    private lateinit var dao: CallRuleDao
    private lateinit var prefs: CallBlockerPrefs
    private lateinit var evaluator: CallEvaluator

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()

        // 1. Spin up an isolated, in-memory database that wipes itself after the test
        db = Room.inMemoryDatabaseBuilder(
            context,
            CallBlockerDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.callRuleDao()

        // 2. Clear the actual SharedPreferences used by the app to ensure a clean test state
        context.getSharedPreferences("call_blocker_prefs", Context.MODE_PRIVATE).edit().clear().commit()
        prefs = CallBlockerPrefs(context)

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
            blockUnsavedPref = prefs.isBlockUnsavedEnabled(),
            blockAllPref = prefs.isBlockAllEnabled()
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
            blockUnsavedPref = prefs.isBlockUnsavedEnabled(),
            blockAllPref = prefs.isBlockAllEnabled()
        )

        // Assert
        assertTrue("Whitelist MUST override any blocklist rules", result is CallResult.Allow)
    }

    @Test
    fun e2e_incomingCall_hiddenNumber_blockedWhenSettingEnabled() = runBlocking {
        // Arrange: User toggles "Block Hidden Numbers" in the UI
        prefs.setBlockHiddenEnabled(true)
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
            blockUnsavedPref = prefs.isBlockUnsavedEnabled(),
            blockAllPref = prefs.isBlockAllEnabled()
        )

        // Assert
        assertTrue("Hidden call should be blocked when preference is enabled", result is CallResult.Block)
    }

    @Test
    fun e2e_failsafe_unsavedContact_notBlockedIfPermissionRevoked() = runBlocking {
        // Arrange: User toggles "Block Unsaved Contacts" ON, but later revokes the permission via Android Settings
        prefs.setBlockUnsavedEnabled(true)
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
            blockUnsavedPref = prefs.isBlockUnsavedEnabled() && hasContactsPermission,
            blockAllPref = prefs.isBlockAllEnabled()
        )

        // Assert
        assertTrue(
            "CRITICAL: App must allow call if Contacts permission was revoked to prevent blocking all calls!",
            result is CallResult.Allow
        )
    }

    @Test
    fun e2e_incomingCall_blockAll_trapsEverythingExceptWhitelist() = runBlocking {
        // Arrange: User turns on "Block All" but has a whitelisted contact
        prefs.setBlockAllEnabled(true)

        val doctorNumber = "5551234"
        val randomSpammer = "9998887777"

        dao.insertRule(CallRuleEntity(pattern = doctorNumber, ruleType = RuleType.WHITELIST))

        // Act: Evaluate random spammer
        val spamResult = evaluator.evaluateCall(
            incomingNumber = randomSpammer,
            isSavedContact = true, // Even if they are saved!
            isHiddenNumber = false,
            whitelist = dao.getWhitelistSync(),
            exactBlocklist = dao.getExactBlocklistSync(),
            regexBlocklist = dao.getRegexBlocklistSync(),
            blockHiddenPref = prefs.isBlockHiddenEnabled(),
            blockUnsavedPref = prefs.isBlockUnsavedEnabled(),
            blockAllPref = prefs.isBlockAllEnabled()
        )

        // Act: Evaluate whitelisted doctor
        val doctorResult = evaluator.evaluateCall(
            incomingNumber = doctorNumber,
            isSavedContact = false,
            isHiddenNumber = false,
            whitelist = dao.getWhitelistSync(),
            exactBlocklist = dao.getExactBlocklistSync(),
            regexBlocklist = dao.getRegexBlocklistSync(),
            blockHiddenPref = prefs.isBlockHiddenEnabled(),
            blockUnsavedPref = prefs.isBlockUnsavedEnabled(),
            blockAllPref = prefs.isBlockAllEnabled()
        )

        // Assert
        assertTrue("Spam must be blocked by Block All", spamResult is CallResult.Block)
        assertTrue("Whitelist MUST override Block All", doctorResult is CallResult.Allow)
    }
}