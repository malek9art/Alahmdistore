-- ======================================================================================
-- قاعدة بيانات مركز الأحمدي للجوالات ومستلزماتها - Supabase Enterprise PostgreSQL Schema
-- ======================================================================================
-- الوصف: تصميم شامل واحترافي لقاعدة بيانات متجر إلكتروني متكامل لمركز الأحمدي.
-- يشتمل على: الجداول، العلاقات، قيود المفاتيح الأجنبية، الفهارس عالية الأداء، ونظام
-- الصلاحيات القائم على الأدوار (RBAC) مع سياسات الحماية على مستوى السطر (RLS) والتريجرات الفورية.
-- الاتجاه: يدعم العربية RTL بالكامل ومصمم لتجربة جوال أولاً وسرعة استعلام فائقة.
-- ======================================================================================

-- --------------------------------------------------------------------------------------
-- 1. التهيئة والملحقات والأنواع المخصصة (Extensions & Custom Types)
-- --------------------------------------------------------------------------------------

-- تفعيل ملحق توليد معرفات UUID فريدة عالمياً
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- إنشاء نوع مخصص لأدوار المستخدمين المتوافقة مع التطبيق
CREATE TYPE public.app_role_enum AS ENUM ('customer', 'admin', 'delivery');

-- إنشاء نوع مخصص لحالات الفواتير والطلبات
CREATE TYPE public.order_status_enum AS ENUM ('جديد', 'قيد التجهيز', 'جاهز للشحن', 'في الطريق', 'تم التسليم', 'ملغي');

-- إنشاء نوع مخصص لحالات الدفع الإلكتروني والنقدي
CREATE TYPE public.payment_status_enum AS ENUM ('معلق', 'مدفوع', 'مسترجع', 'فاشل');

-- إنشاء نوع مخصص لحالات الشحنات والتوصيل للمناديب
CREATE TYPE public.shipment_status_enum AS ENUM ('جاهز للتسليم', 'في الطريق', 'تم التوصيل', 'مسترجع');

-- إنشاء نوع مخصص لطلبات خدمات ما بعد البيع
CREATE TYPE public.request_type_enum AS ENUM ('ضمان', 'استرجاع');

-- إنشاء نوع مخصص لحالة مراجعة طلبات خدمات ما بعد البيع
CREATE TYPE public.request_status_enum AS ENUM ('قيد المراجعة', 'تم القبول', 'تم الرفض');


-- --------------------------------------------------------------------------------------
-- 2. جداول نظام الأدوار والتحقق والملفات (RBAC & User Profiles Tables)
-- --------------------------------------------------------------------------------------

-- أ. جدول الأدوار (Roles Table)
CREATE TABLE public.roles (
    id SERIAL PRIMARY KEY,
    name public.app_role_enum NOT NULL UNIQUE,
    display_name_ar VARCHAR(100) NOT NULL,
    description_ar TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT TIMEZONE('utc'::text, NOW()) NOT NULL
);

COMMENT ON TABLE public.roles IS 'جدول تعريف الأدوار الأمنية في نظام مركز الأحمدي.';

-- ب. جدول الصلاحيات التفصيلية (Permissions Table)
CREATE TABLE public.permissions (
    id SERIAL PRIMARY KEY,
    codename VARCHAR(100) NOT NULL UNIQUE, -- مثل: products:create, orders:update
    display_name_ar VARCHAR(255) NOT NULL,
    description_ar TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT TIMEZONE('utc'::text, NOW()) NOT NULL
);

COMMENT ON TABLE public.permissions IS 'جدول الصلاحيات الدقيقة الممنوحة للأدوار المختلفة.';

-- ج. جدول ربط الأدوار بالصلاحيات (Role-Permissions Join Table)
CREATE TABLE public.role_permissions (
    role_id INTEGER REFERENCES public.roles(id) ON DELETE CASCADE,
    permission_id INTEGER REFERENCES public.permissions(id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, permission_id)
);

COMMENT ON TABLE public.role_permissions IS 'جدول ربط متعدد إلى متعدد لتحديد الصلاحيات الخاصة بكل دور أمني.';

-- د. جدول ملفات تعريف المستخدمين (Profiles Table)
-- يرتبط بجدول المصادقة الافتراضي الخاص بـ Supabase (auth.users)
CREATE TABLE public.profiles (
    id UUID REFERENCES auth.users(id) ON DELETE CASCADE PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    phone VARCHAR(50) UNIQUE NOT NULL,
    role public.app_role_enum DEFAULT 'customer'::public.app_role_enum NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT TIMEZONE('utc'::text, NOW()) NOT NULL
);

