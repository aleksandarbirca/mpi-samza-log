package samza.streaming.client;

import org.apache.log4j.Logger;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Stream;

public class StreamClient {
    private static final Logger log = Logger.getLogger(StreamClient.class);
    private final BlockingQueue<String> queue = new LinkedBlockingQueue<>(1000000);
    private boolean active;

    public StreamClient(String source) {
        StringBuilder stringBuilder = new StringBuilder();
        active = true;
        try (Stream<String> stream = Files.lines(Paths.get(source), Charset.defaultCharset())) {
            stream.forEach(e -> {
                if (e.startsWith("\n") || e == null || e.isEmpty()) {
                    try {
                        queue.put(stringBuilder.toString());
                    } catch (InterruptedException ie) {
                        log.error("Error trying to send message to queue {}", ie);
                    } finally {
                        stringBuilder.setLength(0);
                    }
                } else {
                    final int index = e.indexOf("]") + 1;
                    stringBuilder.append(index > 0 && e.startsWith("[") ? e.substring(index) + "\n" : e + "\n");
                }
            });
        } catch (IOException e) {
            log.error("Some IOException: {}", e);
        }
        /*
        //for testing purposes
		active = true;
    	try {
			queue.put("test");
		} catch (InterruptedException e) {
			log.error("Some random error :D {}", e);
		}
		*/
    }

    public void stop() {
    	active = false;
    }

    public void stream(final LinkProcessor linkProcessor) {
        while (!queue.isEmpty() && active){
        	try {
				linkProcessor.processLink(queue.take());
			} catch (InterruptedException e) {
				log.error("Exception in stream: {}", e);
			}
        }
    }

    public interface LinkProcessor {
        void processLink(String link);
    }

}
