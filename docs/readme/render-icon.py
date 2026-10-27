#!/usr/bin/env python3
"""Render the original SVG store icon (requires cairosvg)."""
from pathlib import Path
import cairosvg
root = Path(__file__).resolve().parents[2]
output = root / 'fastlane/metadata/android/en-US/images/icon.png'
output.parent.mkdir(parents=True, exist_ok=True)
cairosvg.svg2png(url=str(root / 'docs/readme/app-icon.svg'), write_to=str(output), output_width=512, output_height=512)
