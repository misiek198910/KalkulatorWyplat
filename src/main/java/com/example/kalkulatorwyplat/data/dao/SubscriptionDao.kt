package com.example.kalkulatorwyplat.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.kalkulatorwyplat.data.entity.SubscriptionEntity

@Dao
interface SubscriptionDao {

    // USUNIĘTO słówko 'suspend'
    @Query("SELECT * FROM subscription_status WHERE id = 1")
    fun getStatus(): SubscriptionEntity?

    // USUNIĘTO słówko 'suspend' i zwracanie 'Long' (wracamy do klasyki)
    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    fun insert(status: SubscriptionEntity)
}