// تهيئة اتصال Supabase باستخدام مفاتيح البيئة الموفرة
const { createClient } = require('@supabase/supabase-js');

// تحميل متغيرات البيئة من ملف .env إذا كان متوفراً (مفيد للتشغيل المحلي)
try {
  require('dotenv').config();
} catch (e) {
  // تجاهل إذا لم تكن مكتبة dotenv مثبتة
}

const supabaseUrl = process.env.SUPABASE_URL || 'https://your-project.supabase.co';
const supabaseAnonKey = process.env.SUPABASE_ANON_KEY || 'your_supabase_anon_key';

if (!process.env.SUPABASE_URL || !process.env.SUPABASE_ANON_KEY) {
  console.warn('تنبيه: لم يتم العثور على مفاتيح Supabase في متغيرات البيئة. سيتم استخدام القيم الافتراضية.');
}

const supabase = createClient(supabaseUrl, supabaseAnonKey);

module.exports = {
  supabase
};
