package nl.sense_os.service.shared;



/**
 * Interface for data processor implementations. These processors can register at the main sensor
 * modules to divide the handling of the incoming sensor data over different specific processing
 * implementations.
 * 
 * @author Steven Mulder <steven@sense-os.nl>
 */
public interface DataProcessor {

    /**
     * Starts a new sample.
     * 
     * @see #isSampleComplete()
     */
    void startNewSample();

    /**
     * @return <code>true</code> if the data processor has received enough sensor events so that the
     *         sample is complete
     */
    boolean isSampleComplete();

    /**
     * Handles a new data point. Take care: the sensor event is not guaranteed to be from the sensor
     * that the DataProcessor is interested in, so implementing classes need to check this.
     * 
     * @param event
     */
    void onNewData(SensorDataPoint dataPoint);
}
