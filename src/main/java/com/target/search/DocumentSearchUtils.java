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

/**
 * Reads all the files from the sample_txt directory and writes to fileMap where the key is the filename
 * and the value is the content of the file
 */
public class DocumentSearchUtils {

    private static final Map<String, String> filesMap = new HashMap<>();
    private final static Logger logger = LogManager.getLogger(DocumentSearchUtils.class);

    /**
     *
     * @param directory sample_txt directory where the files reside
     * @return map of filename as key and content as value
     */
    public static Map<String, String> readDirectory(String directory) {
        if (filesMap.keySet().size() == 0) {
            iterateAllFilesInDirectory(directory);
        }
        return filesMap;
    }

    /**
     * Iterate all files in the directory and read the contents and write to filemap
     * @param directory sample_txt directory where the files to be tokenized and searched reside
     */
    private static void iterateAllFilesInDirectory(String directory) {
        try (Stream<Path> paths = Files.walk(Paths.get(directory))) {
            paths.filter(Files::isRegularFile).forEach(file -> {
                String contents = readFile(file);
                if (contents != null)
                    filesMap.put(file.getFileName().toString(), readFile(file)); });
        } catch (IOException e) {
            logger.error(e);
        }
    }

    /**
     * Read the Files by Using channel I/O into the buffer based on the allocated size
     * @param file sample file to be tokenized/indexed and searched
     * @return content of the file
     */
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
        } catch (IOException e) {
            logger.error(e);
        }
        return null;
    }
}
