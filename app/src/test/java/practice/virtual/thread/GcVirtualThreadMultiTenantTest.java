package practice.virtual.thread;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

class GcVirtualThreadMultiTenantTest {
    
    @Test
    void multiTenant() {
        String tenantId = "SomeCompany";
        Runnable logic = () -> {
            DataSourceManager manager = new DataSourceManager();
            manager.connect();
        };

        TenantAspect tenant = new TenantAspect();
        tenant.processTenantRequest(tenantId, logic);

        assertThat(TenantAspect.TENANT_ID.isBound()).isFalse();
    }
}

class TenantAspect {
    public static final ScopedValue<String> TENANT_ID = ScopedValue.newInstance();

    public void processTenantRequest(String tenantId, Runnable logic) {
        ScopedValue.where(TENANT_ID, tenantId).run(logic);
    }
}

class DataSourceManager {
    public void connect() {
        // 현재 실행 맥락의 TenantID에 따라 동적으로 커넥션 제공
        String tenant = TenantAspect.TENANT_ID.get();
        System.out.println(tenant + " 전용 데이터베이스에 연결 중...");
    }
}