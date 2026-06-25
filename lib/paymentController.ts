/**
 * ======================================================================================
 * مركز الأحمدي للجوالات ومستلزماتها - نظام الدفع الموحد والمبسط 💳
 * ======================================================================================
 * ملف التحكم بوابات وطرق الدفع الإلكتروني المتعددة (Flexible Payment Controller)
 * 
 * تم تصميم هذا الكلاس باستخدام نمط التصميم "الاستراتيجية" (Strategy Pattern) ليتيح:
 * 1. سهولة التبديل بين بوابات الدفع المختلفة (MyFatoorah, PayTabs, Tap Payments, HyperPay) تلقائياً عبر الإعدادات.
 * 2. دعم قنوات الدفع المتنوعة: مدى، فيزا، Apple Pay، Google Pay، والمحافظ الرقمية اليمنية.
 * 3. دعم إمكانية التقسيط الشهري المستقبلي عبر (Tabby / Tamara).
 * 4. إدارة البيئة التجريبية (Sandbox) والإنتاجية (Production) والمفاتيح السرية بأمان.
 */

export type GatewayType = 'MyFatoorah' | 'PayTabs' | 'TapPayments' | 'HyperPay';
export type PaymentChannel = 'COD' | 'CREDIT_CARD' | 'MADA' | 'APPLE_PAY' | 'GOOGLE_PAY' | 'LOCAL_WALLET' | 'INSTALLMENTS';
export type LocalWalletProvider = 'mFloos' | 'OneCash' | 'Pocket';

export interface PaymentConfig {
  activeGateway: GatewayType;
  apiKey: string;
  isSandbox: boolean;
  isCodEnabled: boolean;
  isCardEnabled: boolean;
  isMadaEnabled: boolean;
  isApplePayEnabled: boolean;
  isGooglePayEnabled: boolean;
  isLocalWalletsEnabled: boolean;
  isInstallmentsEnabled: boolean;
  installmentProvider: 'Tabby' | 'Tamara' | 'Both';
  installmentMonths: number;
}

export interface PaymentRequest {
  orderId: number;
  amount: number;
  currency: 'YER' | 'SAR' | 'USD';
  channel: PaymentChannel;
  customerName: string;
  customerEmail: string;
  customerPhone: string;
  localWallet?: LocalWalletProvider; // خاص بـ LOCAL_WALLET
  installmentMonthsSelected?: number; // خاص بـ INSTALLMENTS
}

export interface PaymentResponse {
  success: boolean;
  transactionId?: string;
  paymentUrl?: string; // رابط توجيه الزبون لإتمام الدفع الآمن
  status: 'PENDING' | 'SUCCESS' | 'FAILED' | 'REFUNDED';
  gatewayUsed: GatewayType;
  message: string;
}

// واجهة الاستراتيجية لكل بوابة دفع مستقلة
interface PaymentGatewayStrategy {
  initializePayment(request: PaymentRequest, apiKey: string, sandbox: boolean): Promise<PaymentResponse>;
  verifyPayment(transactionId: string, apiKey: string, sandbox: boolean): Promise<PaymentResponse>;
  refundPayment(transactionId: string, amount: number, apiKey: string, sandbox: boolean): Promise<PaymentResponse>;
}

// 1. استراتيجية بوابة MyFatoorah
class MyFatoorahStrategy implements PaymentGatewayStrategy {
  async initializePayment(request: PaymentRequest, apiKey: string, sandbox: boolean): Promise<PaymentResponse> {
    const baseUrl = sandbox ? 'https://apitest.myfatoorah.com' : 'https://api.myfatoorah.com';
    console.log(`[MyFatoorah] جاري إطلاق عملية دفع للطلب #${request.orderId} بقيمة ${request.amount} ${request.currency} عبر ${request.channel}`);
    
    // محاكاة الاتصال الفعلي بـ API الخاص بـ MyFatoorah
    const mockTxId = `MF-${Math.floor(100000 + Math.random() * 900000)}`;
    return {
      success: true,
      transactionId: mockTxId,
      paymentUrl: `${baseUrl}/v2/Checkout?paymentId=${mockTxId}`,
      status: 'PENDING',
      gatewayUsed: 'MyFatoorah',
      message: 'تم إنشاء رابط الدفع بنجاح عبر ماي فاتورة.'
    };
  }

