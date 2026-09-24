# Kruize Autotune Onboarding Bob Skill

This Bob skill provides comprehensive onboarding for new team members joining the Kruize Autotune project.

## How to Use

Bob will automatically activate this skill when you ask about onboarding topics like:
- "Help me onboard a new team member"
- "Guide me through the Kruize onboarding process"
- "What should a new joinee know about Kruize?"

## Skill Contents

### Main Skill
- **SKILL.md** - Step-by-step onboarding workflow with 10 structured phases

### Supporting Documentation

1. **setup-checklist.md** (6.9KB)
   - Complete environment setup guide
   - Prerequisites installation (Java, Docker, Kubernetes, etc.)
   - Repository setup and GPG configuration
   - Kruize deployment and verification

2. **architecture-overview.md** (12KB)
   - High-level architecture explanation
   - Core components breakdown
   - Key concepts (Experiments, Profiles, Layers)
   - Data flow diagrams
   - Technology stack

3. **code-structure.md** (14KB)
   - Detailed source code organization
   - Package-by-package walkthrough
   - Common design patterns (DAO, Validation)
   - Coding conventions
   - Code path examples

4. **testing-guide.md** (14KB)
   - Testing philosophy and structure
   - How to run tests (pytest, Maven)
   - Test markers and fixtures
   - Writing new tests
   - Parametrized test patterns
   - Best practices

5. **first-pr-guide.md** (11KB)
   - Complete PR submission workflow
   - Git branching strategy
   - Commit message format
   - GPG-signed commits
   - Handling review feedback
   - Common troubleshooting

6. **api-quick-reference.md** (13KB)
   - Quick reference for all major APIs
   - curl command examples
   - Request/Response formats
   - Common query parameters
   - Testing workflows

## Onboarding Phases

The skill guides new joinee through these phases:

1. **Project Overview & Setup** - Understand Kruize and set up environment
2. **Repository Familiarization** - Learn codebase structure
3. **Local Development Setup** - Deploy Kruize locally
4. **Testing Framework** - Understand and run tests
5. **Key Concepts Deep Dive** - Learn Kruize-specific concepts
6. **API Familiarization** - Practice with APIs
7. **First Contribution** - Submit first PR
8. **Code Review & Best Practices** - Learn team standards
9. **Resources & Communication** - Connect with team
10. **First Task Assignment** - Get real work!

## Quick Start for Mentors

When onboarding a new team member:

```markdown
1. Have them follow setup-checklist.md first
2. Review architecture-overview.md together
3. Walk through code-structure.md
4. Have them run tests using testing-guide.md
5. Guide first PR using first-pr-guide.md
6. Reference api-quick-reference.md for daily work
```

## Success Criteria

By end of onboarding, new joinee should:
- ✅ Deploy Kruize locally and generate recommendations
- ✅ Understand architecture and key modules
- ✅ Run and write tests
- ✅ Make code contributions following team standards
- ✅ Navigate codebase confidently
- ✅ Understand API design and usage

## Maintenance

To update this skill:
1. Edit relevant `.md` files in `.bob/skills/onboarding/`
2. Keep SKILL.md workflow updated
3. Ensure examples remain accurate
4. Update based on new joinee feedback

## Resources Referenced

This skill leverages existing Kruize documentation:
- Main README.md
- CONTRIBUTING.md
- /design/ directory (architecture docs)
- /docs/ directory (installation guides)
- /tests/README.md
- Manifest files in /manifests/

## Author

Created: 2026-05-04
Purpose: Standardize and streamline new team member onboarding

---

**Note**: This is a Bob AI skill. It provides structured guidance but should be supplemented with human mentorship and team interaction.
