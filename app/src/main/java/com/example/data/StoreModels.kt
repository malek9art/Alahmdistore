package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey val id: Int,
    val nameAr: String,
    val iconName: String
)

@Entity(tableName = "products")
data class Product(
    @PrimaryKey val id: Int,
    val nameAr: String,
    val descriptionAr: String,
    val price: Double,
    val oldPrice: Double? = null,
    val imageUrl: String,
    val categoryId: Int,
    val stockQuantity: Int,
    val isFeatured: Boolean = false,
    val isOffer: Boolean = false,
    val rating: Float = 4.5f,
    val specsAr: String = "" // Comma-separated list or JSON representing technical specs
)

@Entity(tableName = "cart_items")
data class CartItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val productId: Int,
    val quantity: Int,
    val selectedVariant: String = ""
)

@Entity(tableName = "orders")
data class Order(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val customerName: String,
    val customerPhone: String,
    val deliveryAddress: String,
    val paymentMethod: String, // e.g., "الدفع عند الاستلام", "بطاقة ائتمان", "مدى", "Apple Pay"
    val shippingCost: Double,
    val totalAmount: Double,
    val status: String, // "جديد", "قيد التجهيز", "جاهز للشحن", "في الطريق", "تم التسليم", "ملغي"
    val timestamp: Long = System.currentTimeMillis(),
    val deliveryAgentName: String? = null,
    val chosenCurrency: String = "الريال اليمني (ر.ي)",
    val convertedAmount: Double = 0.0,
    val failureReason: String? = null,
    val paymentReference: String? = null,
    val paymentStatus: String? = "معلق",
    val latitude: Double? = null,
    val longitude: Double? = null
)

@Entity(tableName = "order_items")
data class OrderItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val orderId: Int,
    val productId: Int,
    val productName: String,
    val quantity: Int,
    val price: Double
)

@Entity(tableName = "notifications")
data class Notification(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val titleAr: String,
    val messageAr: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)

@Entity(tableName = "audit_logs")
data class AuditLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val actionAr: String,
    val userRole: String, // "مدير", "عميل", "مندوب"
    val timestamp: Long = System.currentTimeMillis(),
    val detailsAr: String = ""
)

@Entity(tableName = "favorites")
data class FavoriteItem(
    @PrimaryKey val productId: Int
)
