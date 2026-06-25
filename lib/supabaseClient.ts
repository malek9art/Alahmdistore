import { createClient } from '@supabase/supabase-js';

// ======================================================================================
// 1. استخراج وتأكيد بيئة عمل Supabase (Supabase Configuration)
// ======================================================================================

const supabaseUrl = process.env.SUPABASE_URL || 'https://your-project.supabase.co';
const supabaseAnonKey = process.env.SUPABASE_ANON_KEY || 'your_supabase_anon_key';

export const supabase = createClient(supabaseUrl, supabaseAnonKey);

// ======================================================================================
// 2. واجهات البيانات التعريفية (TypeScript Database Schemas)
// ======================================================================================

export type AppRole = 'customer' | 'admin' | 'delivery';
export type OrderStatus = 'جديد' | 'قيد التجهيز' | 'جاهز للشحن' | 'في الطريق' | 'تم التسليم' | 'ملغي';
export type PaymentStatus = 'معلق' | 'مدفوع' | 'مسترجع' | 'فاشل';
export type ShipmentStatus = 'جاهز للتسليم' | 'في الطريق' | 'تم التوصيل' | 'مسترجع';
export type RequestType = 'ضمان' | 'استرجاع';
export type RequestStatus = 'قيد المراجعة' | 'تم القبول' | 'تم الرفض';

export interface Profile {
  id: string; // UUID references auth.users.id
  name: string;
  phone: string;
  role: AppRole;
  created_at?: string;
}

export interface Category {
  id?: number;
  name_ar: string;
  slug: string;
  icon?: string;
  created_at?: string;
}

export interface Product {
  id?: number;
  category_id: number;
  name_ar: string;
  desc_ar: string;
  price: number;
  old_price?: number | null;
  image_url?: string | null;
  stock: number;
  is_offer: boolean;
  created_at?: string;
}

export interface Order {
  id?: number;
  customer_id?: string | null;
  guest_name?: string | null;
  guest_phone?: string | null;
  status: OrderStatus;
  total_amount: number;
  payment_method: string;
  address: string;
  tracking_token?: string;
  created_at?: string;
}

export interface OrderItem {
  id?: number;
  order_id: number;
  product_id: number;
  quantity: number;
  price: number;
}

export interface Payment {
  id?: number;
  order_id: number;
  transaction_id?: string | null;
  amount: number;
  status: PaymentStatus;
  gateway: string;
  created_at?: string;
}

export interface Shipment {
  id?: number;
  order_id: number;
  delivery_id?: string | null;
  status: ShipmentStatus;
  carrier_notes?: string | null;
  estimated_delivery?: string | null;
  updated_at?: string;
}

export interface ReturnWarrantyRequest {
  id?: number;
  order_id: number;
  product_id: number;
  type: RequestType;
  reason_ar: string;
  status: RequestStatus;
  created_at?: string;
}

export interface Notification {
  id?: number;
  user_id?: string | null;
  title_ar: string;
  message_ar: string;
  is_read: boolean;
  created_at?: string;
}

export interface SupportChat {
  id?: number;
  customer_id?: string | null;
  guest_phone?: string | null;
  message: string;
  is_from_admin: boolean;
  created_at?: string;
}

export interface AuditLog {
  id?: number;
  user_id?: string | null;
  user_role: string;
  action_ar: string;
  details_ar: string;
  created_at?: string;
}

// ======================================================================================
// 3. دوال المصادقة والأدوار (Authentication & Role Operations)
// ======================================================================================

