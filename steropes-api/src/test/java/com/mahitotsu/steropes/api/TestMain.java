package com.mahitotsu.steropes.api;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;

import com.mahitotsu.steropes.api.config.DataConfig;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Import({DataConfig.class})
public abstract class TestMain {

}