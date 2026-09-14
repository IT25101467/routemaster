package com.routemaster.booking.lock;

import org.redisson.api.RBucket;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class SeatLockService {

    private static final Logger log = LoggerFactory.getLogger(SeatLockService.class);
    private static final String LOCK_PREFIX = "rm:lock:seat:";
    private static final String TOKEN_PREFIX = "rm:token:seat:";
    private static final long WAIT_TIMEOUT_SEC = 2;
    private static final long LEASE_TIME_SEC = 300; // 5 minutes TTL (BR-01)

    private final RedissonClient redissonClient;

    public SeatLockService(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    /**
     * Atomically holds a batch of seats using Redisson distributed locks.
     * Enforces BR-01: 300-second TTL.
     * If any seat fails to lock within 2 seconds, rolls back all seats acquired in this request.
     */
    public boolean holdSeats(Long tripId, Long busId, List<String> seatNos, String holdToken) {
        if (seatNos == null || seatNos.isEmpty()) {
            throw new IllegalArgumentException("No seats specified for reservation hold.");
        }

        List<String> lockedSeats = new ArrayList<>();

        for (String seatNo : seatNos) {
            String lockKey = buildLockKey(tripId, seatNo);
            String tokenKey = buildTokenKey(tripId, seatNo);
            RLock lock = redissonClient.getLock(lockKey);

            try {
                boolean acquired = lock.tryLock(WAIT_TIMEOUT_SEC, LEASE_TIME_SEC, TimeUnit.SECONDS);
                if (acquired) {
                    RBucket<String> tokenBucket = redissonClient.getBucket(tokenKey);
                    tokenBucket.set(holdToken, Duration.ofSeconds(LEASE_TIME_SEC));
                    lockedSeats.add(seatNo);
                    log.info("Held distributed lock for seat {} on trip {} with token {}", seatNo, tripId, holdToken);
                } else {
                    log.warn("Failed to acquire lock for seat {} on trip {}", seatNo, tripId);
                    rollbackHeldSeats(tripId, lockedSeats, holdToken);
                    throw new IllegalStateException("Seat [" + seatNo + "] is currently held by another passenger. Please select another seat.");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                rollbackHeldSeats(tripId, lockedSeats, holdToken);
                throw new IllegalStateException("Interrupted while acquiring seat lock: " + seatNo, e);
            }
        }
        return true;
    }

    /**
     * Releases held seats after ownership verification against holdToken.
     */
    public void releaseSeats(Long tripId, List<String> seatNos, String holdToken) {
        if (seatNos == null || seatNos.isEmpty()) {
            return;
        }

        for (String seatNo : seatNos) {
            String lockKey = buildLockKey(tripId, seatNo);
            String tokenKey = buildTokenKey(tripId, seatNo);
            RBucket<String> tokenBucket = redissonClient.getBucket(tokenKey);
            String existingToken = tokenBucket.get();

            // Allow release if token matches or if called from confirm (force release)
            if (holdToken == null || holdToken.equals(existingToken)) {
                tokenBucket.delete();
                RLock lock = redissonClient.getLock(lockKey);
                if (lock.isLocked()) {
                    try {
                        lock.forceUnlock();
                        log.info("Force-unlocked seat {} on trip {}", seatNo, tripId);
                    } catch (Exception e) {
                        log.warn("Could not force unlock {}: {}", lockKey, e.getMessage());
                    }
                }
            }
        }
    }

    /**
     * Checks if a seat lock is currently held in Redis.
     */
    public boolean isSeatLocked(Long tripId, String seatNo) {
        RLock lock = redissonClient.getLock(buildLockKey(tripId, seatNo));
        return lock.isLocked();
    }

    /**
     * Returns remaining TTL in seconds for a held seat lock.
     */
    public long getRemainingTtl(Long tripId, String seatNo) {
        RBucket<String> tokenBucket = redissonClient.getBucket(buildTokenKey(tripId, seatNo));
        long ttlMs = tokenBucket.remainTimeToLive();
        return ttlMs > 0 ? ttlMs / 1000 : 0;
    }

    private void rollbackHeldSeats(Long tripId, List<String> lockedSeats, String holdToken) {
        for (String seatNo : lockedSeats) {
            String lockKey = buildLockKey(tripId, seatNo);
            String tokenKey = buildTokenKey(tripId, seatNo);
            try {
                redissonClient.getBucket(tokenKey).delete();
                RLock lock = redissonClient.getLock(lockKey);
                if (lock.isLocked()) {
                    lock.forceUnlock();
                }
            } catch (Exception e) {
                log.error("Error rolling back lock for seat {}: {}", seatNo, e.getMessage());
            }
        }
    }

    private String buildLockKey(Long tripId, String seatNo) {
        return LOCK_PREFIX + tripId + ":" + seatNo;
    }

    private String buildTokenKey(Long tripId, String seatNo) {
        return TOKEN_PREFIX + tripId + ":" + seatNo;
    }
}
