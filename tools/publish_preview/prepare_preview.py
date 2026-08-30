#!/usr/bin/env python3
"""Prepare an isolated, auditable TeX tree for the publish preview build."""

from __future__ import annotations

import argparse
import difflib
import hashlib
import json
import re
import shutil
import sys
from pathlib import Path


MAIN_FONT_BLOCK = r"""% 明朝体（リュウミン）の直接指定
\setmainjfont[
  Path = /Users/kahei/Library/Fonts/MorisawaFonts/, % フォルダのパス
  BoldFont = AP-OTF-RyuminPr6N-Bold.otf,            % 太字のファイル名
%  ItalicFont = AP-RyuminPr6N-R.otf                  % 必要なら指定
]{AP-OTF-RyuminPr6N-Regular.otf}                    % 標準のファイル名
"""
SANS_FONT_BLOCK = r"""% ゴシック体（新ゴ）の直接指定
\setsansjfont[
  Path = /Users/kahei/Library/Fonts/MorisawaFonts/,
  BoldFont = AP-OTF-ShinGoPr6N-Bold.otf
]{AP-OTF-ShinGoPr6N-Medium.otf}
"""
INCLUDE_PATTERN = re.compile(r"\\(?:input|include)\s*\{([^{}]+)\}")
GRAPHICS_PATTERN = re.compile(
    r"\\includegraphics(?:\s*\[[^]]*\])?\s*\{([^{}]+)\}"
)
BIBLIOGRAPHY_PATTERN = re.compile(r"\\bibliography\s*\{([^{}]+)\}")
LOCAL_PACKAGE_PATTERN = re.compile(
    r"\\usepackage(?:\s*\[[^]]*\])?\s*\{([^{}]+)\}"
)

# Artwork that exists only in the publisher's environment (like the Morisawa
# fonts). When such a file is missing locally, the preview build substitutes a
# generated placeholder page instead of failing validation. Every other
# missing asset keeps failing fast so broken references are still caught.
PUBLISHER_ONLY_ASSETS = frozenset(
    {
        "koubun_tobira.pdf",
    }
)


def _escape_pdf_text(text: str) -> bytes:
    encoded = text.encode("ascii", "replace")
    return encoded.replace(b"\\", b"\\\\").replace(b"(", b"\\(").replace(b")", b"\\)")


def build_placeholder_pdf(label: str) -> bytes:
    """Return a single blank A4 page that names the missing publisher asset."""

    stream = (
        b"BT /F1 14 Tf 60 780 Td ("
        + _escape_pdf_text(f"Preview placeholder: {label}")
        + b") Tj ET\n"
    )
    objects = [
        b"<< /Type /Catalog /Pages 2 0 R >>",
        b"<< /Type /Pages /Kids [3 0 R] /Count 1 >>",
        (
            b"<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] "
            b"/Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >>"
        ),
        b"<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>",
        b"<< /Length "
        + str(len(stream)).encode("ascii")
        + b" >>\nstream\n"
        + stream
        + b"endstream",
    ]
    out = bytearray(b"%PDF-1.4\n")
    offsets: list[int] = []
    for number, body in enumerate(objects, start=1):
        offsets.append(len(out))
        out += str(number).encode("ascii") + b" 0 obj\n" + body + b"\nendobj\n"
    xref_position = len(out)
    out += b"xref\n0 " + str(len(objects) + 1).encode("ascii") + b"\n"
    out += b"0000000000 65535 f \n"
    for offset in offsets:
        out += f"{offset:010d} 00000 n \n".encode("ascii")
    out += (
        b"trailer\n<< /Size "
        + str(len(objects) + 1).encode("ascii")
        + b" /Root 1 0 R >>\nstartxref\n"
        + str(xref_position).encode("ascii")
        + b"\n%%EOF\n"
    )
    return bytes(out)


def write_placeholder_assets(destination: Path, references: list[str]) -> list[str]:
    """Materialise placeholder pages for whitelisted publisher-only assets."""

    written: list[str] = []
    for reference in references:
        target = destination / Path(reference)
        if target.exists():
            continue
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_bytes(build_placeholder_pdf(reference))
        written.append(reference)
    return written


class PreparationError(RuntimeError):
    """A preview tree cannot be prepared safely."""


def source_digest(source_root: Path) -> str:
    """Return a deterministic digest of every file in the publish tree."""

    digest = hashlib.sha256()
    for path in sorted(source_root.rglob("*")):
        if not path.is_file():
            continue
        relative = path.relative_to(source_root).as_posix().encode("utf-8")
        digest.update(len(relative).to_bytes(8, byteorder="big"))
        digest.update(relative)
        with path.open("rb") as source:
            while chunk := source.read(1024 * 1024):
                digest.update(chunk)
    return digest.hexdigest()


