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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.PrintStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.Channel;
import java.nio.channels.Channels;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.felix.gogo.api.Job;
import org.apache.felix.gogo.api.Job.Status;
import org.apache.felix.gogo.api.Process;
import org.apache.felix.gogo.runtime.CommandSessionImpl.JobImpl;
import org.apache.felix.gogo.runtime.Parser.Statement;
import org.apache.felix.gogo.runtime.Pipe.Result;
import org.apache.felix.service.command.Converter;
import org.apache.felix.service.threadio.ThreadIO;

public class Pipe implements Callable<Result>, Process
{
    private static final ThreadLocal<Pipe> CURRENT = new ThreadLocal<>();

    public static class Result implements org.apache.felix.gogo.api.Result {
        public final Object result;
        public final Exception exception;
        public final int error;

        public Result(Object result) {
            this.result = result;
            this.exception = null;
            this.error = 0;
        }

        public Result(Exception exception) {
            this.result = null;
            this.exception = exception;
            this.error = 1;
        }

        public Result(int error) {
            this.result = null;
            this.exception = null;
            this.error = error;
        }

        public boolean isSuccess() {
            return exception == null && error == 0;
        }

        @Override
        public Object result() {
            return result;
        }

        @Override
        public Exception exception() {
            return exception;
        }

        @Override
        public int error() {
            return error;
        }
    }

    public static Pipe getCurrentPipe() {
        return CURRENT.get();
    }

    private static Pipe setCurrentPipe(Pipe pipe) {
        Pipe previous = CURRENT.get();
        CURRENT.set(pipe);
        return previous;
    }

    final Closure closure;
    final Job job;
    final Statement statement;
    final Channel[] streams;
    final boolean[] toclose;
    int error;

    InputStream in;
    PrintStream out;
    PrintStream err;

    public Pipe(Closure closure, JobImpl job, Statement statement, Channel[] streams, boolean[] toclose)
    {
        this.closure = closure;
        this.job = job;
        this.statement = statement;
        this.streams = streams;
        this.toclose = toclose;
    }

    public String toString()
    {
        return "pipe<" + statement + "> out=" + streams[1];
    }

    private static final int READ = 1;
    private static final int WRITE = 2;

    private void setStream(Channel ch, int fd, int readWrite) throws IOException {
        if ((readWrite & (READ | WRITE)) == 0) {
            throw new IllegalArgumentException("Should specify READ and/or WRITE");
        }
        if ((readWrite & READ) != 0 && !(ch instanceof ReadableByteChannel)) {
            throw new IllegalArgumentException("Channel is not readable");
        }
        if ((readWrite & WRITE) != 0 && !(ch instanceof WritableByteChannel)) {
            throw new IllegalArgumentException("Channel is not writable");
        }
        if (fd == 0 && (readWrite & READ) == 0) {
            throw new IllegalArgumentException("Stdin is not readable");
        }
        if (fd == 1 && (readWrite & WRITE) == 0) {
            throw new IllegalArgumentException("Stdout is not writable");
        }
        if (fd == 2 && (readWrite & WRITE) == 0) {
            throw new IllegalArgumentException("Stderr is not writable");
        }
        if (streams[fd] != null && (readWrite & READ) != 0 && (readWrite & WRITE) != 0) {
            throw new IllegalArgumentException("Can not do multios with read/write streams");
        }
        // If channel is inherited (for example standard input / output), replace it
        if (streams[fd] != null && !toclose[fd]) {
            streams[fd] = ch;
            toclose[fd] = true;
        }
        // Else do multios
        else {
            MultiChannel mrbc;
            // If the channel is already multios
            if (streams[fd] instanceof MultiChannel) {
                mrbc = (MultiChannel) streams[fd];
            }
            // Else create a multios channel
            else {
                mrbc = new MultiChannel();
                mrbc.addChannel(streams[fd], toclose[fd]);
                streams[fd] = mrbc;
                toclose[fd] = true;
            }
            mrbc.addChannel(ch, true);
        }
    }

