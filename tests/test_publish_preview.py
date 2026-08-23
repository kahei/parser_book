from __future__ import annotations

import json
import os
import shutil
import subprocess
import sys
import tempfile
import threading
import time
import unittest
from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parents[1]
PREPARE_SCRIPT = PROJECT_ROOT / "tools/publish_preview/prepare_preview.py"
BUILD_SCRIPT = PROJECT_ROOT / "build_publish_preview.sh"

MAIN_TEX = r"""\documentclass[a4paper,9pt,openright,tombow,]{asciibook}
\usepackage[match]{luatexja-fontspec}

% 明朝体（リュウミン）の直接指定
\setmainjfont[
  Path = /Users/kahei/Library/Fonts/MorisawaFonts/, % フォルダのパス
  BoldFont = AP-OTF-RyuminPr6N-Bold.otf,            % 太字のファイル名
%  ItalicFont = AP-RyuminPr6N-R.otf                  % 必要なら指定
]{AP-OTF-RyuminPr6N-Regular.otf}                    % 標準のファイル名

% ゴシック体（新ゴ）の直接指定
\setsansjfont[
  Path = /Users/kahei/Library/Fonts/MorisawaFonts/,
  BoldFont = AP-OTF-ShinGoPr6N-Bold.otf
]{AP-OTF-ShinGoPr6N-Medium.otf}

\begin{document}
\include{ch01}
\end{document}
"""


