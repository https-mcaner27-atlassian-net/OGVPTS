# Kurulum ve çalıştırma

## Gerekenler

- **JDK 17 veya üstü** (IntelliJ IDEA kendi JDK'sını indirebilir)
- **IntelliJ IDEA** (Maven desteği içinde gelir)
- **Microsoft SQL Server** ve **SQL Server Management Studio (SSMS)**
- **Git**

## 1. Projeyi al

```bash
git clone https://github.com/mcaner27/OGVPTS.git
```

IntelliJ'de *File → Open* ile klasörü aç. `pom.xml` algılanınca Maven bağımlılıkları otomatik iner.

## 2. Veritabanını kur

1. SSMS ile `localhost` sunucusuna bağlan.
2. *Yeni Sorgu* aç, `ogvpts_schema_sqlserver.sql` dosyasının tamamını yapıştır ve **F5** ile çalıştır.
3. `OGVPTS` veritabanı ve 13 tablo oluşur.

## 3. SQL Server'da TCP/IP'yi aç

Java uygulamaları SQL Server'a yalnızca **TCP/IP** üzerinden bağlanır. SSMS farklı bir yol kullandığı için TCP/IP kapalıyken de çalışır; bu yüzden bu adım kolayca gözden kaçar.

1. **SQL Server Configuration Manager**'ı aç.
2. *SQL Server Network Configuration → Protocols for MSSQLSERVER → TCP/IP* → sağ tık → **Enable**.
3. *TCP/IP → Properties → IP Addresses → IPAll → TCP Port* değerinin **1433** olduğunu kontrol et.
4. **Önemli:** *SQL Server Services → SQL Server (MSSQLSERVER)* → sağ tık → **Restart**. Ayar, servis yeniden başlamadan geçerli olmaz.

Kontrol etmek için SSMS'te:

```sql
EXEC xp_readerrorlog 0, 1, N'listening';
```

Sonuçta `... 1433` geçen bir satır görünmelidir.

## 4. Uygulama için veritabanı kullanıcısı oluştur

Sunucu *SQL Server and Windows Authentication mode* (karma mod) olmalıdır: SSMS'te sunucuya sağ tık → *Properties → Security*.

Ardından [Veritabanı → Veritabanı kullanıcısı](veritabani.md#veritabanı-kullanıcısı) bölümündeki betiği kendi belirlediğin şifreyle çalıştır.

## 5. Bağlantı ayarlarını yaz

`src/main/resources/application-local.properties.example` dosyasını aynı klasöre **`application-local.properties`** adıyla kopyala ve düzenle:

```properties
spring.datasource.url=jdbc:sqlserver://localhost:1433;databaseName=OGVPTS;encrypt=true;trustServerCertificate=true
spring.datasource.username=ogvpts_app
spring.datasource.password=<4. adımdaki şifre>
```

Bu dosya `.gitignore`'dadır, **GitHub'a gönderilmez**. Alternatif olarak `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` ortam değişkenleri de kullanılabilir.

## 6. Çalıştır

- IntelliJ: `OgvptsApplication.java` → `main` yanındaki yeşil ▶ → **Run**
- Komut satırı: `mvn spring-boot:run`

Tarayıcıda `http://localhost:8080/api/health/db` adresi `"status": "OK"` döndürmelidir (bkz. [API](api.md)).

## Sorun giderme

Geliştirme sırasında karşılaşılan hatalar ve çözümleri:

| Konsoldaki hata | Nedeni | Çözüm |
|---|---|---|
| `java.lang.ExceptionInInitializerError` … `com.sun.tools.javac.tree.EndPosTable` | Lombok'un yeni JDK sürümleriyle uyumsuzluğu | Projeden Lombok kaldırıldı; güncel kodu çek (`git pull`) ve *Maven → Reload* yap |
| `Lombok requires enabled annotation processing` | Eski sürümden kalan uyarı | Artık gerekmiyor; uyarıyı kapat |
| `The TCP/IP connection to the host localhost, port 1433 has failed … Connection refused` | TCP/IP kapalı veya servis yeniden başlatılmamış | [3. adım](#3-sql-serverda-tcpipyi-aç); özellikle **Restart** |
| `Login failed for user '…'` | Kullanıcı adı veya şifre yanlış ya da `application-local.properties` yok | 4. ve 5. adımları kontrol et |
| `Cannot open database "OGVPTS"` | Veritabanı kurulmamış veya kullanıcının yetkisi yok | 2. ve 4. adımlar |
| `Unable to determine Dialect without JDBC metadata` | Yukarıdaki bağlantı hatalarından birinin sonucu | Asıl hata bu satırın üstündedir; onu çöz |
