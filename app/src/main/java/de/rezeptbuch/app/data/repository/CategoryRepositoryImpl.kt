package de.rezeptbuch.app.data.repository

import de.rezeptbuch.app.data.local.AppDatabase
import de.rezeptbuch.app.data.local.toDomain
import de.rezeptbuch.app.data.local.toEntity
import de.rezeptbuch.app.domain.model.Category
import de.rezeptbuch.app.domain.model.CategoryWithCount
import de.rezeptbuch.app.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRepositoryImpl @Inject constructor(
    db: AppDatabase,
) : CategoryRepository {

    private val dao = db.categoryDao()

    override fun observeCategories(): Flow<List<CategoryWithCount>> =
        dao.observeWithCounts().map { list -> list.map { it.toDomain() } }

    override fun observeCategory(id: Long): Flow<Category?> = dao.observe(id).map { it?.toDomain() }

    override suspend fun saveCategory(category: Category): Long {
        val existing = if (category.id != 0L) dao.get(category.id) else null
        return if (existing == null) {
            dao.insert(category.copy(id = 0, position = dao.nextPosition()).toEntity(System.currentTimeMillis()))
        } else {
            dao.update(category.copy(position = existing.position, uuid = existing.uuid).toEntity(existing.createdAt))
            existing.id
        }
    }

    override suspend fun deleteCategory(id: Long) = dao.delete(id)
}
