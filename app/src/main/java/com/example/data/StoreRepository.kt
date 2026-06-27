package com.example.data

import kotlinx.coroutines.flow.Flow

class StoreRepository(private val storeDao: StoreDao) {

    // Categories
    val allCategories: Flow<List<Category>> = storeDao.getAllCategories()
    
    suspend fun insertCategories(categories: List<Category>) = 
        storeDao.insertCategories(categories)

    suspend fun deleteCategoryById(catId: Int) = 
        storeDao.deleteCategoryById(catId)

    // Products
    val allProducts: Flow<List<Product>> = storeDao.getAllProducts()
    
    fun getProductsByCategory(catId: Int): Flow<List<Product>> = 
        storeDao.getProductsByCategory(catId)

    suspend fun getProductById(prodId: Int): Product? = 
        storeDao.getProductById(prodId)

    suspend fun insertProducts(products: List<Product>) = 
        storeDao.insertProducts(products)

    suspend fun updateProduct(product: Product) = 
        storeDao.updateProduct(product)

    suspend fun deleteProduct(product: Product) = 
        storeDao.deleteProduct(product)

    suspend fun updateProductStock(prodId: Int, newStock: Int) = 
        storeDao.updateProductStock(prodId, newStock)

    // Cart
    val cartItems: Flow<List<CartItem>> = storeDao.getCartItems()

    suspend fun addToCart(item: CartItem) = 
        storeDao.addToCart(item)

    suspend fun updateCartQuantity(cartId: Int, newQty: Int) = 
        storeDao.updateCartQuantity(cartId, newQty)

    suspend fun removeFromCart(cartId: Int) = 
        storeDao.removeFromCart(cartId)

    suspend fun clearCart() = 
        storeDao.clearCart()

    // Orders
    val allOrders: Flow<List<Order>> = storeDao.getAllOrders()

    fun getOrderFlow(orderId: Int): Flow<Order?> = 
        storeDao.getOrderFlow(orderId)

    suspend fun createOrder(order: Order, items: List<OrderItem>): Long {
        val orderId = storeDao.insertOrder(order)
        val itemsWithId = items.map { it.copy(orderId = orderId.toInt()) }
        storeDao.insertOrderItems(itemsWithId)
        return orderId
    }

    suspend fun updateOrderStatus(orderId: Int, newStatus: String) = 
        storeDao.updateOrderStatus(orderId, newStatus)

    suspend fun updateOrderStatusWithReason(orderId: Int, newStatus: String, reason: String) =
        storeDao.updateOrderStatusWithReason(orderId, newStatus, reason)

    suspend fun updateOrderPaymentDetails(orderId: Int, ref: String, payStatus: String) =
        storeDao.updateOrderPaymentDetails(orderId, ref, payStatus)

    suspend fun assignOrderToAgent(orderId: Int, agentName: String) = 
        storeDao.assignOrderToAgent(orderId, agentName)

    fun getOrderItems(orderId: Int): Flow<List<OrderItem>> = 
        storeDao.getOrderItems(orderId)

    suspend fun getAllOrderItems(): List<OrderItem> = 
        storeDao.getAllOrderItems()

    suspend fun insertOrderItems(items: List<OrderItem>) = 
        storeDao.insertOrderItems(items)

    // Notifications
    val allNotifications: Flow<List<Notification>> = storeDao.getAllNotifications()

    suspend fun insertNotification(notification: Notification) = 
        storeDao.insertNotification(notification)

    suspend fun markNotificationAsRead(id: Int) = 
        storeDao.markNotificationAsRead(id)

    // Audit Logs
    val allAuditLogs: Flow<List<AuditLog>> = storeDao.getAllAuditLogs()

    suspend fun insertAuditLog(log: AuditLog) = 
        storeDao.insertAuditLog(log)

    // Favorites
    val allFavorites: Flow<List<FavoriteItem>> = storeDao.getAllFavorites()

    suspend fun addFavorite(prodId: Int) = 
        storeDao.addFavorite(FavoriteItem(productId = prodId))

    suspend fun removeFavorite(prodId: Int) = 
        storeDao.removeFavorite(prodId)

    // Clear and restore helper methods
    suspend fun clearAllData() {
        storeDao.clearCategories()
        storeDao.clearProducts()
        storeDao.clearOrders()
        storeDao.clearOrderItems()
        storeDao.clearNotifications()
        storeDao.clearAuditLogs()
    }

    suspend fun insertAuditLogs(logs: List<AuditLog>) =
        storeDao.insertAuditLogs(logs)

    suspend fun insertNotifications(notifications: List<Notification>) =
        storeDao.insertNotifications(notifications)
}
