# OGVPTS – Öğrenci–Danışman Görüşme ve Proje Takip Sistemi

Maltepe Üniversitesi Yazılım Mühendisliği dersi kapsamında geliştirilen, öğrenci grupları ile danışman öğretim üyeleri arasındaki görüşme planlama, görev takibi, teslim ve geri bildirim süreçlerini tek ortamda yöneten uygulama.

Proje takibi Jira üzerinden yürütülmektedir (proje anahtarı: **OGVPTS**).

## Kapsam

- İki kullanıcı rolü: öğrenci ve danışman
- En fazla iki kişilik proje grupları, proje bilgileri ve danışman eşleştirmesi
- Danışman müsait saatleri, öğrenci görüşme talepleri ve takibi
- Danışmanın gruplara son teslim tarihli görev ataması
- Öğrenci teslimleri; danışman onayı veya düzeltme isteği, önceki teslim ve geri bildirimlerin korunması
- Yetkilendirme: öğrenciler yalnızca kendi grubunun kayıtlarına erişir

## Veritabanı

Veritabanı Microsoft SQL Server üzerinde kurulur.

**Kurulum:**

1. SQL Server Management Studio (SSMS) ile sunucuya bağlan.
2. `New Query` ile boş bir sorgu penceresi aç.
3. `ogvpts_schema_sqlserver.sql` dosyasının tamamını yapıştırıp çalıştır (F5).

Betik `OGVPTS` veritabanını oluşturur ve 13 tabloyu kurar:

`users`, `project_groups`, `group_members`, `projects`, `project_milestones`, `project_comments`, `availability_slots`, `meetings`, `meeting_notes`, `tasks`, `submissions`, `feedback`, `notifications`

**Şemaya işlenen iş kuralları:**

- Bir grupta en fazla 2 öğrenci olabilir.
- Bir öğrenci yalnızca tek bir grupta yer alır.
- Aynı saate iki onaylı görüşme eklenemez.
- Yüklenen dosya en fazla 20 MB olabilir.
- Her teslim ayrı kayıt olarak tutulur; önceki teslimler ve geri bildirimler silinmez.

## Uygulama (Java Spring Boot)

**Teknolojiler:** Java 17+, Spring Boot 3.5, Spring Data JPA (Hibernate), Microsoft SQL Server JDBC sürücüsü, Lombok, Maven.

**Klasör yapısı:**

```
src/main/java/edu/maltepe/ogvpts/
├── OgvptsApplication.java      # uygulamanın giriş noktası
├── model/                      # 13 tablonun JPA entity sınıfları
│   ├── enums/                  # rol ve durum değerleri (UserRole, MeetingStatus, ...)
│   └── converter/              # enum ↔ veritabanı değeri dönüşümü (OGRENCI ↔ ogrenci)
├── repository/                 # her entity için Spring Data JPA repository
├── config/                     # veritabanı bağlantı kontrolü
└── controller/                 # REST uç noktaları (/api/health/db)
src/main/resources/application.properties   # bağlantı ayarları
```

| Tablo | Model sınıfı |
|---|---|
| users | `User` |
| project_groups | `ProjectGroup` |
| group_members | `GroupMember` (+ `GroupMemberId` bileşik anahtar) |
| projects | `Project` |
| project_milestones | `ProjectMilestone` |
| project_comments | `ProjectComment` |
| availability_slots | `AvailabilitySlot` |
| meetings | `Meeting` |
| meeting_notes | `MeetingNote` |
| tasks | `Task` |
| submissions | `Submission` |
| feedback | `Feedback` |
| notifications | `Notification` |

Şema `ogvpts_schema_sqlserver.sql` betiğiyle kurulur; Hibernate tablolara dokunmaz (`ddl-auto=none`).

### Kurulum ve çalıştırma

1. **Veritabanı:** Yukarıdaki adımlarla `OGVPTS` veritabanını oluştur.
2. **SQL Server ayarları** (bir kez yapılır):
   - SSMS → sunucuya sağ tık → *Properties → Security* → **SQL Server and Windows Authentication mode** seç.
   - *Security → Logins → sa* → şifre belirle, *Status* sekmesinden **Login: Enabled** yap.
   - *SQL Server Configuration Manager → SQL Server Network Configuration → Protocols* → **TCP/IP: Enabled**, *IP Addresses → IPAll → TCP Port = 1433*.
   - SQL Server servisini yeniden başlat.
3. **Bağlantı bilgileri:** `src/main/resources/application-local.properties.example` dosyasını aynı klasöre `application-local.properties` adıyla kopyala ve şifreni yaz. Bu dosya git'e gönderilmez.
4. **Çalıştır:** Projeyi IntelliJ IDEA ile aç (Maven projesi olarak algılanır), `OgvptsApplication` sınıfını çalıştır.
   Komut satırından: `mvn spring-boot:run`
5. **Kontrol:** Konsolda `Veritabanı bağlantısı başarılı: OGVPTS ...` satırı görünür. Tarayıcıdan `http://localhost:8080/api/health/db` adresi her tablonun kayıt sayısını döndürür.

## Çalışma düzeni

- Çalışma 1 haftalık sprintlerle yürütülür: Sprint 1 gereksinim analizi, Sprint 2 diyagramlar, Sprint 3 veritabanı, Sprint 4 proje altyapısı.
- Commit mesajları ve dal adları Jira görev anahtarını içerir. Örnekler: `OGVPTS-54: veritabanı şeması eklendi`, `feature/OGVPTS-56-grup-olusturma`.

## Geliştirici

Mehmet Caner Kahraman – Maltepe Üniversitesi, Yazılım Mühendisliği
