package practice.virtual.thread;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CThreadPoolTest {

    @Test
    void fixedThreadPool() throws InterruptedException {
        AtomicInteger counter = new AtomicInteger(0);

        try (var executor = Executors.newFixedThreadPool(8)) {
            Stream.iterate(0, n -> n + 1).limit(100)
                    .forEach(_ -> executor.submit(counter::incrementAndGet));
            executor.shutdown(); // deamon 스레드가 아니므로 강제 종료
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }

        assertThat(counter.get()).isEqualTo(100);
    }

    // <주요 스레드 풀>
    // 종류::특징::용도
    // FixedThreadPool::고정된 수의 스레드 유지::부하가 일정할 때 사용
    // CachedThreadPool::필요할 때마다 생성, 노는 스레드는 제거::작업량이 들쑥날쑥할 때 유용
    // ScheduledThreadPool::일정 시간 뒤나 주기적으로 실행::예약 작업, 타이머
    // SingleThreadExecutor::단 하나의 스레드만 사용::순서대로 처리가 중요할 때

    @Test
    @DisplayName("단순 계산 작업에는 ForkJoinPool")
    void forkJoinPool() {
        long[] numbers = LongStream.rangeClosed(1, 1_000_000).toArray();
        long expectedSum = (1_000_000L * 1_000_001L) / 2;

        try (ForkJoinPool pool = new ForkJoinPool()) {
            SumTask task = new SumTask(numbers, 0, numbers.length);
            long actualSum = pool.invoke(task);

            assertEquals(expectedSum, actualSum);
        }
    }

    class SumTask extends RecursiveTask<Long> {
        private final long[] numbers;
        private final int start;
        private final int end;
        private static final int THRESHOLD = 10_000; // 이 값보다 작으면 직접 계산

        public SumTask(long[] numbers, int start, int end) {
            this.numbers = numbers;
            this.start = start;
            this.end = end;
        }

        @Override
        protected Long compute() {
            int length = end - start;

            // 기준보다 작으면 분할을 멈추고 직접 계산
            if (length <= THRESHOLD) {
                long sum = 0;
                for (int i = start; i < end; i++) {
                    sum += numbers[i];
                }
                return sum;
            }

            // 기준보다 크면 작업을 반으로 쪼갬 (Fork)
            int middle = start + length / 2;
            SumTask leftTask = new SumTask(numbers, start, middle);
            SumTask rightTask = new SumTask(numbers, middle, end);

            leftTask.fork(); // 왼쪽 작업을 별도 스레드에서 실행하도록 큐에 넣음
            long rightResult = rightTask.compute(); // 오른쪽 작업은 현재 스레드에서 직접 수행
            long leftResult = leftTask.join(); // 왼쪽 작업의 결과를 기다림 (Join)

            return leftResult + rightResult;
        }

    } // end SumTask

    @Test
    @DisplayName("Stream은 ForkJoinPool에서 동작한다")
    void streamParallel() {
        long sumResult = LongStream.rangeClosed(1, 1_000_000).parallel().sum();
        long expectedSum = (1_000_000L * 1_000_001L) / 2;

        assertThat(sumResult).isEqualTo(expectedSum);
    }

    // 병렬 스트림이나 CompletableFuture에서 별도 풀을 지정하지 않으면 
    // ForkJoinPool.commonPool()을 공유합니다. 만약 이 공통 풀에서 I/O 작업(네트워크 대기 등)을 수행하면 
    // 시스템 전체의 병렬 스트림 성능이 급격히 저하될 수 있습니다. 
    // CPU 계산 작업이 아닌 경우에는 별도의 ExecutorService(또는 가상 스레드)를 사용하는 것이 좋습니다.

    @Test
    @DisplayName("stream에서 parallel을 사용하는 것은 위험할 수 도 있다.")
    void streamParallelUsingAnotherForkJoinPool() throws InterruptedException, ExecutionException {
        ForkJoinPool anotherPool = new ForkJoinPool(4);
        long result =
                anotherPool.submit(() -> LongStream.rangeClosed(1, 1_000_000).parallel().peek(n -> {
                    if (n % 250_000 == 0) {
                        System.out.println(Thread.currentThread().getName() + "가 처리 중: " + n);
                    }
                }).sum()).get();

        anotherPool.shutdown(); // 필수

        assertThat(result).isEqualTo(500000500000L);
    }

// 예외 발생 시 스레드 풀의 반응
// - execute(): 예외 발생 시 스레드가 죽고 새로 생성됨 (로그에 찍힘)
// - submit(): 예외를 Future 안에 삼키고 아무 일 없다는 듯 스레드를 유지함 (결과를 get() 해야만 확인 가능)

}
