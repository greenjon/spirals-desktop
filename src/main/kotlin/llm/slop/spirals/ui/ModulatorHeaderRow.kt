package llm.slop.spirals.ui

import imgui.ImGui
import imgui.type.ImInt
import llm.slop.spirals.parameters.CvModulator
import llm.slop.spirals.parameters.ModulatableParameter
import llm.slop.spirals.parameters.ModulationOperator

object ModulatorHeaderRow {
    private val operatorLabels = arrayOf("ADD", "MUL", "SCALE")

    fun draw(
        state: PatchGridState,
        param: ModulatableParameter,
        existing: CvModulator,
        idx: Int,
        modsToDraw: List<CvModulator>,
        activeMods: List<CvModulator>,
        isVirtual: Boolean,
        isLfo: Boolean,
        hasAdvanced: Boolean,
        onReplace: (CvModulator) -> Unit,
        onReset: () -> Unit
    ) {
        val bypassed = existing.bypassed
        val currentThemeColor = CvTheme.getThemeColor(existing.sourceId)
        
        // Draw background panel for modulator
        val panelStartX = ImGui.getCursorScreenPosX()
        val panelStartY = ImGui.getCursorScreenPosY()
        val dl = ImGui.getWindowDrawList()
        
        if (bypassed) {
            ImGui.pushStyleVar(imgui.flag.ImGuiStyleVar.Alpha, 0.5f)
        }
        
        val bandLabel = when (existing.sourceId) {
            "audio_amp" -> "Amplitude"
            "audio_bass" -> "Low"
            "audio_mid" -> "Mid"
            "audio_high" -> "High"
            "trigger_onset" -> "Onset / Transient"
            "trigger_accent" -> "Accent / Peak"
            else -> null
        }
        val titleText = bandLabel ?: if (modsToDraw.size > 1) {
            val typeLabel = if (isLfo) "LFO" else if (hasAdvanced) "Oscillator" else "Modulator"
            "$typeLabel ${idx + 1}"
        } else {
            val typeLabel = if (isLfo) "LFO" else if (hasAdvanced) "Oscillator" else "Modulator"
            typeLabel
        }

        ImGui.indent(10f) // Indent controls slightly

        val btnHeight = ImGui.getFrameHeight()
        val scale = btnHeight / 30f
        val btnWidth = 50f * scale

        if (bypassed) {
            ImGui.popStyleVar() // Draw header controls at full opacity
        }

        // 1. Power icon (Active/Bypass button)
        val btnX2 = ImGui.getCursorScreenPosX()
        val btnY2 = ImGui.getCursorScreenPosY()
        
        // Push styled button colors: Green for active, Red for bypassed
        val btnColor = if (bypassed) ImGui.colorConvertFloat4ToU32(0.7f, 0.2f, 0.2f, 1f) else ImGui.colorConvertFloat4ToU32(0.1f, 0.6f, 0.2f, 1f)
        val btnHoverColor = if (bypassed) ImGui.colorConvertFloat4ToU32(0.8f, 0.3f, 0.3f, 1f) else ImGui.colorConvertFloat4ToU32(0.2f, 0.7f, 0.3f, 1f)
        val btnActiveColor = if (bypassed) ImGui.colorConvertFloat4ToU32(0.9f, 0.4f, 0.4f, 1f) else ImGui.colorConvertFloat4ToU32(0.3f, 0.8f, 0.4f, 1f)
        
        ImGui.pushStyleColor(imgui.flag.ImGuiCol.Button, btnColor)
        ImGui.pushStyleColor(imgui.flag.ImGuiCol.ButtonHovered, btnHoverColor)
        ImGui.pushStyleColor(imgui.flag.ImGuiCol.ButtonActive, btnActiveColor)
        
        if (ImGui.button("##bypass_bar_$idx", btnWidth, btnHeight)) {
            onReplace(existing.copy(bypassed = !bypassed))
        }
        if (ImGui.isItemHovered() && UITheme.tooltipsEnabled) {
            ImGui.setTooltip(if (bypassed) "Enable modulator (Active)" else "Bypass modulator")
        }
        ImGui.popStyleColor(3)
        
        // Draw universal on/off (power icon scaled/enlarged) on the button
        val pColor = ImGui.colorConvertFloat4ToU32(1f, 1.0f, 1.0f, 1f)
        val pCenterX = btnX2 + btnWidth / 2f
        val pCenterY = btnY2 + btnHeight / 2f
        val pRadius = 11f * scale
        val pThickness = 3f * scale
        
        dl.addCircle(pCenterX, pCenterY, pRadius, pColor, 16, pThickness)
        dl.addLine(pCenterX, pCenterY - pRadius * 1.3f, pCenterX, pCenterY + pRadius * 0.2f, pColor, pThickness)

        // 2. Dice icon (Randomize button)
        ImGui.sameLine(0f, 10f)
        val btnX1 = ImGui.getCursorScreenPosX()
        val btnY1 = ImGui.getCursorScreenPosY()
        if (ImGui.button("##rand_bar_$idx", btnWidth, btnHeight)) {
            val randomized = existing
                .randomizeAmplitude()
                .randomizeDcOffset()
                .randomizeSubdivision()
                .randomizePhaseOffset()
                .randomizeSlope()
            onReplace(randomized)
        }
        if (ImGui.isItemHovered() && UITheme.tooltipsEnabled) {
            ImGui.setTooltip("Randomize primary LFO / modulator values")
        }
        
        // Draw pair of dice inside the randomize button (scaled and enlarged 15% more)
        val diceColor = ImGui.colorConvertFloat4ToU32(0.9f, 0.9f, 0.9f, 1f)
        val dotColor = ImGui.colorConvertFloat4ToU32(0.1f, 0.1f, 0.1f, 1f)
        // Die 1
        val dieW = 23f * scale
        val d1X = btnX1 + 5f * scale
        val d1Y = btnY1 + (btnHeight - dieW) / 2f
        dl.addRectFilled(d1X, d1Y, d1X + dieW, d1Y + dieW, diceColor, 2f * scale)
        dl.addRect(d1X, d1Y, d1X + dieW, d1Y + dieW, dotColor, 2f * scale, 0, 1.2f * scale)
        // Face 3 dots
        val dotRadius = 1.7f * scale
        dl.addCircleFilled(d1X + 5f * scale, d1Y + 5f * scale, dotRadius, dotColor)
        dl.addCircleFilled(d1X + 11.5f * scale, d1Y + 11.5f * scale, dotRadius, dotColor)
        dl.addCircleFilled(d1X + 18f * scale, d1Y + 18f * scale, dotRadius, dotColor)

        // Die 2
        val d2X = btnX1 + 24f * scale
        val d2Y = btnY1 + (btnHeight - dieW) / 2f + 2f * scale
        dl.addRectFilled(d2X, d2Y, d2X + dieW, d2Y + dieW, diceColor, 2f * scale)
        dl.addRect(d2X, d2Y, d2X + dieW, d2Y + dieW, dotColor, 2f * scale, 0, 1.2f * scale)
        // Face 5 dots
        dl.addCircleFilled(d2X + 5f * scale, d2Y + 5f * scale, dotRadius, dotColor)
        dl.addCircleFilled(d2X + 18f * scale, d2Y + 5f * scale, dotRadius, dotColor)
        dl.addCircleFilled(d2X + 11.5f * scale, d2Y + 11.5f * scale, dotRadius, dotColor)
        dl.addCircleFilled(d2X + 5f * scale, d2Y + 18f * scale, dotRadius, dotColor)
        dl.addCircleFilled(d2X + 18f * scale, d2Y + 18f * scale, dotRadius, dotColor)

        // 3. Operator dropdown (ADD/MUL/SCALE combo box)
        ImGui.sameLine(0f, 10f)
        val opIdx = ImInt(when (existing.operator) {
            ModulationOperator.ADD -> 0
            ModulationOperator.MUL -> 1
            ModulationOperator.SCALE -> 2
        })
        ImGui.pushItemWidth(100f)
        if (ImGui.combo("##op", opIdx, operatorLabels)) {
            val newOp = when (opIdx.get()) {
                0 -> ModulationOperator.ADD
                1 -> ModulationOperator.MUL
                else -> ModulationOperator.SCALE
            }
            onReplace(existing.copy(operator = newOp))
        }
        ImGui.popItemWidth()

        // 4. Title Text (vertically centered in the row, left-aligned)
        ImGui.sameLine(0f, 115f)
        val alignY = btnY2 + (btnHeight - ImGui.getTextLineHeightWithSpacing()) / 2f
        ImGui.setCursorScreenPos(ImGui.getCursorScreenPosX(), alignY)
        UITheme.h2(titleText)

        // 5. Reset button (trash can icon)
        if (idx == 0) {
            val resetWidth = 50f * scale
            ImGui.sameLine(ImGui.getCursorPosX() + ImGui.getContentRegionAvailX() - resetWidth)
            if (isVirtual) {
                ImGui.beginDisabled()
            }
            val btnX3 = ImGui.getCursorScreenPosX()
            val btnY3 = ImGui.getCursorScreenPosY()
            
            ImGui.pushStyleColor(imgui.flag.ImGuiCol.Button, ImGui.colorConvertFloat4ToU32(0.25f, 0.25f, 0.25f, 1f))
            ImGui.pushStyleColor(imgui.flag.ImGuiCol.ButtonHovered, ImGui.colorConvertFloat4ToU32(0.8f, 0.2f, 0.2f, 1f)) // Red on hover
            if (ImGui.button("##reset_bar_$idx", resetWidth, btnHeight)) {
                onReset()
                ImGui.popStyleColor(2)
                if (isVirtual) {
                    ImGui.endDisabled()
                }
                ImGui.unindent(10f)
                return
            }
            if (ImGui.isItemHovered() && UITheme.tooltipsEnabled) {
                ImGui.setTooltip("Clear/reset modulators")
            }
            ImGui.popStyleColor(2)
            if (isVirtual) {
                ImGui.endDisabled()
            }
            
            // Draw trash can on reset button (scaled/enlarged)
            val tcColor = ImGui.colorConvertFloat4ToU32(0.9f, 0.9f, 0.9f, 1f)
            val tcX = btnX3 + resetWidth / 2f
            val tcY = btnY3 + btnHeight / 2f
            // Bucket
            dl.addLine(tcX - 8f * scale, tcY - 5f * scale, tcX - 6f * scale, tcY + 11f * scale, tcColor, 2.2f * scale)
            dl.addLine(tcX + 8f * scale, tcY - 5f * scale, tcX + 6f * scale, tcY + 11f * scale, tcColor, 2.2f * scale)
            dl.addLine(tcX - 6f * scale, tcY + 11f * scale, tcX + 6f * scale, tcY + 11f * scale, tcColor, 2.2f * scale)
            // Lid
            dl.addLine(tcX - 11f * scale, tcY - 5f * scale, tcX + 11f * scale, tcY - 5f * scale, tcColor, 2.2f * scale)
            // Handle
            dl.addLine(tcX - 4f * scale, tcY - 5f * scale, tcX - 4f * scale, tcY - 9f * scale, tcColor, 2.2f * scale)
            dl.addLine(tcX - 4f * scale, tcY - 9f * scale, tcX + 4f * scale, tcY - 9f * scale, tcColor, 2.2f * scale)
            dl.addLine(tcX + 4f * scale, tcY - 9f * scale, tcX + 4f * scale, tcY - 5f * scale, tcColor, 2.2f * scale)
            // Vertical lines inside (ribs)
            dl.addLine(tcX - 3f * scale, tcY - 1f * scale, tcX - 2.5f * scale, tcY + 8f * scale, tcColor, 1.5f * scale)
            dl.addLine(tcX + 3f * scale, tcY - 1f * scale, tcX + 2.5f * scale, tcY + 8f * scale, tcColor, 1.5f * scale)
        }
    }
}
