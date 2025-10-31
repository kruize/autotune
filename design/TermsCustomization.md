# Terms Customization

A **term** in Kruize defines the duration over which historical data is collected to formulate recommendations. Previously, users were limited to predefined options with fixed durations. With Terms Customization the users have the flexibility to define their own terms — such as *monthly*, *quarterly*, or a *custom 26-day period* — directly through the **Create Experiment API**.

---

## Term Categories

To simplify usage, Kruize now supports two primary categories of terms, each with its own behavior and configuration flexibility:

1. **Fixed Default Terms**  
   These are predefined, standardized terms provided by Kruize. Their properties, such as duration and thresholds, are **immutable** to ensure consistent and predictable results across experiments.
    - **Examples:** `daily`, `weekly`, `15 days`, `monthly`, `quarterly`, `half-yearly`, `yearly`.
    - **Default Settings**: To use a term like `weekly` with its standard settings, provide its name as a key with an empty object `{}`.

2. **User-Defined Terms**  
   These are custom terms created by users to align with their unique operational or business cycles — for example, `"sprint_cycle"`, `"quarterly"`, or `"custom_26_days"`.
    - Fully customizable: users can set parameters like `duration_in_days`, `duration_threshold`, and plotting intervals.
    - Users have to specify the duration_in_days, as this is a mandatory parameter.
    - By default, the minimum data threshold equals **70% of the total duration**.
    - Users can optionally override this threshold by explicitly specifying `duration_threshold` in the configuration.

---

## API Implementation — `term_settings` Object

The feature is implemented within the `recommendation_settings` section of the **Create Experiment API**, using a `term_settings` object.

```json
"recommendation_settings": {
  "threshold": "0.1",
  "term_settings": {
    "terms": {
      "my_custom_term": {
        "duration_in_days": 26
      },
      "weekly": {}
    }
  }
}
```

### Term Parameters

| Parameter                | Type   | Required                          | Description                                                                                              |
| ------------------------ | ------ | --------------------------------- | -------------------------------------------------------------------------------------------------------- |
| **`duration_in_days`** | Double | **Mandatory for user-defined terms** | Defines the total lookback window (in days) for historical data.                                          |
| **`duration_threshold`** | String | Optional                          | Minimum data required to generate a recommendation. |
| **`plots_datapoint`** | Integer| Optional                          | Number of data points for plots. Defaults to one point per day.                                          |
| **`plots_datapoint_delta_in_days`** | Double | Optional                          | Time interval (in days) between plot points.                                           |