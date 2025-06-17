package com.robotique.aevaweb.buddygpt.utilis;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

public class CustomProperties extends Properties {
    private static final long serialVersionUID = 1L;

    private final Map<Object, Object> linkMap = new LinkedHashMap<>();
    private Map<String, List<String>> propertySpecificComments = new HashMap<>();

    @Override
    public synchronized Object put(Object key, Object value) {
        return linkMap.put(key, value);
    }

    @Override
    public synchronized boolean contains(Object value) {
        return linkMap.containsValue(value);
    }

    @Override
    public boolean containsValue(Object value) {
        return linkMap.containsValue(value);
    }

    @Override
    public synchronized Enumeration<Object> elements() {
        throw new UnsupportedOperationException(
                "Enumerations are so old-school, don't use them, "
                        + "use keySet() or entrySet() instead");
    }

    @Override
    public Set<Entry<Object, Object>> entrySet() {
        return linkMap.entrySet();
    }

    @Override
    public synchronized void clear() {
        linkMap.clear();
    }

    @Override
    public synchronized boolean containsKey(Object key) {
        return linkMap.containsKey(key);
    }

    @Override
    public synchronized Object setProperty(String key, String value) {
        return put(key, value);
    }



    @Override
    public synchronized String getProperty(String key) {
        String val = null;
        for (Iterator<Entry<Object, Object>> e = linkMap.entrySet().iterator(); e.hasNext();) {
            Map.Entry<Object, Object> entry = e.next();
            if (key.equalsIgnoreCase((String) entry.getKey())) {
                val = (String) entry.getValue();
                break;
            }
        }
        return val;
    }
    public synchronized void addPropertyComment(String key, String comment) {
        propertySpecificComments.computeIfAbsent(key, k -> new ArrayList<>()).add(comment);
    }
    @Override
    public synchronized void store(OutputStream out, String comments) throws IOException {
        BufferedWriter awriter = new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8));

        //write comment + fileVersion + Date
        if (comments != null) writeln(awriter, "#" + comments);
        if (linkMap.containsKey("fileVersion")) {
            writeln(awriter, "fileVersion=" + linkMap.get("fileVersion"));
        }
        writeln(awriter, "#" + new Date().toString());


        // Write other properties
        for (Iterator<Map.Entry<Object, Object>> e = linkMap.entrySet().iterator(); e.hasNext();) {
            Map.Entry<Object, Object> entry = e.next();

            String key = (String) entry.getKey();
            String val = (String) entry.getValue();

            if (!"fileVersion".equals(key)) {
                List<String> specificComments = propertySpecificComments.get(key);

                if (specificComments != null) {
                    for (String comment : specificComments) {
                        if (comment.equals("")) {
                            awriter.newLine();
                        } else {
                            writeln(awriter, "#" + comment);
                        }
                    }
                }

                if (!val.contains("#") && !key.contains("#")) {
                    if(key.equals("Pattern_End_Phrase")) writeln(awriter, key + "=" + escapePropertyValue(val));
                    else writeln(awriter, key + "=" + val);
                }
            }

        }

        awriter.flush();
        awriter.close();
    }

    private static String escapePropertyValue(String value) {
        // Escape characters as needed
        value = value.replace("\\", "\\\\"); // escape backslashes
        return value;
    }


    private static void writeln(BufferedWriter bw, String s) throws IOException {
        bw.write(s);
        bw.newLine();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CustomProperties)) return false;
        CustomProperties that = (CustomProperties) o;
        return linkMap.equals(that.linkMap) &&
                propertySpecificComments.equals(that.propertySpecificComments);
    }

    @Override
    public int hashCode() {
        int result = linkMap.hashCode();
        result = 31 * result + propertySpecificComments.hashCode();
        return result;
    }

}
