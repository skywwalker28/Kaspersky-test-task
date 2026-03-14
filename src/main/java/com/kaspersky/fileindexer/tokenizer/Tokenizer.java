package com.kaspersky.fileindexer.tokenizer;

import java.util.Set;

public interface Tokenizer {
    Set<String> tokenizer(String text);
}
