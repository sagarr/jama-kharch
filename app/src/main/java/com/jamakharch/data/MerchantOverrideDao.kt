package com.jamakharch.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MerchantOverrideDao {

    @Query("SELECT * FROM merchant_overrides WHERE merchant = :merchant")
    suspend fun getOverride(merchant: String): MerchantOverrideEntity?

    @Query("SELECT * FROM merchant_overrides")
    fun getAllOverrides(): Flow<List<MerchantOverrideEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setOverride(override: MerchantOverrideEntity)

    @Query("DELETE FROM merchant_overrides WHERE merchant = :merchant")
    suspend fun removeOverride(merchant: String)
}
