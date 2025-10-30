# Terms Customization

A **term** in Kruize defines the duration over which historical data is collected to formulate recommendations.  
Previously, users were limited to predefined *short*, *medium*, and *long-term* options.  
This enhancement introduces flexibility for users to define their own terms — such as *monthly*, *quarterly*, or a *custom 26-day period* — directly through the **Create Experiment API**.

---

## Categorization of Terms

Kruize now supports three distinct categories of terms, each governed by its own rules and behaviors:

### 1. Fixed Default Terms
- **Terms:** `short`, `medium`, `long`
- These are *immutable* and exist to maintain consistency for existing users.
- Users **cannot override** any of their configuration fields.

### 2. Kruize Default Terms
- **Terms:** `daily`, `weekly`, `15 days`, `monthly`, `quarterly`, `half-yearly`, `yearly`
- These are partially modifiable — their `duration_in_days` is fixed, but users can override parameters like `duration_threshold` within permissible limits.

### 3. Custom Terms
- Any term name that does **not** belong to the above two categories is considered a **custom term**.
- These are **fully customizable**.
- The field `duration_in_days` is **mandatory** for all custom terms.

---

## API Implementation — `term_settings` Object

The feature is implemented within the `recommendation_settings` section of the **Create Experiment API**, using a `term_settings` object.

Each term is represented as a key-value pair in the `terms` object.  
The key is the **term name**, and the value defines its configuration.

```json
"recommendation_settings": {
  "threshold": "0.1",
  "term_settings": {
    "terms": {
      "custom_term_1": {
        "duration_in_days": 26,
        "duration_threshold": "15 min",
        "plots_datapoint": 26,
        "plots_datapoint_delta_in_days": 1
      },
      "weekly": {},
      "custom_term_2": {
        "duration_in_days": 115
      }
    }
  }
}
```

---

## Term Parameters

Each term can be defined using the following parameters:

| Parameter | Type | Required | Description |
|------------|------|-----------|-------------|
| **duration_in_days** | Double | Mandatory for custom terms | Defines the total lookback window (in days) for historical data. |
| **duration_threshold** | String | Optional | Minimum data required within `duration_in_days` to generate recommendations. e.g., `"30 min"`, `"2 days"`. |
| **plots_datapoint** | Integer | Optional | Number of data points for plots and visualizations. Defaults to one point per day. |
| **plots_datapoint_delta_in_days** | Double | Optional | Time interval (in days) between data points in plots. Defaults to `1.0`. |

---

## Key Principles

- **Default Terms Usage:**  
  To use a Fixed or Kruize Default term with its standard settings, provide an empty object `{}`.  
  Example: `"weekly": {}` will auto-populate with default values.

- **Custom Terms Definition:**  
  To define a new or overridden term, provide its name and parameters.

- **Mandatory Field:**  
  `duration_in_days` is mandatory for all custom terms.

- **Auto-Population:**  
  If only `duration_in_days` is provided, Kruize automatically fills other fields with sensible defaults.

---

## Default Behavior and Value Calculation

When users omit certain fields, Kruize applies a predefined logic to ensure smooth operation.

### Duration Threshold Calculation

| **Term Category** | **Default Threshold Logic** |
|--------------------|-----------------------------|
| **Fixed Default Terms** (`short`, `medium`, `long`) | Use pre-existing fixed thresholds — Short: *30 min*, Medium: *2 days*, Long: *8 days*. |
| **Kruize Default Terms** | Calculated as a percentage of the total duration. Shorter terms use smaller percentages, while monthly and above use **70%** of total duration. |
| **Custom Terms** | - For durations **≥ 15 days**, the threshold defaults to **70%**. <br> - For durations **< 15 days**, the threshold defaults to **max(45 mins, 50% of duration in days)**. <br> - For **Daily (1 day)**, the minimum threshold remains **30 minutes (2 datapoints)**. |




### Summary Table

Here is the complete table with the new terms and Minimum Data Points included:

| Term Name | Duration (days) | Min. Data Threshold (Threshold in days) | Threshold (%) | Plots Data Points (Same as Duration in Days) | Plots Data Point Delta in days | Minimum Data Points (Formula: Threshold in days * 96) |
|------------|----------------|-----------------------------------------|---------------|---------------------------------------------|-------------------------------|-------------------------------------------------------|
| Short term / Daily | 1 | 0.02 (30 mins) | ~2% | 4 | 0.25 (6 hrs) | 2 |
| Medium term / Weekly | 7 | 2 | ~29% | 7 | 1 | 192 |
| Long term / 15 Days | 15 | 8 | ~53% | 15 | 1 | 768 |
| Monthly | 30 | 21 | ~70% | 30 | 1 | 2016 |
| Quarterly | 90 | 63 | ~70% | 90 | 1 | 6048 |
| Half Yearly | 180 | 126 | ~70% | 180 | 1 | 12096 |
| Yearly | 365 | 256 | ~70% | 365 | 1 | 24576 |

---

## Validation Logic

Robust validation ensures data integrity and provides actionable user feedback.

### Basic Validation

- **Term Name Syntax:** Must contain only letters, numbers, underscores (`_`), or hyphens (`-`).
- **No Whitespace:** Term names cannot be empty or whitespace-only.
- **No Duplicates:** Duplicate term names within an experiment are not allowed.
- **Positive Values:**
    - `duration_in_days`, `plots_datapoint`, and `plots_datapoint_delta_in_days` must be greater than zero.
- **Threshold vs. Duration:**  
  `duration_threshold` cannot exceed `duration_in_days`.
- **Threshold Format:**  
  Must follow a parsable format (e.g., `"30 min"`, `"2 days"`).
- **Immutability Checks:**
    - Fixed Default Terms (`short`, `medium`, `long`) cannot be modified.
    - `duration_in_days` for Kruize Default Terms cannot be changed.


### Additional Validations for Kruize Default Terms

| **Parameter** | **Validation Rule** |
|----------------|---------------------|
| **duration_threshold** | Must be greater than `0` and less than the term’s `duration_in_days`. |
| **plots_datapoint** | Must be a positive integer with a minimum value of `2`. A reasonable upper limit (e.g., `1000`) is recommended to prevent excessively high values. |
| **plots_datapoint_delta_in_days** | Must be a positive number greater than `0.0`. |


## Summary

The **Terms Customization** feature enhances flexibility for Kruize users, allowing:
- Fine-grained control over data collection periods.
- Clear differentiation between immutable defaults and customizable terms.
- Automatic defaults and strict validations for safer configurations.

By enabling tailored term definitions, users can align recommendation windows precisely with their operational or business cycles — from daily checks to multi-month analyses.

---

