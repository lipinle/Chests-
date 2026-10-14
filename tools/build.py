#!/usr/bin/env python3
"""Regenerates the packs and zips them into dist/StoragePlus.mcaddon (+ .mcpack files)."""
import subprocess
import sys
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
PACKS = ROOT / "packs"
DIST = ROOT / "dist"


def add_dir(zf: zipfile.ZipFile, folder: Path, prefix: str = ""):
    for f in sorted(folder.rglob("*")):
        if f.is_file():
            zf.write(f, prefix + f.relative_to(folder).as_posix())


def main():
    subprocess.run([sys.executable, str(ROOT / "tools" / "generate.py")], check=True)
    DIST.mkdir(exist_ok=True)
    with zipfile.ZipFile(DIST / "StoragePlus.mcaddon", "w", zipfile.ZIP_DEFLATED) as zf:
        for pack in sorted(PACKS.iterdir()):
            add_dir(zf, pack, pack.name + "/")
    for pack in sorted(PACKS.iterdir()):
        with zipfile.ZipFile(DIST / f"{pack.name}.mcpack", "w", zipfile.ZIP_DEFLATED) as zf:
            add_dir(zf, pack)
    print("built", *sorted(p.name for p in DIST.iterdir()))


if __name__ == "__main__":
    main()
