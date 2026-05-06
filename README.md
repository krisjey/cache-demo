# cache-demo

Spring Boot 기반 Local Cache Hands-on 예제입니다.

이 레포지터리는 교육생이 1시간 안에 모든 코드를 직접 작성하지 않고, 미리 작성된 API를 호출하면서 Local Cache의 동작 차이를 관찰하도록 구성되어 있습니다.

## Tech Stack

- JDK 17
- Spring Boot 3.5.13
- Gradle 8.13
- Spring Web
- Spring Cache
- Caffeine
- Spring Data JPA
- H2 Database
- Actuator

## 실행

로컬에 Gradle 8.13이 설치되어 있다면 다음 명령으로 실행합니다.

```bash
gradle bootRun
```

Gradle Wrapper를 사용하려면 로컬에서 한 번 생성합니다.

```bash
gradle wrapper --gradle-version=8.13
./gradlew bootRun
```

애플리케이션 기본 포트는 `8081`

AWS EC2 서버에 접속하여 아래와 같은 명령을 실행하여 테스트 환경 실행(15분)
```
[ec2-user@ip-172-31-16-44 ~]$ cd /data/cache-demo
[ec2-user@ip-172-31-16-44 cache-demo]$ git pull
Already up to date.
[ec2-user@ip-172-31-16-44 cache-demo]$ docker compose up -d --build
```

## API 구성

| 시나리오 | API | 목적 |
|---|---|---|
| No Cache | `GET /api/no-cache/products/{productId}` | 캐시 미적용 상태 확인 |
| Local Cache | `GET /api/local-cache/products/{productId}` | 첫 호출과 반복 호출 응답시간 비교 |
| TTL Cache | `GET /api/ttl-cache/products/{productId}` | 10초 TTL 만료 확인 |
| Size Limit Cache | `GET /api/size-limit-cache/products/{productId}` | maximumSize=3 동작 확인 |
| Bad Key | `GET /api/bad-key/products/search?category=BOOK&sort=PRICE` | 잘못된 Cache Key 문제 재현 |
| Good Key | `GET /api/good-key/products/search?category=BOOK&sort=PRICE` | 올바른 Cache Key 설계 확인 |
| Cache Stats | `GET /api/cache/stats` | Hit, Miss, Eviction 확인 |
| Cache Cleanup | `POST /api/cache/cleanup` | Caffeine cleanup 실행 |
| Cache Clear | `DELETE /api/cache/clear` | 전체 캐시 초기화 |
| Change Price | `PATCH /api/products/{productId}/price?price=9999` | 데이터 변경과 Eviction 확인 |

## 실습 1. 캐시 미적용 확인(10분)
### 아래 API들을 호출하면 캐시가 적용되지 않은 결과가 출력됩니다.
### 응답 json에는 응답 시간이 포함되어 있음.
```bash
curl http://localhost:8081/api/no-cache/products/1
curl http://localhost:8081/api/no-cache/products/1
curl http://localhost:8081/api/no-cache/products/1
```

```bash
{"scenario":"no-cache","elapsedMs":344,"data":{"id":1,"name":"Product-1","category":"BOOK","price":1700,"createdAt":"2026-04-17T07:28:20.923678","updatedAt":"2026-04-17T07:28:20.923678"}}
```

확인 포인트:

- 매번 약 300ms 지연이 발생
- 로그에 DB query occurred 메시지가 매번 출력됩니다.

## 실습 2. Local Cache 확인(10분)

### 소스코드에 Lazy loading cache 적용(Local cache)

```bash
curl http://localhost:8081/api/local-cache/products/1
curl http://localhost:8081/api/local-cache/products/1
curl http://localhost:8081/api/local-cache/products/1
curl http://localhost:8081/api/cache/stats
```

```
# 처음호출
[ec2-user@ip-172-31-16-44 cache-demo]$ curl http://localhost:8081/api/ttl-cache/products/1
{"scenario":"ttl-cache","elapsedMs":373,"data":{"id":1,"name":"Product-1","category":"BOOK","price":1700,"createdAt":"2026-04-17T07:28:20.923678","updatedAt":"2026-04-17T07:28:20.923678"}}
# 두 번째 호출
[ec2-user@ip-172-31-16-44 cache-demo]$ curl http://localhost:8081/api/ttl-cache/products/1
{"scenario":"ttl-cache","elapsedMs":1,"data":{"id":1,"name":"Product-1","category":"BOOK","price":1700,"createdAt":"2026-04-17T07:28:20.923678","updatedAt":"2026-04-17T07:28:20.923678"}}
```

