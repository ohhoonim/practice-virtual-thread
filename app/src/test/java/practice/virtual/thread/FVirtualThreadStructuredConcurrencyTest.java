package practice.virtual.thread;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.StructuredTaskScope.Joiner;
import java.util.concurrent.StructuredTaskScope.Subtask;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FVirtualThreadStructuredConcurrencyTest {

    @Test
    void successAllTasks() throws Exception {
        try (var scope = StructuredTaskScope.open(Joiner.<String>allSuccessfulOrThrow())) {
            var task1 = scope.fork(() -> "A");
            var task2 = scope.fork(() -> "B");

            scope.join();

            assertThat(task1.get() + task2.get()).isEqualTo("AB");
        }
    }

    @Test
    @DisplayName("structured concurrency는 virtual thread로 동작한다.")
    void verifyForkUsesVirtualThread() throws InterruptedException {
        try(var scope = StructuredTaskScope.open()) {
            Subtask<ThreadInfo> task1 = scope.fork(() -> {
                var current = Thread.currentThread();
                return new ThreadInfo(
                    current.isVirtual(),
                    current.getName(),
                    current.toString()
                );
            });

            scope.join();

            assertThat(task1.get().isVirtual()).isTrue();
        }
    }

    record ThreadInfo(boolean isVirtual, String name, String fullString){}

}

// Structured Concurrency 
//
// - 에러전파(Error Propagation) : 자식 작업 중 하나가 실패하면 부모에게 알리고,
//      필요시 다른 자식 작업들을 자동으로 취소
// - 수명 주기 일치 (Lifetime Control) : 부모 스레드는 모든 자식 작업이 끝날때까지
//      블록 내에서 대기(실패든 성공이든 '미아스레드'가 발생하지 않음)
// - 가독성 : 비동기 코드를 마치 동기 코드(순차적 코드)처럼 읽히게 작성 가능 

// jdk25에서는 preview 기능이므로 --enable-preview 설정 필요
// 아래는 build.gradle 설정임
//
// ```
// tasks.withType(JavaCompile) {
//     options.compilerArgs += ["--enable-preview"]
// }
// test {
//     jvmArgs += ["--enable-preview"]
// }
// run {
//     jvmArgs += ["--enable-preview"]
// }
// ```