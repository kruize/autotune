# **Experiment Manager**


## Overview
The Experiment Manager deploys these config and then monitors them and reports the results back to the Recommendation Manager. This loop continues until we have a config that provides the best possible result for the performance objective provided by the user.

## Use case
The following diagram shows a high-level representation of use cases.
<br/><br/>
<p align="center">
  <img src="/design/images/EMUsecases.drawio.png">
</p>

## Goals
The experiment manager's goals are as follows:

- Find the best configuration for existing system to get the best performance for a given user objective.
- In order to find the best configuration, EM has to perform the following steps for each trial configuration.
  - pre-validate
  - deploy
  - post validation.
  - Apply for/Wait for a Load.
  - Collect metrics.
  - Summerize metrics result.

## Specifications
The experiment manager's specifications are as follows:

- Parse configurable tunables from JSON provided either by the user or autotune's analyzer.
- Parse all metrics queries sent in JSON, which are required to be executed to measure performance.
- Deploy application using configurable tunables provided in JSON.
- Apply/Wait for load to the deployed application.
- Run metrics queries to collect measurements.
- Summerize collected measurements.
- Send summerized results back to the user/recommendation manager to seek more trials with different sets of tuneables.
- Run multiple trials per deployment in parallel.

## API's
TBD

## EM Architecture
The experiment manager contains the following main building blocks:

- ### Building blocks.
  - RestAPI dispatcher
    - This block is used to accept input requests in the form of JSON format, which contains experimental tunables along with evaluation metrics queries. This handler is used with the Taskmanger handler to run multiple trials in parallel per deployment.
  - TaskManger
    - This block is designed as an event-driven architecture where RestAPI will produce trials and store them in a queue. And there will be a scheduler running for every specific given delay to check if any messages are in queue and trigger the Iteration Manager if any new trials are found.
  - IterationManger
    - This block is used to perform primary tasks such as
    Deploy and gather metrics for a specified number of iterations.
    Following the completion, summarise the metrics results and send them to the recommendation manager or user. The Recommendation Manager further evaluates the metrics results and suggests more trials if needed.
  - Target Handler
    - This block contributes to the provision of a wrapper API around the Kubernetes Java client for running experiment trials on the Kubernetes environment.
  - Datasource Handler
    - These blocks help in the collection of metrics results using data sources like Promethus, Instana, Datadog etc.
  - Summarizer
    - A Summarizer is a sub-block for one of the steps in the Iteration Manager where it helps in summerizing metrics results to find Max, Min, Percentile, Average, Mean, etc.
 
- ### Logical representation.

- ### Schema representation.

- ### RestAPI dispatcher flow.

- ### TaskManger flow.

- ### Target Handler flow.

- ### Datasource Handler flow.

- ### Summarizer flow.



## Download diagrams
[Usecase diagram](https://www.redhat.com/architect/portfolio/tool/index.html?#gitlab.com/msvinaykumar/autotune/blob/createEMDesignDocWithPlaceHolder/design/drawio/UseCase.drawio)