확인 포인트:

- 첫 번째 호출은 느립니다.
- 두 번째 호출부터 빨라집니다.
- `localProducts`의 hitCount가 증가합니다.

```
[ec2-user@ip-172-31-16-44 cache-demo]$ curl http://localhost:8081/api/cache/stats
[{"cacheName":"localProducts","estimatedSize":0,"hitCount":0,"missCount":0,"hitRate":1.0,"evictionCount":0,"averageLoadPenaltyMs":0.0},{"cacheName":"ttlProducts","estimatedSize":1,"hitCount":1,"missCount":1,"hitRate":0.5,"evictionCount":0,"averageLoadPenaltyMs":0.0},{"cacheName":"sizeLimitProducts","estimatedSize":0,"hitCount":0,"missCount":0,"hitRate":1.0,"evictionCount":0,"averageLoadPenaltyMs":0.0},{"cacheName":"badProductSearch","estimatedSize":0,"hitCount":0,"missCount":0,"hitRate":1.0,"evictionCount":0,"averageLoadPenaltyMs":0.0},{"cacheName":"goodProductSearch","estimatedSize":0,"hitCount":0,"missCount":0,"hitRate":1.0,"evictionCount":0,"averageLoadPenaltyMs":0.0}]
```

## 실습 3. TTL 확인(15분)

```bash
curl http://localhost:8081/api/ttl-cache/products/1
curl http://localhost:8081/api/ttl-cache/products/1
sleep 11
curl http://localhost:8081/api/ttl-cache/products/1
curl http://localhost:8081/api/cache/stats
```

확인 포인트:

- TTL은 10초입니다.
- 10초 이후 다시 Cache Miss가 발생합니다.

## 실습 4. Size Limit 확인(10분)

```bash
curl http://localhost:8081/api/size-limit-cache/products/1
curl http://localhost:8081/api/size-limit-cache/products/2
curl http://localhost:8081/api/size-limit-cache/products/3
curl http://localhost:8081/api/size-limit-cache/products/4
curl -X POST http://localhost:8081/api/cache/cleanup
curl http://localhost:8081/api/cache/stats
```

확인 포인트:

- `sizeLimitProducts`의 maximumSize는 3
- 4개 이상의 Key를 조회하면 eviction이 발생

## 실습 5. 잘못된 Cache Key 확인(10분)

```bash
curl "http://localhost:8081/api/bad-key/products/search?category=BOOK&sort=PRICE"
curl "http://localhost:8081/api/bad-key/products/search?category=BOOK&sort=LATEST"
```

확인 포인트:

- `badProductSearch`는 category만 Cache Key로 사용합니다.
- sort 조건이 달라도 같은 캐시 결과가 반환될 수 있습니다.

문제 코드:

```java
@Cacheable(cacheNames = CacheConfig.BAD_PRODUCT_SEARCH, key = "#category")
```

## 실습 6. 올바른 Cache Key 확인(10분)

```bash
curl "http://localhost:8081/api/good-key/products/search?category=BOOK&sort=PRICE"
curl "http://localhost:8081/api/good-key/products/search?category=BOOK&sort=LATEST"
```

확인 포인트:

- `goodProductSearch`는 category와 sort를 함께 Cache Key로 사용합니다.
- 조회 조건별로 다른 캐시 결과가 반환됩니다.

개선 코드:

```java
@Cacheable(cacheNames = CacheConfig.GOOD_PRODUCT_SEARCH, key = "#category + ':' + #sort")
```

## H2 Console
### 실습에 사용된 H2 DB 모니터링
- URL: `http://localhost:8081/h2-console`
- JDBC URL: `jdbc:h2:mem:cachedemo`
- User Name: `sa`
- Password: 빈 값

## 교육 핵심 메시지

Local Cache는 빠르지만 JVM 내부 자원을 사용합니다. 따라서 TTL, maximumSize, Cache Key, Hit/Miss 관측 가능성을 함께 설계해야 합니다.
