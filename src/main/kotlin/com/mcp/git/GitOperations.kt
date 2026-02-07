package com.mcp.git

import java.io.File
import java.io.BufferedReader
import java.io.InputStreamReader

class GitOperations(private val workingDirectory: String = ".") {

    private fun executeCommand(vararg command: String): Result<String> {
        return try {
            val process = ProcessBuilder(*command)
                .directory(File(workingDirectory))
                .redirectErrorStream(true)
                .start()

            val output = process.inputStream.bufferedReader().use { it.readText() }
            val exitCode = process.waitFor()

            if (exitCode == 0) {
                Result.success(output.trim())
            } else {
                Result.failure(Exception("Command failed with exit code $exitCode: $output"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun status(): Result<String> {
        return executeCommand("git", "status")
    }

    fun diff(filePath: String? = null, staged: Boolean = false): Result<String> {
        val args = mutableListOf("git", "diff")
        if (staged) args.add("--staged")
        if (filePath != null) args.add(filePath)
        return executeCommand(*args.toTypedArray())
    }

    fun commit(message: String, addAll: Boolean = false): Result<String> {
        return if (addAll) {
            val addResult = executeCommand("git", "add", ".")
            if (addResult.isFailure) {
                addResult
            } else {
                executeCommand("git", "commit", "-m", message)
            }
        } else {
            executeCommand("git", "commit", "-m", message)
        }
    }

    fun log(maxCount: Int = 10, oneline: Boolean = true): Result<String> {
        val args = mutableListOf("git", "log")
        args.add("--max-count=$maxCount")
        if (oneline) args.add("--oneline")
        return executeCommand(*args.toTypedArray())
    }

    fun branch(action: String = "list", branchName: String? = null): Result<String> {
        return when (action) {
            "list" -> executeCommand("git", "branch", "-a")
            "create" -> {
                if (branchName == null) {
                    Result.failure(Exception("Branch name required for create action"))
                } else {
                    executeCommand("git", "branch", branchName)
                }
            }
            "delete" -> {
                if (branchName == null) {
                    Result.failure(Exception("Branch name required for delete action"))
                } else {
                    executeCommand("git", "branch", "-d", branchName)
                }
            }
            else -> Result.failure(Exception("Unknown action: $action"))
        }
    }

    fun checkout(target: String, createNew: Boolean = false): Result<String> {
        val args = mutableListOf("git", "checkout")
        if (createNew) args.add("-b")
        args.add(target)
        return executeCommand(*args.toTypedArray())
    }

    fun add(files: String): Result<String> {
        return executeCommand("git", "add", files)
    }
}
