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

|    CODE    |  TYPE   |      SHORT NOTATION      |                          DESCRIPTION                           |                             MESSAGE                            |    SCOPE    |
|:----------:|:-------:|:-----------------------:|:------------------------------------------------------------:|:------------------------------------------------------------:|:-----------:|
|   112101   |  INFO   | DURATION_BASED_RECOMMENDATIONS_AVAILABLE |      Specifies that the Duration Based Recommendations are available      |           Duration Based Recommendations Available          |  API USER   |
|   120001   |  INFO   |         NOT_ENOUGH_DATA          | Specifies that required data is not enough to create a recommendation |    There is not enough data available to generate a recommendation.   |  API USER   |
|   323001   | NOTICE |        CPU_RECORDS_ARE_IDLE         | Specifies that the CPU records in the observed period are less than a millicore | CPU Usage is less than a millicore, No CPU Recommendation can be generated |    DATA USER     |
|   323002   | NOTICE |        CPU_RECORDS_ARE_ZERO         |               Specifies that the CPU recordings are ZERO               |                CPU usage is zero, No CPU Recommendations can be generated               |    DATA USER     |
|   323003   | NOTICE |      CPU_RECORDS_NOT_AVAILABLE        |           Specifies that the CPU recordings are NOT AVAILABLE           |              CPU metrics are not available, No CPU Recommendations can be generated             |    DATA USER     |
|   324001   | NOTICE |      MEMORY_RECORDS_ARE_ZERO        |               Specifies that the Memory recordings are ZERO               |                Memory usage is zero, No Memory Recommendations can be generated               |    DATA USER     |
|   324002   | NOTICE |     MEMORY_RECORDS_NOT_AVAILABLE      |           Specifies that the Memory recordings are NOT AVAILABLE           |              Memory metrics not available, No Memory Recommendations can be generated             |    DATA USER     |
|   523001   | CRITICAL |        CPU_REQUEST_NOT_SET         |           Specifies that the CPU Requests are not set for the pod           |                      CPU Request Not Set                     |    DATA USER     |
|   423001   | WARNING |        CPU_LIMIT_NOT_SET          |           Specifies that the CPU Limits are not set for the pod           |                      CPU Limit Not Set                       |    DATA USER     |
|   524001   | CRITICAL |      MEMORY_REQUEST_NOT_SET        |           Specifies that the Memory Requests are not set for the pod          |                    Memory Request Not Set                   |    DATA USER     |
|   524002   | CRITICAL |      MEMORY_LIMIT_NOT_SET         |           Specifies that the Memory Limits are not set for the pod           |                    Memory Limit Not Set                     |    DATA USER     |

