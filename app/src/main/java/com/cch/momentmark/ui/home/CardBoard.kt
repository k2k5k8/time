package com.cch.momentmark.ui.home

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.cch.momentmark.domain.model.TimeEvent
import com.cch.momentmark.domain.model.TravelCardSize
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.cardBoardDataStore by preferencesDataStore(name = "moment_mark_card_board")

/**
 * Stored independently from an event's content. This keeps future card styles
 * compatible with the board: renderers supply their size, while the board owns
 * its place and order.
 */
@Immutable
data class HomeCardLayout(
    val cardId: String,
    val order: Int,
    val gridWidth: Int,
    val gridHeight: Int = 1,
)

internal fun defaultCardLayout(event: TimeEvent, order: Int): HomeCardLayout = HomeCardLayout(
    cardId = event.id,
    order = order,
    gridWidth = if (event.travelCardConfig?.size == TravelCardSize.WIDE) 2 else 1,
    // The current two-column feed has Small and Wide renderers. Retaining a
    // height field now means a future 2 x 2 template will not need a migration.
    gridHeight = 1,
)

/** Local-first persistence boundary required by the editable home card wall. */
internal class CardLayoutStorage(context: Context) {
    private val dataStore = context.applicationContext.cardBoardDataStore

    val loadCardLayout: Flow<Map<String, HomeCardLayout>> = dataStore.data.map { preferences ->
        runCatching {
            val array = JSONArray(preferences[LayoutsKey].orEmpty())
            buildMap {
                repeat(array.length()) { index ->
                    val value = array.getJSONObject(index)
                    val layout = HomeCardLayout(
                        cardId = value.getString("cardId"),
                        order = value.getInt("order"),
                        gridWidth = value.optInt("gridWidth", 1).coerceIn(1, 2),
                        gridHeight = value.optInt("gridHeight", 1).coerceAtLeast(1),
                    )
                    put(layout.cardId, layout)
                }
            }
        }.getOrDefault(emptyMap())
    }

    suspend fun saveCardLayout(layouts: Collection<HomeCardLayout>) {
        dataStore.edit { preferences ->
            val serialized = JSONArray()
            layouts.sortedBy { it.order }.forEach { layout ->
                serialized.put(
                    JSONObject()
                        .put("cardId", layout.cardId)
                        .put("order", layout.order)
                        .put("gridWidth", layout.gridWidth)
                        .put("gridHeight", layout.gridHeight),
                )
            }
            preferences[LayoutsKey] = serialized.toString()
        }
    }

    private companion object {
        val LayoutsKey = stringPreferencesKey("home_card_layouts")
    }
}

/**
 * Reorders only the board metadata; no event presentation/configuration is
 * modified. The result is dense and stable, so it remains valid if a card is
 * hidden by a filter and shown again later.
 */
internal fun reorderedCardLayouts(
    current: List<HomeCardLayout>,
    draggedId: String,
    targetId: String,
): List<HomeCardLayout> {
    if (draggedId == targetId) return current
    val sourceIndex = current.indexOfFirst { it.cardId == draggedId }
    val targetIndex = current.indexOfFirst { it.cardId == targetId }
    if (sourceIndex < 0 || targetIndex < 0) return current

    return current.toMutableList().also { cards ->
        val dragged = cards.removeAt(sourceIndex)
        cards.add(targetIndex.coerceIn(0, cards.size), dragged)
    }.mapIndexed { index, card -> card.copy(order = index) }
}
