package com.aistudio.executivogo.trnsp.data

import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName

@IgnoreExtraProperties
data class AppUser(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val role: String = UserRole.DRIVER, // "ADMIN", "DRIVER", "OPERATOR", "COMPANY"
    val phone: String = "",
    val companyId: String = "",          // OBRIGATÓRIO se role == COMPANY
    val companyName: String = "",
    val active: Boolean = true,
    val fcmToken: String = "",
    val fcmTokenAndroid: String = "",
    val fcmTokenWeb: String = "",
    @get:PropertyName("isOnline")
    @set:PropertyName("isOnline")
    var isOnline: Boolean = true,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val speed: Double = 0.0,
    val bearing: Double = 0.0,
    val lastLocationUpdate: Long = 0L,
    
    // Dados do veículo (para role == DRIVER)
    val vehicleModel: String = "",
    val vehicleColor: String = "",
    val vehicleYear: String = "",
    val vehiclePlate: String = "",
    val commissionPercentage: Double = 20.0,
    val authKey: String = ""
)

@IgnoreExtraProperties
data class Company(
    val id: String = "",
    val name: String = "",
    val fantasyName: String = "",
    val cnpj: String = "",
    val contactPerson: String = "",
    val phone: String = "",
    val address: String = "",
    val email: String = "",
    val active: Boolean = true,
    val paymentTerm: String = PaymentTerms.A_VISTA, // A_VISTA, D7, D15, D30, D60
    val paymentMeans: String = PaymentMeans.PIX     // PIX, CREDITO, DEBITO, DINHEIRO
)

@IgnoreExtraProperties
data class PaymentMethod(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val termDays: Int = 0,               // 0, 7, 15, 30, 60
    val means: String = PaymentMeans.PIX, // PIX, CREDITO, DEBITO, FATURAMENTO, DINHEIRO
    val active: Boolean = true
)

@IgnoreExtraProperties
data class Passenger(
    val id: String = "",
    val name: String = "",
    val phone: String = "",
    val email: String = "",
    val companyId: String = "",
    val active: Boolean = true
)

@IgnoreExtraProperties
data class Trip(
    val id: String = "",
    val companyId: String = "",
    val companyName: String = "",
    val driverId: String? = null,
    val driverName: String = "",
    val driverPhone: String = "",
    val vehicleModel: String = "",
    val vehiclePlate: String = "",
    val vehicleColor: String = "",
    val passengerId: String = "",
    val passengerName: String = "",
    val passengerPhone: String = "",
    val additionalPassengers: List<String> = emptyList(),
    val origin: String = "",
    val destination: String = "",
    val stops: List<String> = emptyList(),
    val currentStopIndex: Int = 0,
    val scheduledTime: Long = System.currentTimeMillis(),
    val status: String = TripStatus.PENDING, // PENDING, ACCEPTED, IN_PROGRESS, COMPLETED, CANCELLED
    val price: Double = 0.0,
    val driverCommission: Double = 0.0,

    // Pagamento empresa
    val companyPaymentStatus: String = PaymentStatus.PENDING, // PENDING, PAID
    val companyPaidAt: Long? = null,
    val companyPaidByAdminId: String? = null,

    // Comissão motorista
    val paymentStatus: String = PaymentStatus.PENDING,        // PENDING, PAID
    val paidAt: Long? = null,
    val paidByAdminId: String? = null,

    // Prazos (calculados na conclusão ou criação)
    val completedAt: Long? = null,
    val dueDate: Long? = null,              // completedAt + termDays em millis
    val commissionAvailableAt: Long? = null, // IGUAL ao dueDate
    val createdAt: Long = System.currentTimeMillis(),
    val notes: String = "",
    val paymentTermSnapshot: String = PaymentTerms.A_VISTA,
    val paymentMeansSnapshot: String = PaymentMeans.PIX,

    // Vínculo com Rota / Precificação e Solicitação Empresa
    val requestedByCompany: Boolean = false,
    val requestedByUserId: String = "",
    val routeId: String = "",
    val distanceKm: Double = 0.0,
    val pricingMode: String = "",
    val fareBandId: String = "",
    val multiplierSnapshot: Double = 0.0,

    // Legado / compatibilidade
    val paymentMethodId: String = "",
    val paymentMethodName: String = ""
)

@IgnoreExtraProperties
data class FareBand(
    val id: String = "",
    val name: String = "",
    val minKm: Double = 0.0,
    val maxKm: Double = 0.0,
    val multiplier: Double = 0.0, // R$ por km
    val active: Boolean = true,
    val sortOrder: Int = 0
)

object PricingMode {
    const val FIXED = "FIXED"
    const val PER_KM = "PER_KM"
}

@IgnoreExtraProperties
data class Route(
    val id: String = "",
    val origin: String = "",
    val destination: String = "",
    val pricingMode: String = PricingMode.FIXED, // "FIXED" | "PER_KM"
    val distanceKm: Double = 0.0,
    val price: Double = 0.0,
    val fareBandId: String = "",
    val multiplierSnapshot: Double = 0.0,
    val active: Boolean = true,
    val notes: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)

