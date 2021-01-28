"""
Copyright (c) 2020, 2021 Red Hat, IBM Corporation and others.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
"""

from tunables import get_all_tunables

from install_package import install

install('python-dotenv')
install('optuna')

from bayes_optuna import optuna_hpo

sla_class, direction, ml_algo_impl, tunables = get_all_tunables()

if ml_algo_impl in ("optuna_tpe", "optuna_tpe_multivariate", "optuna_skopt"):
    optuna_hpo.recommend(direction, ml_algo_impl, sla_class, tunables)
