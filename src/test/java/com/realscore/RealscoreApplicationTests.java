package com.realscore;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(PostgresContainerConfig.class)
class RealscoreApplicationTests {

    @Test
    void contextLoads() {
    }
}
