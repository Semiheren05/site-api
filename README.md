# site-api

[![CI](https://github.com/Semiheren05/site-api/actions/workflows/ci.yml/badge.svg)](https://github.com/Semiheren05/site-api/actions/workflows/ci.yml)

Kişisel web sitemin backend API'si. Hesapları ve oturumları (erişim + refresh token) yönetiyor, sitenin etiketli ve aranabilir blog yazılarını ve portföy çalışmalarını sunuyor, iletişim formundan gelen mesajları topluyor ve yazıların kaç kez okunduğunu ziyaretçiyi takip etmeden sayıyor. Nginx ve Cloudflare Tunnel arkasında, kendi Ubuntu ev sunucumda çalışıyor.

## Teknolojiler

| Alan | Seçim |
|---|---|
| Dil / framework | Java 21, Spring Boot 3.5 |
| Güvenlik | Spring Security, durumsuz (stateless) JWT (jjwt), dönen (rotation) refresh token'lar, BCrypt, rol tabanlı yetkilendirme |
| Veritabanı | PostgreSQL, Spring Data JPA / Hibernate, Specification sorguları, Flyway migration'ları |
| API dokümantasyonu | OpenAPI 3 / Swagger UI (springdoc) |
| Test | JUnit 5, Mockito, AssertJ, Testcontainers |
| E-posta | Spring Mail (SMTP), olay tabanlı ve asenkron bildirim |
| Loglama | SLF4J, istek kimliği (MDC), canlıda JSON log (ECS) |
| Dağıtım | Docker (multi-stage), Docker Compose, GitHub Actions |

## Özellikler

- **Kayıt / giriş**: 15 dakikalık bir JWT erişim token'ı ve 30 günlük bir refresh token döner
- **Refresh token'lar**: her biri tek kullanımlık; kullanılınca yenisiyle değişir (rotation). Veritabanında sadece SHA-256 hash'i durur. Kullanılmış bir token tekrar gelirse (çalınmış olabilir) o oturumun tüm token'ları iptal edilir. Çıkış yapınca da oturum kapanır; süresi dolanlar her gece silinir
- **Rol tabanlı yetkilendirme** (`USER`, `ADMIN`); admin uç noktaları `/api/admin/**` altında
- **Blog**: yazılar taslak olarak oluşur, yayınlanınca herkese açık listede görünür; taslaklar dışarıya 404 döner
- **Etiketler**: yazıya en fazla 10 etiket verilir; "Spring Boot" ile "spring boot" aynı etikettir. Liste etikete göre süzülebilir, etiket listesi sadece yayınlanmış yazılarda kullanılanları sayısıyla verir
- **Arama**: başlık, özet ve içerikte büyük/küçük harf ayırmadan arar; `%` ve `_` gibi karakterler joker değil, aranan metin olarak kullanılır. Etiket ve arama birlikte kullanılabilir
- **Görüntülenme istatistikleri**: çerez yok, IP adresi saklanmıyor. Ziyaretçi, her gün değişen ve sadece bellekte duran bir tuzla hash'lenir; aynı kişi bir yazıyı gün içinde kaç kez açarsa açsın bir kez sayılır. Botlar ve `DNT` / `Sec-GPC` gönderen tarayıcılar hiç sayılmaz. Admin, günlük grafiği ve en çok okunan yazıları görür
- **Portföy**: çalışmalar öne çıkarılabilir ve sıralanabilir; teknolojiler listesiyle birlikte sunulur
- **İletişim formu**: mesajlar admin gelen kutusuna kaydedilir, istenirse e-posta ile bildirilir. Bildirim kayıt kesinleştikten sonra ayrı bir thread'de gider; mail sunucusu çökse bile ziyaretçinin isteği etkilenmez
- **Hız sınırlama**: aynı IP'den 10 dakikada en fazla 3 mesaj (kayan pencere); aşılırsa `429` ve `Retry-After` başlığı döner
- **Bot filtresi**: formda gizli bir "honeypot" alanı var; onu dolduran istekler kabul edilmiş gibi görünür ama kaydedilmez
- **Otomatik slug**: Türkçe başlıklardan adres üretilir ("Spring Boot ile İlk API" → `spring-boot-ile-ilk-api`), aynı slug ikinci kez kullanılamaz (409)
- **İlk admin hesabı** ortam değişkenlerinden açılışta oluşturulur, tekrar çalıştırmak sorun çıkarmaz
- **Tutarlı hata yanıtları**: güvenlik katmanından gelen 401/403 ve Spring'in kendi hataları (bozuk JSON gibi) dahil her hata [RFC 9457](https://www.rfc-editor.org/rfc/rfc9457) problem-detail JSON'u olarak, `timestamp` alanıyla döner
- **Loglama**: her isteğe bir kimlik verilir (`X-Request-Id` başlığı); o isteğin tüm log satırları bu kimliği taşır. Önemli iş olayları `INFO`, şüpheli durumlar (tekrar kullanılan token, hız sınırı) `WARN`, beklenmeyen hatalar `ERROR` olarak bir kez loglanır. `INFO`/`WARN`/`ERROR` satırlarında e-posta adresi, IP ya da token yok, sadece id'ler (veritabanının "bu e-posta zaten var" mesajı bile sadece kısıt adıyla loglanıyor)
- **İstek doğrulama**: hatalı alanlar tek tek mesajla bildirilir
- **Veritabanı şeması Flyway'e ait**; Hibernate sadece doğrular
- **CORS** yalnızca sitenin kendi adresleriyle sınırlı
- **Sağlık kontrolü** uç noktası (`/actuator/health`)

## Mimari

Her özellik kendi paketinde ve aynı katmanlardan geçiyor:

```
HTTP → Controller ──Request DTO──▶ Service ──Entity──▶ Repository → PostgreSQL
HTTP ◀ ResponseEntity<Response DTO> ◀ Service ◀──Entity── Repository
                    Mapper (DTO ⇄ Entity) sadece servislerde kullanılıyor
```

- **Controller**: sadece HTTP. İsteği `@Valid` ile doğrular, servise verir, `ResponseEntity` döner (201'de `Location` başlığıyla). İş kuralı ve repository yok.
- **Service**: iş kuralları, transaction'lar ve hatalar burada. Başka bir özelliğin verisi gerekiyorsa onun servisini çağırır (örneğin `AuthService` → `UserService`), repository'sini değil.
- **Repository**: Spring Data JPA arayüzleri; sadece sorgu.
- **DTO ve Mapper**: istek ve yanıtlar ayrı `record`'lar. Entity'ler servisin dışına çıkmıyor; dönüşümler her özelliğin `XMapper` sınıfında.
- **N+1 yok**: yazı listesinde etiketler `@BatchSize` ile tek sorguda geliyor; bir sayfa kaç yazı içerirse içersin sorgu sayısı sabit (testi var). Etiket ve istatistik listeleri veritabanında gruplanıp DTO projeksiyonu olarak okunuyor.

## Çalıştırma

### Docker Compose ile (API + PostgreSQL)

```bash
cp .env.example .env
# .env içinde JWT_SECRET'ı ayarla, örneğin: openssl rand -base64 48
docker compose up --build
```

API `http://localhost:8080` adresinde, Swagger UI `http://localhost:8080/swagger-ui.html` adresinde açılır.

### IDE'den yerel olarak

```bash
docker compose up -d db     # sadece veritabanı
mvn spring-boot:run
```

`JWT_SECRET` ayarlanmazsa `application.yml` içindeki sadece geliştirme amaçlı anahtar kullanılır. Yerel geliştirme dışında buna asla güvenme.

### Ayarlar

| Değişken | Varsayılan | Amaç |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/siteapi` | JDBC adresi |
| `DB_USERNAME` / `DB_PASSWORD` | `siteapi` / `siteapi` | Veritabanı bilgileri |
| `JWT_SECRET` | geliştirme değeri | Base64 HMAC anahtarı, en az 32 bayt |
| `JWT_EXPIRATION` | `15m` | Erişim token'ının geçerlilik süresi (iptal edilemediği için kısa) |
| `JWT_REFRESH_EXPIRATION` | `30d` | Refresh token'ın geçerlilik süresi |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5500,http://localhost:3000` | Virgülle ayrılmış izinli tarayıcı adresleri |
| `ADMIN_EMAIL` / `ADMIN_PASSWORD` | boş | İkisi de doluysa açılışta ilk admin hesabını oluşturur |
| `CONTACT_MAX_REQUESTS` / `CONTACT_WINDOW` | `3` / `10m` | IP başına izin verilen mesaj sayısı ve süre penceresi |
| `CONTACT_NOTIFY_TO` | boş | Yeni mesaj bildiriminin gideceği adres; boşsa bildirim kapalı |
| `CONTACT_MAIL_FROM` | boş | Bildirimin gönderen adresi |
| `SPRING_MAIL_HOST` / `SPRING_MAIL_PORT` | boş / `587` | SMTP sunucusu; host boşsa e-posta hiç gönderilmez |
| `SPRING_MAIL_USERNAME` / `SPRING_MAIL_PASSWORD` | boş | SMTP giriş bilgileri (Gmail için uygulama şifresi) |
| `STATS_TIME_ZONE` | `Europe/Istanbul` | Görüntülenme istatistiklerinde günün başladığı saat dilimi |
| `SPRING_PROFILES_ACTIVE` | Docker'da `prod` | `prod`: her log satırı bir JSON nesnesi (log toplayıcılar için); boş bırakılırsa okunabilir metin |
| `LOG_LEVEL` | `info` | Uygulamanın kendi log seviyesi (`debug` ayrıntı verir) |

## API

| Metot | Yol | Erişim | Açıklama |
|---|---|---|---|
| `POST` | `/api/auth/register` | herkese açık | Hesap oluşturur, token'ları döner (201) |
| `POST` | `/api/auth/login` | herkese açık | E-posta + şifre → erişim + refresh token |
| `POST` | `/api/auth/refresh` | herkese açık | Refresh token → yeni erişim + yeni refresh token; geçersiz, süresi dolmuş ya da tekrar kullanılmışsa 401 |
| `POST` | `/api/auth/logout` | herkese açık | Refresh token'ın oturumunu kapatır (204) |
| `GET` | `/api/users/me` | giriş yapmış kullanıcı | Mevcut kullanıcının bilgileri |
| `GET` | `/api/admin/users?page=0&size=20` | `ADMIN` | Sayfalı kullanıcı listesi, en yeniler önce |
| `GET` | `/api/posts?tag=spring-boot&q=jwt&page=0&size=10` | herkese açık | Yayınlanmış yazılar (içeriksiz özet, etiketleriyle), en yeniler önce; `tag` ve `q` isteğe bağlı |
| `GET` | `/api/posts/{slug}` | herkese açık | Yayınlanmış bir yazı, tam içeriğiyle |
| `POST` | `/api/posts/{slug}/views` | herkese açık | Yazının okunduğunu bildirir (204); çerez yok, `DNT` / `Sec-GPC` dikkate alınır |
| `GET` | `/api/tags` | herkese açık | Yayınlanmış yazılarda kullanılan etiketler, en çok kullanılan önce |
| `GET` | `/api/projects` | herkese açık | Yayınlanmış çalışmalar, öne çıkanlar önce |
| `GET` | `/api/projects/{slug}` | herkese açık | Yayınlanmış bir çalışma |
| `GET` `POST` | `/api/admin/posts` | `ADMIN` | Taslaklar dahil tüm yazılar / yeni taslak (201) |
| `GET` `PUT` `DELETE` | `/api/admin/posts/{id}` | `ADMIN` | Yazıyı getir, güncelle, sil (204) |
| `POST` | `/api/admin/posts/{id}/publish` · `/unpublish` | `ADMIN` | Yayınla / taslağa geri al |
| `POST` | `/api/contact` | herkese açık | İletişim formu mesajı (202), IP başına hız sınırlı |
| `GET` | `/api/admin/contact-messages?page=0&size=20` | `ADMIN` | Gelen kutusu, en yeniler önce |
| `POST` | `/api/admin/contact-messages/{id}/read` | `ADMIN` | Mesajı okundu işaretle |
| `DELETE` | `/api/admin/contact-messages/{id}` | `ADMIN` | Mesajı sil (204) |
| `GET` `POST` | `/api/admin/projects` | `ADMIN` | Tüm çalışmalar / yeni çalışma (201) |
| `GET` `PUT` `DELETE` | `/api/admin/projects/{id}` | `ADMIN` | Çalışmayı getir, güncelle, sil (204) |
| `GET` | `/api/admin/stats/views?from=2026-10-01&to=2026-10-31` | `ADMIN` | Günlük görüntülenme ve en çok okunan 10 yazı (varsayılan: son 30 gün) |
| `GET` | `/actuator/health` | herkese açık | Sağlık kontrolü |

Örnek:

```bash
curl -X POST localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"semih@example.com","password":"password123"}'
# {"accessToken":"eyJhbGciOi...","tokenType":"Bearer","expiresIn":900,
#  "refreshToken":"q3T0Vx...","refreshExpiresIn":2592000}

curl localhost:8080/api/users/me -H 'Authorization: Bearer eyJhbGciOi...'

# Erişim token'ının süresi dolunca: refresh token'ı bir kez kullan, gelen yenisini sakla
curl -X POST localhost:8080/api/auth/refresh \
  -H 'Content-Type: application/json' -d '{"refreshToken":"q3T0Vx..."}'
```

Blog yazısı oluşturup yayınlama (admin token'ı ile):

```bash
curl -X POST localhost:8080/api/admin/posts \
  -H 'Authorization: Bearer <admin-token>' -H 'Content-Type: application/json' \
  -d '{"title":"Spring Boot ile İlk API","summary":"Kısa özet","content":"# Merhaba","tags":["Spring Boot","Java"]}'
# 201, Location: http://localhost:8080/api/admin/posts/1
# {"id":1,"slug":"spring-boot-ile-ilk-api","status":"DRAFT",
#  "tags":[{"name":"Java","slug":"java"},{"name":"Spring Boot","slug":"spring-boot"}],"publishedAt":null,...}

curl -X POST localhost:8080/api/admin/posts/1/publish -H 'Authorization: Bearer <admin-token>'
curl localhost:8080/api/posts/spring-boot-ile-ilk-api   # artık herkese açık
curl 'localhost:8080/api/posts?tag=java&q=merhaba'      # etiket + arama
curl -X POST localhost:8080/api/posts/spring-boot-ile-ilk-api/views -H 'User-Agent: Mozilla/5.0'   # 204
```

İletişim formu (4. mesaj 10 dakika içinde gelirse):

```bash
curl -i -X POST localhost:8080/api/contact -H 'Content-Type: application/json' \
  -d '{"name":"Ayşe","email":"ayse@example.com","message":"Merhaba Semih, birlikte çalışalım mı?"}'
# HTTP/1.1 429
# Retry-After: 412
# {"title":"Too many requests","status":429,...}
```

Hata örneği (doğrulama):

```json
{
  "type": "about:blank",
  "title": "Validation error",
  "status": 400,
  "detail": "Request validation failed",
  "instance": "/api/auth/register",
  "timestamp": "2026-10-24T09:15:02.481Z",
  "errors": {
    "email": "must be a well-formed email address",
    "password": "size must be between 8 and 72"
  }
}
```

## Proje yapısı

```
src/main/java/com/semih/siteapi
├── auth/        kayıt, giriş, refresh token'lar (rotation, tekrar kullanım tespiti), çıkış, gece temizliği
├── user/        kullanıcı entity'si, UserService, /me ve admin uç noktaları, ilk admin oluşturma
├── post/        blog yazıları: taslak/yayın akışı, etiket filtresi ve arama (Specification), mapper
├── tag/         etiketler: Post ile ManyToMany, etiket listesi (DTO projeksiyonu)
├── project/     portföy çalışmaları: sıralama, herkese açık ve admin uç noktaları, mapper
├── contact/     iletişim formu, admin gelen kutusu, e-posta bildirimi
├── stats/       görüntülenme sayımı (günlük tuz, upsert), admin istatistikleri, gece temizliği
├── security/    JWT servisi ve filtresi, güvenlik ayarları, CORS, 401/403 yanıtları
├── common/      merkezi hata yönetimi, istek kimliği filtresi, hata sınıfları, slug üretimi, hız sınırlayıcı
└── config/      OpenAPI, web, asenkron çalışma ve zamanlanmış görev ayarları
src/main/resources/db/migration   Flyway SQL migration'ları
```

## Testler

```bash
mvn verify
```

- **Unit testler** (Mockito): JWT mantığı, kimlik doğrulama kuralları, refresh token rotation'ı ve tekrar kullanım tespiti (yarışı kaybeden istek dahil), etiket çözme, arama metnindeki joker karakterler, ziyaretçi hash'inin IP içermemesi ve her gün değişmesi, istatistiklerde boş günlerin 0 ile doldurulması, mapper'lar, istek kimliği filtresi, hız sınırlayıcı ve e-posta bildirimi.
- **Entegrasyon testleri**: Testcontainers ile gerçek bir PostgreSQL başlatılıyor (Docker gerekir; Docker yoksa atlanır) ve HTTP akışı baştan sona deneniyor: kayıt, giriş, refresh ve çıkış, tekrar kullanılan token'ın tüm oturumu kapatması, doğrulama hataları, 401/403/409 yanıtlarının biçimi (`timestamp`, `X-Request-Id`), 201'deki `Location` başlığı, etiket filtresi ve arama, etiket sayıları, görüntülenmelerin ziyaretçi başına bir kez sayılması, botların ve `DNT`'nin sayılmaması, iletişim formunun hız sınırı (429). **N+1 testi**: altı yazılık bir sayfanın en fazla üç SQL sorgusu çalıştırdığı Hibernate istatistikleriyle doğrulanıyor.

Tüm entegrasyon testleri tek bir PostgreSQL konteynerini paylaşıyor. GitHub Actions her push'ta tüm testleri çalıştırıp Docker imajını derliyor ve iş özetine kaç testin çalıştığını, kaçının gerçek PostgreSQL'e karşı koştuğunu ve atlanan test olup olmadığını yazıyor.

## Yol haritası

- [x] Kimlik doğrulama, roller, hata yönetimi, testler, CI
- [x] Blog yazıları ve portföy çalışmaları (admin CRUD, herkese açık okuma API'si)
- [x] Hız sınırlamalı ve e-posta bildirimli iletişim formu
- [x] Katmanlı mimari (ResponseEntity, Mapper'lar), zaman damgalı hata yanıtları, istek kimliğiyle loglama
- [x] Etiketler ve arama
- [x] Refresh token'lar
- [x] Gizliliğe saygılı sayfa görüntülenme istatistikleri
- [ ] PostgreSQL tam metin araması (`tsvector`, Türkçe sözlük)
- [ ] GitHub Actions'tan ev sunucusuna otomatik dağıtım

## Lisans

[MIT](LICENSE) © 2026 Semih Eren
