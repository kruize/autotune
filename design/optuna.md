### Flowchart of Machine Learning module

![flowchart](/design/flowchart.png)

### TPE vs Gaussian Processes

Sampling algorithms such as random sampling and TPE do not use relationship between parameters whereas sampling algorithms such as Gaussian Process use relationship between parameters. 

TPE proposes a parameter value for each parameter one-by-one. So there are no interactions between parameters involved in producing the next parameter set for all parameters.

With Gaussian process, the next value of parameter 2 is conditioned on the proposed (next) value for parameter 1 and hence it does take the interactions between parameters into account. This is due to the Bayesian nature of it, it considers conditional probability. For example, given parameter x improves throughput, what is the probability that y improves the throughput. 

However TPE does not work like this, hence it does not take feature interactions into consideration when doing hyper-parameter optimization.

### Flow of Prediction Algorithm

- _Data Preparation_  
Prepare Training data and Test data.  
`train_df, test_df `   

- _Feature Preparation_  
Separate features and label variable.  
`X = train_df.drop('responseTime', axis=1)`  
`y = train_df.responseTime`  

- _Data Splitting_  
Split the train data into training and validation sets.  
`X_train, X_validation, y_train, y_validation = train_test_split(X, y, train_size=0.75)`  

- _Model Training_  
Create the model itself.  
`model.fit(`  
`    X_train, y_train,`  
`    cat_features=categorical_features_indices,`  
`    eval_set=(X_validation, y_validation)`  
`)`  

- _Model Applying_  
Get predictions.  
`predictions = model.predict(X_test)`  

