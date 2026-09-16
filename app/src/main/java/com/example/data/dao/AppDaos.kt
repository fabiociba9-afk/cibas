package com.example.data.dao

import androidx.room.*
import com.example.data.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CompanyDao {
    @Query("SELECT * FROM companies ORDER BY name ASC")
    fun getAllCompanies(): Flow<List<CompanyEntity>>

    @Query("SELECT * FROM companies WHERE id = :id")
    suspend fun getCompanyById(id: Long): CompanyEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCompany(company: CompanyEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(companies: List<CompanyEntity>)

    @Update
    suspend fun updateCompany(company: CompanyEntity)

    @Delete
    suspend fun deleteCompany(company: CompanyEntity)
}

@Dao
interface UserDao {
    @Query("SELECT * FROM users ORDER BY name ASC")
    fun getAllUsers(): Flow<List<UserEntity>>

    @Query("SELECT COUNT(*) FROM users")
    fun getUserCount(): Flow<Int>

    @Query("SELECT * FROM users WHERE companyId = :companyId ORDER BY name ASC")
    fun getUsersByCompany(companyId: Long): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE (email = :loginOrPhone OR phone = :loginOrPhone) AND password = :password LIMIT 1")
    suspend fun authenticate(loginOrPhone: String, password: String): UserEntity?

    @Query("SELECT * FROM users WHERE id = :id")
    suspend fun getUserById(id: Long): UserEntity?

    @Query("SELECT * FROM users WHERE driverId = :driverId LIMIT 1")
    suspend fun getUserByDriverId(driverId: Long): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(users: List<UserEntity>)

    @Update
    suspend fun updateUser(user: UserEntity)

    @Delete
    suspend fun deleteUser(user: UserEntity)
}

@Dao
interface DriverDao {
    @Query("SELECT * FROM drivers ORDER BY fullName ASC")
    fun getAllDrivers(): Flow<List<DriverEntity>>

    @Query("SELECT * FROM drivers WHERE isActive = 1 ORDER BY fullName ASC")
    fun getActiveDrivers(): Flow<List<DriverEntity>>

    @Query("SELECT * FROM drivers WHERE id = :id")
    suspend fun getDriverById(id: Long): DriverEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDriver(driver: DriverEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(drivers: List<DriverEntity>)

    @Update
    suspend fun updateDriver(driver: DriverEntity)

    @Query("UPDATE drivers SET latitude = :lat, longitude = :lng, locationUpdatedAt = :timestamp WHERE id = :driverId")
    suspend fun updateDriverLocation(driverId: Long, lat: Double, lng: Double, timestamp: Long)

    @Query("UPDATE drivers SET isOnline = :isOnline WHERE id = :driverId")
    suspend fun updateDriverOnlineStatus(driverId: Long, isOnline: Boolean)

    @Delete
    suspend fun deleteDriver(driver: DriverEntity)
}

@Dao
interface PaymentMethodDao {
    @Query("SELECT * FROM payment_methods ORDER BY name ASC")
    fun getAllPaymentMethods(): Flow<List<PaymentMethodEntity>>

    @Query("SELECT * FROM payment_methods WHERE isActive = 1 ORDER BY name ASC")
    fun getActivePaymentMethods(): Flow<List<PaymentMethodEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPaymentMethod(method: PaymentMethodEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(methods: List<PaymentMethodEntity>)

    @Update
    suspend fun updatePaymentMethod(method: PaymentMethodEntity)

    @Delete
    suspend fun deletePaymentMethod(method: PaymentMethodEntity)
}

@Dao
interface TripDao {
    @Query("SELECT * FROM trips ORDER BY dateTimeMillis DESC")
    fun getAllTrips(): Flow<List<TripEntity>>

    @Query("SELECT * FROM trips WHERE companyId = :companyId ORDER BY dateTimeMillis DESC")
    fun getTripsByCompany(companyId: Long): Flow<List<TripEntity>>

    @Query("SELECT * FROM trips WHERE driverId = :driverId ORDER BY dateTimeMillis DESC")
    fun getTripsByDriver(driverId: Long): Flow<List<TripEntity>>

    @Query("SELECT * FROM trips WHERE requesterUserId = :userId ORDER BY dateTimeMillis DESC")
    fun getTripsByRequester(userId: Long): Flow<List<TripEntity>>

    @Query("SELECT * FROM trips WHERE id = :tripId")
    suspend fun getTripById(tripId: Long): TripEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrip(trip: TripEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(trips: List<TripEntity>)

    @Update
    suspend fun updateTrip(trip: TripEntity)

    @Query("UPDATE trips SET status = :status WHERE id = :tripId")
    suspend fun updateTripStatus(tripId: Long, status: String)

    @Query("UPDATE trips SET isDriverPaid = :isPaid WHERE id = :tripId")
    suspend fun updateDriverPaymentStatus(tripId: Long, isPaid: Boolean)

    @Query("UPDATE trips SET isClientPaid = :isPaid WHERE id = :tripId")
    suspend fun updateClientPaymentStatus(tripId: Long, isPaid: Boolean)

    @Query("UPDATE trips SET receiptGeneratedAt = :timestamp WHERE id = :tripId")
    suspend fun markReceiptGenerated(tripId: Long, timestamp: Long)

    @Delete
    suspend fun deleteTrip(trip: TripEntity)
}
