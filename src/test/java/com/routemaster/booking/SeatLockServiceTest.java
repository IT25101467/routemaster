package com.routemaster.booking;

import com.routemaster.booking.lock.SeatLockService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RBucket;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SeatLockServiceTest {

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private RLock lockA;

    @Mock
    private RLock lockB;

    @Mock
    @SuppressWarnings("rawtypes")
    private RBucket bucketA;

    private SeatLockService seatLockService;

    @BeforeEach
    void setUp() {
        seatLockService = new SeatLockService(redissonClient);
    }

    @Test
    @DisplayName("BR-01: holdSeats acquires distributed lock on seats with 300s TTL")
    @SuppressWarnings("unchecked")
    void testHoldSeats_Success() throws InterruptedException {
        when(redissonClient.getLock("rm:lock:seat:10:1A")).thenReturn(lockA);
        doReturn(bucketA).when(redissonClient).getBucket("rm:token:seat:10:1A");
        when(lockA.tryLock(2, 300, TimeUnit.SECONDS)).thenReturn(true);

        boolean held = seatLockService.holdSeats(10L, 1L, List.of("1A"), "token-xyz");

        assertTrue(held);
        verify(bucketA, times(1)).set(eq("token-xyz"), eq(Duration.ofSeconds(300)));
    }

    @Test
    @DisplayName("BR-01: If any seat fails in batch, previously acquired seats roll back")
    @SuppressWarnings("unchecked")
    void testHoldSeats_PartialFailure_RollsBack() throws InterruptedException {
        when(redissonClient.getLock("rm:lock:seat:10:1A")).thenReturn(lockA);
        doReturn(bucketA).when(redissonClient).getBucket("rm:token:seat:10:1A");
        when(lockA.tryLock(2, 300, TimeUnit.SECONDS)).thenReturn(true);
        when(lockA.isLocked()).thenReturn(true);

        when(redissonClient.getLock("rm:lock:seat:10:1B")).thenReturn(lockB);
        when(lockB.tryLock(2, 300, TimeUnit.SECONDS)).thenReturn(false); // Fails

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                seatLockService.holdSeats(10L, 1L, List.of("1A", "1B"), "token-xyz")
        );

        assertTrue(ex.getMessage().contains("held by another passenger"));
        // Verify rollback on seat 1A
        verify(lockA, times(1)).forceUnlock();
        verify(bucketA, times(1)).delete();
    }
}
