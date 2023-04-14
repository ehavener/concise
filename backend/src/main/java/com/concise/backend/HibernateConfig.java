package com.concise.backend;

import com.concise.backend.model.UserEntity;
import com.concise.backend.model.VideoEntity;
import com.concise.backend.model.ChapterEntity;
import org.hibernate.SessionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HibernateConfig {
    @Bean
    public SessionFactory createSessionFactory() {
        SessionFactory factory = new org.hibernate.cfg.Configuration()
                .configure("hibernate.cfg.xml")
                .addAnnotatedClass(UserEntity.class)
                .addAnnotatedClass(VideoEntity.class)
                .addAnnotatedClass(ChapterEntity.class)
                .buildSessionFactory();
        return factory;
    }
}