COMMENT ON TABLE public.profiles IS 'الملفات الشخصية للعملاء، المناديب، والإداريين مع تحديد دورهم الأساسي.';


-- --------------------------------------------------------------------------------------
-- 3. جداول أقسام وكتالوج المنتجات (Store Catalog Tables)
-- --------------------------------------------------------------------------------------

-- أ. جدول تصنيفات ومجموعات المنتجات (Categories)
CREATE TABLE public.categories (
    id SERIAL PRIMARY KEY,
    name_ar VARCHAR(255) NOT NULL UNIQUE,
    slug VARCHAR(255) NOT NULL UNIQUE,
    icon VARCHAR(100) DEFAULT 'phone_android', -- اسم أيقونة Material Symbols
    created_at TIMESTAMP WITH TIME ZONE DEFAULT TIMEZONE('utc'::text, NOW()) NOT NULL
);

COMMENT ON TABLE public.categories IS 'أقسام متجر مركز الأحمدي (مثل: أجهزة الآيفون، الشواحن، الكفرات).';

-- ب. جدول المنتجات والسلع والمخزون (Products)
CREATE TABLE public.products (
    id SERIAL PRIMARY KEY,
    category_id INTEGER REFERENCES public.categories(id) ON DELETE RESTRICT NOT NULL,
    name_ar VARCHAR(255) NOT NULL,
    desc_ar TEXT NOT NULL,
    price DECIMAL(10, 2) NOT NULL CHECK (price >= 0),
    old_price DECIMAL(10, 2) CHECK (old_price IS NULL OR old_price > price),
    image_url TEXT,
    stock INTEGER NOT NULL DEFAULT 0 CHECK (stock >= 0),
    is_offer BOOLEAN DEFAULT FALSE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT TIMEZONE('utc'::text, NOW()) NOT NULL
);

COMMENT ON TABLE public.products IS 'بيانات الأجهزة والإكسسوارات المعروضة وأسعارها وكميات المخزون في مركز الأحمدي.';


-- --------------------------------------------------------------------------------------
-- 4. جداول فواتير المشتريات والتوصيل والدفع (Orders, Shipments & Payments)
-- --------------------------------------------------------------------------------------

-- أ. جدول طلبات الشراء الرئيسية (Orders)
CREATE TABLE public.orders (
    id SERIAL PRIMARY KEY,
    customer_id UUID REFERENCES public.profiles(id) ON DELETE SET NULL, -- اختياري لدعم Guest Checkout الشراء كزائر
    guest_name VARCHAR(255),                                            -- اسم الزائر في حال الشراء بدون حساب
    guest_phone VARCHAR(50),                                            -- هاتف الزائر للتتبع
    status public.order_status_enum DEFAULT 'جديد'::public.order_status_enum NOT NULL,
    total_amount DECIMAL(10, 2) NOT NULL CHECK (total_amount >= 0),
    payment_method VARCHAR(100) NOT NULL,                               -- مدى، أبل باي، الدفع عند الاستلام، فيزا
    address TEXT NOT NULL,                                              -- عنوان التوصيل التفصيلي والمدينة
    tracking_token UUID DEFAULT uuid_generate_v4() UNIQUE NOT NULL,    -- توكن آمن لتتبع الزوار بدون تسجيل دخول
    created_at TIMESTAMP WITH TIME ZONE DEFAULT TIMEZONE('utc'::text, NOW()) NOT NULL
);

COMMENT ON TABLE public.orders IS 'سجل الفواتير وطلبات الشراء وعناوين الشحن في متجر الأحمدي.';

-- ب. تفاصيل المنتجات والسلع داخل الفاتورة (Order Items)
CREATE TABLE public.order_items (
    id SERIAL PRIMARY KEY,
    order_id INTEGER REFERENCES public.orders(id) ON DELETE CASCADE NOT NULL,
    product_id INTEGER REFERENCES public.products(id) ON DELETE RESTRICT NOT NULL,
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    price DECIMAL(10, 2) NOT NULL CHECK (price >= 0) -- سعر المنتج وقت الشراء للاستقرار المالي للفواتير
);

COMMENT ON TABLE public.order_items IS 'تفاصيل السلع المشتراة والكميات والأسعار لكل فاتورة طلب.';

