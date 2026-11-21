# React rehberi: bu projeyi okuyarak React öğren

Bu belge, OGVPTS arayüzünü (`frontend/`) okurken karşına çıkacak React ve TypeScript kavramlarını **projedeki gerçek kodla** açıklar. Ezber yerine "şu dosyada şu satır neden böyle?" sorusuna cevap vermeyi hedefler.

> Kodun içinde de bol açıklama var. Bu rehber o açıklamaları bir araya toplayan bir harita gibi düşünülebilir.

## İçindekiler

1. [Önce hangi dosyaları okumalıyım?](#1-okuma-sırası)
2. [Klasör yapısı](#2-klasör-yapısı)
3. [Temel kavramlar](#3-temel-kavramlar): bileşen, JSX, props, state, effect, context, hook, router, form
4. [Bir tıklamanın yolculuğu](#4-bir-tıklamanın-yolculuğu): "Randevu talep et" butonundan veritabanına
5. [TypeScript notları](#5-typescript-notları)
6. [Bootstrap ile görünüm](#6-bootstrap-ile-görünüm)
7. [Sık yapılan hatalar](#7-sık-yapılan-hatalar)
8. [Kendin dene: alıştırmalar](#8-kendin-dene)

---

## 1. Okuma sırası

Aşağıdaki sırayla okursan her dosya bir öncekinin üstüne kurulur:

| Sıra | Dosya | Ne öğrenirsin? |
|---|---|---|
| 1 | `src/main.tsx` | Uygulama nereden başlıyor, sarmalayıcılar (Router, AuthProvider) |
| 2 | `src/App.tsx` | Hangi adres hangi sayfayı açıyor, rol bazlı koruma |
| 3 | `src/api/client.ts` | Sunucuya istek nasıl atılıyor, token nasıl ekleniyor, hatalar nasıl yakalanıyor |
| 4 | `src/api/endpoints.ts` + `types.ts` | Backend'deki her uç noktanın TypeScript karşılığı |
| 5 | `src/auth/AuthContext.tsx` | **State, Effect, Context**: giriş yapmış kullanıcı bilgisi |
| 6 | `src/hooks/useApi.ts` | **Özel hook**: yükleniyor / hata / veri üçlüsü |
| 7 | `src/components/StatusBadge.tsx` | En basit bileşen: **props** |
| 8 | `src/components/Layout.tsx` | Menü, `NavLink`, `<Outlet />` |
| 9 | `src/pages/LoginPage.tsx` | **Form** ve yönlendirme |
| 10 | `src/pages/shared/MeetingsPage.tsx` | Gerçek bir iş akışı: liste, filtre, butonlar, modal |

---

## 2. Klasör yapısı

```
frontend/
├── index.html              # tek HTML sayfa; React her şeyi <div id="root"> içine çizer
├── vite.config.ts          # geliştirme sunucusu, /api → 8080 yönlendirmesi, build çıktısı
└── src/
    ├── main.tsx            # giriş noktası
    ├── App.tsx             # sayfa yönlendirmeleri (routes)
    ├── styles.css          # Bootstrap'a ek birkaç kural
    ├── api/                # sunucu ile konuşan her şey
    │   ├── client.ts       #   fetch sarmalayıcı, token, ApiError
    │   ├── endpoints.ts    #   authApi, meetingApi, taskApi ...
    │   └── types.ts        #   backend DTO'larının TypeScript tipleri
    ├── auth/               # oturum: AuthContext, RequireRole
    ├── hooks/              # useApi, useAction
    ├── utils/format.ts     # tarih biçimleri, durum → etiket/renk eşlemeleri
    ├── components/         # birden çok sayfada kullanılan parçalar (Layout, Modal, StatusBadge ...)
    └── pages/              # her ekran bir dosya
        ├── student/        #   grubum, projem
        ├── advisor/        #   müsaitlik, projeler
        ├── admin/          #   eşleştirme, kullanıcılar, raporlar
        └── shared/         #   randevular, görevler, bildirimler, hesabım ...
```

**Kural:** Sayfalar (`pages/`) sunucuyla doğrudan `fetch` ile konuşmaz; her zaman `api/endpoints.ts` üzerinden konuşur. Böylece bir adres değişirse tek yerden düzeltilir.

---

## 3. Temel kavramlar

### 3.1 Bileşen ve JSX

React'te ekran **bileşenlerden** oluşur. Bileşen, HTML'e benzeyen bir şey döndüren sıradan bir JavaScript fonksiyonudur:

```tsx
// src/components/StatusBadge.tsx
export default function StatusBadge({ badge }: { badge: { label: string; color: string } }) {
  return <span className={`badge text-bg-${badge.color}`}>{badge.label}</span>
}
```

Döndürülen HTML benzeri yazıma **JSX** denir. HTML'den farkları:

| HTML | JSX | Neden |
|---|---|---|
| `class="..."` | `className="..."` | `class` JavaScript'te ayrılmış bir kelime |
| `for="..."` | `htmlFor="..."` | aynı sebep |
| `onclick="..."` | `onClick={fonksiyon}` | olaylar camelCase yazılır ve fonksiyon verilir |
| `style="height: 8px"` | `style={{ height: 8 }}` | stil bir nesnedir |
| | `{ifade}` | süslü parantez içine her JavaScript ifadesi yazılabilir |

Bileşen adları **büyük harfle** başlar (`StatusBadge`). Küçük harfle başlayanlar (`div`, `span`) normal HTML etiketi sayılır.

### 3.2 Props: bileşene dışarıdan veri vermek

Yukarıdaki `badge`, bir **prop**'tur. Kullanan taraf şöyle verir:

```tsx
<StatusBadge badge={meetingStatus[m.status]} />
```

Props **salt okunurdur**: bileşen kendisine verilen prop'u değiştirmez. Değiştirmesi gereken bir şey varsa bunu **state** ile yapar ya da üst bileşenin verdiği bir fonksiyonu çağırır (bkz. `ProjectView`'deki `onChange` prop'u).

Özel bir prop: **`children`**. Bileşenin açılış ve kapanış etiketi arasına yazılanlardır:

```tsx
// Modal.tsx içinde {children} nereye yazıldıysa form oraya gelir
<Modal title="Yeni kullanıcı" onClose={onClose}>
  <form>...</form>
</Modal>
```

### 3.3 State (`useState`): bileşenin hafızası

Bir değer değiştiğinde ekranın da değişmesini istiyorsan o değeri **state**'te tutarsın:

```tsx
// src/pages/shared/AccountPage.tsx
const [current, setCurrent] = useState('')

<input value={current} onChange={(e) => setCurrent(e.target.value)} />
```

- `current`: şu anki değer
- `setCurrent`: değiştirme fonksiyonu. Çağrıldığında React bileşeni **yeniden çizer**.

Altın kural: **state'i doğrudan değiştirme, yenisini ver.**

```tsx
// ❌ YANLIŞ: React değişikliği fark etmez, ekran güncellenmez
list[0].read = true

// ✅ DOĞRU: yeni bir dizi üret (NotificationsPage.tsx)
setData((list) => list?.map((n) => (n.id === id ? { ...n, read: true } : n)) ?? null)
```

`{ ...n, read: true }` ifadesi "n'nin bütün alanlarını kopyala, yalnızca `read`'i değiştir" demektir (spread operatörü).

### 3.4 Effect (`useEffect`): çizimden sonra yapılacak işler

Sunucudan veri çekmek, zamanlayıcı kurmak gibi **dış dünyayla** ilgili işler effect'e yazılır:

```tsx
// src/components/Layout.tsx: her sayfa değişiminde okunmamış bildirim sayısını tazele
useEffect(() => {
  notificationApi.unreadCount().then(...)
}, [location.pathname])
```

Sondaki dizi, **bağımlılık listesidir**:

| Bağımlılık | Effect ne zaman çalışır? |
|---|---|
| `[]` | Bileşen ilk açıldığında bir kez |
| `[location.pathname]` | İlk açılışta ve adres her değiştiğinde |
| (hiç yok) | Her çizimden sonra; nadiren istenir |

Effect bir fonksiyon döndürürse o fonksiyon **temizlik** için çağrılır. `Layout.tsx`'teki 60 saniyelik zamanlayıcı bu sayede sayfadan çıkınca durdurulur:

```tsx
useEffect(() => {
  const id = window.setInterval(() => { /* sayıyı tazele */ }, 60_000)
  return () => window.clearInterval(id) // temizlik
}, [])
```

> **Not:** Geliştirme modunda (`StrictMode`) React effect'leri bilerek **iki kez** çalıştırır; temizlik kodunu unuttuysan hatayı erkenden görürsün. Konsolda aynı isteğin iki kez gitmesi bu yüzdendir; `npm run build` ile alınan üretim sürümünde olmaz.

### 3.5 Context: her yerden erişilen veri

Giriş yapmış kullanıcı bilgisi neredeyse her sayfada lazım. Bunu her bileşene prop olarak tek tek geçirmek yerine **Context** kullanıyoruz:

```tsx
// main.tsx: AuthProvider uygulamanın tamamını sarar
<AuthProvider>
  <App />
</AuthProvider>

// Herhangi bir sayfada:
const { user, logout } = useAuth()
```

`src/auth/AuthContext.tsx` dosyası, Context'in nasıl kurulduğunu satır satır açıklıyor.

### 3.6 Özel hook'lar: tekrarlanan mantığı paketlemek

`use` ile başlayan ve içinde başka hook'lar kullanan fonksiyonlara **hook** denir. Her sayfada "yükleniyor mu, hata var mı, veri ne?" üçlüsü gerektiği için bunu `useApi` olarak bir kez yazdık:

```tsx
// src/pages/shared/TasksPage.tsx
const { data, loading, error, reload } = useApi(() => taskApi.list(), [])

if (loading) return <Loading />
return <ErrorAlert message={error} /> ... data?.map(...)
```

Butonlar için de `useAction` var: işlem sürerken `busy` true olur (buton kilitlenir), hata olursa `error` dolar.

```tsx
const action = useAction()
<button disabled={action.busy} onClick={() => action.run(() => meetingApi.approve(m.id))}>Onayla</button>
```

**Hook kuralları:**
1. Hook'lar yalnızca bileşenin **en üst seviyesinde** çağrılır; `if`, döngü veya iç fonksiyon içinde çağrılmaz.
2. Hook'lar yalnızca bileşenlerde veya başka hook'larda çağrılır.

Bu yüzden `MeetingsPage.tsx`'te `StudentMeetings` bileşeni önce bütün `useState`/`useApi` çağrılarını yapar, **sonra** `if (!user?.groupId) return ...` ile erken çıkar.

### 3.7 Router: adres → sayfa

`react-router-dom` adres çubuğundaki yola göre hangi bileşenin çizileceğine karar verir:

```tsx
// src/App.tsx (sadeleştirilmiş)
<Routes>
  <Route path="/giris" element={<LoginPage />} />
  <Route element={<RequireRole />}>                         {/* giriş zorunlu */}
    <Route element={<Layout />}>                            {/* menü + çerçeve */}
      <Route index element={<DashboardPage />} />           {/* "/" */}
      <Route element={<RequireRole roles={['OGRENCI']} />}> {/* yalnız öğrenci */}
        <Route path="grubum" element={<GroupPage />} />
      </Route>
    </Route>
  </Route>
</Routes>
```

İç içe route'larda dıştaki bileşen (`Layout`) çerçeveyi çizer; içteki sayfa, dıştakinin `<Outlet />` yazdığı yere yerleşir.

Diğer araçlar:

| Araç | Ne işe yarar | Örnek dosya |
|---|---|---|
| `<Link to="/grubum">` | Sayfayı yenilemeden geçiş yapan bağlantı | `ProjectsPage.tsx` |
| `<NavLink>` | Aktif sayfadaysa otomatik `active` sınıfı alır | `Layout.tsx` |
| `useNavigate()` | Koddan yönlendirme (ör. girişten sonra) | `LoginPage.tsx` |
| `useParams()` | Adresteki `:id` değerini okur | `TaskDetailPage.tsx` |
| `<Navigate to="/giris" />` | Çizildiği anda yönlendirir | `RequireRole.tsx` |

> **Güvenlik notu:** `RequireRole` yalnızca kullanıcı deneyimi içindir. Asıl kontrol backend'dedir: yetkisi olmayan bir istek, ön yüz atlatılsa bile sunucudan 403 alır.

### 3.8 Formlar: "controlled input"

React'te form alanının değeri state'te tutulur, alan da o state'i gösterir:

```tsx
const [email, setEmail] = useState('')
<input value={email} onChange={(e) => setEmail(e.target.value)} />
```

Form gönderildiğinde tarayıcının sayfayı yenilemesini engellemek için `e.preventDefault()` çağrılır:

```tsx
async function submit(e: FormEvent) {
  e.preventDefault()
  await action.run(() => authApi.changePassword(current, next))
}
<form onSubmit={submit}>...</form>
```

`required`, `minLength`, `type="email"` gibi HTML doğrulamaları ilk savunma hattıdır; aynı kurallar backend'de de (`@NotBlank`, `@Size`) tekrar kontrol edilir.

### 3.9 Liste çizmek ve `key`

Diziyi ekrana basmak için `map` kullanılır. Her elemana benzersiz bir `key` verilmelidir:

```tsx
{data?.map((n) => (
  <li key={n.id}>{n.message}</li>
))}
```

`key`, React'in hangi satırın eklendiğini, silindiğini veya yer değiştirdiğini anlamasını sağlar. Dizinin sırasını (`index`) key olarak vermek, liste sıralanınca veya eleman silinince yanlış satırın güncellenmesine yol açabilir; veritabanı `id`'si en iyi seçimdir.

### 3.10 Koşullu çizim

```tsx
{loading && <Loading />}                          // loading true ise çiz
{data?.length === 0 && <EmptyState ... />}        // liste boşsa
{sent ? <div>Gönderildi</div> : <button>...</button>} // ya o ya bu
```

`data?.length` içindeki `?.` (optional chaining): `data` henüz `null` ise hata vermek yerine `undefined` döner.

---

## 4. Bir tıklamanın yolculuğu

Öğrenci **Randevular** sayfasında bir saat seçip "Talep et"e bastığında neler oluyor?

```
[Tarayıcı]                                          [Spring Boot]                      [SQL Server]
MeetingsPage.tsx
  requestMeeting(e)
    └─ action.run(...)              useAction: busy=true
        └─ meetingApi.request(slotId, topic)        endpoints.ts
            └─ api.post('/api/meetings', {...})     client.ts
                └─ fetch + "Authorization: Bearer <token>"
                        ──── HTTP POST ────────────▶ SecurityConfig: token geçerli mi?
                                                     MeetingController.request()
                                                     MeetingService.request()
                                                       ├─ AccessGuard: öğrenci mi, grubu var mı?
                                                       ├─ saat danışmanına mı ait, boş mu?
                                                       └─ meetingRepository.save() ──────▶ INSERT meetings
                                                     NotificationService: danışmana bildirim ──▶ INSERT notifications
                        ◀─── 201 Created + JSON ─────
                └─ response.json()
    created !== undefined →
      setSuccess(...), meetings.reload(), slots.reload()
  React ekranı yeniden çizer: yeni talep "Beklemede" rozetiyle listede görünür
```

Hata olursa (ör. o saati başka bir grup az önce aldıysa) sunucu `409` ve şu gövdeyi döner:

```json
{ "status": 409, "code": "SLOT_TAKEN", "detail": "Bu saat artık müsait değil; lütfen başka bir saat seçin." }
```

`client.ts` bunu bir `ApiError`'a çevirir, `useAction` yakalayıp `action.error`'a yazar, sayfadaki `<ErrorAlert>` de mesajı gösterir. Sayfanın kendisi hiçbir `try/catch` yazmak zorunda kalmaz.

---

## 5. TypeScript notları

TypeScript, JavaScript'e **tip** ekler. Hatalar tarayıcıda değil, `npm run build` sırasında yakalanır.

```ts
// src/api/types.ts
export type MeetingStatus = 'BEKLEMEDE' | 'ONAYLANDI' | 'REDDEDILDI' | ...   // union: yalnızca bu değerlerden biri

export type Meeting = {
  id: number
  status: MeetingStatus
  topic: string | null        // null olabilir
  ...
}
```

| Yazım | Anlamı |
|---|---|
| `string \| null` | ya metin ya null |
| `x?: number` | alan hiç olmayabilir |
| `Record<string, string>` | anahtarı ve değeri metin olan sözlük (`ICONS` nesnesi) |
| `useState<Me \| null>(null)` | **generic**: state'in tipini açıkça söylemek |
| `api.get<Meeting[]>(...)` | dönecek verinin tipi `Meeting[]` |
| `import type { Me } from ...` | yalnızca tip içe aktarılıyor; derlenmiş JS'te bu satır silinir |
| `user!.id` | "bunun null olmadığından eminim" (dikkatli kullan) |

Projede `enum` yerine **string union** kullanıldı: derleyici ayarı (`erasableSyntaxOnly`) yalnızca derlemede tamamen silinebilen TypeScript yazımlarına izin veriyor; union tipler bu şartı sağlar ve backend'in JSON'da gönderdiği `"BEKLEMEDE"` gibi değerlerle birebir örtüşür.

**Backend ile eşleşme:** `types.ts`'teki her tip, `src/main/java/.../dto/` altındaki bir Java `record`'unun karşılığıdır. Backend'e yeni bir alan eklersen buraya da eklemelisin; eklemezsen TypeScript seni o alanı kullanırken uyarır.

---

## 6. Bootstrap ile görünüm

Görünüm için ayrı CSS yazmak yerine **Bootstrap 5** sınıfları kullanıldı. En çok geçenler:

| Sınıf | Ne yapar |
|---|---|
| `container`, `row`, `col-md-6` | 12 sütunlu ızgara; `md` ve üstü ekranda yarım genişlik |
| `d-flex gap-2 justify-content-between align-items-center` | esnek kutu yerleşimi |
| `card`, `card-header`, `card-body` | kart |
| `btn btn-primary`, `btn-outline-danger`, `btn-sm` | butonlar |
| `form-control`, `form-select`, `form-label` | form alanları |
| `badge text-bg-success` | renkli rozet |
| `table table-hover`, `table-responsive` | tablo; dar ekranda yatay kaydırma |
| `mb-3`, `mt-4`, `p-2`, `me-1` | boşluk: **m**argin/**p**adding, **t**op/**b**ottom/**s**tart/**e**nd, 0–5 |
| `text-secondary`, `small`, `fw-semibold` | yazı rengi ve kalınlığı |

İkonlar **Bootstrap Icons**'tan gelir: `<i className="bi bi-calendar-event" />`. İkon listesi: [icons.getbootstrap.com](https://icons.getbootstrap.com/).

Uygulamaya özgü birkaç kural `src/styles.css` içinde (ör. yazdırırken menüyü gizleyen `.no-print`).

---

## 7. Sık yapılan hatalar

| Belirti | Neden | Çözüm |
|---|---|---|
| Butona basınca hiçbir şey değişmiyor | State doğrudan değiştirildi (`list.push(x)`) | Yeni dizi/nesne ver: `setList([...list, x])` |
| Konsolda "Each child in a list should have a unique key" | `map` içinde `key` yok | `key={item.id}` ekle |
| "Rendered more hooks than during the previous render" | Hook bir `if`'in içinde veya erken `return`'den sonra | Bütün hook çağrılarını bileşenin en üstüne taşı |
| Effect sonsuz döngüye giriyor | Effect içinde değiştirilen state, bağımlılık listesinde | Bağımlılıkları gözden geçir; çoğu zaman effect'e hiç gerek yoktur |
| `onClick={sil(id)}` tıklamadan çalışıyor | Fonksiyon **çağrıldı**, verilmedi | `onClick={() => sil(id)}` |
| İstek konsolda iki kez görünüyor | Geliştirme modundaki `StrictMode` | Normal; üretimde olmaz |
| "Sunucuya ulaşılamadı" | Backend kapalı | Spring Boot'u başlat (8080) |

Lint aracı (`npm run lint`), React'in yeni kurallarına aykırı yazımları da yakalar. Geliştirme sırasında yakaladığı üç örnek ve düzeltmeleri:

1. **Effect içinde hemen `setState`** (`AuthContext.tsx`): başlangıç değeri effect'te değil, doğrudan `useState(() => tokenStore.get() !== null)` ile hesaplandı.
2. **Sayfa değişince menüyü effect ile kapatmak** (`Layout.tsx`): menü, bağlantıya tıklandığı anda (`onClick`) kapatılıyor; effect'e gerek kalmadı.
3. **Render sırasında `Date.now()`** (`MeetingsPage.tsx`): render fonksiyonu "saf" olmalı (aynı girdiyle aynı çıktı). Zaman, bileşen açılırken bir kez `useState(() => Date.now())` ile alındı.

---

## 8. Kendin dene

Küçükten büyüğe birkaç alıştırma. Her birinde `npm run dev` açıkken dosyayı kaydettiğin anda tarayıcı güncellenir.

1. **Metin değiştir:** `pages/NotFoundPage.tsx`'teki mesajı değiştir. (JSX'e ısınma)
2. **Yeni rozet rengi:** `utils/format.ts`'teki `meetingStatus` eşlemesinde "İptal" rengini `secondary` yerine `dark` yap. Hangi sayfaların etkilendiğine bak. (tek yerden değişiklik)
3. **Sayaç ekle:** `NotificationsPage.tsx` başlığına toplam bildirim sayısını da yaz: `data?.length`. (props/state okuma)
4. **Filtre ekle:** `TasksPage.tsx`'e "Yalnızca gecikenler" onay kutusu ekle; işaretliyken `daysUntil(t.dueDate) < 0` olanları göster. (state + `filter`)
5. **Yeni sayfa:** "Hakkında" sayfası oluştur (`pages/shared/AboutPage.tsx`), `App.tsx`'e `/hakkinda` route'u, `Layout.tsx`'teki menüye de bağlantısını ekle. (router)
6. **Uçtan uca özellik:** Backend'e `GET /api/meetings/count` gibi küçük bir uç nokta ekle → `endpoints.ts`'e karşılığını yaz → bir sayfada `useApi` ile göster. (tam yığın)

---

## Komutlar

```bash
cd frontend
npm install        # bağımlılıkları indir (ilk sefer)
npm run dev        # geliştirme sunucusu: http://localhost:5173
npm run lint       # kod kalitesi kontrolü
npm run build      # TypeScript kontrolü + üretim derlemesi → src/main/resources/static
```

## Daha fazlası

- React'in resmi öğrenme kaynağı: [react.dev/learn](https://react.dev/learn) (özellikle "Thinking in React" ve "You Might Not Need an Effect" bölümleri)
- React Router: [reactrouter.com](https://reactrouter.com/)
- TypeScript el kitabı: [typescriptlang.org/docs/handbook](https://www.typescriptlang.org/docs/handbook/intro.html)
- Bootstrap: [getbootstrap.com/docs/5.3](https://getbootstrap.com/docs/5.3/getting-started/introduction/)
