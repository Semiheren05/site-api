# site-api

[![CI](https://github.com/Semiheren05/site-api/actions/workflows/ci.yml/badge.svg)](https://github.com/Semiheren05/site-api/actions/workflows/ci.yml)

Kişisel web sitemin backend API'si. Şu an hesapları ve kimlik doğrulamayı yönetiyor; ilerledikçe sitenin içeriklerini (blog yazıları, portföy çalışmaları, iletişim formu) da sunacak. Nginx ve Cloudflare Tunnel arkasında, kendi Ubuntu ev sunucumda çalışıyor.

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
| `GET` | `/actuator/health` | herkese açık | Sağlık kontrolü |

Örnek:

```bash
curl -X POST localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"semih@example.com","password":"password123"}'
# {"accessToken":"eyJhbGciOi...","tokenType":"Bearer","expiresIn":3600}

curl localhost:8080/api/users/me -H 'Authorization: Bearer eyJhbGciOi...'
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
├── security/    JWT servisi ve filtresi, güvenlik ayarları, CORS, 401/403 yanıtları
├── common/      merkezi hata yönetimi ve hata sınıfları
└── config/      OpenAPI ve web ayarları
src/main/resources/db/migration   Flyway SQL migration'ları
```

## Testler

```bash
mvn verify
```

Unit testler JWT mantığını ve kimlik doğrulama kurallarını Mockito ile test ediyor. Entegrasyon testleri Testcontainers ile gerçek bir PostgreSQL başlatıyor (Docker gerekir; Docker yoksa atlanır) ve tüm HTTP akışını deniyor: kayıt, giriş, doğrulama hataları, 401/403/409 yanıtları ve sayfalama. GitHub Actions her push'ta tüm testleri çalıştırıp Docker imajını derliyor.

## Yol haritası

- [x] Kimlik doğrulama, roller, hata yönetimi, testler, CI
- [ ] Blog yazıları ve portföy çalışmaları (admin CRUD, herkese açık okuma API'si)
- [ ] Hız sınırlamalı ve e-posta bildirimli iletişim formu
- [ ] Refresh token'lar
- [ ] Gizliliğe saygılı sayfa görüntülenme istatistikleri
- [ ] GitHub Actions'tan ev sunucusuna otomatik dağıtım

## Lisans

[MIT](LICENSE) © 2026 Semih Eren
