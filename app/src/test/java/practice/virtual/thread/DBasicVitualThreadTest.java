package practice.virtual.thread;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DBasicVitualThreadTest {

    @Test
    void basicUsingVirtualThread() throws InterruptedException {
        Thread vThread = Thread.ofVirtual().start(() -> System.out.println("virtual thread"));
        vThread.join(); // virtual thread는 deamon 이다 
        assertThat(vThread.isVirtual()).isTrue();
    }

    @Test
    void executorVirtualThread() throws InterruptedException, ExecutionException {
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<Boolean> future = executor.submit(() -> Thread.currentThread().isVirtual());
            assertThat(future.get()).isTrue();
        }
    }

    @Test
    @DisplayName("병렬 스트림을 실행하는 주체를 가상 스레드로 만들기 - 계산 작업에 유리")
    void parallelStreamInsiceVirtualThread() throws InterruptedException, ExecutionException {
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<Long> future = executor.submit(() -> {
                return LongStream.range(1, 1000).parallel().sum();
            });
            assertThat(future.get()).isEqualTo(499500L);
        }
    }

    @Test
    @DisplayName("I/O 작업은 parallel보다 가상 스레드 처리가 효율적이다")
    void virtualThreadParallelForIO() throws InterruptedException, ExecutionException {
        List<Integer> data = IntStream.range(0, 100).boxed().toList();

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<String>> futures = data.stream().map(num -> executor.submit(() -> {
                Thread.sleep(2000); // IO작업중이라 시간이 걸린다고 가정하자
                return "Result-" + num;
            })).toList();
            assertThat(futures).hasSize(100);
            assertThat(futures.get(12).get()).isEqualTo("Result-12");

        }
    }

    // 병렬 스트림에 가상 스레드를 넣지 않는 이유
    // - 병렬 스트림은 CPU 코어를 100% 활용하기 위한 분할 정복(ForkJoin) 모델이다. 
    // - 반면 가상 스레드는 I/O 대기 시간(Blocking)을 효율적으로 넘기기 위한 모델입니다.
    // - 오히려 성능 저하 위험이 있다
    // 결론 : 전체 흐름은 가상 스레드로 관리하고, 내부 계산만 할 경우 parallel() 활용 

    @Test
    @DisplayName("가상 스레드는 내부적으로 ForkJoinPool 스케줄러를 사용한다")
    void confirmVirtualThreadScheduler() throws Exception {
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<String> threadInfo = executor.submit(() -> Thread.currentThread().toString());

            // VirtualThread[#ID]/runnable@ForkJoinPool-1-worker-1 형태인지 확인
            assertThat(threadInfo.get()).contains("VirtualThread").contains("ForkJoinPool")
                    .contains("worker");
        }
    }

    @Test
    @DisplayName("synchronized 블록 안에서 블로킹 작업 시 캐리어 스레드가 점유(Pinning)될 수 있다(JDK 24에서 해결됨")
    void virtualThreadPinningWarning() throws Exception {
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var futureString = executor.submit(() -> {
                // JDK 24 에서 해결됨.(JEP 491) 
                synchronized (this) { // synchronized는 캐리어 스레드를 고정(Pin)시킴
                    Thread.sleep(100); // 여기서 캐리어 스레드에서 가상스레드가 Unmount 되지 않음
                    return "Pinning 발생 가능";
                }
            }).get();
            assertThat(futureString).isEqualTo("Pinning 발생 가능");
        }
    }
}
