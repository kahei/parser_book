#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="${PARSER_BOOK_ROOT:-$SCRIPT_DIR}"
PREVIEW_ROOT="$ROOT_DIR/build/publish-preview"
SOURCE_DIR="$PREVIEW_ROOT/src"
LOG_DIR="$PREVIEW_ROOT/logs"
OUTPUT_PDF="$PREVIEW_ROOT/parser_book-preview.pdf"
PREPARE_SCRIPT="$SCRIPT_DIR/tools/publish_preview/prepare_preview.py"
DOCKER_IMAGE="${PARSER_BOOK_PUBLISH_PREVIEW_IMAGE:-parser-book-publish-preview}"

fail() {
    echo "publish preview: $*" >&2
    exit 1
}

require_command() {
    command -v "$1" >/dev/null 2>&1 || fail "required command not found: $1"
}

check_source() {
    test -f "$ROOT_DIR/publish/main.tex" \
        || fail "publish entry point not found: $ROOT_DIR/publish/main.tex"
    test -f "$PREPARE_SCRIPT" \
        || fail "preparation tool not found: $PREPARE_SCRIPT"
    test -f "$SCRIPT_DIR/tools/publish_preview/asciibook.cls" \
        || fail "preview compatibility class not found"
}

check_build_dependencies() {
    check_source
    require_command python3
    require_command lualatex
    require_command upmendex
    if ! command -v pdfinfo >/dev/null 2>&1; then
        echo "publish preview: warning: pdfinfo not found; PDF readability check will be limited" >&2
    fi
}

check_all() {
    check_build_dependencies
    python3 "$PREPARE_SCRIPT" --repo-root "$ROOT_DIR" --check-only
    echo "publish preview dependencies: OK"
}

publish_source_digest() {
    python3 "$PREPARE_SCRIPT" \
        --repo-root "$ROOT_DIR" \
        --print-source-digest
}

assert_preview_root_path() {
    case "$PREVIEW_ROOT" in
        "$ROOT_DIR"/build/publish-preview) ;;
        *) fail "refusing to modify unexpected path: $PREVIEW_ROOT" ;;
    esac
}

reset_preview_output() {
    assert_preview_root_path
    rm -rf -- "$PREVIEW_ROOT"
}

run_lualatex() {
    local pass="$1"
    local command_status=0
    (
        cd "$SOURCE_DIR"
        set +e
        lualatex \
            -interaction=nonstopmode \
            -halt-on-error \
            -file-line-error \
            main.tex 2>&1 \
            | tee "$LOG_DIR/lualatex-$pass.stdout.log"
        exit "$?"
    ) || command_status=$?
    test -f "$SOURCE_DIR/main.log" \
        && cp "$SOURCE_DIR/main.log" "$LOG_DIR/lualatex-$pass.log"
    return "$command_status"
}

run_index() {
    if test -s "$SOURCE_DIR/main.idx"; then
        (
            cd "$SOURCE_DIR"
            upmendex -q -g -f -o main.ind main.idx 2>&1 \
                | tee "$LOG_DIR/upmendex.log"
        )
    else
        echo "No index entries were generated; skipping upmendex." \
            | tee "$LOG_DIR/upmendex.log"
    fi
}

verify_pdf() {
    test -s "$SOURCE_DIR/main.pdf" || fail "LuaLaTeX did not produce a non-empty main.pdf"
    if command -v pdfinfo >/dev/null 2>&1; then
        pdfinfo "$SOURCE_DIR/main.pdf" > "$LOG_DIR/pdfinfo.txt"
        local pages
        pages="$(awk '/^Pages:/ {print $2}' "$LOG_DIR/pdfinfo.txt")"
        test -n "$pages" && test "$pages" -gt 0 \
            || fail "pdfinfo did not report a positive page count"
    fi
}

