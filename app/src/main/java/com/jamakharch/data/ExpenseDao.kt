package com.jamakharch.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {

    @Query("SELECT * FROM expenses ORDER BY timestamp DESC")
    fun getAllFlow(): Flow<List<ExpenseEntity>>

    @Query(
        """
        SELECT * FROM expenses 
        WHERE timestamp >= :startOfMonth AND timestamp < :startOfNextMonth 
        ORDER BY timestamp DESC
        """
    )
    fun getByMonth(startOfMonth: Long, startOfNextMonth: Long): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE id = :id")
    suspend fun getById(id: Long): ExpenseEntity?

    @Query(
        """
        SELECT * FROM expenses 
        WHERE category = :category AND timestamp >= :startOfMonth AND timestamp < :startOfNextMonth 
        ORDER BY timestamp DESC
        """
    )
    fun getByCategoryAndMonth(
        category: String,
        startOfMonth: Long,
        startOfNextMonth: Long
    ): Flow<List<ExpenseEntity>>

    @Query("UPDATE expenses SET category = :category WHERE id = :id")
    suspend fun updateCategory(id: Long, category: String)

    @Query("UPDATE expenses SET category = :category WHERE LOWER(merchant) = :merchant")
    suspend fun updateCategoryByMerchant(merchant: String, category: String)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(expenses: List<ExpenseEntity>)

    @Query("DELETE FROM expenses")
    suspend fun clearAll()
}
