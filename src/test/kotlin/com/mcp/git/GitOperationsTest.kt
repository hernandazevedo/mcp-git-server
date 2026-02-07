package com.mcp.git

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertEquals
import org.junit.jupiter.api.BeforeEach

class GitOperationsTest {

    @TempDir
    lateinit var tempDir: File

    private lateinit var gitOps: GitOperations

    @BeforeEach
    fun setup() {
        // Initialize a git repository in the temp directory
        ProcessBuilder("git", "init")
            .directory(tempDir)
            .start()
            .waitFor()

        // Configure git user for commits
        ProcessBuilder("git", "config", "user.email", "test@example.com")
            .directory(tempDir)
            .start()
            .waitFor()

        ProcessBuilder("git", "config", "user.name", "Test User")
            .directory(tempDir)
            .start()
            .waitFor()

        gitOps = GitOperations(tempDir.absolutePath)
    }

    @Test
    fun `test status returns success for clean repository`() {
        val result = gitOps.status()
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()?.contains("On branch") == true)
    }

    @Test
    fun `test diff returns empty for no changes`() {
        val result = gitOps.diff()
        assertTrue(result.isSuccess)
        assertEquals("", result.getOrNull())
    }

    @Test
    fun `test diff shows changes when file is modified`() {
        // Create and add a file
        val testFile = File(tempDir, "test.txt")
        testFile.writeText("initial content")
        gitOps.add("test.txt")
        gitOps.commit("initial commit")

        // Modify the file
        testFile.writeText("modified content")

        val result = gitOps.diff()
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()?.contains("modified content") == true)
    }

    @Test
    fun `test add files successfully`() {
        val testFile = File(tempDir, "test.txt")
        testFile.writeText("test content")

        val result = gitOps.add("test.txt")
        assertTrue(result.isSuccess)
    }

    @Test
    fun `test commit without staged files fails`() {
        val result = gitOps.commit("test commit")
        assertTrue(result.isFailure)
    }

    @Test
    fun `test commit with staged files succeeds`() {
        val testFile = File(tempDir, "test.txt")
        testFile.writeText("test content")
        gitOps.add("test.txt")

        val result = gitOps.commit("test commit")
        assertTrue(result.isSuccess)
    }

    @Test
    fun `test commit with add_all stages and commits`() {
        val testFile = File(tempDir, "test.txt")
        testFile.writeText("test content")

        val result = gitOps.commit("test commit", addAll = true)
        assertTrue(result.isSuccess)
    }

    @Test
    fun `test log returns commit history`() {
        // Create a commit
        val testFile = File(tempDir, "test.txt")
        testFile.writeText("test content")
        gitOps.commit("initial commit", addAll = true)

        val result = gitOps.log(maxCount = 10, oneline = true)
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()?.contains("initial commit") == true)
    }

    @Test
    fun `test branch list returns current branch`() {
        // Create initial commit (required for branch to exist)
        val testFile = File(tempDir, "test.txt")
        testFile.writeText("test content")
        gitOps.commit("initial commit", addAll = true)

        val result = gitOps.branch(action = "list")
        assertTrue(result.isSuccess)
        val output = result.getOrNull() ?: ""
        assertTrue(output.contains("main") || output.contains("master"), "Expected branch name in: $output")
    }

    @Test
    fun `test branch create creates new branch`() {
        // Create initial commit (required for branch creation)
        val testFile = File(tempDir, "test.txt")
        testFile.writeText("test content")
        gitOps.commit("initial commit", addAll = true)

        val result = gitOps.branch(action = "create", branchName = "test-branch")
        assertTrue(result.isSuccess)

        val listResult = gitOps.branch(action = "list")
        val output = listResult.getOrNull() ?: ""
        assertTrue(output.contains("test-branch"), "Expected test-branch in: $output")
    }

    @Test
    fun `test branch delete removes branch`() {
        // Create initial commit (required for branch creation)
        val testFile = File(tempDir, "test.txt")
        testFile.writeText("test content")
        gitOps.commit("initial commit", addAll = true)

        // Create a branch first
        gitOps.branch(action = "create", branchName = "test-branch")

        val result = gitOps.branch(action = "delete", branchName = "test-branch")
        assertTrue(result.isSuccess)

        val listResult = gitOps.branch(action = "list")
        val output = listResult.getOrNull() ?: ""
        assertFalse(output.contains("test-branch"), "test-branch should not be in: $output")
    }

    @Test
    fun `test branch without name for create fails`() {
        val result = gitOps.branch(action = "create", branchName = null)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Branch name required") == true)
    }

    @Test
    fun `test checkout switches to existing branch`() {
        // Create initial commit (required for branch creation)
        val testFile = File(tempDir, "test.txt")
        testFile.writeText("test content")
        gitOps.commit("initial commit", addAll = true)

        // Create and checkout new branch
        gitOps.branch(action = "create", branchName = "test-branch")
        val result = gitOps.checkout("test-branch")

        assertTrue(result.isSuccess)
    }

    @Test
    fun `test checkout with create_new creates and switches to branch`() {
        // Create initial commit (required for branch creation)
        val testFile = File(tempDir, "test.txt")
        testFile.writeText("test content")
        gitOps.commit("initial commit", addAll = true)

        val result = gitOps.checkout("new-branch", createNew = true)
        assertTrue(result.isSuccess)

        val statusResult = gitOps.status()
        assertTrue(statusResult.getOrNull()?.contains("On branch new-branch") == true)
    }

    @Test
    fun `test diff with staged flag shows staged changes`() {
        val testFile = File(tempDir, "test.txt")
        testFile.writeText("test content")
        gitOps.add("test.txt")

        val result = gitOps.diff(staged = true)
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()?.contains("test content") == true)
    }

    @Test
    fun `test diff with file path shows specific file changes`() {
        val testFile1 = File(tempDir, "test1.txt")
        val testFile2 = File(tempDir, "test2.txt")
        testFile1.writeText("content1")
        testFile2.writeText("content2")
        gitOps.add(".")
        gitOps.commit("initial commit")

        testFile1.writeText("modified1")
        testFile2.writeText("modified2")

        val result = gitOps.diff(filePath = "test1.txt")
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()?.contains("modified1") == true)
        assertFalse(result.getOrNull()?.contains("modified2") == true)
    }
}
