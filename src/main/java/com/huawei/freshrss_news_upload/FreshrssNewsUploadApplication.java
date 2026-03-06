package com.huawei.freshrss_news_upload;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan({
        "com.huawei.freshrss_news_upload.rss"
})
public class FreshrssNewsUploadApplication {

	public static void main(String[] args) {
		SpringApplication.run(FreshrssNewsUploadApplication.class, args);
	}
}
