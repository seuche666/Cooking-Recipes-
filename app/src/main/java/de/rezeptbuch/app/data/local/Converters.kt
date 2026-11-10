package de.rezeptbuch.app.data.local

import androidx.room.TypeConverter
import de.rezeptbuch.app.domain.model.IngredientUnit

class Converters {
    @TypeConverter
    fun unitToString(unit: IngredientUnit): String = unit.name

    @TypeConverter
    fun stringToUnit(value: String): IngredientUnit = IngredientUnit.fromName(value)
}
