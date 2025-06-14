import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

public class WordCount {

    private final static int THREAD_USE = 2;

    /*
    * Because the program going to count words by running in multithreaded mode,
    * using ConcurrentMap to store word frequencies is necessary. Ensure thread safe
    * */
    private static final ConcurrentMap<String, Integer> wordCounts = new ConcurrentHashMap<>();


    public static void main(String[] args) throws InterruptedException {
        if (args.length == 0) {
            System.err.println("Usage: java WordCount file1.txt file2.txt ...");
            return;
        }

        /*
        * Setup a threadpool, the number of threads is set to 2.
        * */
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_USE);

        List<Future<?>> futures = new ArrayList<>();

        /*
        * Assign file for threads to process.
        * */
        for (String file : args) {
            Path path = Paths.get(file);
            futures.add(executor.submit(() -> processFile(path)));
        }

        /*
        * Wait for all tasks to complete
        * */
        for (Future<?> f : futures) {
            try {
                f.get();
            } catch (ExecutionException e) {
                System.err.println("Error: " + e.getCause().getMessage());
            }
        }

        executor.shutdown(); // -> close threadpool
        wordCounts.entrySet().stream()
                .sorted(Map.Entry.comparingByKey()) //sort key by alphabetical order
                .forEach(entry -> System.out.println(entry.getKey() + ": " + entry.getValue()));

    }

    private static void processFile(Path path) {
        try (BufferedReader reader = Files.newBufferedReader(path)) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] words = line.trim().toLowerCase().split("\\s+");
                for (String word : words) {
                    if (!word.isEmpty()) {
                        /*
                        * If the specified key is not already associated
                        * with a (non-null) value,
                        * associates it with the given value.
                        * Integer::sum is used to add 1 value to existed value
                        * */
                        wordCounts.merge(word, 1, Integer::sum);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to read file " + path + ": " + e.getMessage());
        }
    }
}