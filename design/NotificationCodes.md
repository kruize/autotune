# Notification Codes

# Code Range table

| Section  | Section Range | Subsection        | Subsection Range | Subsystem            | Subsystem Range    | SCOPE    |
|----------|---------------|-------------------|------------------|----------------------|--------------------|----------|
| INFO     | 100000-199999 | General           | 110000-119999    | General              | 110000-112999      | API USER |
|          |               |                   |                  | Reserved             | 113000-119999      | API USER |
|          |               | Data              | 120000-129999    | General              | 121000-122999      | API USER |
|          |               |                   |                  | CPU                  | 123000-123999      | API USER |
|          |               |                   |                  | Memory               | 124000-124999      | API USER |
|          |               |                   |                  | Network              | 125000-125999      | API USER |
|          |               |                   |                  | Disk                 | 126000-126999      | API USER |
|          |               |                   |                  | Power                | 127000-127999      | API USER |
| ERROR    | 200000-299999 | General           | 210000-219999    | General              | 210000-212999      | API USER |
|          |               |                   |                  | Reserved             | 213000-219999      | API USER |
|          |               | Data              | 220000-229999    | General              | 221000-222999      | API USER |
|          |               |                   |                  | CPU                  | 223000-223999      | API USER |
|          |               |                   |                  | Memory               | 224000-224999      | API USER |
|          |               |                   |                  | Network              | 225000-225999      | API USER |
|          |               |                   |                  | Disk                 | 226000-226999      | API USER |
|          |               |                   |                  | Power                | 227000-227999      | API USER |
| NOTICE   | 300000-399999 | General           | 310000-319999    | General              | 310000-312999      | DATA USER     |
|          |               |                   |                  | Reserved             | 313000-319999      | DATA USER     |
|          |               | Data              | 320000-329999    | General              | 321000-322999      | DATA USER     |
|          |               |                   |                  | CPU                  | 323000-323999      | DATA USER     |
|          |               |                   |                  | Memory               | 324000-324999      | DATA USER     |
|          |               |                   |                  | Network              | 325000-325999      | DATA USER     |
|          |               |                   |                  | Disk                 | 326000-326999      | DATA USER     |
|          |               |                   |                  | Power                | 327000-327999      | DATA USER     |
| WARNING  | 400000-499999 | General           | 410000-419999    | General              | 410000-412999      | DATA USER     |
|          |               |                   |                  | Reserved             | 413000-419999      | DATA USER     |
|          |               | Data              | 420000-429999    | General              | 421000-422999      | DATA USER     |
|          |               |                   |                  | CPU                  | 423000-423999      | DATA USER     |
|          |               |                   |                  | Memory               | 424000-424999      | DATA USER     |
|          |               |                   |                  | Network              | 425000-425999      | DATA USER     |
|          |               |                   |                  | Disk                 | 426000-426999      | DATA USER     |
|          |               |                   |                  | Power                | 427000-427999      | DATA USER     |
| CRITICAL | 500000-599999 | General           | 510000-519999    | General              | 510000-512999      | DATA USER     |
|          |               |                   |                  | Reserved             | 513000-519999      | DATA USER     |
|          |               | Data              | 520000-529999    | General              | 521000-522999      | DATA USER     |
|          |               |                   |                  | CPU                  | 523000-523999      | DATA USER     |
|          |               |                   |                  | Memory               | 524000-524999      | DATA USER     |
|          |               |                   |                  | Network              | 525000-525999      | DATA USER     |
|          |               |                   |                  | Disk                 | 526000-526999      | DATA USER     |
|          |               |                   |                  | Power                | 527000-527999      | DATA USER     |


# Detailed Codes

