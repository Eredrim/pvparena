package net.slipcor.pvparena.core;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;


@ExtendWith(MockitoExtension.class)
public class VersionUtilsTest {

    @ParameterizedTest
    @MethodSource("argumentsForSameVersionOrNewer")
    void isSameVersionOrNewer(String currentVersion, String newVersion, boolean expected) {
        assertThat(VersionUtils.isSameVersionOrNewer(currentVersion, newVersion)).isEqualTo(expected);
    }

    private static Stream<Arguments> argumentsForSameVersionOrNewer() {
        return Stream.of(
                Arguments.of("1.0.0", "1.0.0", true),
                Arguments.of("1.0.1", "1.0.0", true),
                Arguments.of("1.0.0", "1.0.1", false),
                Arguments.of("1.0.0-SNAPSHOT", "1.0.0", false),
                Arguments.of("1.0.1-SNAPSHOT", "1.0.0", true),
                Arguments.of("2.0.0", "1.9.9", true),
                Arguments.of("1.9.9", "2.0.0", false),
                Arguments.of("2.0.0", "1.9.99", true),
                Arguments.of("1.9.99", "2.0.0", false),
                Arguments.of("1.10.0", "1.9.99", true),
                Arguments.of("1.9.99", "1.10.0", false),
                Arguments.of("2.0.0", "1.99.9", true),
                Arguments.of("1.99.9", "2.0.0", false)
        );
    }
}
