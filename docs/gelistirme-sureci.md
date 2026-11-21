# Geliştirme süreci

Proje **Jira** (proje anahtarı `OGVPTS`) üzerinden, 1 haftalık **Scrum sprintleri** ile yürütülür. Kod **GitHub**'da ([mcaner27/OGVPTS](https://github.com/mcaner27/OGVPTS)) tutulur ve Jira'ya bağlıdır.

## Sprintler

| Sprint | Hedef | Çıktılar |
|---|---|---|
| Sprint 1 – Gereksinim Analizi | Problem, kullanıcı rolleri, işlevsel/işlevsel olmayan gereksinimler, iş kuralları | Epic, Story ve Task hiyerarşisi; kabul kriterleri |
| Sprint 2 – Diyagram Çalışması | Sistem tasarım diyagramları | High Level, Context, Use Case, State diyagramları ve ERD |
| Sprint 3 – Veritabanı | Veritabanının tasarlanıp kurulması | DDL betiği (DB-01), SQL Server'da kurulum ve ekran görüntüsü (DB-02) |
| Sprint 4 – Proje Altyapısı | Uygulama iskeleti, DB bağlantısı, modeller, GitHub entegrasyonu | Spring Boot projesi (DEV-01), 13 JPA modeli (DEV-02), GitHub–Jira entegrasyonu (DEV-03) |

## Dal ve commit kuralları

Her commit mesajı ve dal adı ilgili **Jira görev anahtarını** içerir. Böylece commit'ler, dallar ve pull request'ler Jira görevinin **Development** panelinde otomatik görünür.

| | Biçim | Örnek |
|---|---|---|
| Dal | `feature/<ANAHTAR>-<kısa-açıklama>` | `feature/OGVPTS-62-proje-altyapisi` |
| Commit | `<ANAHTAR>: <ne yapıldı>` | `OGVPTS-66: 13 tablo için JPA entity ve repository sınıfları eklendi` |
| Pull request başlığı | `<ANAHTAR>: <özet>` | `OGVPTS-62 / OGVPTS-66: Spring Boot projesi, SQL Server bağlantısı ve JPA modelleri` |

Akış:

1. Jira'da görevi sprinte al, kendine ata, durumunu **Devam ediyor** yap.
2. `master`'dan görev anahtarlı bir dal aç.
3. Commit mesajlarına görev anahtarını yaz.
4. Pull request aç; inceleme sonrası `master`'a birleştir.
5. Jira'da görevi **Tamamlandı** yap.

## GitHub–Jira entegrasyonu

- Jira sitesine **GitHub for Atlassian** uygulaması kuruludur.
- `mcaner27` GitHub hesabı bağlıdır; erişim yalnızca **OGVPTS** reposuna verilmiştir.
- Görev anahtarı geçen commit, dal ve pull request'ler, ilgili Jira görevinin Development panelinde listelenir. Örneğin OGVPTS-62 ve OGVPTS-66'da 1 dal, commit'ler ve 1 pull request görünür.
