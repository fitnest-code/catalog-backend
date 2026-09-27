# Oktyabr Kampaniyası (October Campaign) — Mobil Komanda İnteqrasiya Sənədi

Bu sənəd **FitNest** mobil tətbiq (iOS & Android) komandası üçün Oktyabr Kampaniyasının (October Campaign) server tərəfi API inteqrasiyası, payload strukturları, axınlar və UI/UX tələblərini əks etdirir.

---

## 1. Ümumi Məntiq və Prinsiplər

1. **Qiymət və Ödəniş Dəyişmir:** Kampaniya paketlərin qiymətini dəyişmir (`priceStandard` / `priceDiscounted` və FitCoin güzəşti olduğu kimi qalır). Kampaniya **yalnız pulsuz hədiyyə aylar əlavə edir**.
2. **Hədiyyə Aylar Sxemi:**
   - **3 aylıq paket:** +1 ay hədiyyə (**Cəmi 4 ay giriş**)
   - **6 aylıq paket:** +2 ay hədiyyə (**Cəmi 8 ay giriş**)
   - **12 aylıq paket:** +3 ay hədiyyə (**Cəmi 15 ay giriş**)
   - **1 aylıq paket:** Kampaniyadan **azaddır (0 bonus)**. Avto-ödəniş (auto-renew) yalnız 1 aylıq olduğu üçün hədiyyə ay almır.
3. **Məhdudiyyət (Eligibility):**
   - Hər istifadəçi hesabı kampaniyadan yalnız **1 dəfə** istifadə edə bilər.
   - Kampaniya yalnız **yeni ödənişli satınalmaya** tətbiq olunur. Cari `ACTIVE` və ya `FROZEN` abunəliyi olan istifadəçi yeni paket aldıqda və ya paketini yenilədikdə kampaniya bonusu tətbiq edilmir.
4. **Ödəniş Sorğusunda Kampaniya ID Göndərilmir:** Mobil tətbiq ödəniş iniciasiyası (`POST /api/v2/payment/epoint/init`, Apple/Google Pay və s.) zamanı serverə hər hansı kampaniya ID-si göndərmir. Server ödəmə tamamlandıqdan sonra abunəlik yazılan zaman istifadəçinin uyğunluğunu avtomatik təyin edir.

---

## 2. İnteqrasiya Axını (Sequence Diagram)

```mermaid
sequenceDiagram
    autonumber
    actor User as İstifadəçi
    participant App as Mobile App
    participant Gateway as Gateway / Order Backend
    participant Payment as Payment Backend

    rect rgb(240, 248, 255)
    note over User, Gateway: 1. Tətbiq Açılışı və Popup Yoxlanışı
    App->>Gateway: GET /api/v1/campaigns/active?context=popup
    Gateway-->>App: 200 OK (eligible: true, showPopup: true)
    alt showPopup == true
        App->>User: Popup Modalını Göstər
        App->>Gateway: POST /api/v1/campaigns/{id}/impressions (context: POPUP)
    end
    end

    rect rgb(255, 245, 238)
    note over User, Gateway: 2. Paketlər Ekranı (Plans Screen)
    User->>App: Paketlər Səhifəsinə Keçir
    App->>Gateway: GET /api/v3/subscription-packages
    Gateway-->>App: Paketlər siyahısı (options[].bonus_months, badge: "campaign")
    App->>User: Hədiyyə ay nişanlarını (badge) və qiymətləri göstər
    end

    rect rgb(245, 255, 250)
    note over User, Payment: 3. Ödəniş və Abunəlik Aktivasiyası
    User->>App: Paket seçib ödəniş edir
    App->>Payment: POST /api/v2/payment/epoint/init (packageId, optionId, isCoinUsed)
    Payment-->>App: Bank Checkout URL
    User->>Payment: Bank kartı məlumatlarını daxil edir
    Payment->>Gateway: gRPC AssignSubscriptionToUser
    Gateway-->>Payment: Abunəlik aktiv edildi (+Bonus aylar)
    end

    rect rgb(255, 250, 240)
    note over User, Gateway: 4. Abunəlik Səhifəsi (Active Subscription)
    App->>Gateway: GET /api/v2/me/subscriptions/active
    Gateway-->>App: Active Subscription (+campaign_confirmation_banner, next_payment_due_at, service_end_at)
    App->>User: Hədiyyə ay konfirmasiya bannerini və bitmə tarixlərini göstər
    User->>App: Bannerdə "Bağla" sıxır
    App->>Gateway: POST /api/v1/me/subscriptions/campaign-banner/dismiss
    end
```

