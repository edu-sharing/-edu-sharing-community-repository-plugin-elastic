package org.edu_sharing.elasticsearch.elasticsearch.utils;

import io.micrometer.core.instrument.util.StringUtils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.file.Path;
import java.util.*;

interface DataWriter {
    DataWriter startObject();

    DataWriter startArray();

    void writeFieldName(String name);

    void writeValue(Object value);

    Object getContainer();
}

class ArrayDataWriter implements DataWriter {

    final List<Object> list;

    public ArrayDataWriter(List<Object> list) {
        this.list = list;
    }


    @Override
    public DataWriter startObject() {
        Map<String, Object> newObject = new HashMap<>();
        writeValue(newObject);
        return new ObjectDataWriter(newObject);
    }

    @Override
    public DataWriter startArray() {
        List<Object> newArray = new ArrayList<>();
        writeValue(newArray);
        return new ArrayDataWriter(newArray);
    }

    @Override
    public void writeFieldName(String name) {
        throw new IllegalStateException("arrays don't support field names");
    }

    @Override
    public void writeValue(Object value) {
        list.add(value);
    }

    @Override
    public Object getContainer() {
        return list;
    }
}

class ObjectDataWriter implements DataWriter {
    final Map<String, Object> object;

    String fieldName;

    public ObjectDataWriter(Map<String, Object> object) {
        this.object = object;
    }


    private void ensureFieldNameNotNullOrEmpty() {
        if (StringUtils.isBlank(fieldName)) {
            throw new IllegalStateException("Field name not set");
        }
    }

    @Override
    public DataWriter startObject() {
        Map<String, Object> newObject = new HashMap<>();
        writeValue(newObject);
        return new ObjectDataWriter(newObject);
    }

    @Override
    public DataWriter startArray() {
        List<Object> newArray = new ArrayList<>();
        writeValue(newArray);
        return new ArrayDataWriter(newArray);
    }

    @Override
    public void writeFieldName(String name) {
        fieldName = name;
    }

    @Override
    public void writeValue(Object value) {
        ensureFieldNameNotNullOrEmpty();
        object.put(fieldName, value);
    }

    @Override
    public Object getContainer() {
        return object;
    }

}

class RootDataWriter implements DataWriter {

    @Override
    public DataWriter startObject() {
        return new ObjectDataWriter(new HashMap<>());
    }

    @Override
    public DataWriter startArray() {
        return new ArrayDataWriter(new ArrayList<>());
    }

    @Override
    public void writeFieldName(String name) {
        throw new IllegalStateException("no root element exists");
    }

    @Override
    public void writeValue(Object value) {
        throw new IllegalStateException("no root element exists");
    }

    @Override
    public Object getContainer() {
        throw new IllegalStateException("no root element exists");
    }
}

public class DataBuilder {

    private static final Map<Class<?>, Writer> WRITERS;

    static {
        WRITERS = new HashMap<>() {{
            put(Boolean.class, (b, v) -> b.value((Boolean) v));
            put(boolean[].class, (b, v) -> b.values((boolean[]) v));
            put(Byte.class, (b, v) -> b.value((Byte) v));
            put(byte[].class, (b, v) -> b.value((byte[]) v));
            put(Date.class, (b, v) -> b.value((Date) v));
            put(Date[].class, (b, v) -> b.values((Date[]) v));
            put(Double.class, (b, v) -> b.value((Double) v));
            put(double[].class, (b, v) -> b.values((double[]) v));
            put(Float.class, (b, v) -> b.value((Float) v));
            put(float[].class, (b, v) -> b.values((float[]) v));
            put(Integer.class, (b, v) -> b.value((Integer) v));
            put(int[].class, (b, v) -> b.values((int[]) v));
            put(Long.class, (b, v) -> b.value((Long) v));
            put(long[].class, (b, v) -> b.values((long[]) v));
            put(Short.class, (b, v) -> b.value((Short) v));
            put(short[].class, (b, v) -> b.values((short[]) v));
            put(String.class, (b, v) -> b.value((String) v));
            put(String[].class, (b, v) -> b.values((String[]) v));
            put(Locale.class, (b, v) -> b.value(v.toString()));
            put(Class.class, (b, v) -> b.value(v.toString()));
            put(BigInteger.class, (b, v) -> b.value((BigInteger) v));
            put(BigInteger[].class, (b, v) -> b.value((BigInteger[]) v));
            put(BigDecimal.class, (b, v) -> b.value((BigDecimal) v));
            put(BigDecimal[].class, (b, v) -> b.value((BigDecimal[]) v));
        }};
    }


