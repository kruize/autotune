# **Experiment Manager**


## Overview
The Experiment Manager deploys these config and then monitors them and reports the results back to the Recommendation Manager. This loop continues until we have a config that provides the best possible result for the performance objective provided by the user.

## Use case
The following diagram shows a high-level representation of use cases.
<br/><br/>
<p align="center">
  <img src="/design/images/EMUsecases.drawio.png">
</p>

## Specifications
TBD

## Goals
- Parse configurable tunables from JSON provided by the user/Autotune's analyzer.
- Parse all metrics queries required to be collected for measuring performance.
- Deploy application using configurable tunables provided in JSON.
- Apply/Wait for load to the deployed application.
- Run metrics queries to collect measurements.
- Summerize collected measurements.
- Send summerized results back to the user/recommendation manager to seek more trials with different sets of tuneables.

## Download diagrams
[Usecase diagram](https://www.redhat.com/architect/portfolio/tool/index.html?#gitlab.com/msvinaykumar/autotune/blob/createEMDesignDocWithPlaceHolder/design/drawio/UseCase.drawio)


