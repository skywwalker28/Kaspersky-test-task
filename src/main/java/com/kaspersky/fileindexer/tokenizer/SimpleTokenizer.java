package com.kaspersky.fileindexer.tokenizer;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public class SimpleTokenizer implements Tokenizer {
    @Override
    public Set<String> tokenizer(String text) {
        return Arrays.stream(text.split("\\W+"))
                .map(word -> word.toLowerCase(Locale.ROOT).trim())
                .filter(word -> !word.isEmpty())
                .collect(Collectors.toSet());
    }
}
