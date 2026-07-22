package dslab.util.streams;

import java.io.PrintStream;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Simulates writing lines to an {@link PrintStream}.
 * <p/>
 * Internally, the lines written to the underlying {@link PrintStream} are buffered and can be retrieved on demand for
 * verification purposes.
 */
public class TestOutputStream extends PrintStream {

    private final LinkedBlockingQueue<String> lines = new LinkedBlockingQueue<>();
    private volatile StringBuilder line = new StringBuilder();
    private final PrintStream delegate;

    /**
     * Creates a new {@code TestOutputStream} instance writing to an {@link NullOutputStream}.
     */
    public TestOutputStream() {
        this(new PrintStream(NullOutputStream.getInstance()));
    }

    /**
     * Creates a new {@code TestOutputStream} instance writing to the provided {@link PrintStream}.
     *
     * @param delegate the stream to write to
     */
    public TestOutputStream(PrintStream delegate) {
        super(delegate);
        this.delegate = delegate;
    }

    @Override
    public void close() {
        if (delegate != System.out) {
            super.close();
        }
    }

    @Override
    public void write(int b) {
        delegate.write(b);
        if (b == '\n') {
            addLine();
        } else if (b != '\r') {
            line.append((char) b);
        }
    }

    public void write(byte[] b, int off, int len) {
        if (b == null) {
            throw new NullPointerException();
        } else if ((off < 0) || (off > b.length) || (len < 0) || ((off + len) > b.length) || ((off + len) < 0)) {
            throw new IndexOutOfBoundsException();
        } else if (len == 0) {
            return;
        }
        for (int i = 0; i < len; i++) {
            write(b[off + i]);
        }

        if (b[len - 1] != '\n') {
            addLine();
        }
    }

    /**
     * Returns a copy of the lines written to the {@link PrintStream} so far.
     *
     * @return the written lines
     */
    public List<String> getLines() {
        synchronized (lines) {
            return lines.stream()
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        }
    }

    /**
     * Listens for the next line written to the {@link PrintStream}.
     *
     * @return the next line (without the trailing newline)
     * @throws InterruptedException if the polling was interrupted
     */
    public String readNextLine(long timeout, TimeUnit timeUnit) throws InterruptedException {
        return lines.poll(timeout, timeUnit);
    }


    /**
     * Clears the buffer holding the lines written to the {@link PrintStream} so far.
     */
    public void clear() {
        synchronized (lines) {
            while (!lines.isEmpty()) {
                lines.poll();
            }
            line = new StringBuilder();
        }
    }

    /**
     * Appends the current line to the buffer.
     */
    private void addLine() {
        synchronized (lines) {
            if (line.toString().isEmpty()) {
                return;
            }
            lines.add(line.toString());
            line = new StringBuilder();
        }
    }
}
