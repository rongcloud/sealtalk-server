package com.rcloud.server.sealtalk;

import com.rcloud.server.sealtalk.configuration.DatabaseCreatorApplicationContextInitializer;
import com.rcloud.server.sealtalk.util.SpringContextUtil;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;

@Import(SpringContextUtil.class)
@SpringBootApplication
@EnableScheduling
@MapperScan(basePackages = "com.rcloud.server.sealtalk.dao")
public class SealtalkServerApplication extends SpringBootServletInitializer {
    public static void main(String[] args) {
        new SpringApplicationBuilder(SealtalkServerApplication.class)
            .initializers(new DatabaseCreatorApplicationContextInitializer())
            .run(args);
    }

}
