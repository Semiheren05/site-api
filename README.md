# site-api

[![CI](https://github.com/Semiheren05/site-api/actions/workflows/ci.yml/badge.svg)](https://github.com/Semiheren05/site-api/actions/workflows/ci.yml)

Kişisel web sitemin backend API'si. Hesapları ve kimlik doğrulamayı yönetiyor, sitenin blog yazılarını ve portföy çalışmalarını sunuyor; sırada iletişim formu var. Nginx ve Cloudflare Tunnel arkasında, kendi Ubuntu ev sunucumda çalışıyor.

## Teknolojiler

| Alan | Seçim |
|---|---|
| Dil / framework | Java 21, Spring Boot 3.5 |
| Güvenlik | Spring Security, durumsuz (stateless) JWT (jjwt), BCrypt, rol tabanlı yetkilendirme |
| Veritabanı | PostgreSQL, Spring Data JPA / Hibernate, Flyway migration'ları |
| API dokümantasyonu | OpenAPI 3 / Swagger UI (springdoc) |
| Test | JUnit 5, Mockito, AssertJ, Testcontainers |
| Dağıtım | Docker (multi-stage), Docker Compose, GitHub Actions |

## Özellikler

- **Kayıt / giriş**: imzalı bir JWT erişim token'ı döner
- **Rol tabanlı yetkilendirme** (`USER`, `ADMIN`); admin uç noktaları `/api/admin/**` altında
- **Blog**: yazılar taslak olarak oluşur, yayınlanınca herkese açık listede görünür; taslaklar dışarıya 404 döner
- **Portföy**: çalışmalar öne çıkarılabilir ve sıralanabilir; teknolojiler listesiyle birlikte sunulur
- **Otomatik slug**: Türkçe başlıklardan adres üretilir ("Spring Boot ile İlk API" → `spring-boot-ile-ilk-api`), aynı slug ikinci kez kullanılamaz (409)
- **İlk admin hesabı** ortam değişkenlerinden açılışta oluşturulur, tekrar çalıştırmak sorun çıkarmaz
- **Tutarlı hata yanıtları**: güvenlik katmanından gelen 401/403 dahil her hata [RFC 9457](https://www.rfc-editor.org/rfc/rfc9457) problem-detail JSON'u olarak döner
- **İstek doğrulama**: hatalı alanlar tek tek mesajla bildirilir
- **Veritabanı şeması Flyway'e ait**; Hibernate sadece doğrular
- **CORS** yalnızca sitenin kendi adresleriyle sınırlı
- **Sağlık kontrolü** uç noktası (`/actuator/health`)

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
| `JWT_EXPIRATION` | `1h` | Token geçerlilik süresi |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5500,http://localhost:3000` | Virgülle ayrılmış izinli tarayıcı adresleri |
| `ADMIN_EMAIL` / `ADMIN_PASSWORD` | boş | İkisi de doluysa açılışta ilk admin hesabını oluşturur |

## API

| Metot | Yol | Erişim | Açıklama |
|---|---|---|---|
| `POST` | `/api/auth/register` | herkese açık | Hesap oluşturur, token döner (201) |
| `POST` | `/api/auth/login` | herkese açık | E-posta + şifre → token |
| `GET` | `/api/users/me` | giriş yapmış kullanıcı | Mevcut kullanıcının bilgileri |
| `GET` | `/api/admin/users?page=0&size=20` | `ADMIN` | Sayfalı kullanıcı listesi, en yeniler önce |
| `GET` | `/api/posts?page=0&size=10` | herkese açık | Yayınlanmış yazılar (içeriksiz özet), en yeniler önce |
| `GET` | `/api/posts/{slug}` | herkese açık | Yayınlanmış bir yazı, tam içeriğiyle |
| `GET` | `/api/projects` | herkese açık | Yayınlanmış çalışmalar, öne çıkanlar önce |
| `GET` | `/api/projects/{slug}` | herkese açık | Yayınlanmış bir çalışma |
| `GET` `POST` | `/api/admin/posts` | `ADMIN` | Taslaklar dahil tüm yazılar / yeni taslak (201) |
| `GET` `PUT` `DELETE` | `/api/admin/posts/{id}` | `ADMIN` | Yazıyı getir, güncelle, sil (204) |
| `POST` | `/api/admin/posts/{id}/publish` · `/unpublish` | `ADMIN` | Yayınla / taslağa geri al |
| `GET` `POST` | `/api/admin/projects` | `ADMIN` | Tüm çalışmalar / yeni çalışma (201) |
| `GET` `PUT` `DELETE` | `/api/admin/projects/{id}` | `ADMIN` | Çalışmayı getir, güncelle, sil (204) |
| `GET` | `/actuator/health` | herkese açık | Sağlık kontrolü |

Örnek:

```bash
curl -X POST localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"semih@example.com","password":"password123"}'
# {"accessToken":"eyJhbGciOi...","tokenType":"Bearer","expiresIn":3600}

curl localhost:8080/api/users/me -H 'Authorization: Bearer eyJhbGciOi...'
```

Blog yazısı oluşturup yayınlama (admin token'ı ile):

```bash
curl -X POST localhost:8080/api/admin/posts \
  -H 'Authorization: Bearer <admin-token>' -H 'Content-Type: application/json' \
  -d '{"title":"Spring Boot ile İlk API","summary":"Kısa özet","content":"# Merhaba"}'
# {"id":1,"slug":"spring-boot-ile-ilk-api","status":"DRAFT","publishedAt":null,...}

curl -X POST localhost:8080/api/admin/posts/1/publish -H 'Authorization: Bearer <admin-token>'
curl localhost:8080/api/posts/spring-boot-ile-ilk-api   # artık herkese açık
```

Hata örneği (doğrulama):

```json
{
  "type": "about:blank",
  "title": "Validation error",
  "status": 400,
  "detail": "Request validation failed",
  "instance": "/api/auth/register",
  "errors": {
    "email": "must be a well-formed email address",
    "password": "size must be between 8 and 72"
  }
}
```

## Proje yapısı

```
src/main/java/com/semih/siteapi
├── auth/        kayıt ve giriş (controller, servis, DTO'lar)
├── user/        kullanıcı entity'si, repository, /me ve admin uç noktaları, ilk admin oluşturma
├── post/        blog yazıları: entity, taslak/yayın akışı, herkese açık ve admin uç noktaları
├── project/     portföy çalışmaları: entity, sıralama, herkese açık ve admin uç noktaları
├── security/    JWT servisi ve filtresi, güvenlik ayarları, CORS, 401/403 yanıtları
├── common/      merkezi hata yönetimi, hata sınıfları, slug üretimi, liste ↔ sütun dönüştürücü
└── config/      OpenAPI ve web ayarları
src/main/resources/db/migration   Flyway SQL migration'ları
```

## Testler

```bash
mvn verify
```

Unit testler JWT mantığını ve kimlik doğrulama kurallarını Mockito ile test ediyor. Entegrasyon testleri Testcontainers ile gerçek bir PostgreSQL başlatıyor (Docker gerekir; Docker yoksa atlanır) ve tüm HTTP akışını deniyor: kayıt, giriş, doğrulama hataları, 401/403/409 yanıtları, sayfalama, blog yazısının taslaktan yayına geçişi ve portföy sıralaması. Tüm entegrasyon testleri tek bir PostgreSQL konteynerini paylaşıyor. GitHub Actions her push'ta tüm testleri çalıştırıp Docker imajını derliyor.

## Yol haritası

- [x] Kimlik doğrulama, roller, hata yönetimi, testler, CI
- [x] Blog yazıları ve portföy çalışmaları (admin CRUD, herkese açık okuma API'si)
- [ ] Hız sınırlamalı ve e-posta bildirimli iletişim formu
- [ ] Refresh token'lar
- [ ] Gizliliğe saygılı sayfa görüntülenme istatistikleri
- [ ] GitHub Actions'tan ev sunucusuna otomatik dağıtım

## Lisans

[MIT](LICENSE) © 2026 Semih Eren
