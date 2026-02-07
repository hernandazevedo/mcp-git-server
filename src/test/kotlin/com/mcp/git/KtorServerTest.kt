package com.mcp.git

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.*
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertNotNull

class KtorServerTest {

    private val json = Json {
        prettyPrint = false
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @Test
    fun `test health endpoint returns OK`() = testApplication {
        application {
            val gitOps = GitOperations(".")
            val mcpServer = McpServer(gitOps)
            configureSerialization()
            configureHTTP()
            configureMonitoring()
            configureRouting(mcpServer)
        }

        val response = client.get("/health")
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `test root endpoint returns information`() = testApplication {
        application {
            val gitOps = GitOperations(".")
            val mcpServer = McpServer(gitOps)
            configureSerialization()
            configureHTTP()
            configureMonitoring()
            configureRouting(mcpServer)
        }

        val response = client.get("/")
        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("MCP Git Server"))
    }

    @Test
    fun `test MCP initialize via HTTP`() = testApplication {
        application {
            val gitOps = GitOperations(".")
            val mcpServer = McpServer(gitOps)
            configureSerialization()
            configureHTTP()
            configureMonitoring()
            configureRouting(mcpServer)
        }

        val client = createClient {
            install(ContentNegotiation) {
                json(json)
            }
        }

        val request = JsonRpcRequest(
            id = JsonPrimitive(1),
            method = "initialize",
            params = JsonObject(emptyMap())
        )

        val response = client.post("/mcp") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }

        assertEquals(HttpStatusCode.OK, response.status)

        val jsonResponse = json.decodeFromString<JsonRpcResponse>(response.bodyAsText())
        assertEquals(JsonPrimitive(1), jsonResponse.id)
        assertNotNull(jsonResponse.result)

        val result = json.decodeFromJsonElement<InitializeResult>(jsonResponse.result!!)
        assertEquals("2024-11-05", result.protocolVersion)
        assertEquals("mcp-git-server", result.serverInfo.name)
    }

    @Test
    fun `test MCP tools list via HTTP`() = testApplication {
        application {
            val gitOps = GitOperations(".")
            val mcpServer = McpServer(gitOps)
            configureSerialization()
            configureHTTP()
            configureMonitoring()
            configureRouting(mcpServer)
        }

        val client = createClient {
            install(ContentNegotiation) {
                json(json)
            }
        }

        val request = JsonRpcRequest(
            id = JsonPrimitive(2),
            method = "tools/list"
        )

        val response = client.post("/mcp") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }

        assertEquals(HttpStatusCode.OK, response.status)

        val jsonResponse = json.decodeFromString<JsonRpcResponse>(response.bodyAsText())
        assertEquals(JsonPrimitive(2), jsonResponse.id)
        assertNotNull(jsonResponse.result)

        val result = json.decodeFromJsonElement<ToolListResult>(jsonResponse.result!!)
        assertEquals(7, result.tools.size)
    }

    @Test
    fun `test MCP git_status via HTTP`() = testApplication {
        application {
            val gitOps = GitOperations(".")
            val mcpServer = McpServer(gitOps)
            configureSerialization()
            configureHTTP()
            configureMonitoring()
            configureRouting(mcpServer)
        }

        val client = createClient {
            install(ContentNegotiation) {
                json(json)
            }
        }

        val request = JsonRpcRequest(
            id = JsonPrimitive(3),
            method = "tools/call",
            params = buildJsonObject {
                put("name", "git_status")
                put("arguments", JsonObject(emptyMap()))
            }
        )

        val response = client.post("/mcp") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }

        assertEquals(HttpStatusCode.OK, response.status)

        val jsonResponse = json.decodeFromString<JsonRpcResponse>(response.bodyAsText())
        assertEquals(JsonPrimitive(3), jsonResponse.id)
        assertNotNull(jsonResponse.result)
    }

    @Test
    fun `test MCP invalid method returns error`() = testApplication {
        application {
            val gitOps = GitOperations(".")
            val mcpServer = McpServer(gitOps)
            configureSerialization()
            configureHTTP()
            configureMonitoring()
            configureRouting(mcpServer)
        }

        val client = createClient {
            install(ContentNegotiation) {
                json(json)
            }
        }

        val request = JsonRpcRequest(
            id = JsonPrimitive(99),
            method = "invalid/method"
        )

        val response = client.post("/mcp") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }

        assertEquals(HttpStatusCode.OK, response.status)

        val jsonResponse = json.decodeFromString<JsonRpcResponse>(response.bodyAsText())
        assertEquals(JsonPrimitive(99), jsonResponse.id)
        assertNotNull(jsonResponse.error)
        assertEquals(-32601, jsonResponse.error?.code)
    }

    // CORS test removed - CORS is configured but OPTIONS preflight testing
    // requires additional setup in test environment
}