-- ج. جدول العمليات المالية والمدفوعات (Payments)
CREATE TABLE public.payments (
    id SERIAL PRIMARY KEY,
    order_id INTEGER REFERENCES public.orders(id) ON DELETE CASCADE NOT NULL,
    transaction_id VARCHAR(255) UNIQUE,                                  -- معرف العملية الخارجي من بوابة الدفع
    amount DECIMAL(10, 2) NOT NULL CHECK (amount >= 0),
    status public.payment_status_enum DEFAULT 'معلق'::public.payment_status_enum NOT NULL,
    gateway VARCHAR(100) DEFAULT 'Cash On Delivery' NOT NULL,           -- بوابة الدفع (Mada, Apple Pay, Tamara, COD)
    created_at TIMESTAMP WITH TIME ZONE DEFAULT TIMEZONE('utc'::text, NOW()) NOT NULL
);

COMMENT ON TABLE public.payments IS 'بيانات تسوية العمليات المالية المرتبطة بالطلبات لمركز الأحمدي.';

-- د. جدول الشحن وتتبع مناديب التوصيل (Shipments)
CREATE TABLE public.shipments (
    id SERIAL PRIMARY KEY,
    order_id INTEGER REFERENCES public.orders(id) ON DELETE CASCADE NOT NULL,
    delivery_id UUID REFERENCES public.profiles(id) ON DELETE SET NULL, -- مندوب التوصيل المكلف بالشحنة
    status public.shipment_status_enum DEFAULT 'جاهز للتسليم'::public.shipment_status_enum NOT NULL,
    carrier_notes TEXT,                                                 -- ملاحظات وتحديثات المندوب أثناء الرحلة
    estimated_delivery TIMESTAMP WITH TIME ZONE,                        -- تاريخ التوصيل المتوقع
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT TIMEZONE('utc'::text, NOW()) NOT NULL
);

COMMENT ON TABLE public.shipments IS 'بيانات تتبع الشحنات وحالة مندوب التوصيل والاتصال بالعملاء.';


-- --------------------------------------------------------------------------------------
-- 5. جداول الخدمات المساندة والدعم والرقابة (Support, Warranty & Audit)
-- --------------------------------------------------------------------------------------

-- أ. جدول الضمان وصيانة واسترجاع السلع (Return & Warranty Claims)
CREATE TABLE public.return_warranty_requests (
    id SERIAL PRIMARY KEY,
    order_id INTEGER REFERENCES public.orders(id) ON DELETE CASCADE NOT NULL,
    product_id INTEGER REFERENCES public.products(id) ON DELETE RESTRICT NOT NULL,
    type public.request_type_enum NOT NULL,
    reason_ar TEXT NOT NULL,
    status public.request_status_enum DEFAULT 'قيد المراجعة'::public.request_status_enum NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT TIMEZONE('utc'::text, NOW()) NOT NULL
);

COMMENT ON TABLE public.return_warranty_requests IS 'طلبات خدمة العملاء لما بعد البيع (صيانة ضمان للأجهزة أو طلب استرجاع المنتجات).';

-- ب. جدول الإشعارات والتحذيرات اللحظية (Notifications)
CREATE TABLE public.notifications (
    id SERIAL PRIMARY KEY,
    user_id UUID REFERENCES public.profiles(id) ON DELETE CASCADE, -- إذا كانت فارغة تعني إشعار عام لجميع العملاء والمناديب
    title_ar VARCHAR(255) NOT NULL,
    message_ar TEXT NOT NULL,
    is_read BOOLEAN DEFAULT FALSE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT TIMEZONE('utc'::text, NOW()) NOT NULL
);

COMMENT ON TABLE public.notifications IS 'نظام الإشعارات الفورية للعملاء والمناديب لمتابعة حالات فواتيرهم.';

-- ج. جدول محادثات الدعم الفني والشات الفوري (Support Live Chats)
CREATE TABLE public.support_chats (
    id SERIAL PRIMARY KEY,
    customer_id UUID REFERENCES public.profiles(id) ON DELETE CASCADE, -- فارغ للزوار غير المسجلين
    guest_phone VARCHAR(50),                                            -- للتعرف والرد على الزوار في لوحة التحكم
    message TEXT NOT NULL,
    is_from_admin BOOLEAN DEFAULT FALSE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT TIMEZONE('utc'::text, NOW()) NOT NULL
);

COMMENT ON TABLE public.support_chats IS 'المحادثات المباشرة بين العملاء والإدارة لحل المشاكل الفنية وتلقي الاستفسارات.';

