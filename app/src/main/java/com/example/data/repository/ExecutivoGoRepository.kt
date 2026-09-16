package com.example.data.repository

import com.example.data.database.ExecutivoGoDatabase
import com.example.data.entity.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class ExecutivoGoRepository(private val db: ExecutivoGoDatabase) {

    private val companyDao = db.companyDao()
    private val userDao = db.userDao()
    private val driverDao = db.driverDao()
    private val paymentMethodDao = db.paymentMethodDao()
    private val tripDao = db.tripDao()

    // Companies
    val allCompanies: Flow<List<CompanyEntity>> = companyDao.getAllCompanies()
    suspend fun getCompanyById(id: Long) = withContext(Dispatchers.IO) { companyDao.getCompanyById(id) }
    suspend fun insertCompany(company: CompanyEntity) = withContext(Dispatchers.IO) { companyDao.insertCompany(company) }
    suspend fun updateCompany(company: CompanyEntity) = withContext(Dispatchers.IO) { companyDao.updateCompany(company) }
    suspend fun deleteCompany(company: CompanyEntity) = withContext(Dispatchers.IO) { companyDao.deleteCompany(company) }

    // Users
    val allUsers: Flow<List<UserEntity>> = userDao.getAllUsers()
    val userCount: Flow<Int> = userDao.getUserCount()
    fun getUsersByCompany(companyId: Long): Flow<List<UserEntity>> = userDao.getUsersByCompany(companyId)
    suspend fun authenticate(loginOrPhone: String, pass: String) = withContext(Dispatchers.IO) {
        userDao.authenticate(loginOrPhone.trim(), pass.trim())
    }
    suspend fun getUserById(id: Long) = withContext(Dispatchers.IO) { userDao.getUserById(id) }
    suspend fun getUserByDriverId(driverId: Long) = withContext(Dispatchers.IO) { userDao.getUserByDriverId(driverId) }
    suspend fun insertUser(user: UserEntity) = withContext(Dispatchers.IO) { userDao.insertUser(user) }
    suspend fun updateUser(user: UserEntity) = withContext(Dispatchers.IO) { userDao.updateUser(user) }
    suspend fun deleteUser(user: UserEntity) = withContext(Dispatchers.IO) { userDao.deleteUser(user) }

    // Drivers
    val allDrivers: Flow<List<DriverEntity>> = driverDao.getAllDrivers()
    val activeDrivers: Flow<List<DriverEntity>> = driverDao.getActiveDrivers()
    suspend fun getDriverById(id: Long) = withContext(Dispatchers.IO) { driverDao.getDriverById(id) }
    suspend fun insertDriver(driver: DriverEntity) = withContext(Dispatchers.IO) { driverDao.insertDriver(driver) }
    suspend fun updateDriver(driver: DriverEntity) = withContext(Dispatchers.IO) { driverDao.updateDriver(driver) }
    suspend fun deleteDriver(driver: DriverEntity) = withContext(Dispatchers.IO) { driverDao.deleteDriver(driver) }
    suspend fun updateDriverLocation(driverId: Long, lat: Double, lng: Double) = withContext(Dispatchers.IO) {
        driverDao.updateDriverLocation(driverId, lat, lng, System.currentTimeMillis())
    }
    suspend fun updateDriverOnlineStatus(driverId: Long, isOnline: Boolean) = withContext(Dispatchers.IO) {
        driverDao.updateDriverOnlineStatus(driverId, isOnline)
    }

    // Payment Methods
    val allPaymentMethods: Flow<List<PaymentMethodEntity>> = paymentMethodDao.getAllPaymentMethods()
    val activePaymentMethods: Flow<List<PaymentMethodEntity>> = paymentMethodDao.getActivePaymentMethods()
    suspend fun insertPaymentMethod(method: PaymentMethodEntity) = withContext(Dispatchers.IO) { paymentMethodDao.insertPaymentMethod(method) }
    suspend fun updatePaymentMethod(method: PaymentMethodEntity) = withContext(Dispatchers.IO) { paymentMethodDao.updatePaymentMethod(method) }
    suspend fun deletePaymentMethod(method: PaymentMethodEntity) = withContext(Dispatchers.IO) { paymentMethodDao.deletePaymentMethod(method) }

    // Trips
    val allTrips: Flow<List<TripEntity>> = tripDao.getAllTrips()
    fun getTripsByCompany(companyId: Long): Flow<List<TripEntity>> = tripDao.getTripsByCompany(companyId)
    fun getTripsByDriver(driverId: Long): Flow<List<TripEntity>> = tripDao.getTripsByDriver(driverId)
    fun getTripsByRequester(userId: Long): Flow<List<TripEntity>> = tripDao.getTripsByRequester(userId)
    suspend fun getTripById(id: Long) = withContext(Dispatchers.IO) { tripDao.getTripById(id) }
    suspend fun insertTrip(trip: TripEntity) = withContext(Dispatchers.IO) { tripDao.insertTrip(trip) }
    suspend fun updateTrip(trip: TripEntity) = withContext(Dispatchers.IO) { tripDao.updateTrip(trip) }
    suspend fun updateTripStatus(tripId: Long, status: TripStatus) = withContext(Dispatchers.IO) {
        tripDao.updateTripStatus(tripId, status.name)
    }
    suspend fun updateDriverPaymentStatus(tripId: Long, isPaid: Boolean) = withContext(Dispatchers.IO) {
        tripDao.updateDriverPaymentStatus(tripId, isPaid)
    }
    suspend fun updateClientPaymentStatus(tripId: Long, isPaid: Boolean) = withContext(Dispatchers.IO) {
        tripDao.updateClientPaymentStatus(tripId, isPaid)
    }
    suspend fun markReceiptGenerated(tripId: Long) = withContext(Dispatchers.IO) {
        tripDao.markReceiptGenerated(tripId, System.currentTimeMillis())
    }
    suspend fun deleteTrip(trip: TripEntity) = withContext(Dispatchers.IO) { tripDao.deleteTrip(trip) }
}
