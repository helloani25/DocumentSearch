package com.target.search;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.stream.Stream;
import java.util.logging.Logger;


class DocumentSearchUtils {

    private static Map<Path, String> filesMap = new HashMap<>();
    private final static Logger LOGGER = Logger.getLogger(DocumentSearch.class.getName());

    DocumentSearchUtils() {
        LOGGER.setLevel(Level.INFO);
    }

    static Map<Path, String> readDirectory(String directory) {
        if (filesMap.keySet().size() == 0) {
            iterateAllFilesInDirectory(directory);
        }
        return filesMap;
    }

    private static void iterateAllFilesInDirectory(String directory) {
        try (Stream<Path> paths = Files.walk(Paths.get(directory))) {
            paths.filter(Files::isRegularFile).forEach(file -> {
                String contents = readFile(file);
                if (contents != null)
                    filesMap.put(file, readFile(file)); });
        } catch (IOException x) {
            System.err.println("caught exception: " + x.getMessage());
        }
    }

    private static String readFile(Path file) {
        // Defaults to READ
        try (SeekableByteChannel sbc = Files.newByteChannel(file)) {
            ByteBuffer buf = ByteBuffer.allocate(4096);

            // Read the bytes with the proper encoding for this platform.  If
            // you skip this step, you might see something that looks like
            // Chinese characters when you expect Latin-style characters.
            String encoding = System.getProperty("file.encoding");
            StringBuffer sb = new StringBuffer();
            while (sbc.read(buf) > 0) {
                buf.rewind();
                sb.append(Charset.forName(encoding).decode(buf));
                //System.out.print(sb.toString());
                buf.flip();
            }
            return sb.toString();
        } catch (IOException x) {
            System.err.println("caught exception: " + x.getMessage());
        }
        return null;
    }
}