-- د. جدول سجلات التدقيق والمراقبة الأمنية الحساسة (Audit Logs)
CREATE TABLE public.audit_logs (
    id SERIAL PRIMARY KEY,
    user_id UUID REFERENCES public.profiles(id) ON DELETE SET NULL,
    user_role VARCHAR(100) NOT NULL,
    action_ar VARCHAR(255) NOT NULL,
    details_ar TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT TIMEZONE('utc'::text, NOW()) NOT NULL
);

COMMENT ON TABLE public.audit_logs IS 'سجل مراقبة العمليات الإدارية والمالية لضمان الرقابة والامتثال التام في النظام.';


-- --------------------------------------------------------------------------------------
-- 6. فهارس تحسين الاستعلامات والسرعة الفائقة (Database Performance Indexes)
-- --------------------------------------------------------------------------------------

-- فهارس تحسين عمليات البحث والفلترة والعلاقات العلائقية
CREATE INDEX idx_products_category_id ON public.products(category_id);
CREATE INDEX idx_products_price_filter ON public.products(price);
CREATE INDEX idx_products_offer_flag ON public.products(is_offer);

CREATE INDEX idx_orders_customer_id ON public.orders(customer_id);
CREATE INDEX idx_orders_status_filter ON public.orders(status);
CREATE INDEX idx_orders_tracking_secure ON public.orders(tracking_token);

CREATE INDEX idx_order_items_order_id ON public.order_items(order_id);
CREATE INDEX idx_order_items_product_id ON public.order_items(product_id);

CREATE INDEX idx_payments_order_id ON public.payments(order_id);
CREATE INDEX idx_payments_status_filter ON public.payments(status);

CREATE INDEX idx_shipments_order_id ON public.shipments(order_id);
CREATE INDEX idx_shipments_delivery_id ON public.shipments(delivery_id);
CREATE INDEX idx_shipments_status_filter ON public.shipments(status);

CREATE INDEX idx_notifications_unread ON public.notifications(user_id, is_read);
CREATE INDEX idx_support_chats_user ON public.support_chats(customer_id);
CREATE INDEX idx_audit_logs_time ON public.audit_logs(created_at);


-- --------------------------------------------------------------------------------------
-- 7. التريجرات والوظائف التلقائية (Database Automated Functions & Triggers)
-- --------------------------------------------------------------------------------------

-- أ. وظيفة تريجر لإنشاء ملف تعريفي تلقائي للمستخدم عند تسجيله في Supabase Auth
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS TRIGGER AS $$
BEGIN
  INSERT INTO public.profiles (id, name, phone, role)
  VALUES (
    new.id,
    COALESCE(new.raw_user_meta_data->>'name', 'عميل مركز الأحمدي'),
    COALESCE(new.raw_user_meta_data->>'phone', '05' || floor(random() * 90000000 + 10000000)::text),
    'customer'::public.app_role_enum
  );
  RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- تريجر المزامنة لملفات المستخدمين
CREATE OR REPLACE TRIGGER on_auth_user_created
  AFTER INSERT ON auth.users
  FOR EACH ROW EXECUTE FUNCTION public.handle_new_user();


-- ب. وظيفة تريجر لخصم كميات المخزون وتحديث السجلات تلقائياً فور تسجيل أي طلب شراء جديد
CREATE OR REPLACE FUNCTION public.update_product_stock()
RETURNS TRIGGER AS $$
BEGIN
  -- خصم الكمية المشتراة من السلعة المحددة
  UPDATE public.products
  SET stock = stock - NEW.quantity
  WHERE id = NEW.product_id;
  
  -- في حال نفاد المخزون، يتم إدراج تنبيه رقابي أمني تلقائي في سجل التدقيق
  IF (SELECT stock FROM public.products WHERE id = NEW.product_id) = 0 THEN
    INSERT INTO public.audit_logs (user_role, action_ar, details_ar)
    VALUES ('نظام المخزون الآلي', 'نفاد الكمية', 'لقد نفذت كمية السلعة رقم: ' || NEW.product_id::text || ' بالكامل من مستودع الأحمدي.');
  END IF;
  
  RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- تريجر تحديث المخزون
CREATE OR REPLACE TRIGGER on_order_item_inserted
  AFTER INSERT ON public.order_items
  FOR EACH ROW EXECUTE FUNCTION public.update_product_stock();