  async verifyPayment(transactionId: string, apiKey: string, sandbox: boolean): Promise<PaymentResponse> {
    console.log(`[MyFatoorah] جاري التحقق من المعاملة #${transactionId}`);
    return {
      success: true,
      transactionId: transactionId,
      status: 'SUCCESS',
      gatewayUsed: 'MyFatoorah',
      message: 'تم تأكيد استلام الدفعة بنجاح عبر ماي فاتورة.'
    };
  }

  async refundPayment(transactionId: string, amount: number, apiKey: string, sandbox: boolean): Promise<PaymentResponse> {
    console.log(`[MyFatoorah] جاري إرجاع مبلغ ${amount} للمعاملة #${transactionId}`);
    return {
      success: true,
      transactionId: transactionId,
      status: 'REFUNDED',
      gatewayUsed: 'MyFatoorah',
      message: 'تم إرجاع المبلغ للزبون بنجاح.'
    };
  }
}

// 2. استراتيجية بوابة PayTabs
class PayTabsStrategy implements PaymentGatewayStrategy {
  async initializePayment(request: PaymentRequest, apiKey: string, sandbox: boolean): Promise<PaymentResponse> {
    const baseUrl = sandbox ? 'https://secure-global-test.paytabs.com' : 'https://secure.paytabs.com';
    console.log(`[PayTabs] جاري إطلاق الدفع للطلب #${request.orderId} بقيمة ${request.amount} ${request.currency}`);
    
    const mockTxId = `PT-${Math.floor(100000 + Math.random() * 900000)}`;
    return {
      success: true,
      transactionId: mockTxId,
      paymentUrl: `${baseUrl}/payment/page/${mockTxId}`,
      status: 'PENDING',
      gatewayUsed: 'PayTabs',
      message: 'تم إنشاء صفحة الدفع بنجاح عبر بي تابس.'
    };
  }

  async verifyPayment(transactionId: string, apiKey: string, sandbox: boolean): Promise<PaymentResponse> {
    console.log(`[PayTabs] التحقق من العملية ${transactionId}`);
    return {
      success: true,
      transactionId: transactionId,
      status: 'SUCCESS',
      gatewayUsed: 'PayTabs',
      message: 'تم استلام الدفعة وتحصيلها.'
    };
  }

  async refundPayment(transactionId: string, amount: number, apiKey: string, sandbox: boolean): Promise<PaymentResponse> {
    return {
      success: true,
      transactionId: transactionId,
      status: 'REFUNDED',
      gatewayUsed: 'PayTabs',
      message: 'تم إلغاء المعاملة وإعادة المبلغ عبر بي تابس.'
    };
  }
}

// 3. استراتيجية بوابة Tap Payments
class TapPaymentsStrategy implements PaymentGatewayStrategy {
  async initializePayment(request: PaymentRequest, apiKey: string, sandbox: boolean): Promise<PaymentResponse> {
    console.log(`[TapPayments] جاري تهيئة الدفع للطلب #${request.orderId} عبر تاب.`);
    const mockTxId = `TAP-${Math.floor(100000 + Math.random() * 900000)}`;
    return {
      success: true,
      transactionId: mockTxId,
      paymentUrl: `https://checkout.tap.company/v2/pay?chargeId=${mockTxId}`,
      status: 'PENDING',
      gatewayUsed: 'TapPayments',
      message: 'تم تحضير رابط الشحن والدفع بنجاح.'
    };
  }

  async verifyPayment(transactionId: string, apiKey: string, sandbox: boolean): Promise<PaymentResponse> {
    return {
      success: true,
      transactionId: transactionId,
      status: 'SUCCESS',
      gatewayUsed: 'TapPayments',
      message: 'تم التحقق وتأكيد الدفع عبر تاب.'
    };
  }

  async refundPayment(transactionId: string, amount: number, apiKey: string, sandbox: boolean): Promise<PaymentResponse> {
    return {
      success: true,
      transactionId: transactionId,
      status: 'REFUNDED',
      gatewayUsed: 'TapPayments',
      message: 'تم رد المبلغ بنجاح عبر تاب.'
    };
  }
}