    public Object build() {
        if(!stack.isEmpty()) {
            throw new IllegalStateException("Not all objects are closed");
        }

        return dataWriter.getContainer();
    }

    public interface Writer {
        void write(DataBuilder builder, Object value);
    }

    final Stack<DataWriter> stack = new Stack<>();
    DataWriter dataWriter = new RootDataWriter();

    public DataBuilder startObject(String name) {
        ensureNameNotNullOrEmpty(name);
        dataWriter.writeFieldName(name);
        startObject();
        return this;
    }

    public DataBuilder startObject() {
        DataWriter item = dataWriter.startObject();
        dataWriter = item;
        stack.push(item);
        return this;
    }

    public DataBuilder endObject() {
        if (stack.isEmpty()) {
            throw new IllegalStateException("no object to close");
        }

        if (!(stack.peek() instanceof ObjectDataWriter)) {
            throw new IllegalStateException("object already closed");
        }
        stack.pop();
        if(!stack.isEmpty()) {
            dataWriter = stack.peek();
        }
        return this;
    }

    public DataBuilder startArray(String name) {
        ensureNameNotNullOrEmpty(name);
        dataWriter.writeFieldName(name);
        startArray();
        return this;
    }

    public DataBuilder startArray() {
        DataWriter item = dataWriter.startArray();
        dataWriter = item;
        stack.push(item);
        return this;
    }

    public DataBuilder field(String name) {
        ensureNameNotNullOrEmpty(name);
        dataWriter.writeFieldName(name);
        return this;
    }


    private static void ensureNameNotNullOrEmpty(String name) {
        if (StringUtils.isBlank(name)) {
            throw new IllegalStateException("name can't be null or empty");
        }
    }


    public DataBuilder endArray() {
        if (stack.isEmpty()) {
            throw new IllegalStateException("no object to close");
        }

        if (!(stack.peek() instanceof ArrayDataWriter)) {
            throw new IllegalStateException("list already closed");
        }

        stack.pop();
        if(!stack.isEmpty()) {
            dataWriter = stack.peek();
        }
        return this;
    }


    private DataBuilder writeField(String name, Object value) {
        dataWriter.writeFieldName(name);
        dataWriter.writeValue(value);
        return this;
    }


    public DataBuilder field(String name, Object value) {
        return field(name).value(value);
    }

    public DataBuilder nullValue() {
        dataWriter.writeValue(null);
        return this;
    }

    ////////////////////////////////////////////////////////////////////////////
    // Boolean
    //////////////////////////////////
    public DataBuilder value(Boolean value) {
        dataWriter.writeValue(value);
        return this;
    }

    public DataBuilder value(boolean value) {
        dataWriter.writeValue(value);
        return this;
    }

    public DataBuilder array(String name, boolean[] values) {
        return field(name).values(values);
    }

    private DataBuilder values(boolean[] values) {
        if (values == null) {
            return nullValue();
        }
        startArray();
        for (boolean b : values) {
            value(b);
        }
        endArray();
        return this;
    }


    ////////////////////////////////////////////////////////////////////////////
    // Byte
    //////////////////////////////////

    public DataBuilder field(String name, Byte value) {
        return writeField(name, value);
    }

    public DataBuilder field(String name, byte value) {
        return writeField(name, value);
    }

    public DataBuilder value(Byte value) {
        dataWriter.writeValue(value);
        return this;
    }

    public DataBuilder value(byte value) {
        dataWriter.writeValue(value);
        return this;
    }

    ////////////////////////////////////////////////////////////////////////////
    // Binary
    //////////////////////////////////

    public DataBuilder field(String name, byte[] value) {
        return writeField(name, value != null ? Base64.getEncoder().encodeToString(value) : null);
    }

    public DataBuilder value(byte[] value) {
        dataWriter.writeValue( value != null ? Base64.getEncoder().encodeToString(value) : null);
        return this;
    }

    ////////////////////////////////////////////////////////////////////////////
    // Double
    //////////////////////////////////

    public DataBuilder field(String name, Double value) {
        return writeField(name, value);
    }

    public DataBuilder field(String name, double value) {
        return writeField(name, value);
    }

