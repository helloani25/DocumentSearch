package com.target.search;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import static java.nio.file.StandardOpenOption.READ;


class DocumentSearchUtils {

    private static Map<String, String> filesMap = new HashMap<>();
    private final static Logger logger = LogManager.getLogger(DocumentSearch.class);

    static Map<String, String> readDirectory(String directory) {
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
                    filesMap.put(file.getFileName().toString(), readFile(file)); });
        } catch (IOException x) {
            logger.error(x.getMessage());
            System.err.println("caught exception: " + x.getMessage());
        }
    }

    private static String readFile(Path file) {
        // Defaults to READ
        try (SeekableByteChannel sbc = Files.newByteChannel(file, EnumSet.of(READ))) {
            ByteBuffer buf = ByteBuffer.allocate(2048);
            // Read the bytes with the proper encoding for this platform.  If
            // you skip this step, you might see something that looks like
            // Chinese characters when you expect Latin-style characters.
            String encoding = System.getProperty("file.encoding");
            StringBuffer sb = new StringBuffer();
            while (sbc.read(buf) > 0) {
                buf.flip();
                sb.append(Charset.forName(encoding).decode(buf));
                //System.out.print(sb.toString());
                buf.rewind();
            }
            return sb.toString();
        } catch (IOException x) {
            logger.error(x.getMessage());
            System.err.println("caught exception: " + x.getMessage());
        }
        return null;
    }
}
