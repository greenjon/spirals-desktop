package llm.slop.spirals.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import llm.slop.spirals.MandalaLibrary
import llm.slop.spirals.models.mandala.MandalaRatio
import llm.slop.spirals.RecipeTagManager
import llm.slop.spirals.ui.theme.AppAccent
import llm.slop.spirals.ui.theme.AppBackground
import llm.slop.spirals.ui.theme.AppText

enum class RecipeSortMode(val displayName: String) {
    PETALS("Petals"),
    FAVORITES("Favorites"),
    TO_DELETE("To Delete"),
    SHAPE_RATIO("Shape Ratio"),
    MULTIPLICITY("Arm Symmetry"),
    FREQ_COUNT("Complexity"),
    HIERARCHY("Beat Layering"),
    DOMINANCE("Roundness"),
    RADIAL_VAR("Spikiness")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipePickerDialog(
    currentRecipe: MandalaRatio,
    initialSortMode: RecipeSortMode = RecipeSortMode.PETALS,
    onRecipeSelected: (MandalaRatio) -> Unit,
    onSortModeChanged: (RecipeSortMode) -> Unit = {},
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var sortMode by remember { mutableStateOf(initialSortMode) }
    var sortExpanded by remember { mutableStateOf(false) }
    val tagManager = remember { RecipeTagManager(context) }
    
    val filteredRecipes = remember(searchQuery, sortMode) {
        val filtered = if (searchQuery.isBlank()) {
            MandalaLibrary.MandalaRatios
        } else {
            MandalaLibrary.MandalaRatios.filter { ratio ->
                val query = searchQuery.lowercase()
                "${ratio.a}".contains(query) ||
                "${ratio.b}".contains(query) ||
                "${ratio.c}".contains(query) ||
                "${ratio.d}".contains(query) ||
                "${ratio.petals}".contains(query)
            }
        }
        
        val favorites = tagManager.getFavorites()
        val trash = tagManager.getTrash()
        
        // Sort based on selected mode
        when (sortMode) {
            RecipeSortMode.PETALS -> filtered.sortedBy { it.petals }
            RecipeSortMode.FAVORITES -> {
                // Favorites first (sorted by petals, then by id), then rest (same)
                val faves = filtered.filter { favorites.contains(it.id) }
                    .sortedWith(compareBy({ it.petals }, { it.id }))
                val rest = filtered.filter { !favorites.contains(it.id) }
                    .sortedWith(compareBy({ it.petals }, { it.id }))
                faves + rest
            }
            RecipeSortMode.TO_DELETE -> {
                // Trash first (sorted by id), then rest (sorted by id)
                val trashItems = filtered.filter { trash.contains(it.id) }
                    .sortedBy { it.id }
                val rest = filtered.filter { !trash.contains(it.id) }
                    .sortedBy { it.id }
                trashItems + rest
            }
            RecipeSortMode.SHAPE_RATIO -> filtered.sortedBy { it.shapeRatio }
            RecipeSortMode.MULTIPLICITY -> filtered.sortedBy { it.multiplicityClass }
            RecipeSortMode.FREQ_COUNT -> filtered.sortedBy { it.independentFreqCount }
            RecipeSortMode.HIERARCHY -> filtered.sortedBy { it.hierarchyDepth }
            RecipeSortMode.DOMINANCE -> filtered.sortedBy { it.dominanceRatio }
            RecipeSortMode.RADIAL_VAR -> filtered.sortedBy { it.radialVariance }
        }
    }
    
    // Find current recipe index and scroll to it
    val currentIndex = remember(currentRecipe) {
        filteredRecipes.indexOfFirst { it.id == currentRecipe.id }.coerceAtLeast(0)
    }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = currentIndex)
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.8f),
            color = AppBackground,
            shape = MaterialTheme.shapes.medium
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header with search
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Select Recipe",
                        style = MaterialTheme.typography.titleMedium,
                        color = AppText
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = AppText)
                    }
                }
                
                // Search bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    placeholder = { Text("Search by numbers...", color = AppText.copy(alpha = 0.5f)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = AppText) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AppAccent,
                        unfocusedBorderColor = AppText.copy(alpha = 0.3f),
                        cursorColor = AppAccent,
                        focusedTextColor = AppText,
                        unfocusedTextColor = AppText
                    ),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Sort dropdown
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { sortExpanded = true },
                        color = AppBackground,
                        shape = MaterialTheme.shapes.small,
                        border = androidx.compose.foundation.BorderStroke(1.dp, AppText.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Sort by: ${sortMode.displayName}",
                                color = AppText,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Icon(
                                Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = AppText
                            )
                        }
                    }
                    
                    DropdownMenu(
                        expanded = sortExpanded,
                        onDismissRequest = { sortExpanded = false },
                        modifier = Modifier.fillMaxWidth(0.7f)
                    ) {
                        RecipeSortMode.entries.forEach { mode ->
                            DropdownMenuItem(
                                text = { Text(mode.displayName, color = AppText) },
                                onClick = {
                                    sortMode = mode
                                    onSortModeChanged(mode)
                                    sortExpanded = false
                                }
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Recipe list
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    items(filteredRecipes, key = { it.id }) { ratio ->
                        val isSelected = ratio.id == currentRecipe.id
                        val isFavorite = tagManager.isFavorite(ratio.id)
                        val isTrash = tagManager.isTrash(ratio.id)
                        
                        // Get the sort value to display
                        val sortValue = when (sortMode) {
                            RecipeSortMode.PETALS -> null // Don't show duplicate
                            RecipeSortMode.FAVORITES -> null
                            RecipeSortMode.TO_DELETE -> null
                            RecipeSortMode.SHAPE_RATIO -> "%.2f".format(ratio.shapeRatio)
                            RecipeSortMode.MULTIPLICITY -> ratio.multiplicityClass.toString()
                            RecipeSortMode.FREQ_COUNT -> ratio.independentFreqCount.toString()
                            RecipeSortMode.HIERARCHY -> ratio.hierarchyDepth.toString()
                            RecipeSortMode.DOMINANCE -> "%.2f".format(ratio.dominanceRatio)
                            RecipeSortMode.RADIAL_VAR -> "%.1f".format(ratio.radialVariance)
                        }
                        
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                                .clickable { onRecipeSelected(ratio) },
                            color = if (isSelected) AppAccent.copy(alpha = 0.2f) else AppBackground,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Determine text color based on tags
                                val textColor = when {
                                    isTrash -> androidx.compose.ui.graphics.Color.Red
                                    isFavorite -> androidx.compose.ui.graphics.Color(0xFF00CC00) // Green
                                    isSelected -> AppAccent
                                    else -> AppText
                                }
                                
                                Text(
                                    text = "${ratio.a}, ${ratio.b}, ${ratio.c}, ${ratio.d}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = textColor,
                                    modifier = Modifier.weight(1f)
                                )
                                Row(
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.wrapContentWidth()
                                ) {
                                    Text(
                                        text = "${ratio.petals}P",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = textColor.copy(alpha = 0.8f)
                                    )
                                    if (sortValue != null) {
                                        Text(
                                            text = " • ",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = textColor.copy(alpha = 0.5f)
                                        )
                                        Text(
                                            text = sortValue,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = textColor.copy(alpha = 0.9f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