@IgnoreExtraProperties
data class Settlement(
    val id: String = "",
    val driverId: String = "",
    val driverName: String = "",
    val tripIds: List<String> = emptyList(),
    val totalAmount: Double = 0.0,
    val paidAt: Long = System.currentTimeMillis(),
    val createdByAdminId: String = "",
    val notes: String = "",
    val periodStart: Long? = null,
    val periodEnd: Long? = null
)

@IgnoreExtraProperties
data class CompanyReceipt(
    val id: String = "",
    val companyId: String = "",
    val companyName: String = "",
    val tripIds: List<String> = emptyList(),
    val totalAmount: Double = 0.0,
    val paidAt: Long = System.currentTimeMillis(),
    val createdByAdminId: String = "",
    val means: String = PaymentMeans.PIX,
    val notes: String = ""
)

object UserRole {
    const val ADMIN = "ADMIN"
    const val DRIVER = "DRIVER"
    const val OPERATOR = "OPERATOR"
    const val COMPANY = "COMPANY"
}

object TripStatus {
    const val PENDING = "PENDING"
    const val ACCEPTED = "ACCEPTED"
    const val IN_PROGRESS = "IN_PROGRESS"
    const val COMPLETED = "COMPLETED"
    const val CANCELLED = "CANCELLED"
}

object PaymentStatus {
    const val PENDING = "PENDING"
    const val PAID = "PAID"
}

object PaymentTerms {
    const val A_VISTA = "A_VISTA"
    const val D7 = "D7"
    const val D15 = "D15"
    const val D30 = "D30"
    const val D60 = "D60"

    fun getDays(term: String): Int {
        return when (term.trim().uppercase()) {
            D7, "7 DIAS", "7 DIAS (D7)", "7" -> 7
            D15, "15 DIAS", "15 DIAS (D15)", "15" -> 15
            D30, "30 DIAS", "30 DIAS (D30)", "30" -> 30
            D60, "60 DIAS", "60 DIAS (D60)", "60" -> 60
            else -> 0 // A_VISTA
        }
    }

    fun getLabel(term: String): String {
        return when (term.trim().uppercase()) {
            A_VISTA -> "À vista"
            D7, "7 DIAS", "7 DIAS (D7)", "7" -> "7 dias"
            D15, "15 DIAS", "15 DIAS (D15)", "15" -> "15 dias"
            D30, "30 DIAS", "30 DIAS (D30)", "30" -> "30 dias"
            D60, "60 DIAS", "60 DIAS (D60)", "60" -> "60 dias"
            else -> term
        }
    }
}

object PaymentMeans {
    const val PIX = "PIX"
    const val CREDITO = "CREDITO"
    const val DEBITO = "DEBITO"
    const val FATURAMENTO = "FATURAMENTO"
    const val DINHEIRO = "DINHEIRO"

    fun getLabel(means: String): String {
        return when (means.trim().uppercase()) {
            PIX -> "PIX"
            CREDITO -> "Cartão de Crédito"
            DEBITO -> "Cartão de Débito"
            FATURAMENTO -> "Faturamento"
            DINHEIRO -> "Dinheiro"
            else -> means
        }
    }
}

@IgnoreExtraProperties
data class NotificationRecord(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val role: String = "",
    val companyId: String = "",
    val fcmToken: String = "",
    val title: String = "",
    val body: String = "",
    val tripId: String = "",
    val type: String = "",
    val read: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

object NotificationType {
    const val TRIP_CREATED_ADMIN = "TRIP_CREATED_ADMIN"
    const val TRIP_CREATED_COMPANY = "TRIP_CREATED_COMPANY"
    const val TRIP_ACCEPTED_DRIVER = "TRIP_ACCEPTED_DRIVER"
    const val TRIP_REJECTED_DRIVER = "TRIP_REJECTED_DRIVER"
    const val TRIP_CANCELLED = "TRIP_CANCELLED"
    const val COMMISSION_PAID = "COMMISSION_PAID"
    const val DRIVER_ASSIGNED = "DRIVER_ASSIGNED"
    const val COMPANY_PAYMENT_RECEIVED = "COMPANY_PAYMENT_RECEIVED"
    const val TRIP_STARTED = "TRIP_STARTED"
}

@IgnoreExtraProperties
data class DriverLocation(
    val id: String = "",
    val driverId: String = "",
    val driverName: String = "",
    val driverPhone: String = "",
    val vehicleModel: String = "",
    val vehiclePlate: String = "",
    val vehicleColor: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val speed: Double = 0.0,
    val bearing: Double = 0.0,
    @get:PropertyName("isOnline")
    @set:PropertyName("isOnline")
    var isOnline: Boolean = true,
    val inTrip: Boolean = false,
    val currentTripId: String = "",
    val companyId: String = "",
    val companyName: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)
