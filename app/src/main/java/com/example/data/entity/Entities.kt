package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class UserRole(val displayName: String) {
    SUPER_ADMIN("Super Administrador"),
    COMPANY_ADMIN("Admin da Empresa"),
    DRIVER("Motorista Executivo"),
    COMPANY_USER("Solicitante da Empresa")
}

enum class TripStatus(val label: String) {
    AGENDADA("Agendada"),
    EM_ANDAMENTO("Em andamento"),
    CONCLUIDA("Concluída"),
    CANCELADA("Cancelada")
}

@Entity(tableName = "companies")
data class CompanyEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val cnpj: String,
    val phone: String,
    val address: String,
    val isActive: Boolean = true
)

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val email: String,
    val phone: String,
    val role: String, // from UserRole.name
    val companyId: Long? = null,
    val jobTitle: String? = null,
    val driverId: Long? = null,
    val password: String = "123456",
    val isActive: Boolean = true
)

@Entity(tableName = "drivers")
data class DriverEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fullName: String,
    val phone: String,
    val vehiclePlate: String,
    val vehicleModel: String,
    val commissionPercentage: Double = 75.0, // Default 75%
    val isActive: Boolean = true,
    val latitude: Double = -23.55052,
    val longitude: Double = -46.633308,
    val locationUpdatedAt: Long = System.currentTimeMillis(),
    val isOnline: Boolean = true
)

@Entity(tableName = "payment_methods")
data class PaymentMethodEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String = "",
    val isActive: Boolean = true
)

@Entity(tableName = "trips")
data class TripEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val companyId: Long,
    val companyName: String,
    val requesterUserId: Long,
    val requesterName: String,
    val dateTimeMillis: Long = System.currentTimeMillis(),
    val origin: String,
    val destination: String,
    val price: Double,
    val paymentMethodId: Long,
    val paymentMethodName: String,
    val clientPaymentDueDateMillis: Long,
    val driverId: Long,
    val driverName: String,
    val driverCommissionAmount: Double,
    val driverCommissionDueDateMillis: Long,
    val isDriverPaid: Boolean = false,
    val isClientPaid: Boolean = false,
    val status: String = TripStatus.AGENDADA.name,
    val notes: String = "",
    val receiptGeneratedAt: Long? = null
)