    public DataBuilder array(String name, double[] values) {
        return field(name).values(values);
    }

    private DataBuilder values(double[] values) {
        if (values == null) {
            return nullValue();
        }
        startArray();
        for (double b : values) {
            value(b);
        }
        endArray();
        return this;
    }

    public DataBuilder value(Double value) {
        dataWriter.writeValue(value);
        return this;
    }

    public DataBuilder value(double value) {
        dataWriter.writeValue(value);
        return this;
    }


    ////////////////////////////////////////////////////////////////////////////
    // Float
    //////////////////////////////////

    public DataBuilder field(String name, Float value) {
        return writeField(name, value);
    }

    public DataBuilder field(String name, float value) {
        return writeField(name, value);
    }

    public DataBuilder array(String name, float[] values) {
        return field(name).values(values);
    }

    private DataBuilder values(float[] values) {
        if (values == null) {
            return nullValue();
        }
        startArray();
        for (float f : values) {
            value(f);
        }
        endArray();
        return this;
    }

    public DataBuilder value(Float value) {
        dataWriter.writeValue(value);
        return this;
    }

    public DataBuilder value(float value) {
        dataWriter.writeValue(value);
        return this;
    }

    ////////////////////////////////////////////////////////////////////////////
    // Integer
    //////////////////////////////////

    public DataBuilder field(String name, Integer value) {
        return writeField(name, value);
    }

    public DataBuilder field(String name, int value) {
        return writeField(name, value);
    }

    public DataBuilder array(String name, int[] values) {
        return field(name).values(values);
    }

    private DataBuilder values(int[] values) {
        if (values == null) {
            return nullValue();
        }
        startArray();
        for (int i : values) {
            value(i);
        }
        endArray();
        return this;
    }

    public DataBuilder value(Integer value) {
        dataWriter.writeValue(value);
        return this;
    }

    public DataBuilder value(int value) {
        dataWriter.writeValue(value);
        return this;
    }

    ////////////////////////////////////////////////////////////////////////////
    // Long
    //////////////////////////////////

    public DataBuilder field(String name, Long value) {
        return writeField(name, value);
    }

    public DataBuilder field(String name, long value) {
        return writeField(name, value);
    }

    public DataBuilder array(String name, long[] values) {
        return field(name).values(values);
    }

    private DataBuilder values(long[] values) {
        if (values == null) {
            return nullValue();
        }
        startArray();
        for (long l : values) {
            value(l);
        }
        endArray();
        return this;
    }

    public DataBuilder value(Long value) {
        dataWriter.writeValue(value);
        return this;
    }

    public DataBuilder value(long value) {
        dataWriter.writeValue(value);
        return this;
    }

    ////////////////////////////////////////////////////////////////////////////
    // Short
    //////////////////////////////////

    public DataBuilder field(String name, Short value) {
        return writeField(name, value);
    }

    public DataBuilder field(String name, short value) {
        return writeField(name, value);
    }

    public DataBuilder array(String name, short[] values) {
        return field(name).values(values);
    }

    private DataBuilder values(short[] values) {
        if (values == null) {
            return nullValue();
        }
        startArray();
        for (short s : values) {
            value(s);
        }
        endArray();
        return this;
    }

    public DataBuilder value(Short value) {
        dataWriter.writeValue(value);
        return this;
    }

    public DataBuilder value(short value) {
        dataWriter.writeValue(value);
        return this;
    }

    ////////////////////////////////////////////////////////////////////////////
    // BigInteger
    //////////////////////////////////

    public DataBuilder field(String name, BigInteger value) {
        return writeField(name, value);
    }

    public DataBuilder array(String name, BigInteger[] values) {
        return field(name).values(values);
    }

    private DataBuilder values(BigInteger[] values) {
        if (values == null) {
            return nullValue();
        }
        startArray();
        for (BigInteger b : values) {
            value(b);
        }
        endArray();
        return this;
    }

    public DataBuilder value(BigInteger value) {
        if (value == null) {
            return nullValue();
        }
        dataWriter.writeValue(value);
        return this;
    }


    ////////////////////////////////////////////////////////////////////////////
    // BigDecimal
    //////////////////////////////////

    public DataBuilder field(String name, BigDecimal value) {
        return writeField(name, value);
    }

    public DataBuilder array(String name, BigDecimal[] values) {
        return field(name).values(values);
    }

