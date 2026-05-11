---
name: new-joinee-onboarding
description: Complete onboarding guide for new team members joining Kruize Autotune project
---

# Kruize Autotune New Joinee Onboarding

Welcome to the Kruize Autotune team! This skill will guide you through the complete onboarding process.

## Onboarding Steps

<Steps>
<Step>
**Project Overview & Setup**
- Introduce the new joinee to Kruize Autotune's purpose and architecture
- Review `architecture-overview.md` for high-level understanding
- Follow `setup-checklist.md` for environment setup
- Ensure all prerequisites are installed (Docker, Kubectl, Minikube/Kind, Maven, Python)
</Step>

<Step>
**Repository & Codebase Familiarization**
- Clone the required repositories (autotune, benchmarks)
- Review `code-structure.md` to understand the Java package organization
- Explain the main modules:
  - `analyzer/`: Recommendation engine
  - `database/`: Database layer (PostgreSQL)
  - `experimentManager/`: Experiment lifecycle management
  - `service/`: REST API layer
  - `utils/`: Common utilities
- Walk through key design documents in `/design/` folder
</Step>

<Step>
**Local Development Setup**
- Help set up local Kruize instance
- Deploy Kruize on local cluster (Kind/Minikube)
- Install and configure Prometheus
- Create metadata and metric profiles
- Import sample metadata
- Verify the setup by accessing Kruize UI and APIs
</Step>

<Step>
**Testing Framework Familiarization**
- Review `testing-guide.md` for comprehensive testing overview
- Explain test structure:
  - Local monitoring tests (`tests/scripts/local_monitoring_tests/`)
  - Remote monitoring tests (`tests/scripts/remote_monitoring_tests/`)
  - API tests, stress tests, fault-tolerant tests
- Run sample tests to verify understanding
- Explain pytest markers (@pytest.mark.sanity, @pytest.mark.negative, etc.)
</Step>

<Step>
**Key Concepts Deep Dive**
Review and explain these critical Kruize concepts:
- **Monitoring Modes**: Local vs Remote monitoring
- **Experiments**: How Kruize tracks workload optimization experiments
- **Metadata Profiles**: Queries to collect workload data from monitoring systems
- **Metric Profiles**: Metrics and optimization goals
- **Performance Profiles**: SLO definitions and thresholds
- **Layers**: Runtime and framework detection and tunable configuration
- **Recommendations**: Cost vs Performance optimization
- **Terms**: Short/Medium/Long term recommendations
</Step>

<Step>
**API Familiarization**
- Review `/design/KruizeLocalAPI.md` for Local Monitoring APIs
- Review `/design/MonitoringModeAPI.md` for Remote Monitoring APIs
- Understand key APIs:
  - `/createExperiment`
  - `/updateResults`
  - `/listRecommendations`
  - `/createMetadataProfile`, `/createMetricProfile`
  - `/importMetadata`, `/listMetadata`
  - `/createLayer`, `/listLayers`
- Test APIs using curl or Postman
</Step>

<Step>
**First Contribution Setup**
- Review `first-pr-guide.md` for contribution workflow
- Review CONTRIBUTING.md for commit guidelines
- Set up GPG key for signed commits
- Fork the repository
- Create a feature branch
- Make a small documentation fix or test enhancement
- Submit first PR following team conventions
</Step>

<Step>
**Code Review & Best Practices**
- Explain code review process
- Coding standards:
  - Java best practices
  - REST API conventions
  - Test writing patterns (parametrized tests, fixtures)
  - Error handling and validation
  - Logging practices
- Common pitfalls to avoid
</Step>

<Step>
**Resources & Communication**
- Slack channels: #kruize-autotune
- Weekly team meetings schedule
- Documentation resources:
  - Main README.md
  - Design documents in /design/
  - API samples in /design/APISamples.md
  - Installation guides in /docs/
- External resources:
  - Kruize demos repo
  - Benchmarks repo
  - Kruize operator repo
</Step>

<Step>
**First Task Assignment**
- Assign a beginner-friendly task:
  - Documentation improvement
  - Test enhancement
  - Minor bug fix
  - Code refactoring
- Provide mentorship and code review
- Celebrate first merged PR!
</Step>
</Steps>

## Key Principles

1. **Hands-on Learning**: New joinee should build and run Kruize locally
2. **Ask Questions**: Encourage asking questions on Slack
3. **Documentation First**: Always check docs before asking
4. **Test Driven**: Write tests for every change
5. **Incremental Progress**: Start small, build confidence gradually

## Success Criteria

By the end of onboarding, the new joinee should be able to:
- ✅ Deploy Kruize locally and generate recommendations
- ✅ Understand the architecture and key modules
- ✅ Run and write tests
- ✅ Make code contributions following team standards
- ✅ Navigate the codebase confidently
- ✅ Understand the API design and usage

## Supporting Files

- `setup-checklist.md`: Detailed environment setup steps
- `architecture-overview.md`: Kruize architecture explained
- `code-structure.md`: Codebase organization guide
- `testing-guide.md`: Comprehensive testing documentation
- `first-pr-guide.md`: Step-by-step first PR workflow
- `api-quick-reference.md`: Quick API reference guide