export const authHelpers = {
  /**
   * تسجيل حساب عميل جديد مع إنشاء ملفه التعريفي فوراً في جدول profiles
   */
  async signUpUser(email: string, pass: string, phone: string, name: string, role: AppRole = 'customer') {
    try {
      // 1. تسجيل الحساب عبر مصادقة Supabase
      const { data: authData, error: authError } = await supabase.auth.signUp({
        email,
        password: pass,
        options: {
          data: {
            name,
            phone,
            role
          }
        }
      });

      if (authError) throw authError;
      if (!authData.user) throw new Error('فشلت عملية إنشاء الحساب، يرجى التحقق من المدخلات.');

      // 2. إدخال ملف التعريف الشخصي في جدول profiles لتحديد الدور
      const { data: profileData, error: profileError } = await supabase
        .from('profiles')
        .insert({
          id: authData.user.id,
          name,
          phone,
          role
        })
        .select()
        .single();

      if (profileError) {
        console.error('فشل حفظ ملف التعريف، ولكن تم إنشاء حساب المصادقة:', profileError);
      }

      return { user: authData.user, profile: profileData, error: null };
    } catch (err: any) {
      return { user: null, profile: null, error: err.message || err };
    }
  },

  /**
   * تسجيل الدخول بواسطة البريد الإلكتروني وكلمة المرور
   */
  async signInUser(email: string, pass: string) {
    try {
      const { data, error } = await supabase.auth.signInWithPassword({
        email,
        password: pass,
      });

      if (error) throw error;

      // جلب ملف المستخدم لمعرفة الدور
      const { data: profile } = await supabase
        .from('profiles')
        .select('*')
        .eq('id', data.user?.id)
        .single();

      return { session: data.session, user: data.user, profile, error: null };
    } catch (err: any) {
      return { session: null, user: null, profile: null, error: err.message || err };
    }
  },

  /**
   * تسجيل الخروج وإلغاء الجلسة الحالية
   */
  async signOutUser() {
    const { error } = await supabase.auth.signOut();
    return { success: !error, error };
  },

  /**
   * الحصول على بيانات الجلسة الحالية والملف التعريفي النشط
   */
  async getCurrentUser() {
    try {
      const { data: { user }, error: userError } = await supabase.auth.getUser();
      if (userError || !user) return { user: null, profile: null };

      const { data: profile } = await supabase
        .from('profiles')
        .select('*')
        .eq('id', user.id)
        .single();

      return { user, profile };
    } catch {
      return { user: null, profile: null };
    }
  }
};

// ======================================================================================
// 4. العمليات على قاعدة البيانات (PostgreSQL Database CRUD Operations)
// ======================================================================================

// ----------------------
// أ. الأقسام والكتالوج (Categories)
// ----------------------
export const categoryHelpers = {
  async getCategories() {
    const { data, error } = await supabase
      .from('categories')
      .select('*')
      .order('id', { ascending: true });
    return { data: data as Category[] | null, error };
  },

  async createCategory(category: Category) {
    const { data, error } = await supabase
      .from('categories')
      .insert(category)
      .select()
      .single();
    return { data: data as Category | null, error };
  }
};

// ----------------------
// ب. المنتجات وإدارة المخزون (Products)
// ----------------------
export const productHelpers = {
  async getProducts() {
    const { data, error } = await supabase
      .from('products')
      .select('*, categories(name_ar)')
      .order('created_at', { ascending: false });
    return { data, error };
  },

  async getProductById(id: number) {
    const { data, error } = await supabase
      .from('products')
      .select('*')
      .eq('id', id)
      .single();
    return { data: data as Product | null, error };
  },

  async createProduct(product: Product) {
    const { data, error } = await supabase
      .from('products')
      .insert(product)
      .select()
      .single();
    return { data: data as Product | null, error };
  },

  async updateProduct(id: number, fields: Partial<Product>) {
    const { data, error } = await supabase
      .from('products')
      .update(fields)
      .eq('id', id)
      .select()
      .single();
    return { data: data as Product | null, error };
  },

  async deleteProduct(id: number) {
    const { error } = await supabase
      .from('products')
      .delete()
      .eq('id', id);
    return { success: !error, error };
  }
};

// ----------------------
// ج. الطلبات والفواتير (Orders & Order Items)
// ----------------------
export const orderHelpers = {
  async getOrders() {
    const { data, error } = await supabase
      .from('orders')
      .select('*, profiles(name, phone)')
      .order('created_at', { ascending: false });
    return { data, error };
  },

  async getOrderById(id: number) {
    const { data: order, error: orderError } = await supabase
      .from('orders')
      .select('*')
      .eq('id', id)
      .single();

    if (orderError) return { order: null, items: null, error: orderError };

    const { data: items, error: itemsError } = await supabase
      .from('order_items')
      .select('*, products(name_ar, image_url)')
      .eq('order_id', id);

    return { order: order as Order, items, error: itemsError };
  },

  /**
   * إنشاء طلب جديد بشكل آمن ومتكامل (Atomic Place Order)
   */
  async createOrder(order: Order, items: Omit<OrderItem, 'order_id'>[]) {
    try {
      // 1. إدخال السجل الرئيسي للفاتورة
      const { data: newOrder, error: orderError } = await supabase
        .from('orders')
        .insert(order)
        .select()
        .single();

      if (orderError) throw orderError;
      const orderId = newOrder.id;

      // 2. إعداد مصفوفة عناصر الطلب وربطها بمعرف الفاتورة
      const orderItemsToInsert = items.map(item => ({
        ...item,
        order_id: orderId
      }));

      // 3. إدخال عناصر الطلب دفعة واحدة
      const { error: itemsError } = await supabase
        .from('order_items')
        .insert(orderItemsToInsert);

      if (itemsError) {
        // تنظيف الطلب الرئيسي في حال الفشل للحفاظ على سلامة البيانات
        await supabase.from('orders').delete().eq('id', orderId);
        throw itemsError;
      }

      return { order: newOrder as Order, error: null };
    } catch (err: any) {
      return { order: null, error: err.message || err };
    }
  },

  async updateOrderStatus(id: number, status: OrderStatus) {
    const { data, error } = await supabase
      .from('orders')
      .update({ status })
      .eq('id', id)
      .select()
      .single();
    return { data: data as Order | null, error };
  }
};