|   CODE   |   TYPE   |        SHORT NOTATION        |                         DESCRIPTION                          |                                  MESSAGE                                  |    SCOPE    |
|:--------:|:--------:|:---------------------------:|:----------------------------------------------------------:|:-------------------------------------------------------------------------:|:-----------:|
|  112101  |   INFO   | DURATION_BASED_RECOMMENDATIONS_AVAILABLE | Specifies that the Duration Based Recommendations are available |          Duration Based Recommendations Available          |  API USER   |
|  120001  |   INFO   |         NOT_ENOUGH_DATA         |  Specifies that required data is not enough to create a recommendation |    There is not enough data available to generate a recommendation.    |  API USER   |
|  221001  |   ERROR  |    NUM_PODS_CANNOT_BE_ZERO    |           Specifies that the number of pods cannot be zero           |              Number of pods cannot be zero              |  API USER   |
|  221002  |   ERROR  |   NUM_PODS_CANNOT_BE_NEGATIVE |        Specifies that the number of pods cannot be negative         |           Number of pods cannot be negative            |  API USER   |
|  221003  |   ERROR  |       HOURS_CANNOT_BE_ZERO       |          Specifies that duration hours cannot be zero           |              Duration hours cannot be zero              |  API USER   |
|  221004  |   ERROR  |     HOURS_CANNOT_BE_NEGATIVE     |        Specifies that duration hours cannot be negative         |           Duration hours cannot be negative            |  API USER   |
|  223001  |   ERROR  | AMOUNT_MISSING_IN_CPU_SECTION |   Specifies that the amount field is missing in the CPU Section   |         Amount field is missing in the CPU Section         |  API USER   |
|  223002  |   ERROR  | INVALID_AMOUNT_IN_CPU_SECTION |  Specifies that there is an invalid amount in the CPU Section  |          Invalid Amount in CPU Section          |  API USER   |
|  223003  |   ERROR  | FORMAT_MISSING_IN_CPU_SECTION |   Specifies that the format field is missing in the CPU Section   |        Format field is missing in CPU Section         |  API USER   |
|  223004  |   ERROR  | INVALID_FORMAT_IN_CPU_SECTION |  Specifies that there is an invalid format in the CPU Section  |          Invalid Format in CPU Section          |  API USER   |
|  224001  |   ERROR  | AMOUNT_MISSING_IN_MEMORY_SECTION | Specifies that the amount field is missing in the Memory Section |      Amount field is missing in the Memory Section       |  API USER   |
|  224002  |   ERROR  | INVALID_AMOUNT_IN_MEMORY_SECTION | Specifies that there is an invalid amount in the Memory Section |       Invalid Amount in Memory Section        |  API USER   |
|  224003  |   ERROR  | FORMAT_MISSING_IN_MEMORY_SECTION | Specifies that the format field is missing in the Memory Section |    Format field is missing in Memory Section     |  API USER   |
|  224004  |   ERROR  | INVALID_FORMAT_IN_MEMORY_SECTION | Specifies that there is an invalid format in the Memory Section |      Invalid Format in Memory Section       |  API USER   |
|  323001  |  NOTICE  |        CPU_RECORDS_ARE_IDLE         |  Specifies that the CPU records in the observed period are less than a millicore | CPU Usage is less than a millicore, No CPU Recommendations can be generated |    DATA USER     |
|  323002  |  NOTICE  |        CPU_RECORDS_ARE_ZERO         |            Specifies that the CPU recordings are ZERO            |     CPU usage is zero, No CPU Recommendations can be generated     |    DATA USER     |
|  323003  |  NOTICE  |      CPU_RECORDS_NOT_AVAILABLE      |         Specifies that the CPU recordings are NOT AVAILABLE         |   CPU metrics are not available, No CPU Recommendations can be generated   |    DATA USER     |
|  323004  |  NOTICE  |    CPU_REQUESTS_OPTIMISED    |      Specifies that the workload is optimised wrt CPU REQUESTS      | Workload is optimised wrt CPU REQUESTS, no changes needed |    DATA USER     |
|  323005  |  NOTICE  |    CPU_LIMITS_OPTIMISED    |      Specifies that the workload is optimised wrt CPU LIMITS      | Workload is optimised wrt CPU LIMITS, no changes needed |    DATA USER     |
|  324001  |  NOTICE  |      MEMORY_RECORDS_ARE_ZERO      |            Specifies that the Memory recordings are ZERO            |   Memory usage is zero, No Memory Recommendations can be generated   |    DATA USER     |
|  324002  |  NOTICE  |     MEMORY_RECORDS_NOT_AVAILABLE    |        Specifies that the Memory recordings are NOT AVAILABLE        | Memory metrics are not available, No Memory Recommendations can be generated |    DATA USER     |
|  324003  |  NOTICE  |   MEMORY_REQUESTS_OPTIMISED    |      Specifies that the workload is optimised wrt MEMORY REQUESTS     | Workload is optimised wrt MEMORY REQUESTS, no changes needed |    DATA USER     |
|  324004  |  NOTICE  |   MEMORY_LIMITS_OPTIMISED    |      Specifies that the workload is optimised wrt MEMORY LIMITS      | Workload is optimised wrt MEMORY LIMITS, no changes needed |    DATA USER     |
|  423001  | WARNING  |        CPU_LIMIT_NOT_SET          |          Specifies that the CPU Limits are not set for the pod          |                 CPU Limit Not Set                 |    DATA USER     |
|  523001  | CRITICAL |        CPU_REQUEST_NOT_SET         |          Specifies that the CPU Requests are not set for the pod         |                CPU Request Not Set               |    DATA USER     |
|  524001  | CRITICAL |      MEMORY_REQUEST_NOT_SET       |         Specifies that the Memory Requests are not set for the pod        |              Memory Request Not Set             |    DATA USER     |
|  524002  | CRITICAL |      MEMORY_LIMIT_NOT_SET         |          Specifies that the Memory Limits are not set for the pod         |               Memory Limit Not Set              |    DATA USER     |


