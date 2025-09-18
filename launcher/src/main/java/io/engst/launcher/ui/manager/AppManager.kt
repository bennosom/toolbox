package io.engst.launcher.ui.manager

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.engst.launcher.model.App

enum class AppSortBy {
  Label,
  PackageName,
  LastUpdated,
  TargetSdk,
}

@Preview
@Composable
fun AppManager(
   modifier: Modifier = Modifier,
   apps: List<App> = emptyList(),
   onAppLaunch: (App) -> Unit = {},
   onAppDetails: (App) -> Unit = {},
   onAppRemove: (App) -> Unit = {},
   onClose: () -> Unit = {},
) {
  var sortBy by remember { mutableStateOf(AppSortBy.Label) }
  var sortAscending by remember { mutableStateOf(true) }
  var filterUserApps by remember { mutableStateOf(true) }
  var filterSystemApps by remember { mutableStateOf(false) }
  var showSearch by remember { mutableStateOf(false) }
  var searchQuery by remember { mutableStateOf("") }

  val filtered =
      remember(apps, filterUserApps, filterSystemApps, searchQuery) {
        val base =
            apps.filter { app ->
              val sys = app.isSystemApp
              val passesFilter =
                  when {
                    filterUserApps && filterSystemApps -> true // both → all
                    filterUserApps -> !sys
                    filterSystemApps -> sys
                    else -> true // none → all
                  }
              val query = searchQuery.trim()
              val passesSearch =
                  if (query.isEmpty()) true
                  else {
                    val name = app.label
                    val pkg = app.componentName.packageName
                    name.contains(query, ignoreCase = true) ||
                        pkg.contains(query, ignoreCase = true)
                  }
              passesFilter && passesSearch
            }
        base
      }

  val sorted =
      remember(filtered, sortBy, sortAscending, searchQuery) {
        val withDetails = filtered.map { it to sortKey(it, sortBy) }
        val baseSorted =
            when (sortBy) {
              AppSortBy.PackageName,
              AppSortBy.Label -> withDetails.sortedBy { it.second as String }
              AppSortBy.TargetSdk -> withDetails.sortedBy { it.second as Int? ?: Int.MIN_VALUE }
              AppSortBy.LastUpdated -> withDetails.sortedBy { it.second as Long }
            }.let { if (sortAscending) it else it.asReversed() }

        baseSorted.map { it.first }
      }

  LazyVerticalGrid(
      modifier = modifier,
      columns = GridCells.Adaptive(300.dp),
      verticalArrangement = Arrangement.spacedBy(18.dp),
      horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
      contentPadding =
          WindowInsets.safeDrawing
              .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
              .asPaddingValues(),
  ) {
    // Header
    stickyHeader {
      Surface(
         Modifier
            .padding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top).asPaddingValues())
            .padding(start = 12.dp, top = 12.dp, end = 12.dp),
          shape = MaterialTheme.shapes.large,
          shadowElevation = 6.dp,
      ) {
        Column {
          Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.Center,
          ) {
            if (showSearch) {
              val focusRequester = remember { FocusRequester() }
              BasicTextField(
                  value = searchQuery,
                  onValueChange = { searchQuery = it },
                  modifier =
                     Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.small)
                        .background(MaterialTheme.colorScheme.surfaceBright)
                        .focusRequester(focusRequester),
                  singleLine = true,
                  cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                  textStyle = MaterialTheme.typography.bodyLarge,
                  decorationBox = { innerTextField ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                      IconButton(
                          onClick = {
                            searchQuery = ""
                            showSearch = false
                          }
                      ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Close search",
                        )
                      }
                      Box(Modifier.weight(1f)) {
                        if (searchQuery.isEmpty()) {
                          Text(
                              "Search for apps...",
                              color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                          )
                        }
                        innerTextField()
                      }
                      if (searchQuery.isNotEmpty()) {
                        IconButton({ searchQuery = "" }) {
                          Icon(Icons.Default.Clear, contentDescription = "Clear search input")
                        }
                      }
                    }
                  },
              )
              LaunchedEffect(Unit) { focusRequester.requestFocus() }
            } else {
               IconButton(onClick = onClose) { Icon(Icons.Default.Close, null) }
              Text(
                  "App Manager",
                  style = MaterialTheme.typography.titleMedium,
                  textAlign = TextAlign.Center,
                  modifier = Modifier.weight(1f),
              )
              IconButton(onClick = { showSearch = true }) {
                Icon(Icons.Default.Search, contentDescription = "Search")
              }
            }
          }
          // Sort and filters
          FlowRow(
             Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
              itemVerticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Spacer(Modifier)
            AppSortBy.entries.forEach { option ->
              val selected = sortBy == option
              FilterChip(
                  selected = selected,
                  onClick = {
                    if (selected) {
                      sortAscending = !sortAscending
                    } else {
                      sortBy = option
                      sortAscending = true
                    }
                  },
                  label = {
                    val arrow =
                        if (selected) {
                          if (sortAscending) " ↑" else " ↓"
                        } else ""
                    Text(option.label() + arrow)
                  },
              )
            }
            FilterChip(
                selected = filterUserApps,
                onClick = { filterUserApps = !filterUserApps },
                label = { Text("User apps") },
            )
            FilterChip(
                selected = filterSystemApps,
                onClick = { filterSystemApps = !filterSystemApps },
                label = { Text("System apps") },
            )
            Spacer(Modifier)
          }
        }
      }
    }

    // App list
    items(sorted, key = { it.id }) { app ->
      AppListRow(
          app = app,
          searchQuery = searchQuery,
         onLaunch = { onAppLaunch(app) },
         onDetails = { onAppDetails(app) },
         onRemove = { onAppRemove(app) },
      )
    }
  }
}

private fun AppSortBy.label(): String =
    when (this) {
      AppSortBy.PackageName -> "Package"
      AppSortBy.Label -> "Label"
      AppSortBy.TargetSdk -> "Target SDK"
      AppSortBy.LastUpdated -> "Last Updated"
    }

private fun sortKey(app: App, sortBy: AppSortBy): Any? {
  return when (sortBy) {
    AppSortBy.PackageName -> app.componentName.packageName
    AppSortBy.Label -> app.label
    AppSortBy.TargetSdk -> app.targetSdk
    AppSortBy.LastUpdated -> app.lastUpdatedTimeMillis
  }
}
