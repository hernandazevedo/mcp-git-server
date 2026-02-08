package com.mcp.git

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.serialization.json.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class McpServerTest {

    private lateinit var gitOps: GitOperations
    private lateinit var server: McpServer
    private val json = Json {
        prettyPrint = false
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @BeforeEach
    fun setup() {
        gitOps = mockk()
        server = McpServer(gitOps)
    }

    @Test
    fun `test initialize returns correct protocol version`() {
        val request = JsonRpcRequest(
            id = JsonPrimitive(1),
            method = "initialize"
        )

        val response = server.handleRequest(request)

        assertEquals(JsonPrimitive(1), response.id)
        assertNull(response.error)
        assertNotNull(response.result)

        val result = json.decodeFromJsonElement<InitializeResult>(response.result!!)
        assertEquals("2024-11-05", result.protocolVersion)
        assertEquals("mcp-git-server", result.serverInfo.name)
        assertEquals("1.0.0", result.serverInfo.version)
    }

    @Test
    fun `test tools list returns all available tools`() {
        val request = JsonRpcRequest(
            id = JsonPrimitive(2),
            method = "tools/list"
        )

        val response = server.handleRequest(request)

        assertEquals(JsonPrimitive(2), response.id)
        assertNull(response.error)
        assertNotNull(response.result)

        val result = json.decodeFromJsonElement<ToolListResult>(response.result!!)
        assertEquals(8, result.tools.size)

        val toolNames = result.tools.map { it.name }
        assertTrue(toolNames.contains("git_status"))
        assertTrue(toolNames.contains("git_diff"))
        assertTrue(toolNames.contains("git_commit"))
        assertTrue(toolNames.contains("git_log"))
        assertTrue(toolNames.contains("git_branch"))
        assertTrue(toolNames.contains("git_checkout"))
        assertTrue(toolNames.contains("git_add"))
        assertTrue(toolNames.contains("git_push"))
    }

    @Test
    fun `test git_status tool call`() {
        every { gitOps.status() } returns Result.success("On branch main\nnothing to commit")

        val request = JsonRpcRequest(
            id = JsonPrimitive(3),
            method = "tools/call",
            params = buildJsonObject {
                put("name", "git_status")
                put("arguments", JsonObject(emptyMap()))
            }
        )

        val response = server.handleRequest(request)

        assertEquals(JsonPrimitive(3), response.id)
        assertNull(response.error)
        assertNotNull(response.result)

        verify(exactly = 1) { gitOps.status() }
    }

    @Test
    fun `test git_diff tool call with parameters`() {
        every { gitOps.diff("test.txt", true) } returns Result.success("diff content")

        val request = JsonRpcRequest(
            id = JsonPrimitive(4),
            method = "tools/call",
            params = buildJsonObject {
                put("name", "git_diff")
                put("arguments", buildJsonObject {
                    put("file_path", "test.txt")
                    put("staged", true)
                })
            }
        )

        val response = server.handleRequest(request)

        assertEquals(JsonPrimitive(4), response.id)
        assertNull(response.error)
        assertNotNull(response.result)

        verify(exactly = 1) { gitOps.diff("test.txt", true) }
    }

    @Test
    fun `test git_commit tool call`() {
        every { gitOps.commit("test commit", false) } returns Result.success("Commit successful")

        val request = JsonRpcRequest(
            id = JsonPrimitive(5),
            method = "tools/call",
            params = buildJsonObject {
                put("name", "git_commit")
                put("arguments", buildJsonObject {
                    put("message", "test commit")
                    put("add_all", false)
                })
            }
        )

        val response = server.handleRequest(request)

        assertEquals(JsonPrimitive(5), response.id)
        assertNull(response.error)
        assertNotNull(response.result)

        verify(exactly = 1) { gitOps.commit("test commit", false) }
    }

    @Test
    fun `test git_commit without message returns error`() {
        val request = JsonRpcRequest(
            id = JsonPrimitive(6),
            method = "tools/call",
            params = buildJsonObject {
                put("name", "git_commit")
                put("arguments", buildJsonObject {
                    put("add_all", false)
                })
            }
        )

        val response = server.handleRequest(request)

        assertEquals(JsonPrimitive(6), response.id)
        assertNotNull(response.result)

        val result = json.decodeFromJsonElement<ToolCallResult>(response.result!!)
        assertTrue(result.isError)
        assertTrue(result.content[0].text.contains("Missing commit message"))
    }

    @Test
    fun `test git_log tool call with parameters`() {
        every { gitOps.log(5, true) } returns Result.success("commit1\ncommit2")

        val request = JsonRpcRequest(
            id = JsonPrimitive(7),
            method = "tools/call",
            params = buildJsonObject {
                put("name", "git_log")
                put("arguments", buildJsonObject {
                    put("max_count", 5)
                    put("oneline", true)
                })
            }
        )

        val response = server.handleRequest(request)

        assertEquals(JsonPrimitive(7), response.id)
        assertNull(response.error)
        assertNotNull(response.result)

        verify(exactly = 1) { gitOps.log(5, true) }
    }

    @Test
    fun `test git_branch list action`() {
        every { gitOps.branch("list", null) } returns Result.success("* main\n  develop")

        val request = JsonRpcRequest(
            id = JsonPrimitive(8),
            method = "tools/call",
            params = buildJsonObject {
                put("name", "git_branch")
                put("arguments", buildJsonObject {
                    put("action", "list")
                })
            }
        )

        val response = server.handleRequest(request)

        assertEquals(JsonPrimitive(8), response.id)
        assertNull(response.error)
        assertNotNull(response.result)

        verify(exactly = 1) { gitOps.branch("list", null) }
    }

    @Test
    fun `test git_branch create action`() {
        every { gitOps.branch("create", "new-branch") } returns Result.success("Branch created")

        val request = JsonRpcRequest(
            id = JsonPrimitive(9),
            method = "tools/call",
            params = buildJsonObject {
                put("name", "git_branch")
                put("arguments", buildJsonObject {
                    put("action", "create")
                    put("branch_name", "new-branch")
                })
            }
        )

        val response = server.handleRequest(request)

        assertEquals(JsonPrimitive(9), response.id)
        assertNull(response.error)
        assertNotNull(response.result)

        verify(exactly = 1) { gitOps.branch("create", "new-branch") }
    }

    @Test
    fun `test git_checkout tool call`() {
        every { gitOps.checkout("main", false) } returns Result.success("Switched to branch main")

        val request = JsonRpcRequest(
            id = JsonPrimitive(10),
            method = "tools/call",
            params = buildJsonObject {
                put("name", "git_checkout")
                put("arguments", buildJsonObject {
                    put("target", "main")
                    put("create_new", false)
                })
            }
        )

        val response = server.handleRequest(request)

        assertEquals(JsonPrimitive(10), response.id)
        assertNull(response.error)
        assertNotNull(response.result)

        verify(exactly = 1) { gitOps.checkout("main", false) }
    }

    @Test
    fun `test git_add tool call`() {
        every { gitOps.add(".") } returns Result.success("")

        val request = JsonRpcRequest(
            id = JsonPrimitive(11),
            method = "tools/call",
            params = buildJsonObject {
                put("name", "git_add")
                put("arguments", buildJsonObject {
                    put("files", ".")
                })
            }
        )

        val response = server.handleRequest(request)

        assertEquals(JsonPrimitive(11), response.id)
        assertNull(response.error)
        assertNotNull(response.result)

        verify(exactly = 1) { gitOps.add(".") }
    }

    @Test
    fun `test unknown method returns error`() {
        val request = JsonRpcRequest(
            id = JsonPrimitive(12),
            method = "unknown/method"
        )

        val response = server.handleRequest(request)

        assertEquals(JsonPrimitive(12), response.id)
        assertNotNull(response.error)
        assertEquals(-32601, response.error?.code)
        assertTrue(response.error?.message?.contains("Method not found") == true)
    }

    @Test
    fun `test unknown tool returns error`() {
        val request = JsonRpcRequest(
            id = JsonPrimitive(13),
            method = "tools/call",
            params = buildJsonObject {
                put("name", "unknown_tool")
                put("arguments", JsonObject(emptyMap()))
            }
        )

        val response = server.handleRequest(request)

        assertEquals(JsonPrimitive(13), response.id)
        assertNotNull(response.error)
        assertEquals(-32602, response.error?.code)
        assertTrue(response.error?.message?.contains("Unknown tool") == true)
    }

    @Test
    fun `test git operation failure returns error in result`() {
        every { gitOps.status() } returns Result.failure(Exception("Git error"))

        val request = JsonRpcRequest(
            id = JsonPrimitive(14),
            method = "tools/call",
            params = buildJsonObject {
                put("name", "git_status")
                put("arguments", JsonObject(emptyMap()))
            }
        )

        val response = server.handleRequest(request)

        assertEquals(JsonPrimitive(14), response.id)
        assertNull(response.error)
        assertNotNull(response.result)

        val result = json.decodeFromJsonElement<ToolCallResult>(response.result!!)
        assertTrue(result.isError)
        assertTrue(result.content[0].text.contains("Git error"))
    }
}