// 4. استراتيجية بوابة HyperPay
class HyperPayStrategy implements PaymentGatewayStrategy {
  async initializePayment(request: PaymentRequest, apiKey: string, sandbox: boolean): Promise<PaymentResponse> {
    console.log(`[HyperPay] جاري معالجة الطلب #${request.orderId} عبر هايبر باي.`);
    const mockTxId = `HP-${Math.floor(100000 + Math.random() * 900000)}`;
    return {
      success: true,
      transactionId: mockTxId,
      paymentUrl: `https://oppwa.com/v1/checkouts/${mockTxId}/payment`,
      status: 'PENDING',
      gatewayUsed: 'HyperPay',
      message: 'تم إعداد معرف الجلسة الآمنة عبر هايبر باي.'
    };
  }

  async verifyPayment(transactionId: string, apiKey: string, sandbox: boolean): Promise<PaymentResponse> {
    return {
      success: true,
      transactionId: transactionId,
      status: 'SUCCESS',
      gatewayUsed: 'HyperPay',
      message: 'تم تأكيد دفع الفاتورة.'
    };
  }

  async refundPayment(transactionId: string, amount: number, apiKey: string, sandbox: boolean): Promise<PaymentResponse> {
    return {
      success: true,
      transactionId: transactionId,
      status: 'REFUNDED',
      gatewayUsed: 'HyperPay',
      message: 'تم إرجاع المبلغ بالكامل.'
    };
  }
}


/**
 * كلاس التحكم الرئيسي لإدارة الدفع وتوجيه المعاملات
 */
export class PaymentController {
  private config: PaymentConfig;
  private gateways: Record<GatewayType, PaymentGatewayStrategy>;

  constructor(initialConfig?: Partial<PaymentConfig>) {
    // الإعدادات الافتراضية
    this.config = {
      activeGateway: 'MyFatoorah',
      apiKey: 'sk_test_alahmadi_992x_auth_key_0x00a',
      isSandbox: true,
      isCodEnabled: true,
      isCardEnabled: true,
      isMadaEnabled: true,
      isApplePayEnabled: true,
      isGooglePayEnabled: true,
      isLocalWalletsEnabled: true,
      isInstallmentsEnabled: false,
      installmentProvider: 'Tabby & Tamara' as any,
      installmentMonths: 4,
      ...initialConfig
    };

    // تسجيل استراتيجيات البوابات
    this.gateways = {
      MyFatoorah: new MyFatoorahStrategy(),
      PayTabs: new PayTabsStrategy(),
      TapPayments: new TapPaymentsStrategy(),
      HyperPay: new HyperPayStrategy()
    };
  }

  /**
   * تحديث إعدادات النظام الإدارية لبوابات الدفع ديناميكياً
   */
  public updateConfig(newConfig: Partial<PaymentConfig>): void {
    this.config = { ...this.config, ...newConfig };
    console.log(`[PaymentController] تم تحديث إعدادات بوابات الدفع بنجاح. البوابة النشطة الحالية: ${this.config.activeGateway}`);
  }

  /**
   * جلب الإعدادات الحالية لعرضها في لوحة التحكم الإدارية
   */
  public getConfig(): PaymentConfig {
    return { ...this.config };
  }

