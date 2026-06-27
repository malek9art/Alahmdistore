package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface StoreDao {
    // Categories
    @Query("SELECT * FROM categories ORDER BY id ASC")
    fun getAllCategories(): Flow<List<Category>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<Category>)

    @Query("DELETE FROM categories WHERE id = :catId")
    suspend fun deleteCategoryById(catId: Int)

    // Products
    @Query("SELECT * FROM products ORDER BY id DESC")
    fun getAllProducts(): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE categoryId = :catId ORDER BY id DESC")
    fun getProductsByCategory(catId: Int): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE id = :prodId")
    suspend fun getProductById(prodId: Int): Product?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(products: List<Product>)

    @Update
    suspend fun updateProduct(product: Product)

    @Delete
    suspend fun deleteProduct(product: Product)

    @Query("UPDATE products SET stockQuantity = :newStock WHERE id = :prodId")
    suspend fun updateProductStock(prodId: Int, newStock: Int)

    // Cart Items
    @Query("SELECT * FROM cart_items ORDER BY id DESC")
    fun getCartItems(): Flow<List<CartItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addToCart(item: CartItem)

    @Query("UPDATE cart_items SET quantity = :newQty WHERE id = :cartId")
    suspend fun updateCartQuantity(cartId: Int, newQty: Int)

    @Query("DELETE FROM cart_items WHERE id = :cartId")
    suspend fun removeFromCart(cartId: Int)

    @Query("DELETE FROM cart_items")
    suspend fun clearCart()

    // Orders
    @Query("SELECT * FROM orders ORDER BY timestamp DESC")
    fun getAllOrders(): Flow<List<Order>>

    @Query("SELECT * FROM orders WHERE id = :orderId")
    fun getOrderFlow(orderId: Int): Flow<Order?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: Order): Long

    @Query("UPDATE orders SET status = :newStatus WHERE id = :orderId")
    suspend fun updateOrderStatus(orderId: Int, newStatus: String)

    @Query("UPDATE orders SET status = :newStatus, failureReason = :reason WHERE id = :orderId")
    suspend fun updateOrderStatusWithReason(orderId: Int, newStatus: String, reason: String)

    @Query("UPDATE orders SET paymentReference = :ref, paymentStatus = :payStatus WHERE id = :orderId")
    suspend fun updateOrderPaymentDetails(orderId: Int, ref: String, payStatus: String)

    @Query("UPDATE orders SET deliveryAgentName = :agentName WHERE id = :orderId")
    suspend fun assignOrderToAgent(orderId: Int, agentName: String)

    // Order Items
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrderItems(items: List<OrderItem>)

    @Query("SELECT * FROM order_items WHERE orderId = :orderId")
    fun getOrderItems(orderId: Int): Flow<List<OrderItem>>

    @Query("SELECT * FROM order_items")
    suspend fun getAllOrderItems(): List<OrderItem>

    // Notifications
    @Query("SELECT * FROM notifications ORDER BY timestamp DESC")
    fun getAllNotifications(): Flow<List<Notification>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: Notification)

    @Query("UPDATE notifications SET isRead = 1 WHERE id = :id")
    suspend fun markNotificationAsRead(id: Int)

    // Audit Logs
    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC")
    fun getAllAuditLogs(): Flow<List<AuditLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditLog(log: AuditLog)

    // Favorites
    @Query("SELECT * FROM favorites")
    fun getAllFavorites(): Flow<List<FavoriteItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(favorite: FavoriteItem)

    @Query("DELETE FROM favorites WHERE productId = :prodId")
    suspend fun removeFavorite(prodId: Int)

    // Clear Queries for full restore
    @Query("DELETE FROM categories")
    suspend fun clearCategories()

    @Query("DELETE FROM products")
    suspend fun clearProducts()

    @Query("DELETE FROM orders")
    suspend fun clearOrders()

    @Query("DELETE FROM order_items")
    suspend fun clearOrderItems()

    @Query("DELETE FROM notifications")
    suspend fun clearNotifications()

    @Query("DELETE FROM audit_logs")
    suspend fun clearAuditLogs()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditLogs(logs: List<AuditLog>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotifications(notifications: List<Notification>)
}

@Database(
    entities = [
        Category::class,
        Product::class,
        CartItem::class,
        Order::class,
        OrderItem::class,
        Notification::class,
        AuditLog::class,
        FavoriteItem::class
    ],
    version = 3,
    exportSchema = false
)
abstract class StoreDatabase : RoomDatabase() {
    abstract fun storeDao(): StoreDao
}
