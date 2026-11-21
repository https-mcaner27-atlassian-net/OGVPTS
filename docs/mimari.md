# Mimari

OGVPTS, katmanlı bir **Spring Boot** uygulamasıdır. Veriler **Microsoft SQL Server** üzerindeki `OGVPTS` veritabanında tutulur; uygulama bu veritabanına **Spring Data JPA (Hibernate)** ile erişir.

![Uygulama mimarisi](images/mimari.png)

## Katmanlar

| Katman | Paket | Görevi |
|---|---|---|
| Sunum (REST) | `controller/` | HTTP isteklerini karşılar, JSON yanıt döndürür |
| İş / servis | `config/` (ileride `service/`) | İş kurallarını ve kontrolleri yürütür |
| Veri erişimi | `repository/` | Her tablo için bir `JpaRepository`; SQL yazmadan kayıt okuma/yazma |
| Model | `model/` | Tabloların Java karşılıkları (entity), rol/durum enum'ları ve dönüştürücüler |
| Altyapı | Spring Data JPA, Hibernate, HikariCP, mssql-jdbc | ORM, bağlantı havuzu ve SQL Server sürücüsü |

## Paket yapısı

```
src/main/java/edu/maltepe/ogvpts/
├── OgvptsApplication.java          # giriş noktası (@SpringBootApplication)
├── controller/
│   └── HealthController.java       # GET /api/health/db
├── config/
│   ├── DatabaseHealthService.java  # bağlantı + 13 tablonun model eşleşmesi kontrolü
│   └── DatabaseConnectionCheck.java# uygulama açılırken kontrolü çalıştırır
├── repository/                     # 13 × JpaRepository
├── model/                          # 13 entity + GroupMemberId (bileşik anahtar)
│   ├── enums/                      # UserRole, ProjectStatus, MeetingStatus, TaskStatus, FeedbackDecision
│   └── converter/                  # enum ↔ veritabanı değeri (OGRENCI ↔ ogrenci)
src/main/resources/
├── application.properties                  # ortak ayarlar
└── application-local.properties            # yerel kullanıcı/şifre (git'e gönderilmez)
```

## Bir isteğin yolu

`GET /api/health/db` örneği:

1. **HealthController** isteği alır ve `DatabaseHealthService.check()` metodunu çağırır.
2. **DatabaseHealthService** HikariCP havuzundan bir bağlantı alır; veritabanı adını ve sunucu sürümünü okur.
3. 13 **repository**'nin her biri için `findAll(PageRequest.of(0, 1))` çalıştırılır. Bu sorgu modeldeki bütün sütunları seçtiği için, sütun adı veya tipi şemayla uyuşmazsa hata burada ortaya çıkar. Ardından `count()` ile kayıt sayısı alınır.
4. Sonuç JSON olarak döner (bkz. [API](api.md)).

## Tasarım kararları

- **Şemanın sahibi SQL betiğidir.** Tablolar `ogvpts_schema_sqlserver.sql` ile kurulur; Hibernate tabloları oluşturmaz veya değiştirmez (`spring.jpa.hibernate.ddl-auto=none`). Böylece iş kuralları (UNIQUE, CHECK, tetikleyici) tek bir yerde durur.
- **Rol ve durum değerleri enum'dur.** Veritabanındaki küçük harfli değerler (`ogrenci`, `beklemede` …) `LowercaseEnumConverter` ile Java enum'larına çevrilir. Dönüşümde `Locale.ROOT` kullanılır; Türkçe yerel ayarda `I → ı` hatası oluşmaz.
- **`meetings.is_active` uygulamada hesaplanır.** Durum `onaylandi` veya `tamamlandi` ise `true` olur (`@PrePersist/@PreUpdate`). Bu alan, aynı saate iki onaylı görüşme eklenmesini engelleyen filtreli benzersiz indeksi (BR-02) besler.
- **Lombok kullanılmaz.** Yeni JDK sürümleriyle derleme hatası verdiği için getter/setter'lar açıkça yazılmıştır.
- **Uygulamaya özel veritabanı kullanıcısı.** Uygulama `sa` yerine yalnızca OGVPTS'de okuma/yazma yetkisi olan `ogvpts_app` kullanıcısıyla bağlanır.

## Teknolojiler

| | Sürüm |
|---|---|
| Java | 17+ (geliştirmede OpenJDK 27 ile test edildi) |
| Spring Boot | 3.5.6 |
| Hibernate ORM | 6.6 |
| SQL Server JDBC sürücüsü | mssql-jdbc 12.10 |
| Veritabanı | Microsoft SQL Server 2025 (17.0) |
| Derleme | Maven |