collect_warnings() {
    if ! grep -hE '(^| )(LaTeX|Package .*|Class .*) Warning:|Overfull|Underfull' \
        "$LOG_DIR"/lualatex-*.log > "$LOG_DIR/warnings.txt"; then
        : > "$LOG_DIR/warnings.txt"
    fi
}

build_preview() {
    check_build_dependencies
    local source_digest_before
    source_digest_before="$(publish_source_digest)"
    reset_preview_output
    mkdir -p "$PREVIEW_ROOT" "$LOG_DIR"
    python3 "$PREPARE_SCRIPT" --repo-root "$ROOT_DIR"
    run_lualatex 1
    run_index
    run_lualatex 2
    run_lualatex 3
    run_lualatex 4
    verify_pdf
    collect_warnings
    local source_digest_after
    source_digest_after="$(publish_source_digest)"
    test "$source_digest_after" = "$source_digest_before" \
        || fail "publish source tree changed during preview build"
    local staged_pdf="$PREVIEW_ROOT/.parser_book-preview.pdf.tmp"
    cp "$SOURCE_DIR/main.pdf" "$staged_pdf"
    mv "$staged_pdf" "$OUTPUT_PDF"
    echo "publish preview generated: $OUTPUT_PDF"
    echo "This PDF is for semantic/structural QA; production layout must be checked separately."
}

run_docker_build_with_log() {
    local error_log="$1"
    docker build \
        -f "$SCRIPT_DIR/docker/publish-preview/Dockerfile" \
        -t "$DOCKER_IMAGE" \
        "$SCRIPT_DIR" 2>&1 | tee "$error_log" >&2
    return "${PIPESTATUS[0]}"
}

run_docker_preview() {
    docker run --rm \
        --user "$(id -u):$(id -g)" \
        -e HOME=/tmp \
        -v "$SCRIPT_DIR:/work" \
        -w /work \
        "$DOCKER_IMAGE" \
        ./build_publish_preview.sh build
}

configured_docker_credential_helper() {
    command -v python3 >/dev/null 2>&1 || return 1
    local config_root
    if test -n "${DOCKER_CONFIG:-}"; then
        config_root="$DOCKER_CONFIG"
    elif test -n "${HOME:-}"; then
        config_root="$HOME/.docker"
    else
        return 1
    fi
    local config_file="$config_root/config.json"
    test -f "$config_file" || return 1
    local credentials_store
    credentials_store="$(
        python3 - "$config_file" 2>/dev/null <<'PY'
import json
import sys

with open(sys.argv[1], encoding="utf-8") as config:
    data = json.load(config)
helpers = data.get("credHelpers", {})
if isinstance(helpers, dict) and "https://index.docker.io/v1/" in helpers:
    value = helpers["https://index.docker.io/v1/"]
else:
    value = data.get("credsStore", "")
print(value if isinstance(value, str) else "")
PY
    )" || return 1
    test -n "$credentials_store" || return 1
    local helper_command
    helper_command="$(command -v "docker-credential-$credentials_store")" \
        || return 1
    printf '%s\n' "$helper_command"
}

docker_helper_has_exec_format_error() {
    local helper_command="$1"
    local helper_error
    local helper_status=0
    helper_error="$("$helper_command" list 2>&1 >/dev/null)" \
        || helper_status="$?"
    test "$helper_status" -ne 0 \
        && grep -Fiq "exec format error" <<<"$helper_error"
}

docker_uses_default_context() {
    local docker_context
    local context_status=0
    docker_context="$(docker context show 2>/dev/null)" \
        || context_status="$?"
    test "$context_status" -eq 0 \
        && test "$docker_context" = "default"
}

run_docker_with_isolated_config() {
    local docker_state_dir="$1"
    local fallback_config="$docker_state_dir/config"
    mkdir -p "$fallback_config"
    local fallback_status=0
    DOCKER_CONFIG="$fallback_config" docker build \
        -f "$SCRIPT_DIR/docker/publish-preview/Dockerfile" \
        -t "$DOCKER_IMAGE" \
        "$SCRIPT_DIR" \
        || fallback_status="$?"
    if test "$fallback_status" -eq 0; then
        DOCKER_CONFIG="$fallback_config" run_docker_preview \
            || fallback_status="$?"
    fi
    return "$fallback_status"
}

