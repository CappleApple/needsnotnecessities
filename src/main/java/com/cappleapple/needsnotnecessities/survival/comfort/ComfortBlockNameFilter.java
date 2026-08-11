package com.cappleapple.needsnotnecessities.survival.comfort;

import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import net.minecraft.resources.ResourceLocation;

/** A reload-time block-ID classifier supplied by the standalone automatic comfort configuration. */
public final class ComfortBlockNameFilter {
    private final String namespaceRegex;
    private final String pathRegex;
    private final Optional<String> excludePathRegex;
    private final Pattern namespacePattern;
    private final Pattern pathPattern;
    private final Optional<Pattern> excludePathPattern;

    private ComfortBlockNameFilter(
            String namespaceRegex,
            String pathRegex,
            Optional<String> excludePathRegex,
            Pattern namespacePattern,
            Pattern pathPattern,
            Optional<Pattern> excludePathPattern) {
        this.namespaceRegex = namespaceRegex;
        this.pathRegex = pathRegex;
        this.excludePathRegex = excludePathRegex;
        this.namespacePattern = namespacePattern;
        this.pathPattern = pathPattern;
        this.excludePathPattern = excludePathPattern;
    }

    public static ComfortBlockNameFilter compile(
            String namespaceRegex,
            String pathRegex,
            Optional<String> excludePathRegex) {
        Objects.requireNonNull(namespaceRegex, "namespaceRegex");
        Objects.requireNonNull(pathRegex, "pathRegex");
        Objects.requireNonNull(excludePathRegex, "excludePathRegex");
        if (namespaceRegex.isBlank() || pathRegex.isBlank()
                || excludePathRegex.filter(String::isBlank).isPresent()) {
            throw new IllegalArgumentException("Comfort auto-match regexes cannot be blank");
        }
        try {
            return new ComfortBlockNameFilter(
                    namespaceRegex,
                    pathRegex,
                    excludePathRegex,
                    Pattern.compile(namespaceRegex),
                    Pattern.compile(pathRegex),
                    excludePathRegex.map(Pattern::compile));
        } catch (PatternSyntaxException exception) {
            throw new IllegalArgumentException("Invalid comfort auto-match regex: " + exception.getMessage(), exception);
        }
    }

    public boolean matches(ResourceLocation blockId) {
        return namespacePattern.matcher(blockId.getNamespace()).matches()
                && pathPattern.matcher(blockId.getPath()).find()
                && excludePathPattern.map(pattern -> !pattern.matcher(blockId.getPath()).find()).orElse(true);
    }

    public String namespaceRegex() {
        return namespaceRegex;
    }

    public String pathRegex() {
        return pathRegex;
    }

    public Optional<String> excludePathRegex() {
        return excludePathRegex;
    }
}
