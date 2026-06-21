# Spirals Desktop - TODO

- [ ] **BASE Parameter Column:**
  - Add a column to the left of the CV labeled `BASE` in the Patch Matrix.
  - In the cell config window (when clicking a parameter's BASE cell or perhaps in the config window), have a dual-headed slider that allows setting min/max values.
  - This will be the base value range that all the CVs are modulating (or in the case of no CVs, it will also be the final value of the parameter). *Wait, how does a dual-headed slider for BASE work? It sets min/max values, but how is it evaluated? If there are no CVs, it is the final value. If there are CVs, does the base value modulate between min/max? Or is it a range for the base value itself? Let's check how parameter handles base value.*
- [ ] **FINAL Parameter Column:**
  - Add a column to the left of `BASE` called `FINAL`.
  - It shows/represents the final value of `Base` and all the CVs that actually gets used by the app.
- [ ] **UI Compactness:**
  - Continue working on the UI for compactness.
- [ ] **3 Panel Layout:**
  - Try a 3 panel layout with the mixer on the right and cell config in the middle.