---

## 3. API Endpoint-ləri və Payload Strukturları

### 3.1 Aktiv Kampaniya və Popup Yoxlanışı

- **Endpoint:** `GET /api/v1/campaigns/active`
- **Headers:**
  - `Authorization: Bearer <JWT_TOKEN>` (Tələb olunur)
  - `Accept-Language: az | en | ru` (Standart: `az`)
- **Query Parametr:**
  - `context=popup` — Tətbiq açılışında popup üçün. Günlük 1 dəfə göstərilmə limitini (daily cap) tətbiq edir.
  - `context=banner` — Paketlər və ya Ana səhifə banneri üçün. Günlük limiti nəzərə almadan aktiv kampaniya haqqında məlumat qaytarır.

#### Response (Kampaniya Aktiv və İstifadəçi Uyğundursa — `200 OK`):
```json
{
  "eligible": true,
  "showPopup": true,
  "nextEligibleShowAt": null,
  "campaign": {
    "id": 1,
    "code": "OCT_2026",
    "title": "Daha çox idman, daha çox imkan!",
    "description": "3, 6 və 12 aylıq abunəliklərdə əlavə ayları bizdən hədiyyə al!",
    "bannerImageUrl": "https://media.fitnest.az/stream/october_banner.png",
    "ctaLabel": "Ətraflı Bax",
    "ctaTarget": "CAMPAIGN_DETAIL",
    "startAt": "2026-10-01T00:00:00",
    "endAt": "2026-10-31T23:59:59",
    "offers": [
      {
        "baseDurationMonths": 3,
        "bonusMonths": 1,
        "totalMonths": 4,
        "label": "3 aylıq al + 1 ay hədiyyə"
      },
      {
        "baseDurationMonths": 6,
        "bonusMonths": 2,
        "totalMonths": 8,
        "label": "6 aylıq al + 2 ay hədiyyə"
      },
      {
        "baseDurationMonths": 12,
        "bonusMonths": 3,
        "totalMonths": 15,
        "label": "12 aylıq al + 3 ay hədiyyə"
      }
    ],
    "terms": [
      {
        "text": "Kampaniya yalnız oktyabr ayında keçərlidir.",
        "isPositive": true,
        "sortOrder": 1
      },
      {
        "text": "Hər istifadəçi 1 dəfə yararlana bilər.",
        "isPositive": true,
        "sortOrder": 2
      },
      {
        "text": "1 aylıq abunəlik kampaniyaya daxil deyil.",
        "isPositive": true,
        "sortOrder": 3
      },
      {
        "text": "Hədiyyə ayları avtomatik əlavə olunur.",
        "isPositive": true,
        "sortOrder": 4
      }
    ]
  }
}
```

#### Response (İstifadəçi Bu Gün Popup-ı Artıq Görüb — `200 OK`):
`showPopup: false` qaydır və növbəti göstərilmə zamanını (`nextEligibleShowAt`) növbəti Baku yarımgecəsi olaraq göstərir. Mobil tətbiq `showPopup == false` olduqda popup-ı **göstərməməlidir**.

```json
{
  "eligible": true,
  "showPopup": false,
  "nextEligibleShowAt": "2026-10-16T00:00:00",
  "campaign": { ... }
}
```

