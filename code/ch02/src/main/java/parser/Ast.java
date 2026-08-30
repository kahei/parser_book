package parser;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class Ast {
    // value
    public interface JsonValue {}
    
    // NULL
    public static class JsonNull implements JsonValue {
        private JsonNull(){}
        private static final JsonNull INSTANCE = new JsonNull();
        public static JsonNull getInstance() {
            return INSTANCE;
        }
        
        @Override
        public String toString() {
            return "null";
        }
    }
    
    
    // TRUE
    public static class JsonTrue implements JsonValue {
        private JsonTrue(){}
        private static final JsonTrue INSTANCE = new JsonTrue();
        public static JsonTrue getInstance() {
            return INSTANCE;
        }
    
        @Override
        public String toString() {
            return "true";
        }
    }
    
    // FALSE
    public static class JsonFalse implements JsonValue {
        private JsonFalse(){}
        private static final JsonFalse INSTANCE = new JsonFalse();
        public static JsonFalse getInstance() {
            return INSTANCE;
        }
    
        @Override
        public String toString() {
            return "false";
        }
    }
    
    // NUMBER
    public static class JsonNumber implements JsonValue {
        public final double value;
        public JsonNumber(double value) {
            this.value = value;
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            JsonNumber that = (JsonNumber) o;
            return Double.compare(that.value, value) == 0;
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(value);
        }
    
        @Override
        public String toString() {
            return "JsonNumber{" +
                "value=" + value +
                '}';
        }
    }
    
    // STRING
    public static class JsonString implements JsonValue {
        public final String value;
        public JsonString(String value) {
            this.value = value;
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            JsonString that = (JsonString) o;
            return Objects.equals(value, that.value);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(value);
        }
    
        @Override
        public String toString() {
            return "JsonString{" +
                "value='" + value + '\'' +
                '}';
        }
    }
    
    // object
    public static class JsonObject implements JsonValue {
        public final List<Pair<JsonString, JsonValue>> properties;
        public JsonObject(List<Pair<JsonString, JsonValue>> properties) {
            this.properties = properties;
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            JsonObject object = (JsonObject) o;
            return Objects.equals(properties, object.properties);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(properties);
        }
    
        @Override
        public String toString() {
            return "JsonObject{" +
                "properties=" + properties +
                '}';
        }
    }
    
    // array
    public static class JsonArray implements JsonValue {
        public final List<JsonValue> elements;
        public JsonArray(List<JsonValue> elements) {
            this.elements = elements;
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            JsonArray jsonArray = (JsonArray) o;
            return Objects.equals(elements, jsonArray.elements);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(elements);
        }
    
        @Override
        public String toString() {
            return "JsonArray{" +
                "elements=" + elements +
                '}';
        }
    }
}
