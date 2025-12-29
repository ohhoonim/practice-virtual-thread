package practice.virtual.thread;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class BThreadStateTest {

    @Test
    void threadState() throws InterruptedException {
        Runnable task = () -> {
            try {
                Thread.sleep(1000); // TIMED_WAITING
            } catch (InterruptedException e) {
            }
        };
        Thread branch = new Thread(task);
        assertThat(branch.getState()).isEqualTo(Thread.State.NEW);

        branch.start();
        assertThat(branch.getState()).isEqualTo(Thread.State.RUNNABLE);

        Thread.sleep(500);
        assertThat(branch.getState()).isEqualTo(Thread.State.TIMED_WAITING);

        branch.join(); // WAITING
        assertThat(branch.getState()).isEqualTo(Thread.State.TERMINATED);
    }

    @Test
    void threadInterrupted() throws InterruptedException {
        AtomicLong count = new AtomicLong();

        Runnable task = () -> {
            while(!Thread.currentThread().isInterrupted()) {
               count.getAndIncrement(); 
               System.out.println(count.get());
            }
        };

        Thread counter = new Thread(task);
        counter.start();
        Thread.sleep(10); 
        counter.interrupt();

        assertThat(count.get()).isGreaterThan(1L);
    }


}
