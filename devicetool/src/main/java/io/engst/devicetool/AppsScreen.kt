package io.engst.devicetool

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

private enum class AppSortBy {
  Label,
  PackageName,
  LastUpdated,
  TargetSdk,
}

@Composable
fun AppsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  var refreshCount by remember { mutableIntStateOf(0) }
  var searchQuery by remember { mutableStateOf("") }
  var sortBy by remember { mutableStateOf(AppSortBy.Label) }
  var sortAscending by remember { mutableStateOf(true) }
  var filterUserApps by remember { mutableStateOf(true) }
  var filterSystemApps by remember { mutableStateOf(false) }
  val apps by produceState(initialValue = emptyList<InstalledApp>(), context, refreshCount) {
    value = context.loadInstalledApps()
  }

  val filteredApps =
      remember(apps, searchQuery, filterUserApps, filterSystemApps) {
        val trimmedQuery = searchQuery.trim()
        apps.filter { app ->
          val filterMatch =
              when {
                filterUserApps && filterSystemApps -> true
                filterUserApps -> !app.isSystemApp
                filterSystemApps -> app.isSystemApp
                else -> true
              }
          val searchMatch =
              if (trimmedQuery.isEmpty()) {
                true
              } else {
                app.label.contains(trimmedQuery, ignoreCase = true) ||
                    app.packageName.contains(trimmedQuery, ignoreCase = true)
              }
          filterMatch && searchMatch
        }
      }

  val sortedApps =
      remember(filteredApps, sortBy, sortAscending) {
        val sorted =
            when (sortBy) {
              AppSortBy.Label -> filteredApps.sortedBy { it.label.lowercase() }
              AppSortBy.PackageName -> filteredApps.sortedBy { it.packageName.lowercase() }
              AppSortBy.LastUpdated -> filteredApps.sortedBy { it.lastUpdatedTimeMillis }
              AppSortBy.TargetSdk -> filteredApps.sortedBy { it.targetSdk }
            }
        if (sortAscending) sorted else sorted.asReversed()
      }

  Column(
      modifier =
          modifier
              .fillMaxSize()
              .safeContentPadding()
              .padding(12.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Button(onClick = onNavigateBack) { Text("Back to insets-screen") }

    SearchBar(
        query = searchQuery,
        onQueryChange = { searchQuery = it },
        onClear = { searchQuery = "" },
        onRefresh = { refreshCount += 1 },
    )

    SortAndFilterRow(
        sortBy = sortBy,
        sortAscending = sortAscending,
        onSortChanged = { selected ->
          if (sortBy == selected) {
            sortAscending = !sortAscending
          } else {
            sortBy = selected
            sortAscending = true
          }
        },
        filterUserApps = filterUserApps,
        filterSystemApps = filterSystemApps,
        onUserFilterChanged = { filterUserApps = !filterUserApps },
        onSystemFilterChanged = { filterSystemApps = !filterSystemApps },
    )

    Text(
        text = "Showing ${sortedApps.size} of ${apps.size} apps",
        style = MaterialTheme.typography.labelLarge,
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      items(sortedApps, key = { it.id }) { app ->
        AppListItem(
            app = app,
            onLaunch = { context.launchInstalledApp(app.packageName) },
            onDetails = { context.openAppDetails(app.packageName) },
            onRemove = { context.requestAppRemoval(app.packageName) },
        )
      }
    }
  }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    onRefresh: () -> Unit,
) {
  Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.weight(1f),
        label = { Text("Search apps") },
    )
    if (query.isNotEmpty()) {
      Button(onClick = onClear) { Text("Clear") }
    }
    Button(onClick = onRefresh) { Text("Refresh") }
  }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun SortAndFilterRow(
    sortBy: AppSortBy,
    sortAscending: Boolean,
    onSortChanged: (AppSortBy) -> Unit,
    filterUserApps: Boolean,
    filterSystemApps: Boolean,
    onUserFilterChanged: () -> Unit,
    onSystemFilterChanged: () -> Unit,
) {
  FlowRow(
      modifier = Modifier.fillMaxWidth(),
      itemVerticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    AppSortBy.entries.forEach { option ->
      val selected = sortBy == option
      val label =
          if (selected) {
            "${option.label()} ${if (sortAscending) "↑" else "↓"}"
          } else {
            option.label()
          }
      FilterChip(
          selected = selected,
          onClick = { onSortChanged(option) },
          label = { Text(label) },
      )
    }
    FilterChip(
        selected = filterUserApps,
        onClick = onUserFilterChanged,
        label = { Text("User apps") },
    )
    FilterChip(
        selected = filterSystemApps,
        onClick = onSystemFilterChanged,
        label = { Text("System apps") },
    )
  }
}

private fun AppSortBy.label(): String =
    when (this) {
      AppSortBy.Label -> "Label"
      AppSortBy.PackageName -> "Package"
      AppSortBy.LastUpdated -> "Last Updated"
      AppSortBy.TargetSdk -> "Target SDK"
    }

@Preview(showBackground = true, widthDp = 420, heightDp = 820)
@Composable
private fun AppsScreenPreview() {
  MaterialTheme { AppsScreen(onNavigateBack = {}) }
}