class PublishPreviewPreparationTest(unittest.TestCase):
    def setUp(self) -> None:
        self.temp_dir = tempfile.TemporaryDirectory()
        self.repo = Path(self.temp_dir.name)
        publish = self.repo / "publish"
        publish.mkdir()
        (publish / "main.tex").write_text(MAIN_TEX, encoding="utf-8")
        (publish / "ch01.tex").write_text("本文\n", encoding="utf-8")
        (publish / "main.pdf").write_bytes(b"production pdf sentinel\n")
        self.original_publish = self.read_publish_tree()

    def tearDown(self) -> None:
        self.temp_dir.cleanup()

    def read_publish_tree(self) -> dict[str, bytes]:
        publish = self.repo / "publish"
        return {
            path.relative_to(publish).as_posix(): path.read_bytes()
            for path in sorted(publish.rglob("*"))
            if path.is_file()
        }

    def run_prepare(self) -> subprocess.CompletedProcess[str]:
        return subprocess.run(
            [sys.executable, str(PREPARE_SCRIPT), "--repo-root", str(self.repo)],
            cwd=PROJECT_ROOT,
            text=True,
            capture_output=True,
            check=False,
        )

    def test_prepare_copies_sources_and_replaces_only_known_font_blocks(self) -> None:
        result = self.run_prepare()

        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual(self.read_publish_tree(), self.original_publish)

        output_root = self.repo / "build/publish-preview"
        prepared_main = (output_root / "src/main.tex").read_text(encoding="utf-8")
        self.assertIn(r"\setmainjfont{Noto Serif CJK JP}", prepared_main)
        self.assertIn(r"\setsansjfont{Noto Sans CJK JP}", prepared_main)
        self.assertNotIn("/Users/kahei/Library/Fonts/MorisawaFonts/", prepared_main)
        self.assertEqual(
            (output_root / "src/ch01.tex").read_text(encoding="utf-8"),
            "本文\n",
        )
        self.assertEqual(
            (output_root / "src/main.pdf").read_bytes(),
            b"production pdf sentinel\n",
        )
        self.assertTrue((output_root / "src/asciibook.cls").is_file())

        manifest = json.loads(
            (output_root / "prepare-manifest.json").read_text(encoding="utf-8")
        )
        self.assertEqual(manifest["source"], "publish")
        self.assertEqual(manifest["destination"], "build/publish-preview/src")
        self.assertEqual(manifest["class_mode"], "preview-shim")
        self.assertEqual(manifest["font_mode"], "noto")
        self.assertEqual(
            manifest["replacements"],
            [
                {"file": "main.tex", "kind": "main-font-config", "count": 1},
                {"file": "main.tex", "kind": "sans-font-config", "count": 1},
            ],
        )
        source_diff = (output_root / "logs/source-diff.patch").read_text(
            encoding="utf-8"
        )
        self.assertIn("+\\setmainjfont{Noto Serif CJK JP}", source_diff)
        self.assertEqual(
            [
                line
                for line in source_diff.splitlines()
                if line.startswith("--- ") or line.startswith("+++ ")
            ],
            [
                "--- publish/asciibook.cls",
                "+++ preview/asciibook.cls",
                "--- publish/main.tex",
                "+++ preview/main.tex",
            ],
        )

    def test_prepare_rejects_an_unexpected_font_block(self) -> None:
        main = self.repo / "publish/main.tex"
        main.write_text(
            main.read_text(encoding="utf-8").replace(
                "AP-OTF-RyuminPr6N-Regular.otf", "Unexpected-Regular.otf"
            ),
            encoding="utf-8",
        )

        result = self.run_prepare()

        self.assertNotEqual(result.returncode, 0)
        self.assertIn("main-font-config", result.stderr)
        self.assertIn("expected 1 replacement, found 0", result.stderr)

    def test_prepare_rejects_drift_inside_the_reviewed_font_block(self) -> None:
        main = self.repo / "publish/main.tex"
        main.write_text(
            main.read_text(encoding="utf-8").replace(
                "/Users/kahei/Library/Fonts/MorisawaFonts/", "/tmp/other-fonts/", 1
            ),
            encoding="utf-8",
        )

        result = self.run_prepare()

        self.assertNotEqual(result.returncode, 0)
        self.assertIn("main-font-config", result.stderr)
        self.assertIn("expected 1 replacement, found 0", result.stderr)

    def test_prepare_rejects_missing_direct_graphic(self) -> None:
        main = self.repo / "publish/main.tex"
        main.write_text(
            main.read_text(encoding="utf-8").replace(
                r"\begin{document}",
                "\\begin{document}\n\\includegraphics{img/missing.pdf}",
            ),
            encoding="utf-8",
        )

        result = self.run_prepare()

        self.assertNotEqual(result.returncode, 0)
        self.assertIn("missing local asset", result.stderr)
        self.assertIn("img/missing.pdf", result.stderr)

    def test_missing_extensionless_graphic_lists_the_attempted_candidates(self) -> None:
        main = self.repo / "publish/main.tex"
        main.write_text(
            main.read_text(encoding="utf-8").replace(
                r"\begin{document}",
                "\\begin{document}\n\\includegraphics{img/missing}",
            ),
            encoding="utf-8",
        )

        result = self.run_prepare()

        self.assertNotEqual(result.returncode, 0)
        self.assertIn("img/missing (tried:", result.stderr)
        self.assertIn("img/missing.pdf", result.stderr)
        self.assertIn("img/missing.eps", result.stderr)

    def test_prepare_ignores_commented_local_references(self) -> None:
        main = self.repo / "publish/main.tex"
        main.write_text(
            main.read_text(encoding="utf-8").replace(
                r"\begin{document}",
                "\\begin{document}\n% \\includegraphics{img/missing.pdf}",
            ),
            encoding="utf-8",
        )

        result = self.run_prepare()

        self.assertEqual(result.returncode, 0, result.stderr)

    def test_prepare_rejects_missing_included_tex(self) -> None:
        main = self.repo / "publish/main.tex"
        main.write_text(
            main.read_text(encoding="utf-8").replace("ch01", "missing-chapter"),
            encoding="utf-8",
        )

        result = self.run_prepare()

        self.assertNotEqual(result.returncode, 0)
        self.assertIn("missing local asset", result.stderr)
        self.assertIn("missing-chapter.tex", result.stderr)


