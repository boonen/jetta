package nl.janboonen.jetta.ijhttp;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IntellijHttpClientTestSupportTest {

    @Test
    void constructorWithoutAnnotation_throwsIllegalStateException() {
        assertThatThrownBy(Fixtures.UnannotatedIT::new)
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void getPort_returnsServicePortFromAnnotation_whenNotOverridden() {
        var instance = new Fixtures.DefaultIntelliJHttpClientIT();
        assertThat(instance.getPort()).isEqualTo(8080);
    }

}
