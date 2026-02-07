#!/bin/bash

# MCP Git Server Launcher
# This script builds and runs the MCP Git Server
#
# Usage:
#   ./mcp-git-server.sh [http|stdio] [--rebuild]
#
# Modes:
#   http  - Start HTTP server (default)
#   stdio - Use stdin/stdout communication (for Claude Desktop)

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd "$SCRIPT_DIR"

# Parse arguments
MODE="http"
REBUILD=false

for arg in "$@"; do
    case $arg in
        --rebuild)
            REBUILD=true
            ;;
        http|stdio|stdin|server)
            MODE=$arg
            # Normalize mode names
            if [ "$MODE" == "stdin" ]; then
                MODE="stdio"
            elif [ "$MODE" == "server" ]; then
                MODE="http"
            fi
            ;;
        *)
            echo "Unknown argument: $arg" >&2
            echo "Usage: $0 [http|stdio] [--rebuild]" >&2
            exit 1
            ;;
    esac
done

# Build the project if needed
if [ ! -d "build" ] || [ "$REBUILD" == true ]; then
    echo "Building MCP Git Server..." >&2
    ./gradlew build -q
    if [ $? -ne 0 ]; then
        echo "Build failed!" >&2
        exit 1
    fi
fi

# Run the server with the specified mode
exec ./gradlew run -q --console=plain --args="$MODE"
