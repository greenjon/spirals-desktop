# Patch Grid Visual Improvements TODO

## 1. Grouping in Boxes / Panels
* **Subtle Panel Backgrounds:** For subgroups (like "Background" or "Feedback"), record the cursor position before the subgroup starts and after it ends, then draw a subtle rounded rectangle behind the entire block using `dl.addRectFilled()`.
* **Left Margin Lines:** Draw a vertical colored line (like a left border) next to the indented parameters of a specific subgroup. For example, all "Color" parameters could have a subtle pink 2px line running down their left edge.

## 2. Hover Highlighting (Crosshairs)
* **Row/Column Highlights:** When the mouse hovers over a row's label or cells, highlight that entire row slightly. Even better, track the hovered column and highlight both the row and the column, creating a "crosshair" effect that makes it explicitly clear which intersection the user is looking at.

## 3. Extending Grid Lines
* The vertical lines for the column headers stop after the headers. Extending those vertical lines (with a very low opacity, like 10-15%) all the way down the panel will give the cells a track to sit in, reinforcing the grid structure.

## 4. Intelligent Color Coding (Deferred)
* **Column Tinting:** Give the cell backgrounds a very subtle tint based on their category. Audio-reactive CVs (Onset, Accent, Bass, etc.) could have a faintly warm background; Internal CVs (LFO, Rand) could have a faintly cool background.
* **Deck Colors:** Give "Deck A" elements a subtle primary color tint (e.g., blue) and "Deck B" an accent color tint (e.g., orange).

---
## Completed
* ~~Zebra Striping: Added alternating backgrounds to rows.~~
* ~~Subgroup Indentation: Stair-stepped the UI tree elements.~~
* ~~Extending Grid Lines: Drew subtle column lines all the way down.~~
* ~~Hover Highlighting (Crosshairs): Added highlights for the hovered row and column, creating a crosshair effect.~~
* ~~Grouping Margin Lines: Added color-coded left border lines for subgroups.~~
* ~~Top-Level Font Sizing & Spacing: Made Mixer, Deck A, and Deck B stand out as major headers.~~
* ~~Intelligent Color Coding: Color-coded the CV headers, active knobs, and cell backgrounds by logical category. Added deck header background tints.~~