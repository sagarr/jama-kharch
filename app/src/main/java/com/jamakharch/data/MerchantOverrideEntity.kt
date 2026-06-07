package com.jamakharch.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "merchant_overrides")
data class MerchantOverrideEntity(
    @PrimaryKey val merchant: String,
    val category: String
)
