package org.apache.felix.bundleplugin;

import java.io.*;
import java.text.*;
import java.util.*;
import java.util.regex.*;

/**
 * Properties file timestamped with a specified date.
 */
class TimestampedProperties extends Properties
{
    private Date date;

    public TimestampedProperties(Date date) {
        this.date = date;
    }

    @Override
    public void store(OutputStream out, String comments) throws IOException {
        store(new OutputStreamWriter(out, "ISO-8859-1"), comments);
    }

    @Override
    public void store(Writer out, String comments) throws IOException {
        // store the properties file in memory
        StringWriter buffer = new StringWriter();
        super.store(buffer, comments);

        // Replace the date on the second line of the file
        SimpleDateFormat fmt = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.ENGLISH);
        fmt.setTimeZone(TimeZone.getTimeZone("UTC"));
        String[] lines = buffer.toString().split(Pattern.quote(System.getProperty("line.separator")));
        lines[1] = "#" + fmt.format(date);

        // write the file
        BufferedWriter writer = new BufferedWriter(out);
        try {
            for (String line : lines) {
                writer.write(line);
                writer.newLine();
            }
            writer.flush();
        } finally {
            writer.close();
        }
    }
}
