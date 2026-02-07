package com.mcp.git

import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import java.io.BufferedReader
import java.io.InputStreamReader

fun main(args: Array<String>) {
    val mode = args.firstOrNull() ?: "http"
    val workingDir = System.getenv("GIT_WORKING_DIR") ?: "."
    val port = System.getenv("MCP_PORT")?.toIntOrNull() ?: 8080
    val host = System.getenv("MCP_HOST") ?: "0.0.0.0"

    when (mode.lowercase()) {
        "http", "server" -> {
            println("Starting MCP Git Server (HTTP mode)...")
            println("Working directory: $workingDir")
            println("Server will start on http://$host:$port")

            val server = createKtorServer(port, host, workingDir)
            server.start(wait = true)
        }
        "stdio", "stdin" -> {
            runStdioMode(workingDir)
        }
        else -> {
            System.err.println("Unknown mode: $mode")
            System.err.println("Usage: java -jar mcp-git-server.jar [http|stdio]")
            System.err.println("  http  - Start HTTP server (default)")
            System.err.println("  stdio - Use stdin/stdout communication")
            kotlin.system.exitProcess(1)
        }
    }
}

private fun runStdioMode(workingDir: String) {
    val gitOps = GitOperations(workingDir)
    val server = McpServer(gitOps)
    val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    val reader = BufferedReader(InputStreamReader(System.`in`))

    System.err.println("MCP Git Server starting (STDIO mode)...")
    System.err.println("Working directory: $workingDir")

    try {
        while (true) {
            val line = reader.readLine() ?: break
            if (line.isBlank()) continue

            System.err.println("Received: $line")

            try {
                val request = json.decodeFromString<JsonRpcRequest>(line)
                val response = server.handleRequest(request)
                val responseJson = json.encodeToString(response)

                println(responseJson)
                System.out.flush()

                System.err.println("Sent: $responseJson")
            } catch (e: Exception) {
                System.err.println("Error processing request: ${e.message}")
                e.printStackTrace(System.err)

                val errorResponse = JsonRpcResponse(
                    id = null,
                    error = JsonRpcError(
                        code = -32700,
                        message = "Parse error: ${e.message}"
                    )
                )
                val errorJson = json.encodeToString(errorResponse)
                println(errorJson)
                System.out.flush()
            }
        }
    } catch (e: Exception) {
        System.err.println("Fatal error: ${e.message}")
        e.printStackTrace(System.err)
    }
}
