package base;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.File;
import java.io.IOException;

/*
com.google.gson.JsonIOException: Failed making field 'java.io.File#path' accessible; either increase its visibility or write a custom TypeAdapter for its declaring type.
See https://github.com/google/gson/blob/main/Troubleshooting.md#reflection-inaccessible
Caused by: java.lang.reflect.InaccessibleObjectException: Unable to make field private final java.lang.String java.io.File.path accessible: module java.base does not "opens java.io" to unnamed module @2727aa3d

com.google.gson.JsonSyntaxException: java.lang.IllegalStateException: Expected a string but was BEGIN_OBJECT at line 1 column 283 path $.currentDBFile
See https://github.com/google/gson/blob/main/Troubleshooting.md#unexpected-json-structure

Caused by: java.lang.IllegalStateException: Expected a string but was BEGIN_OBJECT at line 1 column 283 path $.currentDBFile
See https://github.com/google/gson/blob/main/Troubleshooting.md#unexpected-json-structure

 */
public class FileTypeAdapter extends TypeAdapter<File> {
    @Override
    public void write(JsonWriter out, File value) throws IOException {
        out.value(value.getPath());
    }

    @Override
    public File read(JsonReader in) throws IOException {
        switch (in.peek()) {
            case STRING:
                return new File(in.nextString());
            case BEGIN_OBJECT:
                in.beginObject();
                String path = null;
                while (in.hasNext()) {
                    String name = in.nextName();
                    if (name.equals("path")) {
                        path = in.nextString();
                    } else {
                        in.skipValue();
                    }
                }
                in.endObject();
                return new File(path);
            default:
                throw new IllegalStateException("Expected STRING or BEGIN_OBJECT but was " + in.peek());
        }
    }
}

