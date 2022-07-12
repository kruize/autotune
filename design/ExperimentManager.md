# **Kruize Experiment Manager**


## Overview
The experiment manager simply helps in running experiments on target applications to find the best configurable tunables to achieve end user objectives.
The experiment manager expects the following inputs from the end user.
- Application deployment details.
- Objectives.
- Tunable that can be customized.
- Metric queries, which are required to evaluate user objectives.

The Experiment Manager will parse this information and start executing experiments on the application and provide a summary report. Report will help the end user to compare and find the best tunables for their given user objective.

Example : TBD

## Goals
The experiment manager's goals are as follows:

- Find the best configuration for existing system to get the best performance for a given user objective.
- In order to find the best configuration, EM has to perform the following steps for each trial configuration.
  - pre-validate
  - deploy
  - post validation.
  - Apply for/Wait for a Load.
  - Collect metrics.
  - Summarize metrics result.

## Specifications
The experiment manager's specifications are as follows:

- Parse configurable tunable from JSON provided either by the user or autotune's analyzer.
- Parse all metrics queries sent in JSON, which are required to be executed to measure performance.
- Deploy application using configurable tunable provided in JSON.
- Apply/Wait for load to the deployed application.
- Run metrics queries to collect measurements.
- Summarize collected measurements.
- Send summarized results back to the user to verify.
- Run multiple trials per deployment in parallel.

## API’s
The following API's are available to work with Experiment Manager:
- Create Experimental Trials.
- Experiment Trials List.
- Get the current experimental trial status.
- Get a metric summary result.

## Use case representation
The following diagram shows a high-level representation of use cases.
<br/><br/>
<p align="center">
  <img src="/design/images/EmOnly.png">
</p>

## EM Architecture
The experiment manager contains the following main building blocks:

- ### Building blocks.
  - Dispatcher handler
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

- ### Dispatcher flow.

- ### TaskManger flow.

- ### Target Handler flow.

- ### Datasource Handler flow.

- ### Summarizer flow.

## Use Cases

## Download diagrams
[Usecase diagram](https://www.redhat.com/architect/portfolio/tool/index.html?#gitlab.com/msvinaykumar/autotune/blob/createEMDesignDocWithPlaceHolder/design/drawio/UseCase.drawio)


