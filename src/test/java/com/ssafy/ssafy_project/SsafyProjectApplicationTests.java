package com.ssafy.ssafy_project;

import com.ssafy.ssafy_project.support.TestInfraConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestInfraConfig.class)
class SsafyProjectApplicationTests {

    @Test
    void contextLoads() {
    }

}
