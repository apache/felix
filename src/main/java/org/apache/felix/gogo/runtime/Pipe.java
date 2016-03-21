/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.gogo.runtime;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.felix.gogo.runtime.Parser.Executable;
import org.apache.felix.gogo.runtime.Parser.Statement;
import org.apache.felix.service.command.Converter;

public class Pipe extends Thread
{
    static final ThreadLocal<Channel[]> tStreams = new ThreadLocal<Channel[]>();

    public static Channel[] mark() {
        return tStreams.get();
    }

    public static void reset(Channel[] streams) {
        tStreams.set(streams);
    }

    final Closure closure;
    final Executable executable;
    final Channel[] streams;
    final boolean[] toclose;
    Object result;
    Exception exception;
    int exit = 0;

    public Pipe(Closure closure, Executable executable, Channel[] streams, boolean[] toclose)
    {
        super("pipe-" + executable);
        this.closure = closure;
        this.executable = executable;
        this.streams = streams;
        this.toclose = toclose;
    }

    public String toString()
    {
        return "pipe<" + executable + "> out=" + streams[1];
    }

    private static final int READ = 1;
    private static final int WRITE = 2;

    private void setStream(Channel ch, int fd, int readWrite, boolean begOfPipe, boolean endOfPipe) throws IOException {
        if ((readWrite & READ) != 0 && !(ch instanceof ReadableByteChannel)) {
            throw new IllegalArgumentException("Channel is not readable");
        }
        if ((readWrite & WRITE) != 0 && !(ch instanceof WritableByteChannel)) {
            throw new IllegalArgumentException("Channel is not writable");
        }
        if (fd == 0 && !(ch instanceof ReadableByteChannel)) {
            throw new IllegalArgumentException("Stdin is not readable");
        }
        if (fd == 1 && !(ch instanceof WritableByteChannel)) {
            throw new IllegalArgumentException("Stdout is not writable");
        }
        if (fd == 2 && !(ch instanceof WritableByteChannel)) {
            throw new IllegalArgumentException("Stderr is not writable");
        }
        // TODO: externalize
        boolean multios = true;
        if (multios) {
            if (streams[fd] != null && (readWrite & READ) != 0 && (readWrite & WRITE) != 0) {
                throw new IllegalArgumentException("Can not do multios with read/write streams");
            }
            if ((readWrite & READ) != 0) {
                MultiReadableByteChannel mrbc;
                if (streams[fd] instanceof MultiReadableByteChannel) {
                    mrbc = (MultiReadableByteChannel) streams[fd];
                } else {
                    mrbc = new MultiReadableByteChannel();
                    if (streams[fd] != null && begOfPipe) {
                        if (toclose[fd]) {
                            streams[fd].close();
                        }
                    } else {
                        mrbc.addChannel((ReadableByteChannel) streams[fd], toclose[fd]);
                    }
                    streams[fd] = mrbc;
                    toclose[fd] = true;
                }
                mrbc.addChannel((ReadableByteChannel) ch, true);
            } else if ((readWrite & WRITE) != 0) {
                MultiWritableByteChannel mrbc;
                if (streams[fd] instanceof MultiWritableByteChannel) {
                    mrbc = (MultiWritableByteChannel) streams[fd];
                } else {
                    mrbc = new MultiWritableByteChannel();
                    if (streams[fd] != null && endOfPipe) {
                        if (toclose[fd]) {
                            streams[fd].close();
                        }
                    } else {
                        mrbc.addChannel((WritableByteChannel) streams[fd], toclose[fd]);
                    }
                    streams[fd] = mrbc;
                    toclose[fd] = true;
                }
                mrbc.addChannel((WritableByteChannel) ch, true);
            } else {
                throw new IllegalStateException();
            }
        }
        else {
            if (streams[fd] != null && toclose[fd]) {
                streams[fd].close();
            }
            streams[fd] = ch;
            toclose[fd] = true;
        }
    }

    private static class MultiChannel<T extends Channel> implements Channel {
        protected final List<T> channels = new ArrayList<T>();
        protected final List<T> toClose = new ArrayList<T>();
        protected final AtomicBoolean opened = new AtomicBoolean(true);
        public void addChannel(T channel, boolean toclose) {
            channels.add(channel);
            if (toclose) {
                toClose.add(channel);
            }
        }

        public boolean isOpen() {
            return opened.get();
        }

        public void close() throws IOException {
            if (opened.compareAndSet(true, false)) {
                for (T channel : toClose) {
                    channel.close();
                }
            }
        }
    }

    private static class MultiReadableByteChannel extends MultiChannel<ReadableByteChannel> implements ReadableByteChannel {
        int index = 0;
        public int read(ByteBuffer dst) throws IOException {
            int nbRead = -1;
            while (nbRead < 0 && index < channels.size()) {
                nbRead = channels.get(index).read(dst);
                if (nbRead < 0) {
                    index++;
                } else {
                    break;
                }
            }
            return nbRead;
        }
    }

    private static class MultiWritableByteChannel extends MultiChannel<WritableByteChannel> implements WritableByteChannel {
        public int write(ByteBuffer src) throws IOException {
            int pos = src.position();
            for (WritableByteChannel ch : channels) {
                src.position(pos);
                while (src.hasRemaining()) {
                    ch.write(src);
                }
            }
            return src.position() - pos;
        }
    }