// ----------------------
// د. عمليات الدفع (Payments)
// ----------------------
export const paymentHelpers = {
  async getPayments() {
    const { data, error } = await supabase
      .from('payments')
      .select('*, orders(total_amount, status)')
      .order('created_at', { ascending: false });
    return { data, error };
  },

  async createPayment(payment: Payment) {
    const { data, error } = await supabase
      .from('payments')
      .insert(payment)
      .select()
      .single();
    return { data: data as Payment | null, error };
  },

  async updatePaymentStatus(id: number, status: PaymentStatus) {
    const { data, error } = await supabase
      .from('payments')
      .update({ status })
      .eq('id', id)
      .select()
      .single();
    return { data: data as Payment | null, error };
  }
};

// ----------------------
// هـ. الشحنات وتوصيل المناديب (Shipments)
// ----------------------
export const shipmentHelpers = {
  async getShipments() {
    const { data, error } = await supabase
      .from('shipments')
      .select('*, orders(*)')
      .order('updated_at', { ascending: false });
    return { data, error };
  },

  async getShipmentsByDeliveryAgent(deliveryId: string) {
    const { data, error } = await supabase
      .from('shipments')
      .select('*, orders(*)')
      .eq('delivery_id', deliveryId)
      .order('updated_at', { ascending: false });
    return { data, error };
  },

  async createShipment(shipment: Shipment) {
    const { data, error } = await supabase
      .from('shipments')
      .insert(shipment)
      .select()
      .single();
    return { data: data as Shipment | null, error };
  },

  async updateShipmentStatus(id: number, status: ShipmentStatus, carrierNotes?: string) {
    const updatePayload: Partial<Shipment> = { status };
    if (carrierNotes !== undefined) {
      updatePayload.carrier_notes = carrierNotes;
    }
    const { data, error } = await supabase
      .from('shipments')
      .update(updatePayload)
      .eq('id', id)
      .select()
      .single();
    return { data: data as Shipment | null, error };
  }
};

// ----------------------
// و. محادثات الدعم الفني (Support Live Chats)
// ----------------------
export const supportHelpers = {
  async getSupportChats() {
    const { data, error } = await supabase
      .from('support_chats')
      .select('*')
      .order('created_at', { ascending: true });
    return { data: data as SupportChat[] | null, error };
  },

  async sendSupportMessage(msg: Omit<SupportChat, 'id' | 'created_at'>) {
    const { data, error } = await supabase
      .from('support_chats')
      .insert(msg)
      .select()
      .single();
    return { data: data as SupportChat | null, error };
  }
};

// ----------------------
// ز. الإشعارات (Notifications)
// ----------------------
export const notificationHelpers = {
  async getNotifications(userId?: string) {
    let query = supabase.from('notifications').select('*');
    if (userId) {
      // جلب الإشعارات العامة والإشعارات الخاصة بالمستخدم معاً
      query = query.or(`user_id.is.null,user_id.eq.${userId}`);
    } else {
      query = query.is('user_id', null);
    }
    const { data, error } = await query.order('created_at', { ascending: false });
    return { data: data as Notification[] | null, error };
  },

  async createNotification(notification: Notification) {
    const { data, error } = await supabase
      .from('notifications')
      .insert(notification)
      .select()
      .single();
    return { data: data as Notification | null, error };
  },

  async markNotificationAsRead(id: number) {
    const { data, error } = await supabase
      .from('notifications')
      .update({ is_read: true })
      .eq('id', id)
      .select()
      .single();
    return { data: data as Notification | null, error };
  }
};

// ----------------------
// ح. سجلات الرقابة والتدقيق (Audit Logs)
// ----------------------
export const auditLogHelpers = {
  async getAuditLogs() {
    const { data, error } = await supabase
      .from('audit_logs')
      .select('*')
      .order('created_at', { ascending: false });
    return { data: data as AuditLog[] | null, error };
  },

  async createAuditLog(log: AuditLog) {
    const { data, error } = await supabase
      .from('audit_logs')
      .insert(log)
      .select()
      .single();
    return { data: data as AuditLog | null, error };
  }
};
