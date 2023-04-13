## Build Your Own Kruize Recommendation Engine


`Kruize` Recommendation Engine generates personalized recommendations based on specific criteria. Currently, Kruize features two default recommendation engines: `Duration Based Engine` and `Profile Based Engine`

While `Kruize` provides default recommendation engines, users aren't limited to just these options as they `may not cover all use cases`. `Kruize` is designed to be extensible, allowing users to add their own custom engines and develop personalized recommendation logic. However, `With great power comes great responsibility`, So in this article, we'll explore how to create a `custom recommendation engine` for `Kruize` while emphasizing the importance of careful implementation.

### Know your playground

Since Kruize is a significant codebase, the recommendation logic is centralized within the `com.autotune.analyzer.recommendations` package. For the purpose of this article, our focus will mainly be on this package.

### Creating your engine

Every Custom Kruize Recommendation Engine (CKRE) class should be added in the package `com.autotune.analyzer.recommendations.engine`

Let's go ahead and create a class in the package, Every CKRE should implement `KruizeRecommendationEngine` interface. So our CKRE class looks like this

```
package com.autotune.analyzer.recommendations.engine;

public class CustomKruizeRecommendationEngine implements KruizeRecommendationEngine{
    
    private String name;
    private String key;
    private AnalyzerConstants.RecommendationCategory category;

    public CustomKruizeRecommendationEngine() {
        this.name = "My Custom Engine";
        this.key = "customEngine";
        // We need to create this in Recommendation Category
        this.category = AnalyzerConstants.RecommendationCategory.SIMPLE_CATEGORY;
    } 
    @Override
    public String getEngineName() {
        return this.name;
    }

    @Override
    public String getEngineKey() {
        return this.key;
    }

    @Override
    public AnalyzerConstants.RecommendationCategory getEngineCategory() {
        return this.category;
    }

    @Override
    public HashMap<String, Recommendation> getRecommendations(ContainerData containerData, Timestamp monitoringEndTime) {
        // TODO: Needs to be implemented
        return null;
    }
}
```

Every CKRE should have a all these four methods implemented:

