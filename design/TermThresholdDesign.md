# Term Threshold
Presently, recommendations are categorized into Short, Medium, and Long terms based on the duration of historical data considered.

### Earlier Design Issues
The earlier design (v0.0.20.1) related to term durations has identified shortcomings:

**Lack of Strict Enforcement**: Term durations were not strictly enforced, allowing the system to traverse back in time 
upto a threshold to meet term requirements. This created confusion for users who are unclear about the specific duration
considered for a recommendation.

**Insufficient Data Handling**: In cases where the required number of data points for a given term is not available, no 
recommendation was generated. This limitation was particularly evident for short-term recommendations, making it 
impossible to generate insights for short-lived workloads.
We needed at least 24 hours data for Short term, 7 days of data for Medium term and 15 days for Long term to be able to 
generate recommendation. So, to ensure data adequacy for generating recommendations, we have introduced Minimum Data 
Thresholds:

### A. Fixing Term Durations and removal of Buffer thresholds

- To address the identified issues, we have introduced the following changes:

  - **Short Term:** Fixed at 24 hours.
  - **Medium Term:** Fixed at 7 days.
  - **Long Term:** Fixed at 15 days.

- The current buffer periods of 6 hours above the given duration is eliminated now. The system will no longer traverse further back in time beyond the prescribed term durations.

### B. Introduction of Minimum Data Thresholds

To ensure data adequacy for generating recommendations, we introduce minimum data thresholds:

- **Short Term:** 30 minutes.
- **Medium Term:** 2 days.
- **Long Term:** 8 days.

When the available data is less than the above threshold for each term, Kruize response will indicate "_not enough data available to generate a recommendation_."

### Changes Overview:

| Aspect                              | Earlier Design (v0.0.20.1)                                                                                                                                          | Changes (v0.0.20.2)                                                                                                       |
|-------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------|
| **Term Durations**                  | Flexible durations allowing buffers to go beyond max duration.                                                                                                      | Fixed durations: **Short (24 hours)**, **Medium (7 days)**, **Long (15 days)**.                                                       |
| **Buffer Thresholds**               | 6-hour buffer period allowed for historical data.                                                                                                                   | Buffer thresholds removed; Data retrieval strictly within term durations.                                                 |
| **Minimum Data Thresholds**         | We had a requirement of at least: <br/>**1 Day** for **Short term**. <br/>**7 days** for **Medium term**. <br/>**15 days** for **Long term**. | Introduced minimum data thresholds: <br/>**Short Term** (**30 mins**), <br/>**Medium Term** (**2 days**), <br/>**Long** (**8 days**). |
| **Impact on Short-lived Workloads** | Unable to generate recommendations for short-lived workloads.                                                                                                       | Minimum data thresholds allow meaningful recommendations for shorter durations.                                           |
| **User Clarity**                    | Users may not be aware of the actual duration considered.                                                                                                           | Strict enforcement provides clear insights into the duration for each recommendation.                                     |

### Scenarios
#### Case: Contiguous Order
    When data is available in a contiguous order i.e. no data point is missed in between the available duration.
  - #### 15 mins of data
  
    **Response**
      ```
      "120001": {
                  "type": "info",
                  "message": "There is not enough data available to generate a recommendation.",
                  "code": 120001
                }
      ```
  - #### 30 mins to 47 hours and 45 mins of data

    **Response**
      ```
      "111101": {
                  "type": "info",
                  "message": "Short Term Recommendations Available",
                  "code": 111101
                }
      ```
  - #### 2 Days to 7 days, 23 hours and 45 mins of data

    **Response**
      ```
      "111101": {
              "type": "info",
              "message": "Short Term Recommendations Available",
              "code": 111101
              },
      "111102": {
              "type": "info",
              "message": "Medium Term Recommendations Available",
              "code": 111102
              }  
      ```
  - #### 8 Days of data available

    **Response**
      ```
      "111101": {
              "type": "info",
              "message": "Short Term Recommendations Available",
              "code": 111101
              },
      "111102": {
              "type": "info",
              "message": "Medium Term Recommendations Available",
              "code": 111102
              },
      "111103": {
              "type": "info",
              "message": "Long Term Recommendations Available",
              "code": 111103
              }  
      ```

#### Case: Non - Contiguous Order
    When data is available in a non-contiguous order i.e. some data points are missed in between the available duration.
  - #### 30 mins of data with 1 data point available after more than 24 hours  

    **Response**
      ```
      "120001": {
                "type": "info",
                "message": "There is not enough data available to generate a recommendation.",
                "code": 120001
                }
      ```
  - #### 2 days of data with :
    - **1 or no data point available in last 24 hours**  <br><br>

      **Response**<br><br>
      - **Short Term**
          ```
          "120001": {
                    "type": "info",
                    "message": "There is not enough data available to generate a recommendation.",
                    "code": 120001
                    }
          ```
      - **Medium Term**
        ```
        "111102": {
                "type": "info",
                "message": "Medium Term Recommendations Available",
                "code": 111102
                },
        ```
  - #### 8 days or more data with :
      - **1 or no data point available in last 24 hours** 
      - **less than 192 data points available within last 7 days**  <br><br>

        **Response** <br><br>
        - **Short Term**
            ```
            "120001": {
                      "type": "info",
                      "message": "There is not enough data available to generate a recommendation.",
                      "code": 120001
                      }
            ```
        - **Medium Term**
            ```
            "120001": {
                      "type": "info",
                      "message": "There is not enough data available to generate a recommendation.",
                      "code": 120001
                      }
            ```
        - **Long Term**
            ```
            "111103": {
                      "type": "info",
                      "message": "Long Term Recommendations Available",
                      "code": 111103
                      }
            ```