    @Override
    public InputStream in() {
        return in;
    }

    @Override
    public PrintStream out() {
        return out;
    }

    @Override
    public PrintStream err() {
        return err;
    }

    @Override
    public Job job() {
        return job;
    }

    public boolean isTty(int fd) {
        // TODO: this assumes that the session is always created with input/output tty streams
        if (fd < 0 || fd > streams.length) {
            return false;
        }
        return streams[fd] != null && !toclose[fd];
    }

    public void error(int error) {
        this.error = error;
    }

    @Override
    public Result call() throws Exception {
        Thread thread = Thread.currentThread();
        String name = thread.getName();
        try {
            thread.setName("pipe-" + statement);
            return doCall();
        } finally {
            thread.setName(name);
        }
    }

    private Result doCall()
    {
        // The errChannel will be used to print errors to the error stream
        // Before the command is actually executed (i.e. during the initialization,
        // including the redirection processing), it will be the original error stream.
        // This value may be modified by redirections and the redirected error stream
        // will be effective just before actually running the command.
        WritableByteChannel errChannel = (WritableByteChannel) streams[2];

        boolean endOfPipe = !toclose[1];

        ThreadIO threadIo = closure.session().threadIO();

        try
        {
            List<Token> tokens = statement.redirections();
            for (int i = 0; i < tokens.size(); i++)
            {
                Token t = tokens.get(i);
                Matcher m;
                if ((m = Pattern.compile("(?:([0-9])?|(&)?)>(>)?").matcher(t)).matches())
                {
                    int fd;
                    if (m.group(1) != null)
                    {
                        fd = Integer.parseInt(m.group(1));
                    }
                    else if (m.group(2) != null)
                    {
                        fd = -1; // both 1 and 2
                    }
                    else
                    {
                        fd = 1;
                    }
                    boolean append = m.group(3) != null;
                    Set<StandardOpenOption> options = new HashSet<>();
                    options.add(StandardOpenOption.WRITE);
                    options.add(StandardOpenOption.CREATE);
                    options.add(append ? StandardOpenOption.APPEND : StandardOpenOption.TRUNCATE_EXISTING);
                    Token tok = tokens.get(++i);
                    Object val = Expander.expand(tok, closure);
                    for (Path p : toPaths(val))
                    {
                        p = closure.session().currentDir().resolve(p);
                        Channel ch = Files.newByteChannel(p, options);
                        if (fd >= 0)
                        {
                            setStream(ch, fd, WRITE);
                        }
                        else
                        {
                            setStream(ch, 1, WRITE);
                            setStream(ch, 2, WRITE);
                        }
                    }
                }
                else if ((m = Pattern.compile("([0-9])?>&([0-9])").matcher(t)).matches())
                {
                    int fd0 = 1;
                    if (m.group(1) != null)
                    {
                        fd0 = Integer.parseInt(m.group(1));
                    }
                    int fd1 = Integer.parseInt(m.group(2));
                    if (streams[fd0] != null && toclose[fd0])
                    {
                        streams[fd0].close();
                    }
                    // If the stream has to be closed, close it when both streams are closed
                    if (toclose[fd1])
                    {
                        Channel channel = streams[fd1];
                        AtomicInteger references = new AtomicInteger();
                        streams[fd0] = new RefByteChannel(channel, references);
                        streams[fd1] = new RefByteChannel(channel, references);
                        toclose[fd0] = true;
                    }
                    else
                    {
                        streams[fd0] = streams[fd1];
                        toclose[fd0] = false;
                    }
                }
                else if ((m = Pattern.compile("([0-9])?<(>)?").matcher(t)).matches())
                {
                    int fd = 0;
                    if (m.group(1) != null)
                    {
                        fd = Integer.parseInt(m.group(1));
                    }
                    boolean output = m.group(2) != null;
                    Set<StandardOpenOption> options = new HashSet<>();
                    options.add(StandardOpenOption.READ);
                    if (output)
                    {
                        options.add(StandardOpenOption.WRITE);
                        options.add(StandardOpenOption.CREATE);
                    }
                    Token tok = tokens.get(++i);
                    Object val = Expander.expand(tok, closure);
                    for (Path p : toPaths(val))
                    {
                        p = closure.session().currentDir().resolve(p);
                        Channel ch = Files.newByteChannel(p, options);
                        setStream(ch, fd, READ + (output ? WRITE : 0));
                    }
                }
                else if ((m = Pattern.compile("<<-?").matcher(t)).matches())
                {
                    Token hereDoc = tokens.get(++i);
                    boolean stripLeadingTabs = t.charAt(t.length() - 1) == '-';
                    InputStream doc = new InputStream()
                    {
                        final byte[] bytes = hereDoc.toString().getBytes();
                        int index = 0;
                        boolean nl = true;
                        @Override
                        public int read() throws IOException
                        {
                            if (nl && stripLeadingTabs)
                            {
                                while (index < bytes.length && bytes[index] == '\t')
                                {
                                    index++;
                                }
                            }
                            if (index < bytes.length)
                            {
                                int ch = bytes[index++];
                                nl = ch == '\n';
                                return ch;
                            }
                            return -1;
                        }
                    };
                    Channel ch = Channels.newChannel(doc);
                    setStream(ch, 0, READ);
                }
                else if (Token.eq("<<<", t))
                {
                    Token word = tokens.get(++i);
                    Object val = Expander.expand("\"" + word + "\"", closure);
                    String str = val != null ? String.valueOf(val) : "";
                    Channel ch = Channels.newChannel(new ByteArrayInputStream(str.getBytes()));
                    setStream(ch, 0, READ);
                }
            }

            for (int i = 0; i < streams.length; i++) {
                streams[i] = wrap(streams[i]);
            }

            // Create streams
            in = Channels.newInputStream((ReadableByteChannel) streams[0]);
            out = new PrintStream(Channels.newOutputStream((WritableByteChannel) streams[1]), true);
            err = new PrintStream(Channels.newOutputStream((WritableByteChannel) streams[2]), true);
            // Change the error stream to the redirected one, now that
            // the command is about to be executed.
            errChannel = (WritableByteChannel) streams[2];

            if (threadIo != null)
            {
                threadIo.setStreams(in, out, err);
            }

            Pipe previous = setCurrentPipe(this);
            try
            {
                Object result;
                // Very special case for empty statements with redirection
                if (statement.tokens().isEmpty() && toclose[0])
                {
                    ByteBuffer bb = ByteBuffer.allocate(1024);
                    while (((ReadableByteChannel) streams[0]).read(bb) >= 0 || bb.position() != 0)
                    {
                        bb.flip();
                        ((WritableByteChannel) streams[1]).write(bb);
                        bb.compact();
                    }
                    result = null;
                }
                else
                {
                    result = closure.execute(statement);
                }
                // If an error has been set
                if (error != 0)
                {
                    return new Result(error);
                }
                // We don't print the result if we're at the end of the pipe
                if (result != null && !endOfPipe && !Boolean.FALSE.equals(closure.session().get(".FormatPipe")))
                {
                    out.println(closure.session().format(result, Converter.INSPECT));
                }
                return new Result(result);

            }
            finally
            {
                setCurrentPipe(previous);
            }
        }
        catch (Exception e)
        {
            String msg = "gogo: " + e.getClass().getSimpleName() + ": " + e.getMessage() + "\n";
            try
            {
                errChannel.write(ByteBuffer.wrap(msg.getBytes()));
            }
            catch (IOException ioe)
            {
                e.addSuppressed(ioe);
            }
            return new Result(e);
        }
        finally
        {
            if (out != null)
            {
                out.flush();
            }
            if (err != null)
            {
                err.flush();
            }
            if (threadIo != null)
            {
                threadIo.close();
            }

            try
            {
                for (int i = 0; i < 10; i++)
                {
                    if (toclose[i] && streams[i] != null)
                    {
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

    private List<Path> toPaths(Object val) throws IOException
    {
        List<Path> paths = new ArrayList<>();
        if (val instanceof Collection)
        {
            for (Object o : (Collection) val)
            {
                Path p = toPath(o);
                if (p != null)
                {
                    paths.add(p);
                }
            }
        }
        else if (val != null)
        {
            Path p = toPath(val);
            if (p != null)
            {
                paths.add(p);
            }
        }
        if (paths.isEmpty())
        {
            throw new IOException("no such file or directory");
        }
        return paths;
    }

    private Path toPath(Object o)
    {
        if (o instanceof Path)
        {
            return (Path) o;
        }
        else if (o instanceof File)
        {
            return ((File) o).toPath();
        }
        else if (o instanceof URI)
        {
            return Paths.get((URI) o);
        }
        else if (o != null)
        {
            String s = String.valueOf(o);
            if (!s.isEmpty())
            {
                return Paths.get(String.valueOf(o));
            }
        }
        return null;
    }

    private Channel wrap(Channel channel)
    {
        if (channel == null)
        {
            return null;
        }
        if (channel instanceof MultiChannel)
        {
            return channel;
        }
        MultiChannel mch = new MultiChannel();
        mch.addChannel(channel, true);
        return mch;
    }

    private class RefByteChannel implements ByteChannel {

        private final Channel channel;
        private final AtomicInteger references;
        private final AtomicBoolean closed = new AtomicBoolean(false);

        public RefByteChannel(Channel channel, AtomicInteger references) {
            this.channel = channel;
            this.references = references;
            references.incrementAndGet();
        }

        @Override
        public int read(ByteBuffer dst) throws IOException {
            ensureOpen();
            return ((ReadableByteChannel) channel).read(dst);
        }

        @Override
        public int write(ByteBuffer src) throws IOException {
            ensureOpen();
            return ((WritableByteChannel) channel).write(src);
        }

        @Override
        public boolean isOpen() {
            return !closed.get();
        }

        private void ensureOpen() throws ClosedChannelException {
            if (closed.get()) throw new ClosedChannelException();
        }

        @Override
        public void close() throws IOException {
            if (closed.compareAndSet(false, true)) {
                if (references.decrementAndGet() == 0) {
                    channel.close();
                }
            }
        }

    }

    private class MultiChannel implements ByteChannel {
        protected final List<Channel> channels = new ArrayList<>();
        protected final List<Channel> toClose = new ArrayList<>();
        protected final AtomicBoolean opened = new AtomicBoolean(true);
        int index = 0;

        public void addChannel(Channel channel, boolean toclose) {
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
                for (Channel channel : toClose) {
                    channel.close();
                }
            }
        }

        public int read(ByteBuffer dst) throws IOException {
            int nbRead = -1;
            while (nbRead < 0 && index < channels.size()) {
                Channel ch = channels.get(index);
                checkSuspend(ch);
                nbRead = ((ReadableByteChannel) ch).read(dst);
                if (nbRead < 0) {
                    index++;
                } else {
                    break;
                }
            }
            return nbRead;
        }

        public int write(ByteBuffer src) throws IOException {
            int pos = src.position();
            for (Channel ch : channels) {
                checkSuspend(ch);
                src.position(pos);
                while (src.hasRemaining()) {
                    ((WritableByteChannel) ch).write(src);
                }
            }
            return src.position() - pos;
        }

        private void checkSuspend(Channel ch) throws IOException {
            Channel[] sch = closure.session().channels;
            if (ch == sch[0] || ch == sch[1] || ch == sch[2]) {
                synchronized (job) {
                    if (job.status() == Status.Background) {
                        // TODO: Send SIGTIN / SIGTOU
                        job.suspend();
                    }
                }
            }
            synchronized (job) {
                while (job.status() == Status.Suspended) {
                    try {
                        job.wait();
                    } catch (InterruptedException e) {
                        throw (IOException) new InterruptedIOException().initCause(e);
                    }
                }
            }
        }
    }

}
