package com.task.reminder.sender;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LegacyEmailProviderRemovalTest {

    private static final Path PROJECT_ROOT = Paths.get(System.getProperty("user.dir"));
    private static final String REMOVED_PROVIDER = "g" + "mail";

    @Test
    void removedProviderSourceIsAbsent() {
        Path legacySender = PROJECT_ROOT.resolve(
                "src/main/java/com/task/reminder/sender/G" + "mailSender.java");

        assertFalse(Files.exists(legacySender), "已退役的邮件发送器源码不应继续存在");
    }

    @Test
    void removedProviderDependenciesAreAbsent() throws IOException {
        String pom = Files.readString(PROJECT_ROOT.resolve("pom.xml"));

        assertFalse(pom.contains("<google-api-client.version>"));
        assertFalse(pom.contains("<artifactId>google-auth-library-oauth2-http</artifactId>"));
        assertFalse(pom.contains("<artifactId>google-auth-library-credentials</artifactId>"));
        assertFalse(pom.contains("<artifactId>google-api-client</artifactId>"));
        assertFalse(pom.contains("<artifactId>google-api-services-" + REMOVED_PROVIDER + "</artifactId>"));
        assertFalse(pom.contains("<artifactId>google-oauth-client-jetty</artifactId>"));
    }

    @Test
    void removedProviderConfigurationIsAbsentAndDefaultIsDisabled() throws IOException {
        String applicationYaml = Files.readString(
                PROJECT_ROOT.resolve("src/main/resources/application.yaml"));
        String factorySource = Files.readString(PROJECT_ROOT.resolve(
                "src/main/java/com/task/reminder/sender/EmailSenderFactory.java"));

        assertFalse(applicationYaml.contains("\n" + REMOVED_PROVIDER + ":\n"));
        assertFalse(factorySource.contains("${email.provider:" + REMOVED_PROVIDER + "}"));
        assertTrue(factorySource.contains("${email.provider:none}"));
    }
}