#### Response (Kampaniya Yoxdur və ya İstifadəçi Uyğun Deyil — `200 OK`):
> [!IMPORTANT]
> Kampaniya olmadıqda və ya istifadəçi artıq faydalandıqda server `404` yox, `200 OK` statusu qaytarır.
```json
{
  "eligible": false,
  "showPopup": false,
  "nextEligibleShowAt": null,
  "campaign": null
}
```

---

### 3.2 Popup Göstərildi Qeydiyyatı (Impression Tracking)

İstifadəçiyə popup ekranda real olaraq göstərildiyi an mobil tətbiq bu API-ni çağırmalıdır ki, server günlük 1 göstərilmə limitini qeydə alsın.

- **Endpoint:** `POST /api/v1/campaigns/{id}/impressions`
- **Headers:** `Authorization: Bearer <JWT_TOKEN>`
- **Request Body:**
```json
{
  "context": "POPUP"
}
```
- **Response:** `200 OK`

---

### 3.3 Kampaniya Ətraflı Məlumatı (Campaign Detail Screen)

Popup-da "Ətraflı Bax" (CTA) kliklədikdə və ya kampaniya səhifəsinə keçdikdə istifadə olunur.

- **Endpoint:** `GET /api/v1/campaigns/{id}`
- **Headers:** `Authorization: Bearer <JWT_TOKEN>`, `Accept-Language: az|en|ru`
- **Response:** 3.1-dəki `campaign` obyekti ilə eynidir.

---

### 3.4 Paketlər Ekranı (Subscription Packages API)

Mobil tətbiq paketləri siyahılayarkən kampaniya hədiyyə aylarını göstərmək üçün mövcud paket API-lərindən istifadə edir. Option obyektinə yeni sahələr əlavə edilmişdir.

- **Endpoint:** `GET /api/v3/subscription-packages` və ya `GET /api/v1/subscription-packages/{id}`
- **Headers:** `Authorization: Bearer <JWT_TOKEN>` (İstifadəçi tokeni olduqda redemptions yoxlanılır; istifadəçi artıq faydalanıbsa kampaniya sahələri `null` gəlir).

#### Response Snippet (`options[]` daxilində):
```json
{
  "optionId": 20,
  "durationMonths": 3,
  "durationLabel": "3 ay",
  "price": {
    "base": 150.00,
    "discount": 120.00,
    "effective": 120.00,
    "currency": "AZN"
  },
  "badge": "campaign",
  "bonusMonths": 1,
  "totalMonths": 4,
  "campaignId": 1,
  "campaignLabel": "3 ay al + 1 ay hədiyyə",
  "strikeThroughTotal": 150.00,
  "savingsAmount": 30.00,
  "monthlyEquivalent": 40.00
}
```

> [!TIP]
> - `badge`: Kampaniya aktiv olduqda `"campaign"`, standart endirimdə `"discount"`, əks halda `null` olur.
> - `monthlyEquivalent`: Aylıq məbləğ hesablanarkən yalnız **ödənilən aylar** götürülür (`effective / durationMonths`), hədiyyə aylar aylıq qiyməti süni şəkildə aşağı salmır.

---

### 3.5 Aktiv Abunəlik Ekranı (Me Subscriptions Active)

İstifadəçi kampaniya ilə abunəlik aldıqdan sonra məlumatlar ekranında hədiyyə ayları və növbəti ödəniş/xidmət bitmə tarixlərini fərqli şəkillərdə görür.

- **Endpoint:** `GET /api/v2/me/subscriptions/active` (v1, v2 və v3 dəstəklənir)
- **Headers:** `Authorization: Bearer <JWT_TOKEN>`

#### Response Snippet:
```json
{
  "status": "Active",
  "subscription": {
    "subscriptionId": 501,
    "packageName": "Gold Plan",
    "paidDurationMonths": 3,
    "bonusMonths": 1,
    "totalMonths": 4,
    "nextPaymentDueAt": "2027-01-01",
    "serviceEndAt": "2027-02-01",
    "campaignConfirmationBanner": {
      "campaignId": 1,
      "bonusMonths": 1,
      "title": "1 ay hədiyyə qazandınız!",
      "body": "Oktyabr kampaniyasından abunəliyinizə 1 ay hədiyyə əlavə olundu."
    }
  }
}
```