def strip_tex_comments(text: str) -> str:
    """Remove TeX comments while preserving escaped percent signs."""

    stripped: list[str] = []
    for line in text.splitlines(keepends=True):
        comment_at: int | None = None
        for index, character in enumerate(line):
            if character != "%":
                continue
            backslashes = 0
            cursor = index - 1
            while cursor >= 0 and line[cursor] == "\\":
                backslashes += 1
                cursor -= 1
            if backslashes % 2 == 0:
                comment_at = index
                break
        if comment_at is None:
            stripped.append(line)
        elif line.endswith("\n"):
            stripped.append(line[:comment_at] + "\n")
        else:
            stripped.append(line[:comment_at])
    return "".join(stripped)


def body_for_graphics(path: Path, text: str) -> str:
    """Exclude pre-document macro definitions whose assets are resolved lazily."""

    if path.name != "main.tex":
        return text
    marker = r"\begin{document}"
    if marker not in text:
        raise PreparationError(f"{path}: missing {marker}")
    return text.split(marker, maxsplit=1)[1]


def resolve_local_reference(
    source_root: Path,
    raw_reference: str,
    suffixes: tuple[str, ...],
) -> tuple[Path | None, list[Path]]:
    reference = raw_reference.strip()
    if not reference or "\\" in reference or "#" in reference:
        return None, []
    relative = Path(reference)
    if relative.is_absolute() or ".." in relative.parts:
        raise PreparationError(f"unsafe local asset reference: {reference}")
    candidates = [source_root / relative]
    if relative.suffix == "":
        candidates.extend(source_root / f"{reference}{suffix}" for suffix in suffixes)
    resolved = next((candidate for candidate in candidates if candidate.is_file()), None)
    return resolved, candidates


def validate_local_assets(source_root: Path) -> tuple[list[str], list[str]]:
    checked: set[str] = set()
    missing: set[str] = set()
    placeholders: set[str] = set()

    for tex_path in sorted(source_root.rglob("*.tex")):
        text = strip_tex_comments(tex_path.read_text(encoding="utf-8"))
        references: list[tuple[str, tuple[str, ...]]] = []
        references.extend((match, (".tex",)) for match in INCLUDE_PATTERN.findall(text))
        references.extend(
            (match, (".bib",))
            for group in BIBLIOGRAPHY_PATTERN.findall(text)
            for match in group.split(",")
        )
        graphics_body = body_for_graphics(tex_path, text)
        references.extend(
            (match, (".pdf", ".png", ".jpg", ".jpeg", ".eps"))
            for match in GRAPHICS_PATTERN.findall(graphics_body)
        )
        for group in LOCAL_PACKAGE_PATTERN.findall(text):
            for package in group.split(","):
                package = package.strip()
                if "/" in package or package.startswith("."):
                    references.append((package, (".sty",)))

        for raw_reference, suffixes in references:
            resolved, candidates = resolve_local_reference(
                source_root, raw_reference, suffixes
            )
            if not candidates:
                continue
            if resolved is not None:
                relative = resolved.relative_to(source_root).as_posix()
                checked.add(relative)
            else:
                reference = raw_reference.strip()
                if reference in PUBLISHER_ONLY_ASSETS:
                    placeholders.add(reference)
                    continue
                attempted = ", ".join(
                    candidate.relative_to(source_root).as_posix()
                    for candidate in candidates
                )
                missing.add(f"{reference} (tried: {attempted})")

    if missing:
        details = "\n".join(f"  - {path}" for path in sorted(missing))
        raise PreparationError(f"missing local asset(s):\n{details}")
    return sorted(checked), sorted(placeholders)


def validate_replacement_count(
    text: str, expected_block: str, kind: str
) -> None:
    count = text.count(expected_block)
    if count != 1:
        raise PreparationError(
            f"{kind}: expected 1 replacement, found {count}; "
            "publish/main.tex no longer matches the reviewed font configuration"
        )


def validate_preview_inputs(repo_root: Path) -> tuple[Path, list[str], list[str]]:
    source_root = repo_root / "publish"
    class_source = Path(__file__).resolve().with_name("asciibook.cls")
    main_path = source_root / "main.tex"

    if not source_root.is_dir():
        raise PreparationError(f"publish source directory not found: {source_root}")
    if not main_path.is_file():
        raise PreparationError(f"publish entry point not found: {main_path}")
    if not class_source.is_file():
        raise PreparationError(f"preview compatibility class not found: {class_source}")

    checked_assets, placeholder_assets = validate_local_assets(source_root)
    main_text = main_path.read_text(encoding="utf-8")
    validate_replacement_count(main_text, MAIN_FONT_BLOCK, "main-font-config")
    validate_replacement_count(main_text, SANS_FONT_BLOCK, "sans-font-config")
    return class_source, checked_assets, placeholder_assets


def replace_exactly_once(
    text: str,
    expected_block: str,
    replacement: str,
    kind: str,
) -> tuple[str, dict[str, object]]:
    validate_replacement_count(text, expected_block, kind)
    replaced = text.replace(expected_block, replacement)
    count = 1
    return replaced, {"file": "main.tex", "kind": kind, "count": count}


