# Terms Customization

A **term** in Kruize defines the duration over which historical data is collected to formulate recommendations.
With Terms Customization the users are able to set configurable time frames like 3 days, 10 days, 30 days instead of the default 1 day, 7 days, 15 days.
And can also define relevant hours like 7 a.m. to 7 p.m. Monday to Friday, directly through the **Create Experiment API**.

---

## Term Categories

To simplify usage, Kruize now supports two primary categories of terms, each with its own behavior and configuration flexibility:

1. **Fixed Default Terms**  
   These are predefined, standardized terms provided by Kruize. The duration for the these terms are **immutable** to ensure consistent and predictable results across experiments.
    - **Examples:** `daily`, `weekly`, `15 days`, `monthly`, `quarterly`, `half-yearly`, `yearly`.
    - duration_in_days is immutable. Users can customize all other parameters.
   


2. **User-Defined Terms**  
   These are custom terms created by users to align with their unique operational or business cycles — for example, `"sprint_cycle"`, `"quarterly"`, or `"custom_26_days"`.
    - Fully customizable: users can set parameters like `duration_in_days`, `duration_threshold`, and plotting intervals.
    - Users have to specify the duration_in_days, as this is a mandatory parameter.
    - By default, the minimum data threshold equals **70% of the total duration**.
    - Users can optionally override this threshold by explicitly specifying `duration_threshold` in the configuration.

Note: There is an upper limit to the number of terms user can specify and that is 3. 
---

## API Implementation — `term_settings` Object

The feature is implemented within the `recommendation_settings` section of the **Create Experiment API**, using a `term_settings` object.

```json
{
  "recommendation_settings": {
    "threshold": "1",
    "term_settings": {
      "terms": [
        "daily",
        "weekly",
        "my_custom_term"
      ],
      "terms_definition": {
        "my_custom_term": {
          "duration_in_days": 17,
          // duration_in_ days OR timestamps required
          "start_timestamp": "2025-11-01T00:00:00Z",
          "end_timestamp": "2025-11-17T23:59:59Z",
          "days_of_week": [ "mon", "tue", "wed", "thu", "fri"],
          "daily_start_time": "07:00",
          "daily_end_time": "19:00",
          "duration_threshold": "7",
          "plots_datapoint": 17,
          "plots_datapoint_delta_in_days": 1
        }
      }
    }
  }
}
```

Note: start_timestamp and end_timestamp can be used as an alternative to duration_in_days and vice versa. Only one approach is permitted per term.

### Term Parameters

| Parameter                           | Type            | Required                          | Description                                                                             |
|-------------------------------------|-----------------| --------------------------------- |-----------------------------------------------------------------------------------------|
| **`duration_in_days`**              | Double          | Mandatory if ‘start_timestamp’ and ‘end_timestamp’ not mentioned | Defines the total lookback window (in days) for historical data.                        |
| **`duration_threshold`**            | String          | Optional                          | Minimum data required to generate a recommendation.                                     |
| **`plots_datapoint`**               | Integer         | Optional                          | Number of data points for plots. Defaults to one point per day except short/daily term. |
| **`plots_datapoint_delta_in_days`** | Double          | Optional                          | Time interval (in days) between plot points.                                            |
| **`start_timestamp`**               | String          | Mandatory if `duration_in_days` is not present  | Lookback start time. Must be in UTC ISO 8601 format.                                         |  
| **`end_timestamp`**                 | String          | Mandatory if `duration_in_days` is not present  | Lookback end time. Must be in UTC ISO 8601 format and logically after `start_timestamp`.                                           |
| **`days_of_week`**                  | Array of String | Optional                          | Specifies which days (e.g., mon, tue) the term should be active for.                                            |
| **`daily_start_time`**              | String(HH:MM)   | Optional                          | The start time (daily) for data collection (e.g., "07:00").                             |
| **`daily_end_time`**                | String(HH:MM)   | Optional                          | The end time (daily) for data collection (e.g., "19:00").                               |

