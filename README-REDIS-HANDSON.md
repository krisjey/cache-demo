# Hands-on ② Redis Distributed Cache & Consistency

이 문서는 EC2 VM에서 Redis 기반 Distributed Cache 실습을 2시간 안에 진행하기 위한 가이드입니다.

## 실습 목표

- 두 개의 Spring Boot 인스턴스가 하나의 Redis 캐시를 공유하는 구조 확인
- DB 변경 후 Redis 캐시에 stale data가 남는 상황 재현
- TTL, 명시적 Eviction, Redis Pub/Sub 기반 Invalidation 비교
- Redis 장애 시 API 영향과 fail-open fallback 확인

## 실행 환경

```text
EC2 VM
 ├─ app1      : localhost:8081
 ├─ app2      : localhost:8082
 ├─ redis     : localhost:6379
 └─ postgres  : localhost:5432
```

## 실행
### 이전 세션에서 실행 완료 함.
```bash
git clone https://github.com/krisjey/cache-demo.git
cd cache-demo
docker compose up -d --build
```

상태 확인:

```bash
docker compose ps
curl http://localhost:8081/actuator/health
curl http://localhost:8082/actuator/health
```

로그 확인:

```bash
docker compose logs -f app1 app2
```

## API 목록

| 목적 | API |
|---|---|
| Redis Cache Aside 조회 | `GET /api/redis-cache/products/{productId}` |
| Redis Key 목록 확인 | `GET /api/redis-cache/keys` |
| Redis 캐시 값 확인 | `GET /api/redis-cache/value/products/{productId}` |
| DB만 변경 | `PATCH /api/db-only/products/{productId}/price?price=9999` |
| DB 변경 + 명시적 Eviction | `PATCH /api/evict/products/{productId}/price?price=12000` |
| DB 변경 + Pub/Sub Event Invalidation | `PATCH /api/event/products/{productId}/price?price=15000` |
| 수동 Redis 캐시 삭제 | `PATCH /api/redis-cache/products/{productId}/evict` |
| Redis 실습 캐시 전체 삭제 | `DELETE /api/redis-cache/keys` |

## 실습 1. 두 인스턴스의 Redis 캐시 공유 확인(10분)

```bash
curl http://localhost:8081/api/redis-cache/products/1
curl http://localhost:8082/api/redis-cache/products/1
curl http://localhost:8081/api/redis-cache/keys
```

확인 포인트:

- app1 첫 호출은 DB 조회가 발생
- app2 같은 상품 호출은 Redis Cache Hit
- app1/app2가 Redis 캐시를 공유
- 그러므로 마지막 API에서 1개의 키만 조회됨.
- 상품번호 2, 3도 조회 후 키 결과 확인

## 실습 2. Redis Key와 TTL 확인(15분)
### TTL이 30초로 짧으므로 터미널 2개 실행 후 확인 필요

```bash
curl http://localhost:8081/api/redis-cache/value/products/1
```

Redis CLI로 직접 확인:

```bash
docker exec -it cache-demo-redis redis-cli
keys *
ttl cache:redisProducts::1
get cache:redisProducts::1
```

확인 포인트:

- Redis Key prefix는 `cache:redisProducts::`
- TTL 만료 후 다시 조회하면 DB 조회가 발생

## 실습 3. DB 변경 후 Stale Cache 재현(10분)

```bash
curl http://localhost:8081/api/redis-cache/products/1
curl -X PATCH "http://localhost:8081/api/db-only/products/1/price?price=9999"
curl http://localhost:8082/api/redis-cache/products/1
curl http://localhost:8081/api/redis-cache/value/products/1
```

확인 포인트:

- DB의 가격 변경
- Redis 캐시에는 이전 가격
- app2 조회 결과가 stale data 반환

핵심 메시지:

```text
DB 변경은 Redis 캐시 변경을 자동으로 보장하지 않는다.
```

## 실습 4. 명시적 Eviction 확인(10분)

```bash
curl -X PATCH "http://localhost:8081/api/evict/products/1/price?price=12000"
curl http://localhost:8082/api/redis-cache/products/1
```

확인 포인트:

- DB 변경 시 `redisProducts` 캐시의 productId=1 entry가 삭제됩니다.
- 다음 조회 시 DB 재조회 후 새 값이 캐시에 저장됩니다.

## 실습 5. Event 기반 Invalidation 확인(10분)

```bash
curl http://localhost:8081/api/redis-cache/products/2
curl -X PATCH "http://localhost:8081/api/event/products/2/price?price=15000"
curl http://localhost:8082/api/redis-cache/products/2
```

로그 확인:

```bash
docker compose logs -f app1 app2
```

확인 포인트:

- app1에서 Redis Pub/Sub 이벤트를 발행합니다.
- app1/app2가 이벤트를 수신합니다.
- 이벤트 수신 후 Redis 캐시를 Evict합니다.

## 실습 6. Redis 장애와 fail-open 확인(20분)

Redis 중지:

```bash
docker compose stop redis
curl http://localhost:8081/api/redis-cache/products/3
```

확인 포인트:

- Redis Cache GET/PUT 오류 로그가 발생합니다.
- `APP_CACHE_FAIL_OPEN=true` 설정 때문에 DB 조회로 fallback됩니다.
- Redis 장애가 곧바로 API 장애가 되지 않도록 설계할 수 있습니다.

Redis 재시작:

```bash
docker compose start redis
curl http://localhost:8081/api/redis-cache/products/3
curl http://localhost:8082/api/redis-cache/products/3
```

## 정리 메시지

Redis Distributed Cache는 단순히 빠른 저장소가 아닙니다. 여러 애플리케이션 인스턴스가 공유하는 분산 상태이므로 일관성, 무효화, TTL, 장애 대응을 함께 설계해야 합니다.
