package com.kaspersky.fileindexer.tokenizer;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class SimpleTokenizer implements Tokenizer {
    @Override
    public Set<String> tokenizer(String text) {
        return Arrays.stream(text.split("\\W+"))
                .map(String::toLowerCase)
                .filter(word -> !word.isEmpty())
                .collect(Collectors.toSet());
    }
}