# Notification Scope

| **Notification Id** | **Type** | **Notification Short Representation** | **Recommendations Object level** | **Timestamp level** | **Term level** | **Engine level** |
|---------------------|:--------:|:-------------------------------------:|:--------------------------------:|:-------------------:|:--------------:|:----------------:|
| 111000              |   INFO   |       RECOMMENDATIONS_AVAILABLE       |                1                 |          0          |       0        |        0         |
| 120001              |   INFO   |            NOT_ENOUGH_DATA            |                1                 |          0          |       1        |        0         |
| 423001              | WARNING  |           CPU_LIMIT_NOT_SET           |                0                 |          1          |       0        |        0         |
| 523001              | CRITICAL |          CPU_REQUEST_NOT_SET          |                0                 |          1          |       0        |        0         |
| 524001              | CRITICAL |        MEMORY_REQUEST_NOT_SET         |                0                 |          1          |       0        |        0         |
| 524002              | CRITICAL |         MEMORY_LIMIT_NOT_SET          |                0                 |          1          |       0        |        0         |
| 223001              |  ERROR   |     AMOUNT_MISSING_IN_CPU_SECTION     |                0                 |          1          |       0        |        1         |
| 223002              |  ERROR   |     INVALID_AMOUNT_IN_CPU_SECTION     |                0                 |          1          |       0        |        1         |
| 224001              |  ERROR   |   AMOUNT_MISSING_IN_MEMORY_SECTION    |                0                 |          1          |       0        |        1         |
| 224002              |  ERROR   |   INVALID_AMOUNT_IN_MEMORY_SECTION    |                0                 |          1          |       0        |        1         |
| 111101              |   INFO   | SHORT_TERM_RECOMMENDATIONS_AVAILABLE  |                0                 |          1          |       0        |        0         |
| 111102              |   INFO   | MEDIUM_TERM_RECOMMENDATIONS_AVAILABLE |                0                 |          1          |       0        |        0         |
| 111103              |   INFO   |  LONG_TERM_RECOMMENDATIONS_AVAILABLE  |                0                 |          1          |       0        |        0         |
| 112101              |   INFO   |    COST_RECOMMENDATIONS_AVAILABLE     |                0                 |          0          |       1        |        0         |
| 112102              |   INFO   | PERFORMANCE_RECOMMENDATION_AVAILABLE  |                0                 |          0          |       1        |        0         |
| 223003              |  ERROR   |     FORMAT_MISSING_IN_CPU_SECTION     |                0                 |          1          |       1        |        0         |
| 223004              |  ERROR   |     INVALID_FORMAT_IN_CPU_SECTION     |                0                 |          1          |       1        |        0         |
| 224003              |  ERROR   |   FORMAT_MISSING_IN_MEMORY_SECTION    |                0                 |          1          |       1        |        0         |
| 224004              |  ERROR   |   INVALID_FORMAT_IN_MEMORY_SECTION    |                0                 |          1          |       1        |        0         |
| 323001              |  NOTICE  |         CPU_RECORDS_ARE_IDLE          |                0                 |          0          |       1        |        0         |
| 323002              |  NOTICE  |         CPU_RECORDS_ARE_ZERO          |                0                 |          0          |       1        |        0         |
| 323003              |  NOTICE  |       CPU_RECORDS_NOT_AVAILABLE       |                0                 |          0          |       1        |        0         |
| 324001              |  NOTICE  |        MEMORY_RECORDS_ARE_ZERO        |                0                 |          0          |       1        |        0         |
| 324002              |  NOTICE  |     MEMORY_RECORDS_NOT_AVAILABLE      |                0                 |          0          |       1        |        0         |
| 221003              |  ERROR   |         HOURS_CANNOT_BE_ZERO          |                0                 |          0          |       1        |        0         |
| 221004              |  ERROR   |       HOURS_CANNOT_BE_NEGATIVE        |                0                 |          0          |       1        |        0         |
| 323004              |  NOTICE  |        CPU_REQUESTS_OPTIMISED         |                0                 |          0          |       0        |        1         |
| 323005              |  NOTICE  |         CPU_LIMITS_OPTIMISED          |                0                 |          0          |       0        |        1         |
| 324003              |  NOTICE  |       MEMORY_REQUESTS_OPTIMISED       |                0                 |          0          |       0        |        1         |
| 324004              |  NOTICE  |        MEMORY_LIMITS_OPTIMISED        |                0                 |          0          |       0        |        1         |
| 221001              |  ERROR   |        NUM_PODS_CANNOT_BE_ZERO        |                0                 |          0          |       0        |        1         |
| 221002              |  ERROR   |      NUM_PODS_CANNOT_BE_NEGATIVE      |                0                 |          0          |       0        |        1         |
