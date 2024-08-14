package com.limechain.utils.json;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JsonParser {

    private final String json;
    private int index;

    public JsonParser(String json) {
        this.json = json.trim();
        this.index = 0;
    }

    Object parse() {
        skipWhitespace();
        char currentChar = peek();
        if (currentChar == '{') {
            return parseObject();
        } else if (currentChar == '[') {
            return parseArray();
        } else if (currentChar == '"') {
            return parseString();
        } else if (Character.isDigit(currentChar) || currentChar == '-') {
            return parseNumber();
        } else if (json.startsWith("true", index)) {
            index += 4;
            return true;
        } else if (json.startsWith("false", index)) {
            index += 5;
            return false;
        } else if (json.startsWith("null", index)) {
            index += 4;
            return null;
        }
        throw new RuntimeException("Unexpected character: " + currentChar);
    }

    private Map<String, Object> parseObject() {
        Map<String, Object> object = new HashMap<>();
        index++; // Skip '{'
        skipWhitespace();
        if (peek() == '}') {
            index++; // Skip '}'
            return object;
        }
        while (true) {
            skipWhitespace();
            String key = parseString();
            skipWhitespace();
            if (peek() != ':') {
                throw new RuntimeException("Expected ':' after key in object");
            }
            index++; // Skip ':'
            skipWhitespace();
            Object value = parse();
            object.put(key, value);
            skipWhitespace();
            char currentChar = peek();
            if (currentChar == '}') {
                index++; // Skip '}'
                break;
            } else if (currentChar != ',') {
                throw new RuntimeException("Expected ',' or '}' in object");
            }
            index++; // Skip ','
        }
        return object;
    }

    private List<Object> parseArray() {
        List<Object> array = new ArrayList<>();
        index++; // Skip '['
        skipWhitespace();
        if (peek() == ']') {
            index++; // Skip ']'
            return array;
        }
        while (true) {
            skipWhitespace();
            Object value = parse();
            array.add(value);
            skipWhitespace();
            char currentChar = peek();
            if (currentChar == ']') {
                index++; // Skip ']'
                break;
            } else if (currentChar != ',') {
                throw new RuntimeException("Expected ',' or ']' in array");
            }
            index++; // Skip ','
        }
        return array;
    }

    private String parseString() {
        index++; // Skip '"'
        StringBuilder sb = new StringBuilder();
        while (true) {
            char currentChar = next();
            if (currentChar == '"') {
                break;
            } else if (currentChar == '\\') {
                char escapedChar = next();
                switch (escapedChar) {
                    case '"':
                        sb.append('"');
                        break;
                    case '\\':
                        sb.append('\\');
                        break;
                    case '/':
                        sb.append('/');
                        break;
                    case 'b':
                        sb.append('\b');
                        break;
                    case 'f':
                        sb.append('\f');
                        break;
                    case 'n':
                        sb.append('\n');
                        break;
                    case 'r':
                        sb.append('\r');
                        break;
                    case 't':
                        sb.append('\t');
                        break;
                    default:
                        throw new RuntimeException("Invalid escape character: " + escapedChar);
                }
            } else {
                sb.append(currentChar);
            }
        }
        return sb.toString();
    }

    private Object parseNumber() {
        int start = index;
        while (index < json.length() && isValidNumberChar()) {
            index++;
        }
        String numberStr = json.substring(start, index).trim();
        try {
            if (numberStr.contains(".") || numberStr.contains("e") || numberStr.contains("E")) {
                return Double.parseDouble(numberStr);
            } else {
                return numberStr;
            }
        } catch (NumberFormatException e) {
            throw new RuntimeException("Invalid number format: " + numberStr);
        }
    }

    private boolean isValidNumberChar() {
        return (Character.isDigit(json.charAt(index))
            || json.charAt(index) == '-'
            || json.charAt(index) == '.'
            || json.charAt(index) == 'e'
            || json.charAt(index) == 'E');
    }

    private void skipWhitespace() {
        while (index < json.length() && Character.isWhitespace(json.charAt(index))) {
            index++;
        }
    }

    private char peek() {
        if (index >= json.length()) {
            throw new RuntimeException("Unexpected end of input");
        }
        return json.charAt(index);
    }

    private char next() {
        char currentChar = peek();
        index++;
        return currentChar;
    }
}