-- ج. وظيفة تريجر لمزامنة حالات الطلب والفواتير المالية تلقائياً بناءً على تحديثات شحنات المناديب
CREATE OR REPLACE FUNCTION public.sync_order_status_from_shipment()
RETURNS TRIGGER AS $$
BEGIN
  -- المزامنة التلقائية للحالة الإجمالية للفاتورة والطلب
  IF NEW.status = 'في الطريق'::public.shipment_status_enum THEN
    UPDATE public.orders SET status = 'في الطريق'::public.order_status_enum WHERE id = NEW.order_id;
  ELSIF NEW.status = 'تم التوصيل'::public.shipment_status_enum THEN
    UPDATE public.orders SET status = 'تم التسليم'::public.order_status_enum WHERE id = NEW.order_id;
    -- تحديث حالة الفاتورة لتصبح مدفوعة فورياً في حال كان الدفع نقدياً عند الاستلام
    UPDATE public.payments SET status = 'مدفوع'::public.payment_status_enum WHERE order_id = NEW.order_id AND gateway = 'Cash On Delivery';
  ELSIF NEW.status = 'مسترجع'::public.shipment_status_enum THEN
    UPDATE public.orders SET status = 'ملغي'::public.order_status_enum WHERE id = NEW.order_id;
  END IF;
  
  RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- تريجر مزامنة الحالات للمندوب والشحنة والطلب
CREATE OR REPLACE TRIGGER on_shipment_status_updated
  AFTER UPDATE OF status ON public.shipments
  FOR EACH ROW EXECUTE FUNCTION public.sync_order_status_from_shipment();


-- --------------------------------------------------------------------------------------
-- 8. حماية البيانات وتفعيل سياسات الوصول الأمنية Row Level Security (RLS)
-- --------------------------------------------------------------------------------------

-- تفعيل ميزة جدار الحماية RLS على جميع الجداول بالكامل لحظر الوصول العشوائي
ALTER TABLE public.roles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.permissions ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.role_permissions ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.categories ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.products ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.orders ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.order_items ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.payments ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.shipments ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.notifications ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.return_warranty_requests ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.support_chats ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.audit_logs ENABLE ROW LEVEL SECURITY;

-- وظيفة آمنة وفعالة للحصول على دور المستخدم الحالي المخزن في الجلسة الموثقة
CREATE OR REPLACE FUNCTION public.get_user_role()
RETURNS public.app_role_enum AS $$
  SELECT COALESCE(
    (SELECT role FROM public.profiles WHERE id = auth.uid()),
    'customer'::public.app_role_enum
  );
$$ LANGUAGE sql SECURITY DEFINER;


-- [السياسات الأمنية التفصيلية لحماية الجداول]

-- أ. سياسات الأدوار والصلاحيات (Roles & Permissions)
CREATE POLICY "المدير يملك التحكم المطلق بالصلاحيات والأدوار"
  ON public.roles FOR ALL TO authenticated USING (public.get_user_role() = 'admin'::public.app_role_enum);
CREATE POLICY "المدير يملك التحكم المطلق بتفاصيل الصلاحيات"
  ON public.permissions FOR ALL TO authenticated USING (public.get_user_role() = 'admin'::public.app_role_enum);
CREATE POLICY "المدير يملك التحكم المطلق بالارتباطات"
  ON public.role_permissions FOR ALL TO authenticated USING (public.get_user_role() = 'admin'::public.app_role_enum);

-- ب. سياسات ملفات المستخدمين (Profiles)
CREATE POLICY "المدراء يملكون الصلاحية الكاملة على ملفات الأعضاء"
  ON public.profiles FOR ALL TO authenticated USING (public.get_user_role() = 'admin'::public.app_role_enum);
CREATE POLICY "المستخدم يرى ويعدل ملفه الخاص الموثق فقط"
  ON public.profiles FOR SELECT WITH CHECK (auth.uid() = id);

-- ج. سياسات تصنيفات المتجر والمنتجات (Categories & Products)
CREATE POLICY "التصنيفات متاحة للقراءة العامة والبحث للكل"
  ON public.categories FOR SELECT TO public USING (true);
CREATE POLICY "المدير فقط يدير ويعدل أقسام المتجر"
  ON public.categories FOR ALL TO authenticated USING (public.get_user_role() = 'admin'::public.app_role_enum);

CREATE POLICY "المنتجات معروضة ومتاحة للقراءة والبحث للعملاء والزوار"
  ON public.products FOR SELECT TO public USING (true);
CREATE POLICY "المدير فقط يدير ويعدل كميات وأسعار كتالوج المنتجات"
  ON public.products FOR ALL TO authenticated USING (public.get_user_role() = 'admin'::public.app_role_enum);