    private DataBuilder values(BigDecimal[] values) {
        if (values == null) {
            return nullValue();
        }
        startArray();
        for (BigDecimal b : values) {
            value(b);
        }
        endArray();
        return this;
    }

    public DataBuilder value(BigDecimal value) {
        if (value == null) {
            return nullValue();
        }
        dataWriter.writeValue(value);
        return this;
    }

    ////////////////////////////////////////////////////////////////////////////
    // String
    //////////////////////////////////

    public DataBuilder field(String name, String value) {
        return writeField(name, value);
    }

    public DataBuilder array(String name, String... values) {
        return field(name).values(values);
    }

    private DataBuilder values(String[] values) {
        if (values == null) {
            return nullValue();
        }
        startArray();
        for (String s : values) {
            value(s);
        }
        endArray();
        return this;
    }

    public DataBuilder value(String value) {
        dataWriter.writeValue(value);
        return this;
    }


    ////////////////////////////////////////////////////////////////////////////
    // Date
    //////////////////////////////////

    public DataBuilder field(String name, Date value) {
        return writeField(name, value);
    }

    public DataBuilder array(String name, Date... values) {
        return field(name).values(values);
    }

    private DataBuilder values(Date[] values) {
        if (values == null) {
            return nullValue();
        }
        startArray();
        for (Date s : values) {
            value(s);
        }
        endArray();
        return this;
    }

    public DataBuilder value(Date value) {
        dataWriter.writeValue(value);
        return this;
    }

    ////////////////////////////////////////////////////////////////////////////
    // Objects
    //
    // These methods are used when the type of value is unknown. It tries to fallback
    // on typed methods and use Object.toString() as a last resort. Always prefer using
    // typed methods over this.
    //////////////////////////////////

    public DataBuilder value(Object value) {
        unknownValue(value, true);
        return this;
    }

    private void unknownValue(Object value, boolean ensureNoSelfReferences) {
        if (value == null) {
            nullValue();
            return;
        }

        Writer writer = WRITERS.get(value.getClass());
        if (writer != null) {
            writer.write(this, value);
        } else if (value instanceof Map) {
            @SuppressWarnings("unchecked") final Map<String, ?> valueMap = (Map<String, ?>) value;

            // checks that the map does not contain references to itself because
            // iterating over map entries will cause a stackoverflow error
            if (ensureNoSelfReferences) {
                ensureNoSelfReferences(valueMap);
            }

            startObject();
            for (Map.Entry<String, ?> entry : valueMap.entrySet()) {
                field(entry.getKey());
                unknownValue(entry.getValue(), false);
            }
            endObject();

        } else if (value instanceof Iterable) {
            // checks that the map does not contain references to itself because
            // iterating over map entries will cause a stackoverflow error
            final Iterable<?> iterabel = (Iterable<?>) value;
            if (ensureNoSelfReferences) {
                ensureNoSelfReferences(iterabel);
            }

            startArray();
            for (Object entry : iterabel) {
                unknownValue(entry, false);
            }
            endArray();

        } else if (value instanceof Object[]) {
            unknownValue(Arrays.asList((Object[]) value), true);
        } else if (value instanceof Enum<?>) {
            value(Objects.toString(value));
        } else {
            throw new IllegalArgumentException("cannot write unknown value of type " + value.getClass());
        }
    }


    private static Iterable<?> convert(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof Map) {
            return ((Map<?, ?>) value).values();
        } else if ((value instanceof Iterable) && (!(value instanceof Path))) {
            return (Iterable<?>) value;
        } else if (value instanceof Object[]) {
            return Arrays.asList((Object[]) value);
        } else {
            return null;
        }
    }

    private static void ensureNoSelfReferences(Object value) {
        Iterable<?> it = convert(value);
        if (it != null) {
            ensureNoSelfReferences(it, value, Collections.newSetFromMap(new IdentityHashMap<>()));
        }
    }

    private static void ensureNoSelfReferences(final Iterable<?> value, Object originalReference, final Set<Object> ancestors) {
        if (value != null) {
            if (!ancestors.add(originalReference)) {
                throw new IllegalArgumentException("iterable object is self-referencing itself");
            }
            for (Object o : value) {
                ensureNoSelfReferences(convert(o), o, ancestors);
            }
            ancestors.remove(originalReference);
        }
    }


}
