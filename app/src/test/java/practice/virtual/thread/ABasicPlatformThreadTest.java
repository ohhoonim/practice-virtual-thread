package practice.virtual.thread;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

class ABasicPlatformThreadTest {

    @Test
    void basicUsingPlatformThreadTest() {
        Runnable task = () -> System.out.println("branch thread");
        Thread branchThread = new Thread(task);
        // start() 메서드를 사용한다
        branchThread.start();

        assertTrue(true);
    }

    @Test
    void usingAtomic() throws InterruptedException {
        // atomic 클래스들은 전체 스레드가 변수를 thread safe 하게 공유할 때 사용 
        AtomicBoolean isRunned = new AtomicBoolean(false);
        Runnable sleepThread = () -> {
            try {
                Thread.sleep(1000);
                isRunned.set(true);
            } catch (InterruptedException e) {
                throw new RuntimeException("sleepThread is interrupted");
            }
        };
        Thread task = new Thread(sleepThread);
        task.start();

        assertFalse(isRunned.get());

        Thread.sleep(2000);
        // (참고) Platform thread는 메인 스레드가 끝나도 이 스레드가 살아있으면 
        //        프로그램이 종료되지 않음

        assertTrue(isRunned.get());
    }

    ThreadLocal<UUID> transactionId = new ThreadLocal<UUID>();

    @Test
    void threadLocalTest() {

        Function<String, Runnable> request = (String user) -> () -> {
            transactionId.set(UUID.randomUUID());
            System.out.println("transactionId: " + transactionId.get());
            System.out.println("name: " + user);

            OrderService order = new OrderService();
            order.orderProcess();

            transactionId.remove();
            System.out.println("[" + transactionId.get() + "] 주문 프로세스 종료");
        };

        Thread matthewRequest = new Thread(request.apply("matthew"));
        Thread alisonRequest = new Thread(request.apply("alison"));

        matthewRequest.start();
        alisonRequest.start();

        assertThat(transactionId.get()).isNull();

    }

    class OrderService {
        void orderProcess() {
            System.out.println("[" + transactionId.get() + "] 주문 프로세스 진행");
        }
    }

    @Test
    void callableThread() throws InterruptedException, ExecutionException {
        Callable<String> task = () -> "completed";
        FutureTask<String> futureTask = new FutureTask<>(task);

        Thread branch = new Thread(futureTask);
        branch.start();

        assertThat(futureTask.get()).isEqualTo("completed");
    }

    @Test
    void callableThreadException() {

        Callable<String> task = () -> {
            throw new IllegalStateException("error!!");
        };
        FutureTask<String> futureTask = new FutureTask<>(task);

        Thread branch = new Thread(futureTask);
        branch.start();

        assertThatThrownBy(futureTask::get).isInstanceOf(ExecutionException.class)
                .hasRootCauseInstanceOf(IllegalStateException.class).hasRootCauseMessage("error!!");
    }

}
