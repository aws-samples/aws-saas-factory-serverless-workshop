/**
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify,
 * merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.amazon.aws.partners.saasfactory.configuration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertiesPropertySource;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParameterResponse;

import java.util.Properties;

/**
 * This class will execute early as Spring boots up prior to the dispatcher
 * servlet coming online. You must list any post processors in spring.factories
 * in META-INF for it to be picked up.
 * @author mibeard
 */
public class ParameterStoreSecretsConfig implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String awsRegion = environment.getProperty("AWS_REGION");

        // This value is passed as the DB_PASS environment variable, but
        // it's not really the password, it's the parameter name for SSM
        String appPwParam = environment.getProperty("spring.datasource.password");

        // Fetch the secret value for the application user database
        // password from parameter store.
        SsmClient ssm = SsmClient.builder().region(Region.of(awsRegion)).build();
        GetParameterResponse response = ssm.getParameter(builder -> builder
                .name(appPwParam)
                .withDecryption(Boolean.TRUE)
        );
        String decryptedAppPassword = response.parameter().value();

        // Create properties with the same names as we launched Spring with
        Properties decryptedProps = new Properties();
        decryptedProps.put("spring.datasource.password", decryptedAppPassword);

        // Now replace the existing environment variables with our new values
        environment.getPropertySources().addFirst(new PropertiesPropertySource("myProps", decryptedProps));
    }

}

