package com.initcn.powertools.feature.callblocker.domain

import com.initcn.powertools.feature.callblocker.data.CallRuleEntity
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CallEvaluatorTest {

    private lateinit var evaluator: CallEvaluator

    @Before
    fun setup() {
        evaluator = CallEvaluator()
    }

    @Test
    fun `whitelist overrides blocklists and strict settings`() {
        // Even if the number is an unsaved contact and exactly matches the blocklist,
        // the Whitelist must force it to be allowed.
        val testNumber = "+18005551234"

        val whitelist = listOf(CallRuleEntity(pattern = "+18005551234", ruleType = RuleType.WHITELIST))
        val blocklist = listOf(CallRuleEntity(pattern = "+18005551234", ruleType = RuleType.BLOCKLIST_EXACT))

        val result = evaluator.evaluateCall(
            incomingNumber = testNumber,
            isSavedContact = false, // Not a contact
            isHiddenNumber = false,
            whitelist = whitelist,
            exactBlocklist = blocklist,
            regexBlocklist = emptyList(),
            blockHiddenPref = false,
            blockUnsavedPref = true // Block unsaved is ON
        )

        assertTrue("Whitelist must override all block settings", result is CallResult.Allow)
    }

    @Test
    fun `exact match blocklist successfully blocks number`() {
        val testNumber = "+18005559999"
        val exactBlocklist = listOf(CallRuleEntity(pattern = "18005559999", ruleType = RuleType.BLOCKLIST_EXACT))

        val result = evaluator.evaluateCall(
            incomingNumber = testNumber,
            isSavedContact = false,
            isHiddenNumber = false,
            whitelist = emptyList(),
            exactBlocklist = exactBlocklist,
            regexBlocklist = emptyList(),
            blockHiddenPref = false,
            blockUnsavedPref = false
        )

        assertTrue("Number matching exact blocklist must be blocked", result is CallResult.Block)
    }

    @Test
    fun `regex blocklist correctly catches area codes and prefixes`() {
        val spamNumber = "+18004445555" // Toll free number

        // Regex rule meaning "Starts with +1800"
        val regexRule = CallRuleEntity(pattern = "^\\+1800.*", ruleType = RuleType.BLOCKLIST_REGEX)

        val result = evaluator.evaluateCall(
            incomingNumber = spamNumber,
            isSavedContact = false,
            isHiddenNumber = false,
            whitelist = emptyList(),
            exactBlocklist = emptyList(),
            regexBlocklist = listOf(regexRule),
            blockHiddenPref = false,
            blockUnsavedPref = false
        )

        assertTrue("Regex pattern should match and block the prefix", result is CallResult.Block)
    }

    @Test
    fun `malformed regex rule does not crash evaluator and allows call`() {
        val testNumber = "+12223334444"

        // Missing closing bracket makes this an invalid Regex
        val badRegex = CallRuleEntity(pattern = "^[0-9", ruleType = RuleType.BLOCKLIST_REGEX)

        val result = evaluator.evaluateCall(
            incomingNumber = testNumber,
            isSavedContact = true,
            isHiddenNumber = false,
            whitelist = emptyList(),
            exactBlocklist = emptyList(),
            regexBlocklist = listOf(badRegex),
            blockHiddenPref = false,
            blockUnsavedPref = false
        )

        // If the test completes without throwing an Exception, the try/catch is working.
        assertTrue("Malformed regex should gracefully fail and allow the call", result is CallResult.Allow)
    }

    @Test
    fun `block hidden numbers setting correctly traps missing caller ID`() {
        val result = evaluator.evaluateCall(
            incomingNumber = "", // Empty caller ID
            isSavedContact = false,
            isHiddenNumber = true,
            whitelist = emptyList(),
            exactBlocklist = emptyList(),
            regexBlocklist = emptyList(),
            blockHiddenPref = true, // Preference is ON
            blockUnsavedPref = false
        )

        assertTrue("Hidden caller ID must be blocked when preference is enabled", result is CallResult.Block)
    }

    @Test
    fun `formatting differences like spaces and dashes are ignored`() {
        // User inputs rule as "800-555-1234"
        val exactBlocklist = listOf(CallRuleEntity(pattern = "800-555-1234", ruleType = RuleType.BLOCKLIST_EXACT))

        // Telecom system delivers number as "800 555 1234"
        val testNumber = "800 555 1234"

        val result = evaluator.evaluateCall(
            incomingNumber = testNumber,
            isSavedContact = false,
            isHiddenNumber = false,
            whitelist = emptyList(),
            exactBlocklist = exactBlocklist,
            regexBlocklist = emptyList(),
            blockHiddenPref = false,
            blockUnsavedPref = false
        )

        assertTrue("Evaluator must sanitize spaces and dashes before comparing", result is CallResult.Block)
    }
}