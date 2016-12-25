import com.amirkhawaja.RedisQueue;
import com.amirkhawaja.models.Message;
import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.IOException;

@RunWith(JUnit4.class)
public class RedisQueueTests {

    private final static String CONNECTION_URI = "redis://localhost:6379/1";
    private static RedisQueue queue;

    @BeforeClass
    public static void beforeTestCase() {
        queue = new RedisQueue(CONNECTION_URI);
    }

    @AfterClass
    public static void afterTestCase() {
        try {
            queue.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Before
    public void beforeEachTest() {
        queue.drainQueues();
    }

    @After
    public void afterEachTest() {
        queue.drainQueues();
    }

    @Test
    public void testWorkingQueueIsEmpty() {
        Assert.assertTrue(queue.getWorkingQueueSize() == 0);
    }

    @Test
    public void testPendingQueueIsEmpty() {
        Assert.assertTrue(queue.getPendingQueueSize() == 0);
    }

    @Test
    public void testMessageIsQueuedCorrectly() {
        queue.enqueue("test", "This is a test");
        Assert.assertTrue(queue.getPendingQueueSize() == 1);
    }

    @Test
    public void testMessageContentsAreRetrievedAndQueueSizeIsWithinExpectation() {
        final String data = "This is a test";
        final String topic = "Test Topic";

        queue.enqueue(topic, data);
        Assert.assertTrue(queue.getPendingQueueSize() == 1);

        final Message message = queue.dequeue();
        Assert.assertNotNull(message);
        Assert.assertTrue(queue.getPendingQueueSize() == 0);
        Assert.assertTrue(queue.getWorkingQueueSize() == 1);

        Assert.assertTrue(message.getTopic().equals(topic));
        Assert.assertTrue(message.getData().equals(data));

        queue.release(message.getUuid());
        Assert.assertTrue(queue.getPendingQueueSize() == 0);
        Assert.assertTrue(queue.getWorkingQueueSize() == 0);
    }

    @Test
    public void testMessageContentsIsRequeuedCorrectly() {
        final String data = "This is a test";
        final String topic = "Test Requeue";

        queue.enqueue(topic, data);
        Assert.assertTrue(queue.getPendingQueueSize() == 1);

        final Message message = queue.dequeue();
        Assert.assertNotNull(message);
        Assert.assertTrue(queue.getPendingQueueSize() == 0);
        Assert.assertTrue(queue.getWorkingQueueSize() == 1);

        Assert.assertTrue(message.getTopic().equals(topic));
        Assert.assertTrue(message.getData().equals(data));

        queue.requeue(String.valueOf(message.getUuid()));
        Assert.assertTrue(queue.getPendingQueueSize() == 1);
    }

}
