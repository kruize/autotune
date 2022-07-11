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




## Download diagrams
[Usecase diagram](https://www.redhat.com/architect/portfolio/tool/index.html?#gitlab.com/msvinaykumar/autotune/blob/createEMDesignDocWithPlaceHolder/design/drawio/UseCase.drawio)


