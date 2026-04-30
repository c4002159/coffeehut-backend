package com.coffeehut.coffeehut.fiveTrain;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

/**
 * REST controller for train arrival information at Cramlington Station.
 * <p>
 * Exposes a single endpoint under {@code /api/train} that returns upcoming
 * train arrivals for a given station name. Delegates all data retrieval and
 * caching logic to {@link TrainService}.
 * </p>
 */
@RestController
@RequestMapping("/api/train")
@CrossOrigin(origins = "*")
public class TrainController {

    /** Service responsible for fetching and caching train arrival data. */
    @Autowired
    private TrainService trainService;

    /**
     * Returns a list of upcoming train arrivals for the specified station.
     * <p>
     * Results may be served from an in-memory cache if the data was fetched
     * within the last 60 seconds, reducing unnecessary external calls.
     * </p>
     *
     * @param station the name of the station to query (e.g. {@code "Cramlington"})
     * @return a list of train arrival maps, each containing keys such as
     *         {@code trainId}, {@code scheduledArrival}, {@code expectedArrival},
     *         {@code status}, and {@code origin}
     */
    @GetMapping
    public List<Map<String, String>> getTrains(@RequestParam String station) {
        return trainService.getTrains(station);
    }
}