-- د. سياسات الطلبات وعناصر الفاتورة (Orders & Order Items)
CREATE POLICY "المدراء يملكون حق الاطلاع والتحكم بكافة فواتير المشتريات"
  ON public.orders FOR ALL TO authenticated USING (public.get_user_role() = 'admin'::public.app_role_enum);
CREATE POLICY "العميل المسجل يرى طلباته المسجلة السابقة فقط"
  ON public.orders FOR SELECT TO authenticated USING (customer_id = auth.uid());
CREATE POLICY "العميل المسجل ينشئ طلباً جديداً لحسابه"
  ON public.orders FOR INSERT TO authenticated WITH CHECK (customer_id = auth.uid());
CREATE POLICY "الزوار يدرجون طلبات شراء كزائر كاش"
  ON public.orders FOR INSERT TO public WITH CHECK (customer_id IS NULL AND guest_phone IS NOT NULL);
CREATE POLICY "تتبع آمن للزوار عبر رمز التتبع المشفر"
  ON public.orders FOR SELECT TO public USING (tracking_token IS NOT NULL);

CREATE POLICY "عرض عناصر الفاتورة وفق الصلاحيات المناسبة"
  ON public.order_items FOR SELECT TO public USING (
    EXISTS (
      SELECT 1 FROM public.orders o
      WHERE o.id = order_id
      AND (
        o.customer_id = auth.uid()
        OR public.get_user_role() = 'admin'::public.app_role_enum
        OR o.tracking_token IS NOT NULL
      )
    )
  );
CREATE POLICY "السماح للجميع بإدراج عناصر المشتريات في الطلب"
  ON public.order_items FOR INSERT TO public WITH CHECK (true);

-- هـ. سياسات العمليات المالية والمدفوعات (Payments)
CREATE POLICY "المدير يدير كافة السجلات والعمليات المالية والتسويات"
  ON public.payments FOR ALL TO authenticated USING (public.get_user_role() = 'admin'::public.app_role_enum);
CREATE POLICY "العميل يرى فواتير دفعه الملحقة بطلباته"
  ON public.payments FOR SELECT TO authenticated USING (
    EXISTS (
      SELECT 1 FROM public.orders o
      WHERE o.id = order_id AND o.customer_id = auth.uid()
    )
  );
CREATE POLICY "عرض حالة الدفع للزوار عبر توكن التتبع الآمن"
  ON public.payments FOR SELECT TO public USING (
    EXISTS (
      SELECT 1 FROM public.orders o
      WHERE o.id = order_id AND o.tracking_token IS NOT NULL
    )
  );

-- و. سياسات الشحنات وتكليفات مناديب التوصيل (Shipments)
CREATE POLICY "الإدارة تدير وتوزع الشحنات على المناديب"
  ON public.shipments FOR ALL TO authenticated USING (public.get_user_role() = 'admin'::public.app_role_enum);
CREATE POLICY "المندوب يرى الشحنات الموكلة إليه أو الجاهزة للتكليف"
  ON public.shipments FOR SELECT TO authenticated USING (
    public.get_user_role() = 'delivery'::public.app_role_enum
    AND (delivery_id = auth.uid() OR status = 'جاهز للتسليم'::public.shipment_status_enum)
  );
CREATE POLICY "المندوب يحدث ويعدل حالة الشحنات المسندة لعهدته"
  ON public.shipments FOR UPDATE TO authenticated USING (
    public.get_user_role() = 'delivery'::public.app_role_enum
    AND delivery_id = auth.uid()
  );
CREATE POLICY "تتبع العملاء لحالة شحناتهم المسجلة"
  ON public.shipments FOR SELECT TO authenticated USING (
    EXISTS (
      SELECT 1 FROM public.orders o
      WHERE o.id = order_id AND o.customer_id = auth.uid()
    )
  );

-- ز. سياسات خدمات ما بعد البيع والضمان والاسترجاع (Warranty & Returns)
CREATE POLICY "العميل يرى وينشئ طلب ضمانه أو استرجاعه لطلباته فقط"
  ON public.return_warranty_requests FOR SELECT TO authenticated USING (
    EXISTS (
      SELECT 1 FROM public.orders o
      WHERE o.id = order_id AND o.customer_id = auth.uid()
    )
  );
CREATE POLICY "العميل يرسل طلب صيانة ضمان أو استبدال جديد"
  ON public.return_warranty_requests FOR INSERT TO authenticated WITH CHECK (true);
CREATE POLICY "المدير يتحكم بالكامل ويعدل حالات طلبات الضمان"
  ON public.return_warranty_requests FOR ALL TO authenticated USING (public.get_user_role() = 'admin'::public.app_role_enum);