    public void run()
    {
        InputStream in = null;
        PrintStream out = null;
        PrintStream err = null;
        WritableByteChannel errChannel = (WritableByteChannel) streams[2];

        Channel[] prevStreams = tStreams.get();

        // TODO: not sure this is the correct way
        boolean begOfPipe = !toclose[0];
        boolean endOfPipe = !toclose[1];

        try
        {
            if (executable instanceof Statement) {
                Statement statement = (Statement) executable;
                List<Token> tokens = statement.redirections();
                for (int i = 0; i < tokens.size(); i++) {
                    Token t = tokens.get(i);
                    Matcher m;
                    if ((m = Pattern.compile("(?:([0-9])?|(&)?)>(>)?").matcher(t)).matches()) {
                        int fd;
                        if (m.group(1) != null) {
                            fd = Integer.parseInt(m.group(1));
                        }
                        else if (m.group(2) != null) {
                            fd = -1; // both 1 and 2
                        } else {
                            fd = 1;
                        }
                        boolean append = m.group(3) != null;
                        Token file = tokens.get(++i);
                        Path outPath = closure.session().currentDir().resolve(file.toString());
                        Set<StandardOpenOption> options = new HashSet<StandardOpenOption>();
                        options.add(StandardOpenOption.WRITE);
                        options.add(StandardOpenOption.CREATE);
                        if (append) {
                            options.add(StandardOpenOption.APPEND);
                        } else {
                            options.add(StandardOpenOption.TRUNCATE_EXISTING);
                        }
                        Channel ch = Files.newByteChannel(outPath, options);
                        if (fd >= 0) {
                            setStream(ch, fd, WRITE, begOfPipe, endOfPipe);
                        } else {
                            setStream(ch, 1, WRITE, begOfPipe, endOfPipe);
                            setStream(ch, 2, WRITE, begOfPipe, endOfPipe);
                        }
                    }
                    else if ((m = Pattern.compile("([0-9])?>&([0-9])").matcher(t)).matches()) {
                        int fd0 = 1;
                        if (m.group(1) != null) {
                            fd0 = Integer.parseInt(m.group(1));
                        }
                        int fd1 = Integer.parseInt(m.group(2));
                        if (streams[fd0] != null && toclose[fd0]) {
                            streams[fd0].close();
                        }
                        streams[fd0] = streams[fd1];
                        // TODO: this is wrong, we should keep a counter somehow so that the
                        // stream is closed when both are closed
                        toclose[fd0] = false;
                    }
                    else if ((m = Pattern.compile("([0-9])?<(>)?").matcher(t)).matches()) {
                        int fd = 0;
                        if (m.group(1) != null) {
                            fd = Integer.parseInt(m.group(1));
                        }
                        boolean output = m.group(2) != null;
                        Token file = tokens.get(++i);
                        Path inPath = closure.session().currentDir().resolve(file.toString());
                        Set<StandardOpenOption> options = new HashSet<StandardOpenOption>();
                        options.add(StandardOpenOption.READ);
                        if (output) {
                            options.add(StandardOpenOption.WRITE);
                            options.add(StandardOpenOption.CREATE);
                        }
                        Channel ch = Files.newByteChannel(inPath, options);
                        setStream(ch, fd, READ + (output ? WRITE : 0), begOfPipe, endOfPipe);
                    }
                }
            } else {
                new UnsupportedOperationException("what to do ?").printStackTrace();
            }

            tStreams.set(streams);

            in = Channels.newInputStream((ReadableByteChannel) streams[0]);
            out = new PrintStream(Channels.newOutputStream((WritableByteChannel) streams[1]), true);
            err = new PrintStream(Channels.newOutputStream((WritableByteChannel) streams[2]), true);
            errChannel = (WritableByteChannel) streams[2];

            closure.session().threadIO().setStreams(in, out, err);

            result = closure.execute(executable);
            // We don't print the result if toclose[1] == false, which means we're at the end of the pipe
            if (result != null && !endOfPipe && !Boolean.FALSE.equals(closure.session().get(".FormatPipe"))) {
                out.println(closure.session().format(result, Converter.INSPECT));
            }
        }
        catch (Exception e)
        {
            exception = e;
            if (exit == 0) {
                exit = 1; // failure
            }
            // TODO: use shell name instead of 'gogo'
            // TODO: use color if not redirected
            // TODO: use conversion ?
            String msg = "gogo: " + e.getClass().getSimpleName() + ": " + e.getMessage() + "\n";
            try {
                errChannel.write(ByteBuffer.wrap(msg.getBytes()));
            } catch (IOException ioe) {
                e.addSuppressed(ioe);
            }
        }
        finally
        {
            if (out != null) {
                out.flush();
            }
            if (err != null) {
                err.flush();
            }
            closure.session().threadIO().close();

            tStreams.set(prevStreams);

            try
            {
                for (int i = 0; i < 10; i++) {
                    if (toclose[i] && streams[i] != null) {
                        streams[i].close();
                    }
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
    }
}
