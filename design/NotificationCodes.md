# Notification Codes

# Code Range table

| Section  | Section Range | Subsection        | Subsection Range | Subsystem            | Subsystem Range    | SCOPE    |
|----------|---------------|-------------------|------------------|----------------------|--------------------|----------|
| INFO     | 100000-199999 | General           | 110000-119999    | General              | 110000-112999      | Internal |
|          |               |                   |                  | Reserved             | 113000-119999      | Internal |
|          |               | Data              | 120000-129999    | General              | 121000-122999      | Internal |
|          |               |                   |                  | CPU                  | 123000-123999      | Internal |
|          |               |                   |                  | Memory               | 124000-124999      | Internal |
|          |               |                   |                  | Network              | 125000-125999      | Internal |
|          |               |                   |                  | Disk                 | 126000-126999      | Internal |
|          |               |                   |                  | Power                | 127000-127999      | Internal |
| ERROR    | 200000-299999 | General           | 210000-219999    | General              | 210000-212999      | Internal |
|          |               |                   |                  | Reserved             | 213000-219999      | Internal |
|          |               | Data              | 220000-229999    | General              | 221000-222999      | Internal |
|          |               |                   |                  | CPU                  | 223000-223999      | Internal |
|          |               |                   |                  | Memory               | 224000-224999      | Internal |
|          |               |                   |                  | Network              | 225000-225999      | Internal |
|          |               |                   |                  | Disk                 | 226000-226999      | Internal |
|          |               |                   |                  | Power                | 227000-227999      | Internal |
| NOTICE   | 300000-399999 | General           | 310000-319999    | General              | 310000-312999      | User     |
|          |               |                   |                  | Reserved             | 313000-319999      | User     |
|          |               | Data              | 320000-329999    | General              | 321000-322999      | User     |
|          |               |                   |                  | CPU                  | 323000-323999      | User     |
|          |               |                   |                  | Memory               | 324000-324999      | User     |
|          |               |                   |                  | Network              | 325000-325999      | User     |
|          |               |                   |                  | Disk                 | 326000-326999      | User     |
|          |               |                   |                  | Power                | 327000-327999      | User     |
| WARNING  | 400000-499999 | General           | 410000-419999    | General              | 410000-412999      | User     |
|          |               |                   |                  | Reserved             | 413000-419999      | User     |
|          |               | Data              | 420000-429999    | General              | 421000-422999      | User     |
|          |               |                   |                  | CPU                  | 423000-423999      | User     |
|          |               |                   |                  | Memory               | 424000-424999      | User     |
|          |               |                   |                  | Network              | 425000-425999      | User     |
|          |               |                   |                  | Disk                 | 426000-426999      | User     |
|          |               |                   |                  | Power                | 427000-427999      | User     |
| CRITICAL | 500000-599999 | General           | 510000-519999    | General              | 510000-512999      | User     |
|          |               |                   |                  | Reserved             | 513000-519999      | User     |
|          |               | Data              | 520000-529999    | General              | 521000-522999      | User     |
|          |               |                   |                  | CPU                  | 523000-523999      | User     |
|          |               |                   |                  | Memory               | 524000-524999      | User     |
|          |               |                   |                  | Network              | 525000-525999      | User     |
|          |               |                   |                  | Disk                 | 526000-526999      | User     |
|          |               |                   |                  | Power                | 527000-527999      | User     |


# Detailed Codes

|    CODE    |  TYPE   |      SHORT NOTATION      |                          DESCRIPTION                           |                             MESSAGE                            |    SCOPE    |
|:----------:|:-------:|:-----------------------:|:------------------------------------------------------------:|:------------------------------------------------------------:|:-----------:|
|   112101   |  INFO   | DURATION_BASED_RECOMMENDATIONS_AVAILABLE |      Specifies that the Duration Based Recommendations are available      |           Duration Based Recommendations Available          |  Internal   |
|   120001   |  INFO   |         NOT_ENOUGH_DATA          | Specifies that required data is not enough to create a recommendation |    There is not enough data available to generate a recommendation.   |  Internal   |
|   323001   | NOTICE |        CPU_RECORDS_ARE_IDLE         | Specifies that the CPU recordings are IDLE (Not much active CPU usage is identified) | CPU usage is mostly idle (< 0.001 cores or < 1 millicore), No CPU Recommendation can be generated |    User     |
|   323002   | NOTICE |        CPU_RECORDS_ARE_ZERO         |               Specifies that the CPU recordings are ZERO               |                CPU usage is zero, No CPU Recommendations can be generated               |    User     |
|   323003   | NOTICE |      CPU_RECORDS_ARE_MISSING        |           Specifies that the CPU recordings are MISSING           |              CPU metrics are missing, No CPU Recommendations can be generated             |    User     |
|   324001   | NOTICE |      MEMORY_RECORDS_ARE_ZERO        |               Specifies that the Memory recordings are ZERO               |                Memory usage is zero, No Memory Recommendations can be generated               |    User     |
|   324002   | NOTICE |     MEMORY_RECORDS_ARE_MISSING      |           Specifies that the Memory recordings are MISSING           |              Memory metrics are missing, No Memory Recommendations can be generated             |    User     |
|   523001   | CRITICAL |        CPU_REQUEST_NOT_SET         |           Specifies that the CPU Requests are not set for the pod           |                      CPU Request Not Set                     |    User     |
|   423001   | WARNING |        CPU_LIMIT_NOT_SET          |           Specifies that the CPU Limits are not set for the pod           |                      CPU Limit Not Set                       |    User     |
|   524001   | CRITICAL |      MEMORY_REQUEST_NOT_SET        |           Specifies that the Memory Requests are not set for the pod          |                    Memory Request Not Set                   |    User     |
|   524002   | CRITICAL |      MEMORY_LIMIT_NOT_SET         |           Specifies that the Memory Limits are not set for the pod           |                    Memory Limit Not Set                     |    User     |

