package com.coffeehut.coffeehut.fiveTrain;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Service for retrieving train arrival data for Cramlington Station.
 * <p>
 * Currently returns mock data simulating live train arrivals. Results are
 * cached in memory for {@value #CACHE_DURATION} milliseconds to avoid
 * redundant computation. When a real-time data source becomes available,
 * {@link #getMockTrains(String)} should be replaced with an API call.
 * </p>
 */
@Service
public class TrainService {

    /** In-memory cache of the most recently fetched train arrivals. */
    private List<Map<String, String>> cachedTrains = new ArrayList<>();

    /** Timestamp (epoch ms) of the last successful data fetch. */
    private long lastFetchTime = 0;

    /** Duration in milliseconds for which cached train data remains valid (60 seconds). */
    private static final long CACHE_DURATION = 60 * 1000;

    /**
     * Returns upcoming train arrivals for the given station, using a short-lived cache.
     * <p>
     * If the cache is still valid (i.e. fetched within the last 60 seconds and non-empty),
     * the cached list is returned immediately. Otherwise, fresh mock data is generated
     * and stored in the cache before being returned.
     * </p>
     *
     * @param station the station name used to generate or look up arrival data
     * @return a non-{@code null} list of train arrival detail maps
     */
    public List<Map<String, String>> getTrains(String station) {
        long now = System.currentTimeMillis();
        // Return cached result if still within the cache window
        if (now - lastFetchTime < CACHE_DURATION && !cachedTrains.isEmpty()) {
            return cachedTrains;
        }
        cachedTrains = getMockTrains(station);
        lastFetchTime = now;
        return cachedTrains;
    }

    /**
     * Generates mock train arrival data relative to the current time.
     * <p>
     * Produces five simulated trains departing at 12-minute intervals, with
     * varying statuses ({@code "On Time"}, {@code "Delayed"}, {@code "Cancelled"}).
     * Delayed trains have their expected arrival shifted forward by 8 minutes.
     * This method should be replaced with a real-time data source in production.
     * </p>
     *
     * @param station the station name (currently unused; reserved for future filtering)
     * @return a list of five train arrival maps with scheduling and status information
     */
    private List<Map<String, String>> getMockTrains(String station) {
        List<Map<String, String>> trains = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
        LocalDateTime now = LocalDateTime.now();
        String[] origins = {"Newcastle", "Morpeth", "Edinburgh", "York"};
        String[] statuses = {"On Time", "On Time", "Delayed", "On Time", "Cancelled"};
        for (int i = 1; i <= 5; i++) {
            Map<String, String> train = new HashMap<>();
            LocalDateTime scheduled = now.plusMinutes(i * 12);
            // Delayed trains arrive 8 minutes after their scheduled time
            LocalDateTime expected = statuses[i - 1].equals("Delayed")
                    ? scheduled.plusMinutes(8)
                    : scheduled;
            train.put("trainId", "NT" + String.format("%04d", i * 111));
            train.put("scheduledArrival", scheduled.format(fmt));
            train.put("expectedArrival", expected.format(fmt));
            train.put("status", statuses[i - 1]);
            train.put("origin", origins[i % origins.length]);
            trains.add(train);
        }
        return trains;
    }
}