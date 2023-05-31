# Notification Codes

# Code Range table

| Section  | Section Range | Subsection        | Subsection Range | Subsystem            | Subsystem Range    |
|----------|---------------|-------------------|------------------|----------------------|--------------------|
| INFO     | 100000-199999 | General           | 110000-119999    | General              | 110000-112999      |
|          |               |                   |                  | Reserved             | 113000-119999      |
|          |               | Data              | 120000-129999    | General              | 121000-122999      |
|          |               |                   |                  | CPU                  | 123000-123999      |
|          |               |                   |                  | Memory               | 124000-124999      |
|          |               |                   |                  | Network              | 125000-125999      |
|          |               |                   |                  | Disk                 | 126000-126999      |
|          |               |                   |                  | Power                | 127000-127999      |
|          |               |                   |                  | Future Subsystem 1   | 128000-128999      |
|          |               |                   |                  | Future Subsystem 2   | 129000-129999      |
| ERROR    | 200000-299999 | General           | 210000-219999    | General              | 210000-212999      |
|          |               |                   |                  | Reserved             | 213000-219999      |
|          |               | Data              | 220000-229999    | General              | 221000-222999      |
|          |               |                   |                  | CPU                  | 223000-223999      |
|          |               |                   |                  | Memory               | 224000-224999      |
|          |               |                   |                  | Network              | 225000-225999      |
|          |               |                   |                  | Disk                 | 226000-226999      |
|          |               |                   |                  | Power                | 227000-227999      |
|          |               |                   |                  | Future Subsystem 1   | 228000-228999      |
|          |               |                   |                  | Future Subsystem 2   | 229000-229999      |
| NOTICE   | 300000-399999 | General           | 310000-319999    | General              | 310000-312999      |
|          |               |                   |                  | Reserved             | 313000-319999      |
|          |               | Data              | 320000-329999    | General              | 321000-322999      |
|          |               |                   |                  | CPU                  | 323000-323999      |
|          |               |                   |                  | Memory               | 324000-324999      |
|          |               |                   |                  | Network              | 325000-325999      |
|          |               |                   |                  | Disk                 | 326000-326999      |
|          |               |                   |                  | Power                | 327000-327999      |
|          |               |                   |                  | Future Subsystem 1   | 328000-328999      |
|          |               |                   |                  | Future Subsystem 2   | 329000-329999      |
| WARNING  | 400000-499999 | General           | 410000-419999    | General              | 410000-412999      |
|          |               |                   |                  | Reserved             | 413000-419999      |
|          |               | Data              | 420000-429999    | General              | 421000-422999      |
|          |               |                   |                  | CPU                  | 423000-423999      |
|          |               |                   |                  | Memory               | 424000-424999      |
|          |               |                   |                  | Network              | 425000-425999      |
|          |               |                   |                  | Disk                 | 426000-426999      |
|          |               |                   |                  | Power                | 427000-427999      |
|          |               |                   |                  | Future Subsystem 1   | 428000-428999      |
|          |               |                   |                  | Future Subsystem 2   | 429000-429999      |
| CRITICAL | 500000-599999 | General           | 510000-519999    | General              | 510000-512999      |
|          |               |                   |                  | Reserved             | 513000-519999      |
|          |               | Data              | 520000-529999    | General              | 521000-522999      |
|          |               |                   |                  | CPU                  | 523000-523999      |
|          |               |                   |                  | Memory               | 524000-524999      |
|          |               |                   |                  | Network              | 525000-525999      |
|          |               |                   |                  | Disk                 | 526000-526999      |
|          |               |                   |                  | Power                | 527000-527999      |
|          |               |                   |                  | Future Subsystem 1   | 528000-528999      |
|          |               |                   |                  | Future Subsystem 2   | 529000-529999      |



# Detailed Codes

|   CODE   |  TYPE   |     SHORT NOTATION      |                         DESCRIPTION                         |                          MESSAGE                            |
|----------|---------|------------------------|-------------------------------------------------------------|------------------------------------------------------------|
| 112101   |  INFO   | DURATION_BASED_RECOMMENDATIONS_AVAILABLE | Specifies that the Duration Based Recommendations are available |       Duration Based Recommendations Available        |
| 120001   |  INFO   |       NOT_ENOUGH_DATA      |       Specifies that required data is not enough to create a recommendation     |  There is not enough data available to generate a recommendation. |
| 313001   | NOTICE |      CPU_RECORDS_ARE_IDLE        |       Specifies that the CPU recordings are IDLE (Not much active CPU usage is identified)      |  CPU usage is mostly idle, No CPU Recommendation can be generated |
| 323001   | NOTICE |      CPU_RECORDS_ARE_ZERO       |       Specifies that the CPU recordings are ZERO      |  CPU usage is zero, No CPU Recommendations can be generated |
| 523001   | CRITICAL |     CPU_REQUEST_NOT_SET       |      Specifies that the CPU Requests are not set for the pod       |  CPU Request Not Set |
| 523002   | CRITICAL |     CPU_LIMIT_NOT_SET       |     Specifies that the CPU Limits are not set for the pod        |  CPU Limit Not Set |
| 524001   | CRITICAL |     MEMORY_REQUEST_NOT_SET       |    Specifies that the Memory Requests are not set for the pod          |  Memory Request Not Set |
| 524002   | CRITICAL |     MEMORY_LIMIT_NOT_SET       |     Specifies that the Memory Limits are not set for the pod        |  Memory Limit Not Set |




