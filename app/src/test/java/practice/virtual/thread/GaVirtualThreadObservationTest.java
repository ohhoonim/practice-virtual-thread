package practice.virtual.thread;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GaVirtualThreadObservationTest {

    @Test
    void observationTest() {
        ObservationAspect observation = new ObservationAspect();
        observation.trace(() -> {
            OrderService order = new OrderService();
            order.create();
        });

        assertThat(ObservationAspect.TX_ID.isBound()).isFalse();
    }
    
}

class ObservationAspect {
    public static final ScopedValue<String> TX_ID = ScopedValue.newInstance();

    public void trace(Runnable businessLogic) {
        String generatedId = UUID.randomUUID().toString();
        ScopedValue.where(TX_ID, generatedId).run(businessLogic);
    }
}

class OrderService {
    public void create() {
        System.out.println("[LOG] TraceId" + ObservationAspect.TX_ID.get());
    }
}