class PublishPreviewCliTest(unittest.TestCase):
    def setUp(self) -> None:
        self.temp_dir = tempfile.TemporaryDirectory()
        self.repo = Path(self.temp_dir.name)

    def tearDown(self) -> None:
        self.temp_dir.cleanup()

    def run_cli(
        self,
        command: str,
        extra_env: dict[str, str] | None = None,
    ) -> subprocess.CompletedProcess[str]:
        env = {**os.environ, "PARSER_BOOK_ROOT": str(self.repo)}
        if extra_env:
            env.update(extra_env)
        with tempfile.TemporaryFile(mode="w+", encoding="utf-8") as stdout:
            with tempfile.TemporaryFile(mode="w+", encoding="utf-8") as stderr:
                result = subprocess.run(
                    ["bash", str(BUILD_SCRIPT), command],
                    cwd=self.repo,
                    env=env,
                    text=True,
                    stdout=stdout,
                    stderr=stderr,
                    check=False,
                )
                stdout.seek(0)
                stderr.seek(0)
                return subprocess.CompletedProcess(
                    result.args,
                    result.returncode,
                    stdout.read(),
                    stderr.read(),
                )

    def prepare_fake_docker(self) -> tuple[Path, Path, Path]:
        publish = self.repo / "publish"
        publish.mkdir()
        (publish / "main.tex").write_text(MAIN_TEX, encoding="utf-8")

        fake_bin = self.repo / "fake-bin"
        fake_bin.mkdir()
        fake_docker = fake_bin / "docker"
        fake_docker.write_text(
            """#!/usr/bin/env bash
set -eu
printf '%s|%s\\n' "$1" "${DOCKER_CONFIG:-}" >> "$FAKE_DOCKER_LOG"
case "$1" in
    build)
        if test "${DOCKER_CONFIG:-}" = "$BROKEN_DOCKER_CONFIG"; then
            initial_status="${FAKE_DOCKER_INITIAL_STATUS:-1}"
            if test "$initial_status" -ne 0; then
                echo "${FAKE_DOCKER_INITIAL_ERROR:-error getting credentials - err: fork/exec /usr/bin/docker-credential-desktop.exe: exec format error}" >&2
            fi
            exit "$initial_status"
        fi
        if test "${FAKE_DOCKER_SIGNAL_FALLBACK:-0}" = 1; then
            kill -TERM "$PPID"
            exit 143
        fi
        exit "${FAKE_DOCKER_FALLBACK_STATUS:-0}"
        ;;
    context)
        test "${2:-}" = show
        echo "${FAKE_DOCKER_CONTEXT:-default}"
        exit "${FAKE_DOCKER_CONTEXT_STATUS:-0}"
        ;;
    run)
        exit "${FAKE_DOCKER_RUN_STATUS:-0}"
        ;;
    *) exit 2 ;;
esac
""",
            encoding="utf-8",
        )
        fake_docker.chmod(0o755)
        broken_config = self.repo / "broken-docker-config"
        broken_config.mkdir()
        (broken_config / "sentinel").write_text("keep\n", encoding="utf-8")
        docker_log = self.repo / "docker-calls.log"
        return fake_bin, broken_config, docker_log

    def fake_docker_env(
        self,
        fake_bin: Path,
        broken_config: Path,
        docker_log: Path,
        **overrides: str,
    ) -> dict[str, str]:
        return {
            "PATH": f"{fake_bin}{os.pathsep}{os.environ['PATH']}",
            "DOCKER_CONFIG": str(broken_config),
            "BROKEN_DOCKER_CONFIG": str(broken_config),
            "FAKE_DOCKER_LOG": str(docker_log),
            **overrides,
        }

    @staticmethod
    def install_exec_format_helper(fake_bin: Path, helper_suffix: str) -> None:
        fake_helper = fake_bin / f"docker-credential-{helper_suffix}"
        fake_helper.write_text(
            "#!/usr/bin/env bash\n"
            "if test -n \"${FAKE_HELPER_LOG:-}\"; then\n"
            "    echo \"${1:-}\" >> \"$FAKE_HELPER_LOG\"\n"
            "fi\n"
            "echo 'cannot execute binary file: Exec format error' >&2\n"
            "exit 126\n",
            encoding="utf-8",
        )
        fake_helper.chmod(0o755)

    def test_clean_removes_only_publish_preview_output(self) -> None:
        keep = self.repo / "build/keep.txt"
        remove = self.repo / "build/publish-preview/remove.txt"
        keep.parent.mkdir(parents=True)
        remove.parent.mkdir(parents=True)
        keep.write_text("keep", encoding="utf-8")
        remove.write_text("remove", encoding="utf-8")

        result = self.run_cli("clean")

        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertTrue(keep.exists())
        self.assertFalse(remove.parent.exists())

    def test_help_names_all_supported_commands_and_output(self) -> None:
        result = self.run_cli("help")

        self.assertEqual(result.returncode, 0, result.stderr)
        for command in ("docker", "build", "clean", "check", "help"):
            self.assertIn(command, result.stdout)
        self.assertIn("build/publish-preview/parser_book-preview.pdf", result.stdout)

    def test_check_rejects_a_missing_local_asset_without_creating_output(self) -> None:
        publish = self.repo / "publish"
        publish.mkdir()
        (publish / "main.tex").write_text(MAIN_TEX, encoding="utf-8")

        result = self.run_cli("check")

        self.assertNotEqual(result.returncode, 0)
        self.assertIn("missing local asset", result.stderr)
        self.assertIn("ch01.tex", result.stderr)
        self.assertFalse((self.repo / "build/publish-preview").exists())

    def test_unknown_command_fails(self) -> None:
        result = self.run_cli("unknown")

        self.assertNotEqual(result.returncode, 0)
        self.assertIn("unknown command", result.stderr)

    def test_docker_skips_a_known_broken_wsl_helper_before_building(self) -> None:
        fake_bin, broken_config, docker_log = self.prepare_fake_docker()
        (broken_config / "config.json").write_text(
            json.dumps({"credsStore": "desktop.exe"}),
            encoding="utf-8",
        )
        self.install_exec_format_helper(fake_bin, "desktop.exe")

        result = self.run_cli(
            "docker",
            self.fake_docker_env(fake_bin, broken_config, docker_log),
        )

        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertNotIn("error getting credentials", result.stderr)
        self.assertIn("isolated anonymous config", result.stderr)
        calls = [
            line.split("|", 1)
            for line in docker_log.read_text().splitlines()
        ]
        self.assertEqual([command for command, _ in calls], ["context", "build", "run"])
        fallback_config = calls[1][1]
        self.assertNotEqual(fallback_config, str(broken_config))
        self.assertEqual(calls[2][1], fallback_config)
        self.assertFalse(Path(fallback_config).parent.exists())

    def test_docker_skips_a_broken_docker_hub_credential_helper(self) -> None:
        fake_bin, broken_config, docker_log = self.prepare_fake_docker()
        (broken_config / "config.json").write_text(
            json.dumps(
                {
                    "credHelpers": {
                        "https://index.docker.io/v1/": "desktop.exe",
                    },
                }
            ),
            encoding="utf-8",
        )
        self.install_exec_format_helper(fake_bin, "desktop.exe")

        result = self.run_cli(
            "docker",
            self.fake_docker_env(fake_bin, broken_config, docker_log),
        )

        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertNotIn("error getting credentials", result.stderr)
        calls = [line.split("|", 1) for line in docker_log.read_text().splitlines()]
        self.assertEqual([command for command, _ in calls], ["context", "build", "run"])
        self.assertNotEqual(calls[1][1], str(broken_config))
        self.assertEqual(calls[2][1], calls[1][1])

    def test_docker_does_not_probe_the_helper_on_a_non_default_context(self) -> None:
        fake_bin, broken_config, docker_log = self.prepare_fake_docker()
        (broken_config / "config.json").write_text(
            json.dumps({"credsStore": "desktop.exe"}),
            encoding="utf-8",
        )
        self.install_exec_format_helper(fake_bin, "desktop.exe")
        helper_log = self.repo / "credential-helper-calls.log"

        result = self.run_cli(
            "docker",
            self.fake_docker_env(
                fake_bin,
                broken_config,
                docker_log,
                FAKE_DOCKER_CONTEXT="desktop-linux",
                FAKE_HELPER_LOG=str(helper_log),
            ),
        )

        self.assertEqual(result.returncode, 1)
        self.assertFalse(helper_log.exists())

    def test_docker_retries_with_an_isolated_config_when_desktop_helper_cannot_run(
        self,
    ) -> None:
        fake_bin, broken_config, docker_log = self.prepare_fake_docker()

        result = self.run_cli(
            "docker",
            self.fake_docker_env(fake_bin, broken_config, docker_log),
        )

        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertIn("retrying public image build", result.stderr)
        calls = [line.split("|", 1) for line in docker_log.read_text().splitlines()]
        self.assertEqual([command for command, _ in calls], ["build", "context", "build", "run"])
        self.assertEqual(calls[0][1], str(broken_config))
        self.assertEqual(calls[1][1], str(broken_config))
        fallback_config = calls[2][1]
        self.assertNotEqual(fallback_config, str(broken_config))
        self.assertEqual(calls[3][1], fallback_config)
        self.assertFalse(Path(fallback_config).parent.exists())
        self.assertEqual(
            (broken_config / "sentinel").read_text(encoding="utf-8"),
            "keep\n",
        )

    def test_docker_normal_success_keeps_the_user_config(self) -> None:
        fake_bin, broken_config, docker_log = self.prepare_fake_docker()
        temp_root = self.repo / "tmp"
        temp_root.mkdir()

        result = self.run_cli(
            "docker",
            self.fake_docker_env(
                fake_bin,
                broken_config,
                docker_log,
                FAKE_DOCKER_INITIAL_STATUS="0",
                TMPDIR=str(temp_root),
            ),
        )

        self.assertEqual(result.returncode, 0, result.stderr)
        calls = [line.split("|", 1) for line in docker_log.read_text().splitlines()]
        self.assertEqual([command for command, _ in calls], ["build", "run"])
        self.assertEqual(
            [config for _, config in calls],
            [str(broken_config), str(broken_config)],
        )
        self.assertEqual(list(temp_root.iterdir()), [])
        self.assertEqual(
            (broken_config / "sentinel").read_text(encoding="utf-8"),
            "keep\n",
        )

    def test_docker_requires_both_credential_error_markers_before_retry(self) -> None:
        for error in (
            "error getting credentials: permission denied",
            "helper failed with exec format error",
        ):
            with self.subTest(error=error):
                self.temp_dir.cleanup()
                self.temp_dir = tempfile.TemporaryDirectory()
                self.repo = Path(self.temp_dir.name)
                fake_bin, broken_config, docker_log = self.prepare_fake_docker()

                result = self.run_cli(
                    "docker",
                    self.fake_docker_env(
                        fake_bin,
                        broken_config,
                        docker_log,
                        FAKE_DOCKER_INITIAL_ERROR=error,
                    ),
                )

                self.assertEqual(result.returncode, 1)
                calls = [
                    line.split("|", 1)
                    for line in docker_log.read_text().splitlines()
                ]
                self.assertEqual([command for command, _ in calls], ["build"])

    def test_docker_does_not_retry_with_a_non_default_context(self) -> None:
        fake_bin, broken_config, docker_log = self.prepare_fake_docker()

        result = self.run_cli(
            "docker",
            self.fake_docker_env(
                fake_bin,
                broken_config,
                docker_log,
                FAKE_DOCKER_CONTEXT="desktop-linux",
            ),
        )

        self.assertEqual(result.returncode, 1)
        calls = [line.split("|", 1) for line in docker_log.read_text().splitlines()]
        self.assertEqual([command for command, _ in calls], ["build", "context"])

    def test_docker_does_not_probe_context_or_retry_for_an_unrelated_error(
        self,
    ) -> None:
        fake_bin, broken_config, docker_log = self.prepare_fake_docker()

        result = self.run_cli(
            "docker",
            self.fake_docker_env(
                fake_bin,
                broken_config,
                docker_log,
                FAKE_DOCKER_INITIAL_ERROR="daemon unavailable",
            ),
        )

        self.assertEqual(result.returncode, 1)
        calls = [line.split("|", 1) for line in docker_log.read_text().splitlines()]
        self.assertEqual([command for command, _ in calls], ["build"])

    def test_docker_does_not_retry_when_context_lookup_fails(self) -> None:
        fake_bin, broken_config, docker_log = self.prepare_fake_docker()

        result = self.run_cli(
            "docker",
            self.fake_docker_env(
                fake_bin,
                broken_config,
                docker_log,
                FAKE_DOCKER_CONTEXT_STATUS="1",
            ),
        )

        self.assertEqual(result.returncode, 1)
        calls = [line.split("|", 1) for line in docker_log.read_text().splitlines()]
        self.assertEqual([command for command, _ in calls], ["build", "context"])

    def test_docker_signal_during_fallback_cleans_isolated_config(self) -> None:
        fake_bin, broken_config, docker_log = self.prepare_fake_docker()

        result = self.run_cli(
            "docker",
            self.fake_docker_env(
                fake_bin,
                broken_config,
                docker_log,
                FAKE_DOCKER_SIGNAL_FALLBACK="1",
            ),
        )

        self.assertEqual(result.returncode, 143)
        calls = [line.split("|", 1) for line in docker_log.read_text().splitlines()]
        self.assertEqual([command for command, _ in calls], ["build", "context", "build"])
        fallback_config = Path(calls[2][1])
        self.addCleanup(shutil.rmtree, fallback_config.parent, True)
        self.assertFalse(fallback_config.parent.exists())

    def test_docker_fallback_build_failure_cleans_isolated_config(self) -> None:
        fake_bin, broken_config, docker_log = self.prepare_fake_docker()

        result = self.run_cli(
            "docker",
            self.fake_docker_env(
                fake_bin,
                broken_config,
                docker_log,
                FAKE_DOCKER_FALLBACK_STATUS="17",
            ),
        )

        self.assertEqual(result.returncode, 17)
        calls = [line.split("|", 1) for line in docker_log.read_text().splitlines()]
        self.assertEqual([command for command, _ in calls], ["build", "context", "build"])
        self.assertFalse(Path(calls[2][1]).parent.exists())

    def test_docker_fallback_run_failure_cleans_isolated_config(self) -> None:
        fake_bin, broken_config, docker_log = self.prepare_fake_docker()

        result = self.run_cli(
            "docker",
            self.fake_docker_env(
                fake_bin,
                broken_config,
                docker_log,
                FAKE_DOCKER_RUN_STATUS="19",
            ),
        )

        self.assertEqual(result.returncode, 19)
        calls = [line.split("|", 1) for line in docker_log.read_text().splitlines()]
        self.assertEqual(
            [command for command, _ in calls],
            ["build", "context", "build", "run"],
        )
        self.assertFalse(Path(calls[2][1]).parent.exists())

    def test_failed_build_does_not_leave_a_stale_preview_pdf(self) -> None:
        publish = self.repo / "publish"
        publish.mkdir()
        (publish / "main.tex").write_text(
            MAIN_TEX.replace(
                "AP-OTF-RyuminPr6N-Regular.otf", "Unexpected-Regular.otf"
            ),
            encoding="utf-8",
        )
        (publish / "ch01.tex").write_text("本文\n", encoding="utf-8")
        (publish / "main.pdf").write_bytes(b"production pdf sentinel\n")
        stale = self.repo / "build/publish-preview/parser_book-preview.pdf"
        stale.parent.mkdir(parents=True)
        stale.write_bytes(b"stale preview\n")

        result = self.run_cli("build")

        self.assertNotEqual(result.returncode, 0)
        self.assertIn("main-font-config", result.stderr)
        self.assertFalse(stale.exists())

    @unittest.skipUnless(
        all(shutil.which(command) for command in ("python3", "lualatex", "upmendex")),
        "local TeX build dependencies are required",
    )
    def test_lualatex_failure_preserves_the_failure_log(self) -> None:
        publish = self.repo / "publish"
        publish.mkdir()
        (publish / "main.tex").write_text(MAIN_TEX, encoding="utf-8")
        (publish / "ch01.tex").write_text(
            "\\undefinedPreviewCommand\n", encoding="utf-8"
        )
        (publish / "main.pdf").write_bytes(b"production pdf sentinel\n")
        before = self.read_tree(publish)

        result = self.run_cli("build")

        self.assertNotEqual(result.returncode, 0)
        logs = self.repo / "build/publish-preview/logs"
        self.assertTrue((logs / "lualatex-1.stdout.log").is_file())
        self.assertTrue((logs / "lualatex-1.log").is_file())
        self.assertFalse(
            (self.repo / "build/publish-preview/parser_book-preview.pdf").exists()
        )
        self.assertEqual(self.read_tree(publish), before)

    @unittest.skipUnless(
        all(shutil.which(command) for command in ("python3", "lualatex", "upmendex")),
        "local TeX build dependencies are required",
    )
    def test_source_change_during_build_does_not_publish_a_stable_pdf(self) -> None:
        publish = self.repo / "publish"
        publish.mkdir()
        (publish / "main.tex").write_text(MAIN_TEX, encoding="utf-8")
        chapter = publish / "ch01.tex"
        chapter.write_text("変更前\n", encoding="utf-8")
        (publish / "main.pdf").write_bytes(b"production pdf sentinel\n")
        fourth_pass_stdout = (
            self.repo / "build/publish-preview/logs/lualatex-4.stdout.log"
        )

        def mutate_source_during_final_pass() -> None:
            deadline = time.monotonic() + 30
            while time.monotonic() < deadline:
                if fourth_pass_stdout.exists():
                    chapter.write_text("build中の外部変更\n", encoding="utf-8")
                    return
                time.sleep(0.01)
            self.fail("fourth LuaLaTeX pass did not start before the deadline")

        mutator = threading.Thread(target=mutate_source_during_final_pass)
        mutator.start()
        result = self.run_cli("build")
        mutator.join()

        self.assertNotEqual(result.returncode, 0)
        self.assertIn("source tree changed", result.stderr)
        self.assertFalse(
            (self.repo / "build/publish-preview/parser_book-preview.pdf").exists()
        )

    @unittest.skipUnless(
        all(shutil.which(command) for command in ("git", "python3", "lualatex", "upmendex")),
        "local TeX build dependencies are required",
    )
    def test_build_accepts_preexisting_publish_edits_without_changing_them(self) -> None:
        publish = self.repo / "publish"
        publish.mkdir()
        (publish / "main.tex").write_text(MAIN_TEX, encoding="utf-8")
        chapter = publish / "ch01.tex"
        chapter.write_text("変更前\n", encoding="utf-8")
        (publish / "main.pdf").write_bytes(b"production pdf sentinel\n")
        subprocess.run(["git", "init", "-q"], cwd=self.repo, check=True)
        subprocess.run(["git", "add", "publish"], cwd=self.repo, check=True)
        subprocess.run(
            [
                "git",
                "-c",
                "user.name=Preview Test",
                "-c",
                "user.email=preview@example.invalid",
                "commit",
                "-qm",
                "fixture",
            ],
            cwd=self.repo,
            check=True,
        )
        chapter.write_text("変更後\n", encoding="utf-8")
        before = self.read_tree(publish)

        result = self.run_cli("build")

        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual(self.read_tree(publish), before)
        self.assertTrue(
            (self.repo / "build/publish-preview/parser_book-preview.pdf").is_file()
        )
        final_log = self.repo / "build/publish-preview/logs/lualatex-4.log"
        self.assertTrue(final_log.is_file())
        final_log_text = final_log.read_text(encoding="utf-8", errors="replace")
        self.assertNotIn("Label(s) may have changed", final_log_text)
        self.assertNotIn("rerunfilecheck Warning", final_log_text)

    @staticmethod
    def read_tree(root: Path) -> dict[str, bytes]:
        return {
            path.relative_to(root).as_posix(): path.read_bytes()
            for path in sorted(root.rglob("*"))
            if path.is_file()
        }


if __name__ == "__main__":
    unittest.main()
