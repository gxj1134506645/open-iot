package com.openiot.connect.parser;

import com.openiot.connect.parser.impl.BinaryParser;
import com.openiot.connect.parser.impl.JavaScriptParser;
import com.openiot.connect.parser.impl.JsonPathParser;
import com.openiot.connect.parser.impl.RegexParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 解析器工厂
 * 根据规则类型获取对应的解析器实现
 *
 * @author open-iot
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ParserFactory {

    private final JsonPathParser jsonPathParser;
    private final JavaScriptParser javaScriptParser;
    private final RegexParser regexParser;
    private final BinaryParser binaryParser;

    /**
     * 解析器映射表（按类型）
     */
    private Map<String, ParseRuleEngine> parserMap;

    /**
     * 初始化解析器映射表
     */
    private Map<String, ParseRuleEngine> getParserMap() {
        if (parserMap == null) {
            parserMap = List.of(jsonPathParser, javaScriptParser, regexParser, binaryParser)
                    .stream()
                    .collect(Collectors.toMap(
                            ParseRuleEngine::getType,
                            Function.identity()
                    ));
        }
        return parserMap;
    }

    /**
     * 根据规则类型获取解析器
     *
     * @param ruleType 规则类型（JSON/JAVASCRIPT/REGEX/BINARY）
     * @return 对应的解析器
     * @throws IllegalArgumentException 如果规则类型不支持
     */
    public ParseRuleEngine getParser(String ruleType) {
        ParseRuleEngine parser = getParserMap().get(ruleType);
        if (parser == null) {
            throw new IllegalArgumentException("不支持的规则类型: " + ruleType);
        }
        return parser;
    }

    /**
     * 检查规则类型是否支持
     *
     * @param ruleType 规则类型
     * @return 是否支持
     */
    public boolean isSupported(String ruleType) {
        return getParserMap().containsKey(ruleType);
    }

    /**
     * 获取所有支持的规则类型
     *
     * @return 支持的规则类型列表
     */
    public List<String> getSupportedTypes() {
        return List.copyOf(getParserMap().keySet());
    }
}
