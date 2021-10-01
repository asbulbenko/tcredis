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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class RedisTestContainerTest {

    private static GenericContainer<?> redis;
    private static final Logger LOGGER = LoggerFactory.getLogger(RedisTestContainerTest.class);

    private Cache cache;
    private Jedis jedis;


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
        jedis = new Jedis(redis.getContainerIpAddress(), redis.getMappedPort(6379));
        cache = new RedisBackedCache(jedis, "test");
    }

    @Test
    public void insertAndFindValueInCache() {
        cache.put("testcont", "CONT");

        Optional<String> foundObject = cache.get("testcont", String.class);

        assertThat("Objects are found and exists in cache", foundObject.isPresent(), is(Boolean.TRUE));
        assertThat("The value retrieved from cache is the same before we put it",
                foundObject.get(), equalTo("CONT"));
    }

    @Test
    public void emptyResultForNotExistingKey() {
        Optional<String> foundObject = cache.get("foo", String.class);
        assertThat("Nothing is found, since objects were not put in cache and do not exist",
                foundObject.isPresent(), is(Boolean.FALSE));
    }

    @Test
    public void insertDifferentValuesForSameKey() {
        cache.put("idea", "AA");
        Optional<String> foundObject = cache.get("idea", String.class);
        assertThat("Value present in cache is set to AA",
                foundObject.get(), equalTo("AA"));
        cache.put("idea", "BB");
        foundObject = cache.get("idea", String.class);
        assertThat("Value present in cache after insert with previous key is set to BB",
                foundObject.get(), equalTo("BB"));
    }

    @Test
    public void insertAndDeleteKeyValue() {
        cache.put("foo", "BAR");
        Optional<String> foundObject = cache.get("foo", String.class);
        assertThat("Key Object foo do not exist", foundObject.isPresent(), is(Boolean.TRUE));

        cache.del("foo");
        foundObject = cache.get("foo", String.class);
        assertThat("Objects are found, but should be deleted",
                foundObject.isPresent(), is(Boolean.FALSE));
    }

    @Test
    public void checkSizeOfKeysInCache() {
        // create new cache name
        cache = new RedisBackedCache(jedis, "check");

        // insert first Object value
        cache.put("qwerty", "QWERTY");
        assertThat("Key Object 'qwerty' do not exist",
                cache.get("qwerty", String.class).isPresent(), is(Boolean.TRUE));
        // insert second Object value
        cache.put("zxc", "ZXC");
        assertThat("Key Object 'zxc' do not exist",
                cache.get("qwerty", String.class).isPresent(), is(Boolean.TRUE));
        assertThat("Cache is holding 2 keys", cache.len(), equalTo(2L));
    }
}