#### İki Fərqli Tarixin Mənası:
1. **`nextPaymentDueAt` (`paid_until`):** İstifadəçinin növbəti ödəniş müddəti (Ödənilmiş 3 ayın sonu).
2. **`serviceEndAt` (`end_at`):** Hədiyyə aylar daxil olmaqla zal girişlərinin tam bitmə tarixi (4 ayın sonu).

---

### 3.6 Konfirmasiya Bannerini Bağlamaq (Dismiss Banner)

İstifadəçi Abunəlik ekranında hədiyyə qazandığını bildirən konfirmasiya bannerindəki "X" və ya "Bağla" düyməsini sıxdıqda çağırılır.

- **Endpoint:** `POST /api/v1/me/subscriptions/campaign-banner/dismiss`
- **Headers:** `Authorization: Bearer <JWT_TOKEN>`
- **Response:** `200 OK`

Bu API çağırıldıqdan sonra `GET /api/v2/me/subscriptions/active` cavabında `campaignConfirmationBanner` obyekti `null` gələcəkdir.

---

## 4. UI / UX Təlimatları

| Ekran / Komponent | Mobil UI Görünüşü və Qaydalar |
|-------------------|--------------------------------|
| **Popup Modal** | `showPopup == true` olduqda açılır. Şəkil (`bannerImageUrl`), Başlıq, Mətn və CTA düyməsi (`ctaLabel`) göstərilir. Modal açılan kimi `/impressions` API-si çağırılır. |
| **CTA Klik (ctaTarget)** | `CAMPAIGN_DETAIL` gəldikdə mobil tətbiq kampaniyanın şərtləri və təklifləri olan detail ekranını açmalıdır. |
| **Paket Seçimi (Plans)** | Option kartının üzərində `badge: "campaign"` olduqda xüsusi "Oktyabr Kampaniyası" və ya `campaignLabel` (məs: *3 ay al + 1 ay hədiyyə*) nişanı vurulur. |
| **Abunəlik Səhifəsi** | Banner olduqda yaşıl/hədiyyə konfirmasiya bloku göstərilir. `serviceEndAt` xidmətin bitmə tarixi kimi, `nextPaymentDueAt` isə ödəniş tarixi kimi təqdim olunur. |

---

## 5. Kampaniyanın Bitmə Tarixi və Avtomatik Keçid (Auto-Expiration Rules)

1. **Bitmə Vaxtı:** Kampaniya **31.10.2026 23:59:59** (`Asia/Baku` vaxtı ilə) avtomatik olaraq başa çatır.
2. **Server İdarəetməsi (Backend Controlled):**
   - 31 Oktyabr 23:59:59 keçən kimi `GET /api/v1/campaigns/active` sorğusu avtomatik olaraq `{ "eligible": false, "showPopup": false, "nextEligibleShowAt": null, "campaign": null }` qaytaracaq.
   - `GET /api/v3/subscription-packages` (Paketlər ekranı) sorğusunda opsiyalardan `badge: "campaign"`, `bonusMonths` və `campaignLabel` avtomatik ləğv ediləcək və görünüş standart paketlərinə keçəcəkdir.
   - Server tərəfində saatlıq çalışan `CampaignExpiryJob` cron vasitəsilə 31.10 bitən kimi kampaniyanın statusu bazada avtomatik `EXPIRED` edilir.
3. **Mobil Tətbiq Tələbi (Mobile App Rule):**
   - Mobil tətbiqin (iOS / Android) istifadəçi telefonunun yerli saatı üzərindən hər hansı hardcoded tarix yoxlaması aparmasına **ehtiyac yoxdur**.
   - Mobil tətbiq sadəcə backend-dən gələn `eligible: false` və ya `campaign: null` bayraqlarına əsasən Popup və Banner-ləri göstərməməlidir.
