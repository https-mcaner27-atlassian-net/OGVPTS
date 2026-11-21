# API

Backend `http://localhost:8080` adresinde çalışan bir REST API'dir. İstek ve yanıt gövdeleri JSON'dur (dosya yükleme hariç).

> **Canlı belge:** Uygulama çalışırken **http://localhost:8080/swagger-ui.html** adresinde bütün uç noktaları görebilir, sağ üstteki *Authorize* ile token girip doğrudan deneyebilirsin.

## Kimlik doğrulama

1. `POST /api/auth/login` ile e-posta ve şifre gönderilir; yanıtta bir **JWT** gelir.
2. Sonraki her istekte bu token başlığa eklenir:

```
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

Token **8 saat** geçerlidir (`ogvpts.jwt.expiration-hours`). İçinde kullanıcının `id`'si ve rolü (`OGRENCI`, `DANISMAN`, `YONETICI`) bulunur; sunucu oturum tutmaz (stateless).

**Erişim katmanları**

| Katman | Nerede | Ne kontrol eder |
|---|---|---|
| Giriş | `SecurityConfig` | `/api/**` için geçerli token; `/api/admin/**` için `YONETICI` rolü |
| Rol | Servislerdeki `AccessGuard.requireStudent/requireAdvisor` | İşlemi yapmaya rolü uygun mu? |
| Kayıt (FR-16) | `AccessGuard.requireMember/requireGroupAdvisor/requireCanView` | Öğrenci yalnızca **kendi grubunun**, danışman yalnızca **sorumlu olduğu grupların** kayıtlarına erişir |

### Örnek: giriş

```http
POST /api/auth/login
Content-Type: application/json

{ "email": "ali.demir@ogvpts.test", "password": "Demo1234!" }
```

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "expiresInHours": 8,
  "user": {
    "id": 4, "fullName": "Ali Demir", "email": "ali.demir@ogvpts.test", "role": "OGRENCI",
    "groupId": 1, "groupName": "Akıllı Kampüs Ekibi", "advisorName": "Dr. Ayşe Yıldız"
  }
}
```

## Hata biçimi

Bütün hatalar aynı yapıda döner ([RFC 9457 Problem Details](https://www.rfc-editor.org/rfc/rfc9457)); arayüz `detail` alanındaki Türkçe mesajı doğrudan gösterir, `code` alanına göre de özel davranabilir:

```json
{
  "type": "about:blank",
  "title": "Conflict",
  "status": 409,
  "detail": "Bu saat artık müsait değil; lütfen başka bir saat seçin.",
  "instance": "/api/meetings",
  "code": "SLOT_TAKEN"
}
```

| HTTP | `code` | Ne zaman |
|---|---|---|
| 400 | `VALIDATION` | Zorunlu alan boş, biçim hatalı. Ek alan: `fields` (alan → mesaj) |
| 400 | `BAD_REQUEST` | Geçersiz istek (ör. geçmiş tarih, izin verilmeyen dosya uzantısı) |
| 401 | `BAD_CREDENTIALS` | E-posta veya şifre yanlış |
| 401 | — | Token yok, süresi dolmuş veya imzası geçersiz |
| 403 | `FORBIDDEN` | Rol veya kayıt yetkisi yok (FR-16) |
| 404 | `NOT_FOUND` | Kayıt yok |
| 409 | `GROUP_FULL`, `ALREADY_IN_GROUP` | Grupta 2 kişi var / öğrenci zaten bir grupta |
| 409 | `NO_GROUP`, `NO_ADVISOR`, `PROJECT_EXISTS` | Grup veya danışman olmadan işlem; ikinci proje |
| 409 | `SLOT_OVERLAP`, `SLOT_PAST`, `SLOT_IN_USE` | Müsaitlik çakışması, geçmiş saat, dolu saati silme |
| 409 | `SLOT_TAKEN`, `SLOT_CONFLICT` | Saat dolu / aynı saatte onaylı başka görüşme var (BR-02) |
| 409 | `INVALID_STATUS`, `CANCEL_TOO_LATE`, `NOT_STARTED` | Durum makinesine aykırı geçiş; 24 saatten az kala iptal (BR-03); başlamamış görüşmeyi tamamlama |
| 409 | `NOT_LATEST`, `ALREADY_REVIEWED`, `TASK_APPROVED` | Eski teslime geri bildirim, ikinci geri bildirim, onaylı göreve yeni teslim |
| 409 | `ADVISOR_QUOTA_FULL` | Danışman kontenjanı dolu (FR-10); `force: true` ile yine de atanabilir |
| 409 | `EMAIL_EXISTS`, `LAST_MILESTONE` | E-posta kayıtlı; projenin son aşaması silinemez |
| 409 | `DB_CONSTRAINT` | Veritabanı kısıtı (UNIQUE/CHECK/tetikleyici) devreye girdi; mesaj Türkçeye çevrilir |
| 413 | `FILE_TOO_LARGE` | Dosya 20 MB'tan büyük (FR-06, FR-14) |

## Uç noktalar

Rol sütunu: **Ö** öğrenci, **D** danışman, **Y** yönetici, **herkes** giriş gerektirmez. Tarihler ISO biçimindedir: `2026-11-25`, `2026-11-25T10:30:00`.

### Oturum ve hesap

| Yöntem | Yol | Rol | Gövde | Açıklama |
|---|---|---|---|---|
| `POST` | `/api/auth/login` | herkes | `{ email, password }` | Giriş; token döner (FR-01) |
| `GET` | `/api/auth/me` | Ö D Y | | Oturumdaki kullanıcı, grubu ve danışmanı |
| `PUT` | `/api/auth/password` | Ö D Y | `{ currentPassword, newPassword }` | Şifre değiştir (en az 8 karakter) |
| `POST` | `/api/me/data-deletion-request` | Ö D Y | | KVKK veri silme talebi; yöneticilere bildirim gider (NFR-03) |

### Grup

| Yöntem | Yol | Rol | Gövde | Açıklama |
|---|---|---|---|---|
| `GET` | `/api/groups/mine` | Ö | | Grubum; grubu yoksa `204 No Content` |
| `POST` | `/api/groups` | Ö | `{ name }` | Grup kur; kuran öğrenci otomatik üye olur (FR-12) |
| `POST` | `/api/groups/{id}/members` | Ö | `{ email }` | Gruba arkadaş ekle; en fazla 2 kişi, öğrenci yalnızca tek grupta (FR-12, BR-01) |
| `GET` | `/api/groups` | D | | Sorumlu olduğum gruplar |
| `GET` | `/api/groups/{id}` | Ö D Y | | Grup ayrıntısı (kayıt yetkisi kontrol edilir) |

### Proje ve aşamalar

| Yöntem | Yol | Rol | Gövde | Açıklama |
|---|---|---|---|---|
| `POST` | `/api/projects` | Ö | `{ title, description, milestones: [{ title, dueDate }] }` | Proje oluştur (FR-05, FR-06) |
| `GET` | `/api/projects/mine` | Ö | | Grubumun projesi; yoksa `204` |
| `GET` | `/api/projects` | D | | Gruplarımın projeleri: ilerleme yüzdesi, sıradaki teslim tarihi (FR-07) |
| `GET` | `/api/projects/{id}` | Ö D Y | | Proje ayrıntısı, aşamalar, yorumlar |
| `PUT` | `/api/projects/{id}` | Ö | `{ title, description }` | Başlık/açıklama güncelle |
| `POST` | `/api/projects/{id}/milestones` | Ö | `{ title, dueDate }` | Aşama ekle |
| `PATCH` | `/api/milestones/{id}/status` | Ö | `{ status }` | İlerleme güncelleme (FR-06). Aşama durumu: `BASLAMADI`, `DEVAM_EDIYOR`, `TAMAMLANDI`. Proje durumu aşamalardan hesaplanır |
| `DELETE` | `/api/milestones/{id}` | Ö | | Aşama sil (son aşama silinemez) |
| `POST` | `/api/projects/{id}/comments` | D | `{ body }` | Danışman yorumu; gruba bildirim gider (FR-07) |

### Müsaitlik ve görüşmeler

| Yöntem | Yol | Rol | Gövde | Açıklama |
|---|---|---|---|---|
| `GET` | `/api/slots/mine` | D | | Saatlerim ve doluluk durumu |
| `GET` | `/api/slots/mine/free` | D | | Boş saatlerim (yeni saat önermek için) |
| `POST` | `/api/slots` | D | `{ start, end }` | Müsait saat ekle; çakışma kontrol edilir |
| `DELETE` | `/api/slots/{id}` | D | | Boş saati sil |
| `GET` | `/api/slots/available` | Ö | | Danışmanımın gelecekteki boş saatleri (FR-02) |
| `GET` | `/api/meetings?status=&from=&to=` | Ö D | | Görüşmelerim, filtreler isteğe bağlı (FR-09) |
| `GET` | `/api/meetings/{id}` | Ö D | | Görüşme ayrıntısı ve notu |
| `POST` | `/api/meetings` | Ö | `{ slotId, topic }` | Randevu talebi → `BEKLEMEDE` (FR-02) |
| `POST` | `/api/meetings/{id}/approve` | D | | Onayla → `ONAYLANDI` (FR-03, BR-02) |
| `POST` | `/api/meetings/{id}/reject` | D | | Reddet → `REDDEDILDI` (FR-03) |
| `POST` | `/api/meetings/{id}/propose` | D | `{ slotId }` | Başka saat öner → `YENI_SAAT_ONERILDI` (FR-03) |
| `POST` | `/api/meetings/{id}/accept-proposal` | Ö | | Önerilen saati kabul → `ONAYLANDI` |
| `POST` | `/api/meetings/{id}/decline-proposal` | Ö | | Önerilen saati reddet → `REDDEDILDI` |
| `POST` | `/api/meetings/{id}/cancel` | Ö D | | Beklemede, yeni saat önerilmiş veya onaylı görüşmeyi iptal → `IPTAL`; onaylıysa en geç 24 saat önce (BR-03) |
| `POST` | `/api/meetings/{id}/complete` | D | | Görüşme yapıldı → `TAMAMLANDI` (başlangıç saati geçmiş olmalı) |
| `PUT` | `/api/meetings/{id}/note` | D | `{ note, decisions }` | Görüşme notu; yalnızca görüşmeyi yapan danışman, tamamlanmış görüşmeye (FR-04, BR-04) |

**Randevu durum makinesi** (Drive'daki *Randevu State* diyagramı):

| Şu anki durum | İşlem | Kim | Yeni durum |
|---|---|---|---|
| — | `POST /meetings` | Ö | `BEKLEMEDE` |
| `BEKLEMEDE` | `approve` | D | `ONAYLANDI` |
| `BEKLEMEDE` | `reject` | D | `REDDEDILDI` |
| `BEKLEMEDE` | `propose` | D | `YENI_SAAT_ONERILDI` |
| `YENI_SAAT_ONERILDI` | `accept-proposal` | Ö | `ONAYLANDI` |
| `YENI_SAAT_ONERILDI` | `decline-proposal` | Ö | `REDDEDILDI` |
| `BEKLEMEDE`, `YENI_SAAT_ONERILDI` | `cancel` | Ö D | `IPTAL` |
| `ONAYLANDI` | `cancel` (en az 24 saat kala) | Ö D | `IPTAL` |
| `ONAYLANDI` | `complete` (başlangıç saati geçtikten sonra) | D | `TAMAMLANDI` |
| `TAMAMLANDI` | `note` | D | (not kaydedilir) |

Yalnızca `ONAYLANDI` ve `TAMAMLANDI` görüşmeler saati "kilitler"; aynı saate ikinci bir onaylı görüşme veritabanındaki filtreli benzersiz indeksle de engellenir (BR-02).

### Görev, teslim ve geri bildirim

| Yöntem | Yol | Rol | Gövde | Açıklama |
|---|---|---|---|---|
| `POST` | `/api/tasks` | D | `{ groupId, title, description, dueDate }` | Gruba görev ata (FR-13) |
| `GET` | `/api/tasks` | Ö D | | Görevlerim / gruplarıma verdiğim görevler |
| `GET` | `/api/tasks/{id}` | Ö D | | Görev ve **bütün teslim sürümleri** ile geri bildirimleri |
| `POST` | `/api/tasks/{id}/submissions` | Ö | `multipart/form-data`, alan adı `file` | Teslim yükle: pdf, doc, docx, zip, pptx; en fazla 20 MB (FR-06). Her yükleme yeni sürüm olur, eskiler silinmez; son tarihten sonra gelenler `late: true` (FR-14) |
| `GET` | `/api/submissions/{id}/file` | Ö D | | Teslim dosyasını indir |
| `POST` | `/api/submissions/{id}/feedback` | D | `{ decision, comment }` | `ONAY` veya `DUZELTME` (düzeltmede yorum zorunlu). Yalnızca **en son** teslime ve bir kez (FR-15) |

Görev durumu teslimlerden hesaplanır: `ATANDI` → `TESLIM_EDILDI` → `ONAYLANDI` veya `DUZELTME_ISTENDI`.

### Bildirimler

| Yöntem | Yol | Rol | Açıklama |
|---|---|---|---|
| `GET` | `/api/notifications` | Ö D Y | Bildirimlerim (yeniden eskiye) (FR-08) |
| `GET` | `/api/notifications/unread-count` | Ö D Y | `{ count }` |
| `POST` | `/api/notifications/{id}/read` | Ö D Y | Okundu işaretle |
| `POST` | `/api/notifications/read-all` | Ö D Y | Tümünü okundu işaretle |

Zamanlanmış hatırlatmalar (`ReminderScheduler`, Europe/Istanbul saatiyle):

- **Görüşme:** 15 dakikada bir kontrol edilir; önümüzdeki 24 saat içinde başlayacak onaylı görüşmeler için danışmana ve grup üyelerine.
- **Teslim:** her gün 08:00'de; 2 gün sonra teslim tarihi olan, tamamlanmamış proje aşamaları ve onaylanmamış görevler için grup üyelerine.

Aynı hatırlatma bir kişiye iki kez gönderilmez.

### Yönetici (`/api/admin/**`, yalnızca Y)

| Yöntem | Yol | Gövde | Açıklama |
|---|---|---|---|
| `GET` | `/api/admin/users?role=` | | Kullanıcı listesi, rol filtresi isteğe bağlı |
| `POST` | `/api/admin/users` | `{ fullName, email, role, password }` | Kullanıcı oluştur |
| `PUT` | `/api/admin/users/{id}/password` | `{ password }` | Şifre sıfırla; kullanıcıya bildirim gider |
| `GET` | `/api/admin/groups` | | Bütün gruplar, üyeleri, danışmanı ve projesi |
| `GET` | `/api/admin/advisors` | | Danışman başına grup sayısı ve kontenjan (`ogvpts.advisor.max-groups`, varsayılan 8; FR-10) |
| `PUT` | `/api/admin/groups/{id}/advisor` | `{ advisorId, force }` | Grup–danışman eşleştirmesi; her grubun tek sorumlu danışmanı olur (FR-10, BR-01). Kontenjan doluysa `409 ADVISOR_QUOTA_FULL`; `force: true` ile yine de atanır |
| `GET` | `/api/admin/reports?from=&to=` | | Dönem raporu: danışman başına görüşmeler (durumlara göre), proje durum dağılımı (FR-11) |
| `GET` | `/api/admin/reports/excel?from=&to=` | | Aynı rapor `.xlsx` olarak |

### Sağlık kontrolü

| Yöntem | Yol | Rol | Açıklama |
|---|---|---|---|
| `GET` | `/api/health/db` | herkes | Veritabanı bağlantısı ve 13 tablonun modellerle eşleşmesi |

## Bilinen sınırlama

**FR-06'daki "aşamaya dosya ekleme"** için şemada bir tablo yok (`project_milestones` yalnızca başlık, tarih ve durum tutar). Şemayı değiştirmemek için proje dosyaları, danışmanın açtığı bir **görev** üzerinden teslim olarak yüklenir (FR-14); böylece sürüm geçmişi ve geri bildirim de korunur. İleride `milestone_files` tablosu eklenerek doğrudan aşamaya bağlanabilir.

---

## `GET /api/health/db`

Veritabanına bağlanır, sunucu bilgisini okur ve her tabloyu kendi JPA modeliyle sorgular. Bir sütun adı veya tipi şemayla uyuşmazsa hata verir; bu yüzden modellerin doğruluğunu da test eder.

**Başarılı yanıt — `200 OK`**

```json
{
  "database": "OGVPTS",
  "server": "Microsoft SQL Server 17.00.1000",
  "tables": {
    "users": 0,
    "project_groups": 0,
    "group_members": 0,
    "projects": 0,
    "project_milestones": 0,
    "project_comments": 0,
    "availability_slots": 0,
    "meetings": 0,
    "meeting_notes": 0,
    "tasks": 0,
    "submissions": 0,
    "feedback": 0,
    "notifications": 0
  },
  "status": "OK"
}
```

`tables` altındaki sayılar, her tablodaki kayıt sayısıdır.

**Hata yanıtı — `503 Service Unavailable`**

```json
{
  "status": "HATA",
  "message": "Login failed for user 'ogvpts_app'."
}
```

**Ekran görüntüsü** (14.11.2026, yerel ortam):

![GET /api/health/db yanıtı](images/health-check.png)

## Uygulama açılışındaki kontrol

Aynı kontrol uygulama açılırken de çalışır (`DatabaseConnectionCheck`). Başarılıysa konsolda şu satırlar görünür:

```
Veritabanı bağlantısı başarılı: OGVPTS (Microsoft SQL Server 17.00.1000)
Tablo kayıt sayıları: {users=0, project_groups=0, ...}
```

Bağlantı kurulamazsa uygulama açılmaz ve hatanın nedeni konsola yazılır. Sık görülen hatalar için bkz. [Kurulum → Sorun giderme](kurulum.md#sorun-giderme).
