package com.peraton.cicd.service;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GitHubRepoRefTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "https://github.com/jaeminbird/Pulse",
            "https://github.com/jaeminbird/Pulse/",
            "https://github.com/jaeminbird/Pulse.git",
            "http://www.github.com/jaeminbird/Pulse",
            "github.com/jaeminbird/Pulse",
            "jaeminbird/Pulse",
            "  jaeminbird/Pulse  "
    })
    void parsesSupportedFormats(String input) {
        GitHubRepoRef ref = GitHubRepoRef.parse(input);

        assertThat(ref.owner()).isEqualTo("jaeminbird");
        assertThat(ref.repo()).isEqualTo("Pulse");
        assertThat(ref.htmlUrl()).isEqualTo("https://github.com/jaeminbird/Pulse");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "Pulse", "https://gitlab.com/a/b", "https://github.com/a/b/c", "a/b?x=1"})
    void rejectsInvalidInput(String input) {
        assertThatThrownBy(() -> GitHubRepoRef.parse(input))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
