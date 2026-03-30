package com.huawei.ai_platform.rss;

import com.huawei.ai_platform.rss.infrastructure.job.RssJob;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class BootTest {
    @Autowired
    private RssJob rssJob;

    @Test
    public void foo() {
        rssJob.runTranslation();
    }
}
