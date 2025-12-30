package practice.virtual.thread;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

class GbVirtualThreadUserPrincipalTest {

    @Test
    void userPrincipal() {

        var user = new User("matthew", true);
        Runnable task = () -> {
            DocumentService service = new DocumentService();
            service.delete(123123L);
        };

        SecurityAspect security = new SecurityAspect();
        security.runWithAuth(user, task);

        assertThat(SecurityAspect.CURRENT_USER.isBound()).isFalse();
    }
}


class SecurityAspect {
    public static final ScopedValue<User> CURRENT_USER = ScopedValue.newInstance();

    public void runWithAuth(User user, Runnable task) {
        ScopedValue.where(CURRENT_USER, user).run(task);
    }
}

class DocumentService {
    public void delete(Long docId) {
        User user = SecurityAspect.CURRENT_USER.get();
        if (!user.isAdmin()) {
            throw new SecurityException("권한이 없습니다: " + user.name());
        }
        System.out.println(docId + "번 문서 삭제 완료 (By " + user.name() + ")");
    }
}

record User(String name, Boolean isAdmin) {}