`getEngineName` - Return the name of the engine (in our case it's `My custom Engine`)

`getEngineKey` - Return the key of engine (an unique non spaced string which is specific to the engine and is not duplicate of existing engines, this is used to point out your recommendations in the output, in our case we set it to `customEngine`)

`getEngineCategory` - Return the category of the Engine (We need to create a new category, Each engine has a specific category)

`getRecommendations` - Return a map of sub category and recommendation object


Each Recommendation category within `Kruize` contains multiple subcategories. 
For example, the `Duration Based category` includes subcategories such as `Short Term`,
`Medium Term`, and `Long Term`. The necessary data for each subcategory is stored within
the corresponding category class. 
For instance, the `DurationBasedRecommendationSubCategory` class holds information such
as the `name`, `number of days`, and `time unit` for each subcategory 
(e.g., the Short Term subcategory has a name of "short_term," a duration of `1` day,
and a time unit of "days").

Each subcategory class must implement the `RecommendationSubCategory` interface and 
return the `key` that is expected in the `recommendation JSON` as the return value 
of `getSubCategory` method. In the `Duration Based` category, the `getSubCategory`
method returns the names "short_term," "medium_term," and "long_term" for 
their respective subcategories.


Our Custom recommendation category is straightforward, consisting of a single 
subcategory called `SimpleSubCategory`. This subcategory has one variable, which 
holds a `multiplication factor`. The recommendation engine returns a memory 
recommendation of `100 x multiplication factor` and a CPU recommendation 
of `10 x multiplication factor`.

Now, Let's create a Recommendation Category `SIMPLE_CATEGORY` in `Analyser Constants` 
and subcategory's related to that in `algos` package of recommendation.

We add the Recommendation Category to the `RecommendationCategory` enum
```
public enum RecommendationCategory {
        DURATION_BASED(
                KruizeConstants.JSONKeys.DURATION_BASED,
                new DurationBasedRecommendationSubCategory[]{
                        new DurationBasedRecommendationSubCategory(
                                KruizeConstants.JSONKeys.SHORT_TERM,
                                1,
                                TimeUnit.DAYS
                        ),
                        new DurationBasedRecommendationSubCategory(
                                KruizeConstants.JSONKeys.MEDIUM_TERM,
                                7,
                                TimeUnit.DAYS
                        ),
                        new DurationBasedRecommendationSubCategory(
                                KruizeConstants.JSONKeys.LONG_TERM,
                                15,
                                TimeUnit.DAYS
                        ),
                }
        ),
        // Need to update with profile based sub categories
        PROFILE_BASED(KruizeConstants.JSONKeys.PROFILE_BASED, null),
+--------------------------------------------------------------------+        
|        // We add our Category here                                 |
|        SIMPLE_CATEGORY("simple_category",                          |
|            new SimpleRecommendationSubCategory[]{                  |
|                        new SimpleRecommendationSubCategory(        |
|                                "simple_subcategory",               |
|                                1.5, // Multiplication Factor       |
|                }                                                   |
|        );                                                          |
+--------------------------------------------------------------------+        

        private String name;
        private RecommendationSubCategory[] recommendationSubCategories;

        private RecommendationCategory(String name, RecommendationSubCategory[] recommendationSubCategories) {
            this.name = name;
            this.recommendationSubCategories = recommendationSubCategories;
        }

        public String getName() {
            return this.name;
        }

        public RecommendationSubCategory[] getRecommendationSubCategories() {
            return this.recommendationSubCategories;
        }
    }
```


Now let's create the `SimpleRecommendationSubCategory` which we mentioned above in `RecommendationCategory`

As previously mentioned, the SimpleRecommendationSubCategory class implements the
`RecommendationSubCategory` interface and maintains a `multiplication factor variable`,
which is initialized to `1.5`.

Let's add this in `com.autotune.analyzer.recommendations.algos`

```
package com.autotune.analyzer.recommendations.algos;

import com.autotune.analyzer.recommendations.algos.RecommendationSubCategory;


public class SimpleRecommendationSubCategory implements RecommendationSubCategory {
    private String name;
    private float multiplicationFactor;

    public SimpleRecommendationSubCategory(String name, float multiplicationFactor) {
        this.name = name;
        this.multiplicationFactor = multiplicationFactor;
    }

    public float getMultiplicationFactor() {
        return this.multiplicationFactor;
    }

    @Override
    public String getSubCategory() {
        return this.name;
    }
}
```

With everything in place, we can now add the engine to an existing performance profile 
and integrate the recommendation logic into the `getRecommendations` method.

To add the engine to `ResourceOptimizationOpenshiftImpl`, we'll create a custom engine
instance within the `init` function of `ResourceOptimizationOpenshiftImpl`. Then, 
we'll add this custom engine instance to the list of engines.

Update this part of code in `ResourceOptimizationOpenshiftImpl`
```
private void init() {
    // Add new engines
    kruizeRecommendationEngineList = new ArrayList<KruizeRecommendationEngine>();
    // Create Duration based engine
    DurationBasedRecommendationEngine durationBasedRecommendationEngine =  new DurationBasedRecommendationEngine();
    // TODO: Create profile based engine
    AnalyzerConstants.RegisterRecommendationEngineStatus _unused_status = registerEngine(durationBasedRecommendationEngine);
+--------------------------------------------------------------------------------------------------------------------+    
|    CustomKruizeRecommendationEngine customKruizeRecommendationEngine = new CustomKruizeRecommendationEngine();     |
|    _unused_status = registerEngine(customKruizeRecommendationEngine);                                              |
+--------------------------------------------------------------------------------------------------------------------+
    // TODO: Add profile based once recommendation algos are available          
}
```

Now that we've registered our custom engine, we can generate recommendations. 
However, currently, the `getRecommendation` function within `CustomKruizeRecommendationEngine`
has not been implemented, and as a result, our recommendations will be `null`. 
Let's proceed to implement the function.

```
package com.autotune.analyzer.recommendations.engine;

public class CustomKruizeRecommendationEngine implements KruizeRecommendationEngine{
    
    private String name;
    private String key;
    private AnalyzerConstants.RecommendationCategory category;

    public CustomKruizeRecommendationEngine() {
        this.name = "My Custom Engine";
        this.key = "customEngine";
        // We need to create this in Recommendation Category
        this.category = AnalyzerConstants.RecommendationCategory.SIMPLE_CATEGORY;
    } 
    @Override
    public String getEngineName() {
        return this.name;
    }

    @Override
    public String getEngineKey() {
        return this.key;
    }

    @Override
    public AnalyzerConstants.RecommendationCategory getEngineCategory() {
        return this.category;
    }

    @Override
    public HashMap<String, Recommendation> getRecommendations(ContainerData containerData, Timestamp monitoringEndTime) {
        HashMap<Timestamp, IntervalResults> filteredResultsMap = containerData.getResults();
        HashMap<String, Recommendation> resultRecommendation = new HashMap<String, Recommendation>();
        for (RecommendationSubCategory recommendationSubCategory : this.category.getRecommendationSubCategories()) {
            SimpleRecommendationSubCategory simpleRecommendationSubCategory = (SimpleRecommendationSubCategory) recommendationSubCategory;
            String recPeriod = simpleRecommendationSubCategory.getSubCategory();
            float  mf = simpleRecommendationSubCategory.getMultiplicationFactor();
            // Making the recommendation for same time stamps as this is not duration based
            Timestamp monitorStartDate = monitoringEndTime;
            Recommendation recommendation = new Recommendation(monitorStartDate, monitoringEndTime);
            HashMap<AnalyzerConstants.ResourceSetting, HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem>> config = new HashMap<>();
            HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem> requestsMap = new HashMap<>();
            // Setting cpu recommendation as 10 X Multiplication Factor
            requestsMap.put(AnalyzerConstants.RecommendationItem.cpu, 10 * mf);
            // Setting memory recommendation as 100 X Multiplication Factor
            requestsMap.put(AnalyzerConstants.RecommendationItem.memory, 100 * mf);
            config.put(AnalyzerConstants.ResourceSetting.requests, requestsMap);
            HashMap<AnalyzerConstants.RecommendationItem, RecommendationConfigItem> limitsMap = new HashMap<>();
            // Setting cpu recommendation as 10 X Multiplication Factor
            limitsMap.put(AnalyzerConstants.RecommendationItem.cpu, 10 * mf);
            // Setting memory recommendation as 100 X Multiplication Factor
            limitsMap.put(AnalyzerConstants.RecommendationItem.memory, 100 * mf);
            config.put(AnalyzerConstants.ResourceSetting.limits, limitsMap);
            Double hours = filteredResultsMap.values().stream().map((x) -> (x.getDurationInMinutes()))
                    .collect(Collectors.toList())
                    .stream()
                    .mapToDouble(f -> f.doubleValue()).sum() / 60;
            recommendation.setDuration_in_hours(hours);
            recommendation.setConfig(config);
            resultRecommendation.put(recPeriod, recommendation);
        }
        return resultRecommendation;
    }
}
```

We updated the `getRecommendations` function within `CustomKruizeRecommendationEngine`,
which means that we should now receive our custom engine recommendations within the `data`
section of the `recommendation JSON`.

```
[
    {
        "cluster_name": "cluster-one-division-bell",
        "kubernetes_objects": [
            {
                "type": "deployment",
                "name": "tfb-qrh-deployment_0",
                "namespace": "default_0",
                "containers": [
                    {
                        "container_image_name": "kruize/tfb-qrh:1.13.2.F_et17",
                        "container_name": "tfb-server-1",
                        "recommendations": {
                            "notifications": [
                                {
                                    "type": "info",
                                    "message": "Duration Based Recommendations Available"
                                }
                            ],
                            "data": {
                                "2022-12-21T00:40:17.000Z": {
                                    "customEngine": {
                                        "simple_subcategory": {
                                            "monitoring_start_time": "2022-12-20T00:40:17.000Z",
                                            "monitoring_end_time": "2022-12-21T00:40:17.000Z",
                                            "duration_in_hours": 9.0,
                                            "pods_count": 0,
                                            "confidence_level": 0.0,
                                            "config": {
                                                "limits": {
                                                    "memory": {
                                                        "amount": 150.00,
                                                        "format": "MiB"
                                                    },
                                                    "cpu": {
                                                        "amount": 15.00,
                                                        "format": "cores"
                                                    }
                                                },
                                                "requests": {
                                                    "memory": {
                                                        "amount": 150.00,
                                                        "format": "MiB"
                                                    },
                                                    "cpu": {
                                                        "amount": 15.00,
                                                        "format": "cores"
                                                    }
                                                }
                                            },
                                            "notifications": []
                                        }
                                    },
                                    "duration_based": {
                                        "short_term": {
                                            "monitoring_start_time": "2022-12-20T00:40:17.000Z",
                                            "monitoring_end_time": "2022-12-21T00:40:17.000Z",
                                            "duration_in_hours": 9.0,
                                            "pods_count": 0,
                                            "confidence_level": 0.0,
                                            "config": {
                                                "limits": {
                                                    "memory": {
                                                        "amount": 982.5997506234414,
                                                        "format": "MiB"
                                                    },
                                                    "cpu": {
                                                        "amount": 6.51,
                                                        "format": "cores"
                                                    }
                                                },
                                                "requests": {
                                                    "memory": {
                                                        "amount": 123.6,
                                                        "format": "MiB"
                                                    },
                                                    "cpu": {
                                                        "amount": 1.03,
                                                        "format": "cores"
                                                    }
                                                }
                                            },
                                            "notifications": []
                                        },
                                        "medium_term": {
                                            "pods_count": 0,
                                            "confidence_level": 0.0,
                                            "notifications": [
                                                {
                                                    "type": "info",
                                                    "message": "There is not enough data available to generate a recommendation."
                                                }
                                            ]
                                        },
                                        "long_term": {
                                            "pods_count": 0,
                                            "confidence_level": 0.0,
                                            "notifications": [
                                                {
                                                    "type": "info",
                                                    "message": "There is not enough data available to generate a recommendation."
                                                }
                                            ]
                                        }
                                    }
                                }
                            }
                        }
                    },
                    {
                        "container_image_name": "kruize/tfb-db:1.15",
                        "container_name": "tfb-server-0",
                        "recommendations": {
                            "notifications": [
                                {
                                    "type": "info",
                                    "message": "There is not enough data available to generate a recommendation."
                                }
                            ],
                            "data": {}
                        }
                    }
                ]
            }
        ],
        "version": "1.0",
        "experiment_name": "quarkus-resteasy-kruize-min-http-response-time-db_0"
    }
]
```