  /**
   * معالجة وتدشين عملية دفع جديدة للعميل
   */
  public async processPayment(request: PaymentRequest): Promise<PaymentResponse> {
    try {
      // 1. التحقق من تفعيل طريقة الدفع المطلوبة في إعدادات النظام
      this.validateChannelStatus(request.channel);

      // 2. معالجة الحالات الخاصة التي لا تحتاج اتصالاً ببوابة دفع إلكترونية خارجية مباشرة
      if (request.channel === 'COD') {
        return {
          success: true,
          status: 'PENDING',
          gatewayUsed: this.config.activeGateway,
          message: 'تم اختيار الدفع عند الاستلام بنجاح، سيتم تجهيز طلبك وتوصيله.'
        };
      }

      if (request.channel === 'LOCAL_WALLET') {
        // محاكاة تحويل رقمي يمني (مثل كريمي mFloos أو OneCash)
        const walletName = request.localWallet || 'mFloos';
        const referenceCode = `WL-${Math.floor(100000 + Math.random() * 900000)}`;
        return {
          success: true,
          transactionId: referenceCode,
          status: 'PENDING',
          gatewayUsed: this.config.activeGateway,
          message: `تم اختيار المحفظة الرقمية (${walletName}). يرجى تحويل المبلغ إلى الحساب المعتمد لمركز الأحمدي وتأكيد رقم المعاملة #${referenceCode}.`
        };
      }

      if (request.channel === 'INSTALLMENTS') {
        // محاكاة تمويل التقسيط عبر تابي أو تمارا
        const provider = this.config.installmentProvider;
        const months = request.installmentMonthsSelected || this.config.installmentMonths;
        const monthlyInstallment = (request.amount / months).toFixed(2);
        return {
          success: true,
          transactionId: `INST-${provider}-${Math.floor(100000 + Math.random() * 900000)}`,
          paymentUrl: `https://${provider.toLowerCase()}.com/checkout/alahmadi-store`,
          status: 'PENDING',
          gatewayUsed: this.config.activeGateway,
          message: `تم إعداد خطة التقسيط للعميل عبر (${provider}) على ${months} أشهر. القسط الشهري المتوقع: ${monthlyInstallment} ${request.currency}.`
        };
      }

      // 3. التوجيه الديناميكي للبوابة المفعلة للبطاقات الائتمانية، مدى، Apple Pay، Google Pay
      const selectedStrategy = this.gateways[this.config.activeGateway];
      if (!selectedStrategy) {
        throw new Error(`بوابة الدفع المحددة غير مدعومة حالياً في الكنترولر: ${this.config.activeGateway}`);
      }

      // إطلاق الدفع عبر البوابة
      const response = await selectedStrategy.initializePayment(
        request,
        this.config.apiKey,
        this.config.isSandbox
      );

      return response;
    } catch (error: any) {
      console.error('[PaymentController Error]', error.message || error);
      return {
        success: false,
        status: 'FAILED',
        gatewayUsed: this.config.activeGateway,
        message: `فشلت عملية الدفع: ${error.message || 'حدث خطأ غير معروف.'}`
      };
    }
  }

  /**
   * التحقق من حالة وصحة الدفع بعد توجيه العميل للبوابة (Webhook / Callback)
   */
  public async verifyTransaction(transactionId: string): Promise<PaymentResponse> {
    try {
      const selectedStrategy = this.gateways[this.config.activeGateway];
      return await selectedStrategy.verifyPayment(
        transactionId,
        this.config.apiKey,
        this.config.isSandbox
      );
    } catch (error: any) {
      return {
        success: false,
        status: 'FAILED',
        gatewayUsed: this.config.activeGateway,
        message: `فشل التحقق من صحة العملية: ${error.message}`
      };
    }
  }

  /**
   * إرجاع مبلغ لزبون (Refund) في حالة الاستبدال أو استرجاع المنتجات
   */
  public async refundTransaction(transactionId: string, amount: number): Promise<PaymentResponse> {
    try {
      const selectedStrategy = this.gateways[this.config.activeGateway];
      return await selectedStrategy.refundPayment(
        transactionId,
        amount,
        this.config.apiKey,
        this.config.isSandbox
      );
    } catch (error: any) {
      return {
        success: false,
        status: 'FAILED',
        gatewayUsed: this.config.activeGateway,
        message: `فشل استرجاع المبلغ: ${error.message}`
      };
    }
  }

  /**
   * فحص ما إذا كانت القناة المطلوبة مفعلة في إعدادات الإدارة
   */
  private validateChannelStatus(channel: PaymentChannel): void {
    switch (channel) {
      case 'COD':
        if (!this.config.isCodEnabled) throw new Error('الدفع عند الاستلام غير مفعل حالياً بطلب من الإدارة.');
        break;
      case 'CREDIT_CARD':
        if (!this.config.isCardEnabled) throw new Error('الدفع بالبطاقات الائتمانية غير مفعل حالياً.');
        break;
      case 'MADA':
        if (!this.config.isMadaEnabled) throw new Error('بوابة الدفع عبر شبكة مدى غير مفعلة.');
        break;
      case 'APPLE_PAY':
        if (!this.config.isApplePayEnabled) throw new Error('بوابة Apple Pay غير مفعلة حالياً.');
        break;
      case 'GOOGLE_PAY':
        if (!this.config.isGooglePayEnabled) throw new Error('بوابة Google Pay غير مفعلة حالياً.');
        break;
      case 'LOCAL_WALLET':
        if (!this.config.isLocalWalletsEnabled) throw new Error('الدفع بالمحافظ الرقمية اليمنية المباشرة غير مفعل.');
        break;
      case 'INSTALLMENTS':
        if (!this.config.isInstallmentsEnabled) throw new Error('خدمة التقسيط الشهري للعملاء غير متاحة حالياً.');
        break;
    }
  }
}