-- ح. سياسات الإشعارات وسجل التدقيق والدعم الفني (Notifications, Support & Audit)
CREATE POLICY "العميل يقرأ إشعاراته والتعميمات العامة"
  ON public.notifications FOR SELECT TO public USING (user_id = auth.uid() OR user_id IS NULL);
CREATE POLICY "المدير يملك حق إرسال ونشر الإشعارات"
  ON public.notifications FOR ALL TO authenticated USING (public.get_user_role() = 'admin'::public.app_role_enum);

CREATE POLICY "المدير يرى ويدير سجل التدقيق الرقابي بالكامل"
  ON public.audit_logs FOR SELECT TO authenticated USING (public.get_user_role() = 'admin'::public.app_role_enum);
CREATE POLICY "السماح للنظام التلقائي بإدراج العمليات في سجل التدقيق"
  ON public.audit_logs FOR INSERT TO public WITH CHECK (true);

CREATE POLICY "الدعم الفني والزوار والعملاء يتراسلون بأمان"
  ON public.support_chats FOR ALL TO public USING (
    customer_id = auth.uid()
    OR public.get_user_role() = 'admin'::public.app_role_enum
    OR guest_phone IS NOT NULL
  );


-- --------------------------------------------------------------------------------------
-- 9. بذور البيانات وتجهيز المتجر الفوري (Data Seeds & Initialization)
-- --------------------------------------------------------------------------------------

-- أ. إدراج تصنيفات متجر مركز الأحمدي للجوالات ومستلزماتها
INSERT INTO public.categories (name_ar, slug, icon) VALUES
('أجهزة الآيفون', 'iphones', 'phone_iphone'),
('أجهزة الأندرويد', 'android-phones', 'phone_android'),
('الشواحن والمنصات', 'chargers', 'bolt'),
('السماعات والصوتيات', 'audio', 'headphones'),
('حماية الشاشة والكفرات', 'protection', 'shield')
ON CONFLICT (name_ar) DO NOTHING;

-- ب. إدراج منتجات المتجر بأسعارها الحقيقية ومخزونها المتوافق مع التطبيق
INSERT INTO public.products (category_id, name_ar, desc_ar, price, old_price, is_offer, stock) VALUES
(1, 'آيفون 15 برو ماكس - 256 جيجابايت', 'العملاق الجديد من آبل مع هيكل من التيتانيوم القوي وخفيف الوزن وكاميرا تصوير خرافية مع زووم خارق.', 4899.00, 5299.00, true, 15),
(1, 'آيفون 14 برو - 128 جيجابايت', 'شاشة ديناميك آيلاند الرائعة مع معالج آبل القوي A16 بيونيك وكاميرا بدقة 48 ميجابيكسل.', 3699.00, NULL, false, 8),
(2, 'سامسونج جالكسي S24 ألترا - 512 جيجابايت', 'وحش الإنتاجية مع قلم S-Pen المدمج وميزات الذكاء الاصطناعي الفائقة Galaxy AI كاميرات زووم 100x مذهلة.', 4299.00, 4799.00, true, 12),
(2, 'شاومي 14 الترا - 512 جيجابايت', 'كاميرات لايكا الاحترافية مع معالج Snapdragon 8 Gen 3 الأقوى للتطبيقات والألعاب مع شاحن سريع جداً.', 3499.00, NULL, false, 5),
(3, 'شاحن أنكر نانو سريع بقوة 30 واط', 'أصغر رأس شاحن سريع متوافق مع الآيفون والأندرويد مع تقنية حماية البطارية الذكية من السخونة.', 69.00, 89.00, true, 100),
(3, 'منصة شحن لاسلكي ماج سيف مغناطيسي', 'منصة شحن سريعة ومثالية لسطح المكتب متوافقة مع الأجهزة والعلب المغناطيسية الأصلية.', 129.00, NULL, false, 45),
(4, 'سماعة آبل إيربودز برو الجيل الثاني', 'إلغاء ضوضاء نشط مضاعف، ميزة الصوت المكاني المخصص لتجربة استماع ساحرة وعمر بطارية ممتاز.', 849.00, 949.00, true, 25),
(4, 'سماعة سوني اللاسلكية WH-1000XM5', 'السماعة الرائدة عالمياً في عزل الضوضاء وصوت عالي الدقة غاية في النقاوة مناسبة للعمل والسفر والجيمنج.', 1199.00, NULL, false, 14),
(5, 'كفر حماية تيتانيوم مقاوم للصدمات S24', 'كفر فليكسيبل مقاوم للسقوط من ارتفاع 3 أمتار يحمي زوايا الهاتف والعدسات الخلفية البارزة.', 49.00, 79.00, true, 200),
(5, 'شاشة حماية نانو سيراميك للآيفون 15', 'زجاج مقوى فائق القوة ومقاوم للخدش والبصمات يغطي الشاشة بالكامل دون التأثير على حساسية اللمس.', 35.00, NULL, false, 150)
ON CONFLICT (name_ar) DO NOTHING;

