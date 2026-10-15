package com.example

import com.example.ai.AgentBrain
import com.example.ai.AgentBrainResult
import com.example.ai.BuiltInOfflineLlm
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class BuiltInOfflineLlmTest {

  @Test
  fun testOrganizeDownloadsNaturalLanguage() {
    val prompt = AgentBrain.buildPrompt("could you please organize my downloads folder?", "10 GB used of 64 GB")
    val rawOutput = BuiltInOfflineLlm.generate(prompt)
    val result = AgentBrain.parseOutput(rawOutput, "/storage/emulated/0")

    assertTrue(result is AgentBrainResult.ActionPlan)
    val plan = result as AgentBrainResult.ActionPlan
    assertTrue(plan.action is PendingAction.ConfirmOrganize)
    val organize = plan.action as PendingAction.ConfirmOrganize
    assertTrue(organize.path.endsWith("Download"))
  }

  @Test
  fun testSearchPdfFilesNaturalLanguage() {
    val prompt = AgentBrain.buildPrompt("can you find all my pdf documents?", "10 GB used of 64 GB")
    val rawOutput = BuiltInOfflineLlm.generate(prompt)
    val result = AgentBrain.parseOutput(rawOutput, "/storage/emulated/0")

    assertTrue(result is AgentBrainResult.DirectAction)
    val action = result as AgentBrainResult.DirectAction
    assertTrue(action.command.contains("pdf"))
  }

  @Test
  fun testCheckStorageNaturalLanguage() {
    val prompt = AgentBrain.buildPrompt("how much free space is left on my phone?", "10 GB used of 64 GB")
    val rawOutput = BuiltInOfflineLlm.generate(prompt)
    val result = AgentBrain.parseOutput(rawOutput, "/storage/emulated/0")

    assertTrue(result is AgentBrainResult.DirectAction)
    val action = result as AgentBrainResult.DirectAction
    assertEquals("size", action.command)
  }

  @Test
  fun testCleanEmptyFoldersNaturalLanguage() {
    val prompt = AgentBrain.buildPrompt("clean up all empty folders please", "10 GB used of 64 GB")
    val rawOutput = BuiltInOfflineLlm.generate(prompt)
    val result = AgentBrain.parseOutput(rawOutput, "/storage/emulated/0")

    assertTrue(result is AgentBrainResult.ActionPlan)
    val plan = result as AgentBrainResult.ActionPlan
    assertTrue(plan.action is PendingAction.ConfirmCleanEmpty)
  }

  @Test
  fun testDeleteFileNaturalLanguage() {
    val prompt = AgentBrain.buildPrompt("please delete the old file 'old_report.txt'", "10 GB used of 64 GB")
    val rawOutput = BuiltInOfflineLlm.generate(prompt)
    val result = AgentBrain.parseOutput(rawOutput, "/storage/emulated/0")

    assertTrue(result is AgentBrainResult.ActionPlan)
    val plan = result as AgentBrainResult.ActionPlan
    assertTrue(plan.action is PendingAction.ConfirmDelete)
    val del = plan.action as PendingAction.ConfirmDelete
    assertTrue(del.path.contains("old_report.txt"))
  }

  @Test
  fun testConversationalGreeting() {
    val prompt = AgentBrain.buildPrompt("Hello! Who are you and how do you work?", "10 GB used of 64 GB")
    val rawOutput = BuiltInOfflineLlm.generate(prompt)
    val result = AgentBrain.parseOutput(rawOutput, "/storage/emulated/0")

    assertTrue(result is AgentBrainResult.Conversational)
    val conv = result as AgentBrainResult.Conversational
    assertTrue(conv.text.contains("Local File Agent") || conv.text.contains("offline"))
  }
}
