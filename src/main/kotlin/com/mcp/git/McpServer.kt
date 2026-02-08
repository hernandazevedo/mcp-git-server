package com.mcp.git

import kotlinx.serialization.json.*

class McpServer(private val gitOps: GitOperations) {

    private val json = Json {
        prettyPrint = false
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun handleRequest(request: JsonRpcRequest): JsonRpcResponse {
        return try {
            when (request.method) {
                "initialize" -> handleInitialize(request)
                "tools/list" -> handleToolsList(request)
                "tools/call" -> handleToolsCall(request)
                else -> errorResponse(request.id, -32601, "Method not found: ${request.method}")
            }
        } catch (e: Exception) {
            errorResponse(request.id, -32603, "Internal error: ${e.message}")
        }
    }

    private fun handleInitialize(request: JsonRpcRequest): JsonRpcResponse {
        val result = InitializeResult(
            protocolVersion = "2024-11-05",
            capabilities = Capabilities(
                tools = ToolsCapability(listChanged = false)
            ),
            serverInfo = ServerInfo(
                name = "mcp-git-server",
                version = "1.0.0"
            )
        )
        return JsonRpcResponse(
            id = request.id,
            result = json.encodeToJsonElement(result)
        )
    }

    private fun handleToolsList(request: JsonRpcRequest): JsonRpcResponse {
        val tools = listOf(
            Tool(
                name = "git_status",
                description = "Show the working tree status",
                inputSchema = buildJsonObject {
                    put("type", "object")
                    put("properties", buildJsonObject {})
                }
            ),
            Tool(
                name = "git_diff",
                description = "Show changes between commits, commit and working tree, etc",
                inputSchema = buildJsonObject {
                    put("type", "object")
                    put("properties", buildJsonObject {
                        put("file_path", buildJsonObject {
                            put("type", "string")
                            put("description", "Optional: specific file to show diff for")
                        })
                        put("staged", buildJsonObject {
                            put("type", "boolean")
                            put("description", "Show staged changes (default: false)")
                        })
                    })
                }
            ),
            Tool(
                name = "git_commit",
                description = "Record changes to the repository",
                inputSchema = buildJsonObject {
                    put("type", "object")
                    put("properties", buildJsonObject {
                        put("message", buildJsonObject {
                            put("type", "string")
                            put("description", "Commit message")
                        })
                        put("add_all", buildJsonObject {
                            put("type", "boolean")
                            put("description", "Stage all changes before committing (default: false)")
                        })
                    })
                    put("required", buildJsonArray { add("message") })
                }
            ),
            Tool(
                name = "git_log",
                description = "Show commit logs",
                inputSchema = buildJsonObject {
                    put("type", "object")
                    put("properties", buildJsonObject {
                        put("max_count", buildJsonObject {
                            put("type", "number")
                            put("description", "Limit the number of commits (default: 10)")
                        })
                        put("oneline", buildJsonObject {
                            put("type", "boolean")
                            put("description", "Show each commit on a single line (default: true)")
                        })
                    })
                }
            ),
            Tool(
                name = "git_branch",
                description = "List, create, or delete branches",
                inputSchema = buildJsonObject {
                    put("type", "object")
                    put("properties", buildJsonObject {
                        put("action", buildJsonObject {
                            put("type", "string")
                            put("enum", buildJsonArray {
                                add("list")
                                add("create")
                                add("delete")
                            })
                            put("description", "Action to perform (default: list)")
                        })
                        put("branch_name", buildJsonObject {
                            put("type", "string")
                            put("description", "Branch name (required for create/delete)")
                        })
                    })
                }
            ),
            Tool(
                name = "git_checkout",
                description = "Switch branches or restore working tree files",
                inputSchema = buildJsonObject {
                    put("type", "object")
                    put("properties", buildJsonObject {
                        put("target", buildJsonObject {
                            put("type", "string")
                            put("description", "Branch name or file path")
                        })
                        put("create_new", buildJsonObject {
                            put("type", "boolean")
                            put("description", "Create new branch (default: false)")
                        })
                    })
                    put("required", buildJsonArray { add("target") })
                }
            ),
            Tool(
                name = "git_add",
                description = "Add file contents to the staging area",
                inputSchema = buildJsonObject {
                    put("type", "object")
                    put("properties", buildJsonObject {
                        put("files", buildJsonObject {
                            put("type", "string")
                            put("description", "Files to add (e.g., '.', '*.kt', 'file.txt')")
                        })
                    })
                    put("required", buildJsonArray { add("files") })
                }
            ),
            Tool(
                name = "git_push",
                description = "Update remote refs along with associated objects",
                inputSchema = buildJsonObject {
                    put("type", "object")
                    put("properties", buildJsonObject {
                        put("remote", buildJsonObject {
                            put("type", "string")
                            put("description", "Remote repository name (default: origin)")
                        })
                        put("branch", buildJsonObject {
                            put("type", "string")
                            put("description", "Branch name to push")
                        })
                        put("set_upstream", buildJsonObject {
                            put("type", "boolean")
                            put("description", "Set upstream tracking branch (default: false)")
                        })
                        put("force", buildJsonObject {
                            put("type", "boolean")
                            put("description", "Force push (default: false)")
                        })
                    })
                }
            )
        )

        val result = ToolListResult(tools)
        return JsonRpcResponse(
            id = request.id,
            result = json.encodeToJsonElement(result)
        )
    }

    private fun handleToolsCall(request: JsonRpcRequest): JsonRpcResponse {
        val params = request.params ?: return errorResponse(request.id, -32602, "Missing params")
        val toolName = params["name"]?.jsonPrimitive?.content
            ?: return errorResponse(request.id, -32602, "Missing tool name")
        val arguments = params["arguments"]?.jsonObject ?: JsonObject(emptyMap())

        val result = when (toolName) {
            "git_status" -> callGitStatus()
            "git_diff" -> callGitDiff(arguments)
            "git_commit" -> callGitCommit(arguments)
            "git_log" -> callGitLog(arguments)
            "git_branch" -> callGitBranch(arguments)
            "git_checkout" -> callGitCheckout(arguments)
            "git_add" -> callGitAdd(arguments)
            "git_push" -> callGitPush(arguments)
            else -> return errorResponse(request.id, -32602, "Unknown tool: $toolName")
        }

        return JsonRpcResponse(
            id = request.id,
            result = json.encodeToJsonElement(result)
        )
    }

    private fun callGitStatus(): ToolCallResult {
        return gitOps.status().fold(
            onSuccess = { ToolCallResult(content = listOf(TextContent(text = it))) },
            onFailure = { ToolCallResult(content = listOf(TextContent(text = it.message ?: "Unknown error")), isError = true) }
        )
    }

    private fun callGitDiff(arguments: JsonObject): ToolCallResult {
        val filePath = arguments["file_path"]?.jsonPrimitive?.contentOrNull
        val staged = arguments["staged"]?.jsonPrimitive?.booleanOrNull ?: false

        return gitOps.diff(filePath, staged).fold(
            onSuccess = { ToolCallResult(content = listOf(TextContent(text = if (it.isEmpty()) "No changes" else it))) },
            onFailure = { ToolCallResult(content = listOf(TextContent(text = it.message ?: "Unknown error")), isError = true) }
        )
    }

    private fun callGitCommit(arguments: JsonObject): ToolCallResult {
        val message = arguments["message"]?.jsonPrimitive?.contentOrNull
            ?: return ToolCallResult(content = listOf(TextContent(text = "Missing commit message")), isError = true)
        val addAll = arguments["add_all"]?.jsonPrimitive?.booleanOrNull ?: false

        return gitOps.commit(message, addAll).fold(
            onSuccess = { ToolCallResult(content = listOf(TextContent(text = it))) },
            onFailure = { ToolCallResult(content = listOf(TextContent(text = it.message ?: "Unknown error")), isError = true) }
        )
    }

    private fun callGitLog(arguments: JsonObject): ToolCallResult {
        val maxCount = arguments["max_count"]?.jsonPrimitive?.intOrNull ?: 10
        val oneline = arguments["oneline"]?.jsonPrimitive?.booleanOrNull ?: true

        return gitOps.log(maxCount, oneline).fold(
            onSuccess = { ToolCallResult(content = listOf(TextContent(text = it))) },
            onFailure = { ToolCallResult(content = listOf(TextContent(text = it.message ?: "Unknown error")), isError = true) }
        )
    }

    private fun callGitBranch(arguments: JsonObject): ToolCallResult {
        val action = arguments["action"]?.jsonPrimitive?.contentOrNull ?: "list"
        val branchName = arguments["branch_name"]?.jsonPrimitive?.contentOrNull

        return gitOps.branch(action, branchName).fold(
            onSuccess = { ToolCallResult(content = listOf(TextContent(text = it))) },
            onFailure = { ToolCallResult(content = listOf(TextContent(text = it.message ?: "Unknown error")), isError = true) }
        )
    }

    private fun callGitCheckout(arguments: JsonObject): ToolCallResult {
        val target = arguments["target"]?.jsonPrimitive?.contentOrNull
            ?: return ToolCallResult(content = listOf(TextContent(text = "Missing target")), isError = true)
        val createNew = arguments["create_new"]?.jsonPrimitive?.booleanOrNull ?: false

        return gitOps.checkout(target, createNew).fold(
            onSuccess = { ToolCallResult(content = listOf(TextContent(text = it))) },
            onFailure = { ToolCallResult(content = listOf(TextContent(text = it.message ?: "Unknown error")), isError = true) }
        )
    }

    private fun callGitAdd(arguments: JsonObject): ToolCallResult {
        val files = arguments["files"]?.jsonPrimitive?.contentOrNull
            ?: return ToolCallResult(content = listOf(TextContent(text = "Missing files parameter")), isError = true)

        return gitOps.add(files).fold(
            onSuccess = { ToolCallResult(content = listOf(TextContent(text = if (it.isEmpty()) "Files added successfully" else it))) },
            onFailure = { ToolCallResult(content = listOf(TextContent(text = it.message ?: "Unknown error")), isError = true) }
        )
    }

    private fun callGitPush(arguments: JsonObject): ToolCallResult {
        val remote = arguments["remote"]?.jsonPrimitive?.contentOrNull
        val branch = arguments["branch"]?.jsonPrimitive?.contentOrNull
        val setUpstream = arguments["set_upstream"]?.jsonPrimitive?.booleanOrNull ?: false
        val force = arguments["force"]?.jsonPrimitive?.booleanOrNull ?: false

        return gitOps.push(remote, branch, setUpstream, force).fold(
            onSuccess = { ToolCallResult(content = listOf(TextContent(text = if (it.isEmpty()) "Pushed successfully" else it))) },
            onFailure = { ToolCallResult(content = listOf(TextContent(text = it.message ?: "Unknown error")), isError = true) }
        )
    }

    private fun errorResponse(id: JsonElement?, code: Int, message: String): JsonRpcResponse {
        return JsonRpcResponse(
            id = id,
            error = JsonRpcError(code = code, message = message)
        )
    }
}