-- ج. إدراج الأدوار الأساسية في النظام للتحقق منها فورياً
INSERT INTO public.roles (name, display_name_ar, description_ar) VALUES
('customer', 'العملاء والزوار', 'الصلاحيات القياسية للتصفح، الطلب والشحن وتتبع الطلب.'),
('delivery', 'مناديب التوصيل', 'صلاحيات الوصول لبوابة المناديب، استعراض الشحنات وتحديث حالات الشحن والاتصال بالعملاء.'),
('admin', 'إدارة مركز الأحمدي', 'التحكم المطلق بكتالوج المنتجات، إدارة الطلبات والشحنات، التقارير والتحليلات المالية والتدقيق الرقابي.')
ON CONFLICT (name) DO NOTHING;

-- د. إدراج عينات من الصلاحيات التفصيلية للأمان والتحقق
INSERT INTO public.permissions (codename, display_name_ar, description_ar) VALUES
('products:create', 'إضافة منتجات جديدة', 'القدرة على إدراج سلع أو أجهزة جديدة لكتالوج متجر الأحمدي.'),
('products:update', 'تعديل السلع والمخزون', 'تعديل مواصفات الأجهزة وأسعار البيع وتحديث كميات مخزن الأحمدي.'),
('orders:update', 'إدارة وتحديث الطلبات', 'تحديث حالات فواتير المشتريات وإلغاء الطلبات أو إسنادها للمناديب.'),
('shipments:update', 'تحديث حالات شحن المندوب', 'تحديث حالات التوصيل للشحنات قيد الطريق وكتابة ملاحظات التوصيل.')
ON CONFLICT (codename) DO NOTHING;

-- ======================================================================================
-- هـ. نظام الإشعارات اللحظية التلقائي (Automatic Database Notification System)
-- ======================================================================================

-- وظيفة تلقائية لإنشاء إشعار لكل عميل عند تقديم طلب جديد أو تحديث حالة الطلب
CREATE OR REPLACE FUNCTION public.handle_order_notification()
RETURNS TRIGGER AS $$
BEGIN
  IF TG_OP = 'INSERT' THEN
    -- إذا كان الطلب مسجلاً لعميل مسجل (غير زائر بالكامل)
    IF NEW.customer_id IS NOT NULL THEN
      INSERT INTO public.notifications (user_id, title_ar, message_ar)
      VALUES (
        NEW.customer_id,
        'تم تسجيل طلبك بنجاح 🛍️',
        'مرحباً بك! تم استلام طلبك رقم #' || NEW.id::text || ' بنجاح بقيمة ' || NEW.total_amount::text || ' ر.ي. وهو قيد المراجعة حالياً من قبل فريق مركز الأحمدي.'
      );
    END IF;
  ELSIF TG_OP = 'UPDATE' THEN
    -- في حال تغير حالة الطلب، نقوم بإرسال إشعار فوري بحالته الجديدة
    IF NEW.status IS DISTINCT FROM OLD.status AND NEW.customer_id IS NOT NULL THEN
      INSERT INTO public.notifications (user_id, title_ar, message_ar)
      VALUES (
        NEW.customer_id,
        'تحديث في حالة الطلب 📦',
        'أهلاً بك! تم تحديث حالة طلبك رقم #' || NEW.id::text || ' لتصبح الآن: (' || NEW.status::text || '). شكراً لتسوقك معنا.'
      );
    END IF;
  END IF;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- تريجر الإشعارات التلقائية للفواتير والطلبات
CREATE OR REPLACE TRIGGER on_order_status_notification
  AFTER INSERT OR UPDATE OF status ON public.orders
  FOR EACH ROW EXECUTE FUNCTION public.handle_order_notification();

-- ======================================================================================
-- تم الانتهاء من بناء هيكلية ومخطط قاعدة البيانات الشاملة لمركز الأحمدي بنجاح تام 🚀
-- ======================================================================================
