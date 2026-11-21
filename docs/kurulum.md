# Kurulum ve çalıştırma

## Gerekenler

- **JDK 17 veya üstü** (IntelliJ IDEA kendi JDK'sını indirebilir)
- **IntelliJ IDEA** (Maven desteği içinde gelir)
- **Microsoft SQL Server** ve **SQL Server Management Studio (SSMS)**
- **Node.js 22 LTS** (en az 20.19 veya 22.12; arayüz için). Kontrol: `node -v`
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

## 6. Backend'i çalıştır

### İlk denemede: demo verisiyle

Boş bir veritabanında, içinde örnek kullanıcılar, gruplar, proje, görev ve randevular olan bir ortamla başlamak için **`demo` profilini** aç:

- IntelliJ: sağ üstteki çalıştırma yapılandırması (*OgvptsApplication*) → **Edit Configurations…** → *Active profiles* alanına `demo` yaz → **Run**
- Komut satırı: `mvn spring-boot:run -Dspring-boot.run.profiles=demo`

Demo verisi **yalnızca `users` tablosu boşsa** eklenir; uygulamayı tekrar başlatınca ikinci kez eklenmez. Konsolda şu satır görünür:

```
Demo verisi oluşturuldu. Giriş: yonetici@ogvpts.test / ayse.yildiz@ogvpts.test / ali.demir@ogvpts.test — şifre Demo1234!
```

| Rol | E-posta | Durum |
|---|---|---|
| Yönetici | `yonetici@ogvpts.test` | |
| Danışman | `ayse.yildiz@ogvpts.test` | *Akıllı Kampüs Ekibi*'nin danışmanı |
| Danışman | `emre.kaya@ogvpts.test` | |
| Öğrenci | `ali.demir@ogvpts.test`, `zeynep.celik@ogvpts.test` | Aynı grupta; projeleri ve görevleri var |
| Öğrenci | `can.sahin@ogvpts.test` | Tek kişilik grup, henüz projesi yok |
| Öğrenci | `elif.aydin@ogvpts.test` | Grubu yok (grup kurmayı denemek için) |

Bütün demo hesaplarının şifresi **`Demo1234!`**. Bu hesaplar yalnızca yerel deneme içindir.

**Verileri sıfırlamak** (ör. demo verisini baştan oluşturmak) için uygulamayı durdur, SSMS'te aşağıdaki betiği çalıştır ve proje klasöründeki `uploads/` klasörünü sil. Tablolar yerinde kalır, yalnızca içleri boşalır; sıra, yabancı anahtarlar yüzünden önemlidir:

```sql
USE OGVPTS;
DELETE FROM feedback;           DELETE FROM submissions;        DELETE FROM tasks;
DELETE FROM meeting_notes;      DELETE FROM meetings;           DELETE FROM availability_slots;
DELETE FROM project_comments;   DELETE FROM project_milestones; DELETE FROM projects;
DELETE FROM group_members;      DELETE FROM project_groups;
DELETE FROM notifications;      DELETE FROM users;
```

Ardından uygulamayı `demo` profiliyle yeniden başlat.

### Gerçek kullanımda: ilk yönetici hesabı

Demo profili olmadan, boş veritabanında ilk **yönetici** hesabını ortam değişkenleriyle oluştur:

| Değişken | Örnek |
|---|---|
| `ADMIN_EMAIL` | `bolum@maltepe.edu.tr` |
| `ADMIN_PASSWORD` | en az 8 karakter |
| `ADMIN_NAME` | (isteğe bağlı) `Bölüm Sekreterliği` |

IntelliJ: *Edit Configurations… → Environment variables*. Uygulama açılırken yönetici yoksa bu hesap oluşturulur; yönetici zaten varsa değişkenler yok sayılır. Diğer kullanıcıları yönetici, arayüzdeki **Kullanıcılar** sayfasından ekler.

### Kontrol

- `http://localhost:8080/api/health/db` → `"status": "OK"` (bkz. [API](api.md))
- `http://localhost:8080/swagger-ui.html` → bütün uç noktaların canlı listesi

## 7. Arayüzü çalıştır (geliştirme modu)

Backend açıkken **ikinci bir terminalde**:

```bash
cd frontend
npm install      # yalnızca ilk sefer ve package.json değişince
npm run dev
```

Tarayıcıda **http://localhost:5173** adresini aç ve yukarıdaki hesaplardan biriyle giriş yap.

Geliştirme modunda kod dosyasını kaydettiğin anda tarayıcı kendini günceller. Arayüzün `/api` ile başlayan istekleri `vite.config.ts`'teki ayar sayesinde 8080'deki backend'e iletilir.

IntelliJ'de terminal: alttaki **Terminal** sekmesi. *(Ultimate sürümünde `frontend/package.json` açıkken `dev` satırının yanındaki ▶ ile de çalıştırılabilir.)*

React kodunu okumaya nereden başlayacağın: **[React rehberi](react-rehberi.md)**.

## 8. Tek dosya halinde paketleme (isteğe bağlı)

Arayüz derlenip Spring Boot'un içine konabilir; böylece yalnızca 8080 portu ve tek bir `.jar` yeterli olur:

```bash
cd frontend
npm run build          # → src/main/resources/static/ klasörüne yazar
cd ..
mvn clean package      # → target/ogvpts-*.jar
java -jar target/ogvpts-0.0.1-SNAPSHOT.jar --spring.profiles.active=demo
```

Ardından **http://localhost:8080** adresi doğrudan arayüzü açar. `src/main/resources/static/` derleme çıktısı olduğu için `.gitignore`'dadır.

## Ayarlar

`application.properties` içindeki uygulamaya özel ayarlar. Hepsi ortam değişkeniyle veya `application-local.properties` ile değiştirilebilir:

| Ayar | Varsayılan | Açıklama |
|---|---|---|
| `ogvpts.jwt.secret` (`JWT_SECRET`) | geliştirme anahtarı | Token imzalama anahtarı, en az 32 karakter. **Gerçek kullanımda mutlaka değiştir** |
| `ogvpts.jwt.expiration-hours` | `8` | Oturum süresi |
| `ogvpts.cors.allowed-origins` (`CORS_ORIGINS`) | `http://localhost:5173` | Arayüzün adresi |
| `ogvpts.upload-dir` (`UPLOAD_DIR`) | `./uploads` | Teslim dosyalarının klasörü (git'e gönderilmez) |
| `ogvpts.upload.allowed-extensions` | `pdf,doc,docx,zip,pptx` | İzin verilen dosya türleri |
| `ogvpts.advisor.max-groups` | `8` | Bir danışmana atanabilecek en fazla grup (FR-10) |
| `ogvpts.meeting.cancel-min-hours` | `24` | Onaylı görüşmenin en geç kaç saat önce iptal edilebileceği (BR-03) |
| `ogvpts.mail.enabled` (`MAIL_ENABLED`) | `false` | `true` + `spring.mail.*` ayarlarıyla bildirimler e-posta olarak da gönderilir |

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
| `Sistemde yönetici yok. ADMIN_EMAIL ve ADMIN_PASSWORD … ayarlayın` (uyarı) | Boş veritabanı, demo profili kapalı | [6. adım](#6-backendi-çalıştır): `demo` profili ya da `ADMIN_*` değişkenleri |
| `Demo verisi atlandı: kullanıcı tablosu boş değil.` | Daha önce veri eklenmiş | Normal. Sıfırdan başlamak için verileri sıfırla ([6. adım](#6-backendi-çalıştır)) |
| Arayüzde *"Sunucuya ulaşılamadı. Backend çalışıyor mu?"* | Backend kapalı ya da 8080 dışında bir portta | Backend'i başlat; port farklıysa `frontend/vite.config.ts` içindeki `target`'ı düzelt |
| `npm` komutu bulunamıyor / `Vite requires Node.js version …` | Node.js yok veya eski | Node.js 22 LTS kur, terminali kapatıp aç |
| Arayüz açılıyor ama girişte *"E-posta veya şifre hatalı"* | Demo verisi eklenmemiş | Backend konsolunda *Demo verisi oluşturuldu* satırını ara; yoksa `demo` profilini kontrol et |
| Teslim yüklerken *"Dosya en fazla 20 MB olabilir"* | Dosya büyük | Gereksinim gereği sınır 20 MB (FR-06) |
