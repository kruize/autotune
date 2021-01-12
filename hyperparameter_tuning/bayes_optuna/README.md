# Optuna

A sampler in Optuna determines the parameter values to be evaluated in a trial.

The appropriate sampler is used in [`optuna_hpo.py`](./optuna_hpo.py) to create a study object based on the value of
`ml_algo_impl`.

| Value of `ml_algo_impl`   | Sampler                                       |
|---------------------------|-----------------------------------------------|
| `optuna_tpe`              | optuna.samplers.TPESampler()                  |
| `optuna_tpe_multivariate` | optuna.samplers.TPESampler(multivariate=True) |
| `optuna_skopt`            | optuna.integration.SkoptSampler()             |
