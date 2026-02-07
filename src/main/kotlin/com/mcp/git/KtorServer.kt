package com.mcp.git

import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

fun Application.configureSerialization() {
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
            encodeDefaults = true
        })
    }
}

fun Application.configureHTTP() {
    install(CORS) {
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Get)
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Authorization)
        anyHost()
    }
}

fun Application.configureMonitoring() {
    // Logging is configured through logback
}

fun Application.configureRouting(mcpServer: McpServer) {
    val logger = LoggerFactory.getLogger("McpServer")

    routing {
        // Health check endpoint
        get("/health") {
            call.respond(HttpStatusCode.OK, mapOf("status" to "healthy"))
        }

        // MCP JSON-RPC endpoint
        post("/mcp") {
            try {
                val request = call.receive<JsonRpcRequest>()
                logger.info("Received MCP request: method=${request.method}, id=${request.id}")

                val response = mcpServer.handleRequest(request)

                if (response.error != null) {
                    logger.error("MCP error: ${response.error}")
                }

                call.respond(HttpStatusCode.OK, response)
            } catch (e: Exception) {
                logger.error("Error processing MCP request", e)
                call.respond(
                    HttpStatusCode.OK,
                    JsonRpcResponse(
                        id = null,
                        error = JsonRpcError(
                            code = -32603,
                            message = "Internal error: ${e.message}"
                        )
                    )
                )
            }
        }

        // Information endpoint
        get("/") {
            call.respondText(
                """
                MCP Git Server (HTTP)
                ====================

                POST /mcp        - MCP JSON-RPC endpoint
                GET  /health     - Health check

                Send JSON-RPC 2.0 requests to /mcp endpoint.

                Example:
                POST /mcp
                {"jsonrpc":"2.0","id":1,"method":"initialize","params":{}}
                """.trimIndent(),
                ContentType.Text.Plain
            )
        }
    }
}

fun createKtorServer(
    port: Int = 8080,
    host: String = "0.0.0.0",
    gitWorkingDir: String = "."
): NettyApplicationEngine {
    val gitOps = GitOperations(gitWorkingDir)
    val mcpServer = McpServer(gitOps)

    return embeddedServer(Netty, port = port, host = host) {
        configureSerialization()
        configureHTTP()
        configureMonitoring()
        configureRouting(mcpServer)
    }
}
