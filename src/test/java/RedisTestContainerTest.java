import cache.Cache;
import cache.RedisBackedCache;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;
import redis.clients.jedis.Jedis;

import java.util.Optional;

import static org.junit.Assert.*;

public class RedisTestContainerTest {

    private static GenericContainer<?> redis;
    private static final Logger LOGGER = LoggerFactory.getLogger(RedisTestContainerTest.class);

    private Cache cache;


    @BeforeClass
    public static void setup() {
        redis = new GenericContainer<>(DockerImageName.parse("redis:5.0.13-alpine")).withExposedPorts(6379);
        LOGGER.info("<<< Starting Redis container");

        redis.start();
        LOGGER.info("<<< Started Redis container");
    }

    @AfterClass
    public static void tearDown() {
        redis.close();
        LOGGER.info("<<< Close connection");
    }

    @Before
    public void startTest() {
        Jedis jedis = new Jedis(redis.getContainerIpAddress(), redis.getMappedPort(6379));
        cache = new RedisBackedCache(jedis, "test");
    }

    @Test
    public void insertAndFindValueInCache() {
        cache.put("testcont", "CONT");

        Optional<String> foundObject = cache.get("testcont", String.class);

        assertTrue("Objects are found and exists in cache", foundObject.isPresent());
        assertEquals("The value retrieved from cache is the same before we put it",
                "CONT", foundObject.get());

    }

    @Test
    public void emptyResultForNotExistingKey() {
        Optional<String> foundObject = cache.get("foo", String.class);
        assertFalse("Nothing is found, since objects were not put in cache and do not exist",
                foundObject.isPresent());
    }

    @Test
    public void insertDifferentValuesForSameKey() {
        cache.put("idea", "AA");
        Optional<String> foundObject = cache.get("idea", String.class);
        assertEquals("Value present in cache is set to AA",
                "AA", foundObject.get());
        cache.put("idea", "BB");
        foundObject = cache.get("idea", String.class);
        assertEquals("Value present in cache after insert with previous key is set to BB",
                "BB", foundObject.get());
    }

    @Test
    public void insertAndDeleteKeyValue() {
        cache.put("foo", "BAR");
        Optional<String> foundObject = cache.get("foo", String.class);
        assertTrue("Key Object foo do not exist", foundObject.isPresent());

        cache.del("foo");
        foundObject = cache.get("foo", String.class);
        assertFalse("Nothing is found, since objects were deleted",
                foundObject.isPresent());
    }

    @Test
    public void checkSizeOfKeysInCache() {
        cache.put("qwerty", "QWERTY");
        assertTrue("Key Object 'qwerty' do not exist",
                cache.get("qwerty", String.class).isPresent());

        cache.put("zxc", "ZXC");
        assertTrue("Key Object 'zxc' do not exist",
                cache.get("qwerty", String.class).isPresent());
        assertEquals("Size of cache is  1", Long.valueOf(1), cache.len());
    }
}
