package practice.virtual.thread;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EVirtualThreadScopedValueTest {

    private static final ScopedValue<String> CONTEXT = ScopedValue.newInstance();

    @Test
    @DisplayName("ScopedValue는 지정된 run 블록 내에서만 유효하다")
    void scopedValueLifecycle() {

        assertThat(CONTEXT.isBound()).isFalse(); // 범위 밖

        ScopedValue.where(CONTEXT, "Test-Data").run(() -> {
            assertThat(CONTEXT.isBound()).isTrue();
            assertThat(CONTEXT.get()).isEqualTo("Test-Data");
            // ScopedValue는 set() 메서드가 없어 값 변경이 불가능(불변성)
        });

        assertThat(CONTEXT.isBound()).isFalse(); // 다시 범위 밖
    }

    @Test
    @DisplayName("바인딩되지 않은 ScopedValue에 접근하면 예외가 발생")
    void accessUnboundScopedValue() {
        assertThatThrownBy(CONTEXT::get).isInstanceOf(java.util.NoSuchElementException.class);
    }

    private static final ScopedValue<String> TRACE_ID = ScopedValue.newInstance();

    @Test
    @DisplayName("ScopedValue는 where-run 블록을 벗어나면 즉시 해제되어야 한다")
    void scopeLifecycleTest() {
        String testId = "REQ-101";

        // 1. 바인딩 전
        assertThat(TRACE_ID.isBound()).isFalse();

        // 2. 바인딩 구역
        ScopedValue.where(TRACE_ID, testId).run(() -> {
            assertThat(TRACE_ID.get()).isEqualTo(testId);

            // 중첩 바인딩 테스트
            ScopedValue.where(TRACE_ID, "REQ-102").run(() -> {
                assertThat(TRACE_ID.get()).isEqualTo("REQ-102");
            });

            // 중첩을 빠져나오면 이전 값 유지
            assertThat(TRACE_ID.get()).isEqualTo(testId);
        });

        // 3. 바인딩 해제 후
        assertThat(TRACE_ID.isBound()).isFalse();
        assertThatThrownBy(TRACE_ID::get).isInstanceOf(NoSuchElementException.class);
    }

    // scoped value는 '전역적으로 선언하고, 지역적으로 바인딩하여 사용한다'

    // 다음의 예시와 같이 context 클래스를 만들어 static 필드들을 모아 관리 

    // public class UserContext {
    //     // 1. static final로 선언하여 전역 어디서든 참조 가능하게 함
    //     public static final ScopedValue<User> CURRENT_USER = ScopedValue.newInstance();
    //     public static final ScopedValue<String> TRACE_ID = ScopedValue.newInstance();

    //     private UserContext() {} // 인스턴스화 방지
    // }

    // // --- 사용처 ---
    // public class OrderService {
    //     public void createOrder() {
    //         // 어디서든 클래스명으로 접근하여 안전하게 값을 꺼냄
    //         User user = UserContext.CURRENT_USER.get();
    //         String traceId = UserContext.TRACE_ID.get();
    //         System.out.println("Processing order for: " + user.name());
    //     }
    // }
}

// <ScopedValue 주요 활용 분야 분석> 
// 소스는 G****Test.java 참고
//
// 관찰 가능성 (Obserability) : 분산 시스템에서의 요청 흐름 추적  : TransactionID
// 보안 및 인증(Security) : 현재 요청을 수행하는 주체의 권한 검증 : UserPrincipal, Role, AuthToken
// 테넌트 격리 : 멀티테넌트 환경에서 데이터베이스나 설정 분리 : TenantID, RegionCode
// 설정 및 환경 : 특정 실행 맥락에서만 유요햔 정책 제어 : TimeoutValue, RetryPolicy

