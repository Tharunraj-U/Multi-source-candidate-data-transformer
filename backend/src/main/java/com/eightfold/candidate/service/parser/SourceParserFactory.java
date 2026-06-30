package com.eightfold.candidate.service.parser;

import com.eightfold.candidate.domain.enums.SourceType;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class SourceParserFactory {

    private final Map<SourceType, SourceParser> parsers = new EnumMap<>(SourceType.class);

    public SourceParserFactory(List<SourceParser> parserList) {
        for (SourceType type : SourceType.values()) {
            parserList.stream()
                    .filter(p -> p.supports(type))
                    .findFirst()
                    .ifPresent(p -> parsers.put(type, p));
        }
    }

    public SourceParser getParser(SourceType type) {
        SourceParser parser = parsers.get(type);
        if (parser == null) {
            throw new IllegalArgumentException("No parser registered for source type: " + type);
        }
        return parser;
    }
}
