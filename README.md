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

## Çalışma düzeni

- Çalışma 1 haftalık sprintlerle yürütülür: Sprint 1 gereksinim analizi, Sprint 2 diyagramlar, Sprint 3 veritabanı.
- Commit mesajları ve dal adları Jira görev anahtarını içerir. Örnekler: `OGVPTS-54: veritabanı şeması eklendi`, `feature/OGVPTS-56-grup-olusturma`.

## Geliştirici

Mehmet Caner Kahraman – Maltepe Üniversitesi, Yazılım Mühendisliği