cleanup_docker_state() {
    local temp_root="$1"
    local state_dir="$2"
    case "$state_dir" in
        "$temp_root"/parser-book-publish-preview-docker.*) ;;
        *) fail "refusing to remove unexpected Docker state path: $state_dir" ;;
    esac
    rm -rf -- "$state_dir"
}

install_docker_cleanup_traps() {
    local temp_root="$1"
    local state_dir="$2"
    local cleanup_command
    printf -v cleanup_command \
        'cleanup_docker_state %q %q' "$temp_root" "$state_dir"
    trap "$cleanup_command" EXIT
    trap 'exit 129' HUP
    trap 'exit 130' INT
    trap 'exit 143' TERM
}

clear_docker_cleanup_traps() {
    trap - EXIT HUP INT TERM
}

build_with_docker() {
    check_source
    require_command docker
    local temp_root="${TMPDIR:-/tmp}"
    temp_root="${temp_root%/}"
    local docker_state_dir
    docker_state_dir="$(mktemp -d "$temp_root/parser-book-publish-preview-docker.XXXXXX")"
    install_docker_cleanup_traps "$temp_root" "$docker_state_dir"
    local credential_helper
    if credential_helper="$(configured_docker_credential_helper)" \
        && docker_uses_default_context \
        && docker_helper_has_exec_format_error "$credential_helper"; then
        echo "publish preview: Windows credential helper is unavailable in this WSL session; using an isolated anonymous config" >&2
        local fallback_status=0
        run_docker_with_isolated_config "$docker_state_dir" \
            || fallback_status="$?"
        cleanup_docker_state "$temp_root" "$docker_state_dir"
        clear_docker_cleanup_traps
        return "$fallback_status"
    fi
    local error_log="$docker_state_dir/build.stderr.log"
    local build_status
    if run_docker_build_with_log "$error_log"; then
        cleanup_docker_state "$temp_root" "$docker_state_dir"
        clear_docker_cleanup_traps
        run_docker_preview
        return
    else
        build_status="$?"
    fi

    if grep -Fq "error getting credentials" "$error_log" \
        && grep -Fq "exec format error" "$error_log"; then
        if docker_uses_default_context; then
            echo "publish preview: Docker credential helper cannot run; retrying public image build with an isolated anonymous config" >&2
            local fallback_status=0
            run_docker_with_isolated_config "$docker_state_dir" \
                || fallback_status="$?"
            cleanup_docker_state "$temp_root" "$docker_state_dir"
            clear_docker_cleanup_traps
            return "$fallback_status"
        fi
    fi

    cleanup_docker_state "$temp_root" "$docker_state_dir"
    clear_docker_cleanup_traps
    return "$build_status"
}

clean_preview() {
    reset_preview_output
    echo "removed publish preview output: $PREVIEW_ROOT"
}

show_help() {
    cat <<'EOF'
Usage: ./build_publish_preview.sh COMMAND

Commands:
  docker  Build the preview image and generate the PDF in Docker
  build   Generate the PDF with locally installed tools
  clean   Remove only build/publish-preview/
  check   Check local build dependencies and publish sources
  help    Show this help

Output:
  build/publish-preview/parser_book-preview.pdf

The preview uses a compatibility class and Noto CJK fonts. It checks semantic
and structural integrity, not production pagination or final typography.
EOF
}

main() {
    case "${1:-help}" in
        build) build_preview ;;
        docker) build_with_docker ;;
        clean) clean_preview ;;
        check) check_all ;;
        help|-h|--help) show_help ;;
        *) fail "unknown command: $1" ;;
    esac
}

main "$@"