def render_source_diff(source_root: Path, destination: Path) -> str:
    chunks: list[str] = []
    source_files = {
        path.relative_to(source_root).as_posix(): path
        for path in source_root.rglob("*")
        if path.is_file() and path.suffix in {".tex", ".cls"}
    }
    preview_files = {
        path.relative_to(destination).as_posix(): path
        for path in destination.rglob("*")
        if path.is_file() and path.suffix in {".tex", ".cls"}
    }
    for relative in sorted(source_files.keys() | preview_files.keys()):
        before = (
            source_files[relative].read_text(encoding="utf-8").splitlines(keepends=True)
            if relative in source_files
            else []
        )
        after = (
            preview_files[relative].read_text(encoding="utf-8").splitlines(keepends=True)
            if relative in preview_files
            else []
        )
        chunks.extend(
            difflib.unified_diff(
                before,
                after,
                fromfile=f"publish/{relative}",
                tofile=f"preview/{relative}",
            )
        )
    return "".join(chunks)


def prepare(repo_root: Path) -> dict[str, object]:
    repo_root = repo_root.resolve()
    source_root = repo_root / "publish"
    output_root = repo_root / "build/publish-preview"
    destination = output_root / "src"
    logs = output_root / "logs"

    digest_before = source_digest(source_root)
    class_source, checked_assets, placeholder_assets = validate_preview_inputs(repo_root)
    output_root.mkdir(parents=True, exist_ok=True)
    logs.mkdir(parents=True, exist_ok=True)
    if destination.exists():
        shutil.rmtree(destination)
    shutil.copytree(source_root, destination)
    written_placeholders = write_placeholder_assets(destination, placeholder_assets)

    main_path = destination / "main.tex"
    main_text = main_path.read_text(encoding="utf-8")
    replacements: list[dict[str, object]] = []
    main_text, replacement = replace_exactly_once(
        main_text,
        MAIN_FONT_BLOCK,
        "% Preview font fallback: production uses Morisawa Ryumin.\n"
        r"\setmainjfont{Noto Serif CJK JP}",
        "main-font-config",
    )
    replacements.append(replacement)
    main_text, replacement = replace_exactly_once(
        main_text,
        SANS_FONT_BLOCK,
        "% Preview font fallback: production uses Morisawa ShinGo.\n"
        r"\setsansjfont{Noto Sans CJK JP}",
        "sans-font-config",
    )
    replacements.append(replacement)
    main_path.write_text(main_text, encoding="utf-8")
    shutil.copy2(class_source, destination / "asciibook.cls")

    manifest: dict[str, object] = {
        "source": "publish",
        "destination": "build/publish-preview/src",
        "class_mode": "preview-shim",
        "font_mode": "noto",
        "replacements": replacements,
        "checked_assets": checked_assets,
        "placeholder_assets": written_placeholders,
        "source_digest": digest_before,
    }
    (output_root / "prepare-manifest.json").write_text(
        json.dumps(manifest, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )
    (logs / "source-diff.patch").write_text(
        render_source_diff(source_root, destination), encoding="utf-8"
    )
    digest_after = source_digest(source_root)
    if digest_after != digest_before:
        raise PreparationError("publish source tree changed during preparation")
    return manifest


def parse_args(argv: list[str]) -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--repo-root",
        type=Path,
        default=Path(__file__).resolve().parents[2],
        help="repository root (defaults to the root containing this tool)",
    )
    mode = parser.add_mutually_exclusive_group()
    mode.add_argument(
        "--print-source-digest",
        action="store_true",
        help="print the current publish tree digest without preparing a preview",
    )
    mode.add_argument(
        "--check-only",
        action="store_true",
        help="validate sources, local assets, and reviewed font patterns without writing",
    )
    return parser.parse_args(argv)


def main(argv: list[str] | None = None) -> int:
    args = parse_args(sys.argv[1:] if argv is None else argv)
    try:
        if args.print_source_digest:
            source_root = args.repo_root.resolve() / "publish"
            if not source_root.is_dir():
                raise PreparationError(
                    f"publish source directory not found: {source_root}"
                )
            print(source_digest(source_root))
            return 0
        if args.check_only:
            _class_source, checked_assets, placeholder_assets = validate_preview_inputs(
                args.repo_root.resolve()
            )
            message = (
                "Publish preview sources: OK "
                f"({len(checked_assets)} local asset(s) checked)."
            )
            if placeholder_assets:
                message += (
                    " Publisher-only asset(s) will use generated placeholder(s): "
                    + ", ".join(placeholder_assets)
                    + "."
                )
            print(message)
            return 0
        manifest = prepare(args.repo_root)
    except (OSError, UnicodeError, PreparationError) as error:
        print(f"publish preview preparation failed: {error}", file=sys.stderr)
        return 1
    print(
        "Prepared build/publish-preview/src "
        f"with {len(manifest['replacements'])} reviewed replacement(s)."
    )
    placeholder_assets = manifest.get("placeholder_assets") or []
    if placeholder_assets:
        print(
            "Substituted generated placeholder(s) for publisher-only asset(s): "
            + ", ".join(str(asset) for asset in placeholder_assets)
            + "."
        )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
