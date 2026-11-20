# Your First Pull Request Guide

## Overview

This guide walks you through submitting your first pull request (PR) to Kruize Autotune. We'll start with a simple documentation or test enhancement to get you familiar with the process.

## Prerequisites

- ✅ Development environment set up (see `setup-checklist.md`)
- ✅ Fork of autotune repo on GitHub
- ✅ Local clone with upstream remote configured
- ✅ GPG key configured for signed commits

## Step-by-Step Workflow

### 1. Pick a Good First Issue

**Where to find tasks:**
- GitHub Issues labeled `good-first-issue`
- Documentation improvements (typos, clarity, missing examples)
- Test enhancements (add missing test cases)
- Code refactoring (small, focused improvements)

**Good first PR ideas:**
- Fix a typo in README.md or docs/
- Add a missing test case
- Improve code comments
- Add validation for edge cases
- Update outdated documentation

**Example first PRs:**
- "docs: Fix typo in installation guide"
- "test: Add negative test for null layer name"
- "refactor: Extract duplicate validation logic to util function"

---

### 2. Sync Your Fork

Always start with the latest code from upstream:

```bash
# Ensure you're on master
git checkout master

# Fetch latest changes from upstream
git fetch upstream

# Merge upstream changes into your master
git merge upstream/master

# Push to your fork
git push origin master
```

---

### 3. Create a Feature Branch

```bash
# Create and switch to new branch
git checkout -b fix/documentation-typo

# Branch naming conventions:
# - feat/feature-name    (new features)
# - fix/bug-description  (bug fixes)
# - test/test-description (test additions)
# - docs/doc-update      (documentation)
# - refactor/improvement (code refactoring)
```

**Branch naming best practices:**
- Use descriptive names
- Use hyphens, not underscores
- Keep it short but clear
- Examples:
  - `feat/add-gpu-support`
  - `fix/null-pointer-in-layer-validation`
  - `test/add-label-validation-tests`
  - `docs/update-api-examples`

---

### 4. Make Your Changes

#### Example 1: Documentation Fix

```bash
# Edit the file
vim docs/autotune_install.md

# Check what changed
git diff

# Stage the change
git add docs/autotune_install.md
```

#### Example 2: Add a Test Case

```bash
# Edit test file
vim tests/scripts/local_monitoring_tests/rest_apis/test_create_layer.py

# Run the test to verify
cd tests/scripts/local_monitoring_tests/rest_apis
pytest test_create_layer.py::test_your_new_test -v

# Stage the change
git add test_create_layer.py
```

#### Example 3: Code Change

```bash
# Edit Java file
vim src/main/java/com/autotune/service/CreateLayer.java

# Build to verify no compilation errors
mvn clean install

# Run relevant tests
mvn test -Dtest=CreateLayerTest

# Stage the change
git add src/main/java/com/autotune/service/CreateLayer.java
```

---

### 5. Commit Your Changes

Kruize requires **GPG-signed commits**:

```bash
# Commit with GPG signature (-S flag)
git commit -S -m "docs: fix typo in installation guide

Fixed spelling error in the Prometheus installation section.

Signed-off-by: Your Name <your.email@example.com>"
```

**Commit Message Format:**

```
<type>: <short summary>

<optional detailed description>

Signed-off-by: Full Name <email>
```

**Types:**
- `feat:` - New feature
- `fix:` - Bug fix
- `test:` - Test additions or fixes
- `docs:` - Documentation changes
- `refactor:` - Code refactoring
- `style:` - Code style changes (formatting)
- `chore:` - Build/tooling changes

**Examples of good commit messages:**

```
docs: update API examples in KruizeLocalAPI.md

Added missing examples for createLayer API with label-based
detection. Clarified the difference between queries and label
presence types.

Signed-off-by: John Doe <john@example.com>
```

```
test: add validation tests for label special characters

Added negative tests to verify label values properly reject
special characters that could cause PromQL injection issues.
Tests cover quotes, backslashes, and bracket characters.

Signed-off-by: Jane Smith <jane@example.com>
```

```
fix: handle null layer_name in validation

Previously, null layer_name would cause NullPointerException.
Now returns proper 400 error with message.

Fixes #1234

Signed-off-by: John Doe <john@example.com>
```

**Important:**
- First line: max 72 characters
- Blank line between summary and description
- Wrap description at 72 characters
- Always include `Signed-off-by` footer
- Use `-S` flag for GPG signature

---

### 6. Push Your Branch

```bash
# Push to your fork
git push origin fix/documentation-typo

# If push fails due to GPG:
# - Check: git config user.signingkey
# - Ensure GPG agent is running
# - Try: gpg --clearsign (to test GPG works)
```

---

### 7. Create Pull Request

#### On GitHub:

1. Go to your fork: `https://github.com/YOUR_USERNAME/autotune`
2. Click "Compare & pull request" (green button)
3. **Base repository:** `kruize/autotune` **base:** `master`
4. **Head repository:** `YOUR_USERNAME/autotune` **compare:** `your-branch-name`

#### Fill PR Template:

**Title:** Clear, concise summary (same as commit message)
```
docs: fix typo in installation guide
```

**Description:**
```markdown
## Summary
Fixed spelling error in the Prometheus installation section of the installation guide.

## Changes
- Corrected "Promethues" to "Prometheus" in docs/autotune_install.md

## Type of Change
- [x] Documentation update
- [ ] Bug fix
- [ ] New feature
- [ ] Test addition

## Checklist
- [x] I have signed my commits
- [x] I have tested my changes
- [x] I have updated relevant documentation
```

5. Click "Create pull request"

---

### 8. Respond to Review Feedback

#### Common review comments and how to address them:

**Comment: "Please add a test for this change"**
```bash
# Add the test
vim tests/...

# Commit
git commit -S -m "test: add test for layer validation"

# Push (will update PR automatically)
git push origin fix/documentation-typo
```

**Comment: "Please fix linting issues"**
```bash
# Fix the issues
# ...

# Amend the previous commit
git add .
git commit --amend -S

# Force push (since you amended)
git push origin fix/documentation-typo --force
```

**Comment: "Please rebase on latest master"**
```bash
# Fetch latest upstream
git fetch upstream

# Rebase your branch
git rebase upstream/master

# Resolve any conflicts if they appear
# Edit conflicted files
git add <resolved-files>
git rebase --continue

# Force push
git push origin fix/documentation-typo --force
```

#### Best practices for PR reviews:
- Respond to every comment
- Be open to feedback
- Ask questions if unclear
- Make requested changes promptly
- Be respectful and professional

---

### 9. PR Gets Approved and Merged! 🎉

Once your PR is approved:
- Maintainer will merge it
- Your branch can be deleted
- Celebrate your first contribution!

**Cleanup after merge:**
```bash
# Switch to master
git checkout master

# Pull latest (includes your merged changes)
git pull upstream master

# Delete local branch
git branch -d fix/documentation-typo

# Delete remote branch
git push origin --delete fix/documentation-typo
```

---

## Common Issues and Solutions

### Issue: GPG Signature Failed

```bash
# Check GPG key is configured
git config --global user.signingkey

# Check GPG key exists
gpg --list-secret-keys

# Test GPG signing
echo "test" | gpg --clearsign

# If GPG agent not running:
gpg-agent --daemon

# Re-configure Git to use GPG
git config --global commit.gpgsign true
```

### Issue: Commit Has Conflicts

```bash
# Fetch latest upstream
git fetch upstream

# Rebase on upstream/master
git rebase upstream/master

# If conflicts:
# 1. Fix conflicts in files
vim <conflicted-file>

# 2. Stage resolved files
git add <conflicted-file>

# 3. Continue rebase
git rebase --continue

# 4. Force push
git push origin your-branch --force
```

### Issue: Need to Update PR with New Changes

```bash
# Make your changes
# ...

# Amend existing commit (if single commit PR)
git add .
git commit --amend -S

# Or add new commit (if multi-commit PR)
git commit -S -m "address review feedback"

# Push
git push origin your-branch --force  # if amended
# OR
git push origin your-branch  # if new commit
```

### Issue: Accidentally Committed to Master

```bash
# Create branch from current HEAD
git branch fix/my-changes

# Reset master to upstream
git reset --hard upstream/master

# Switch to new branch
git checkout fix/my-changes

# Push new branch
git push origin fix/my-changes
```

---

## PR Checklist

Before submitting, verify:

- [ ] Branch is up-to-date with upstream/master
- [ ] Changes are focused and related
- [ ] Commit messages follow format
- [ ] Commits are GPG-signed
- [ ] Tests pass (`mvn test` or `pytest`)
- [ ] Build succeeds (`mvn clean install`)
- [ ] Documentation updated (if needed)
- [ ] No unrelated changes included
- [ ] PR description is clear

---

## Example Complete Workflow

```bash
# 1. Sync fork
git checkout master
git fetch upstream
git merge upstream/master
git push origin master

# 2. Create branch
git checkout -b test/add-label-validation

# 3. Make changes
vim tests/scripts/local_monitoring_tests/rest_apis/test_create_layer.py

# 4. Test changes
cd tests/scripts/local_monitoring_tests/rest_apis
pytest test_create_layer.py::test_create_layer_label_validation -v

# 5. Stage and commit
git add test_create_layer.py
git commit -S -m "test: add validation for label special characters

Added negative tests to verify label values properly reject
special characters that could cause PromQL injection.

Signed-off-by: John Doe <john@example.com>"

# 6. Push
git push origin test/add-label-validation

# 7. Create PR on GitHub

# 8. Address review comments
# ... make changes ...
git commit -S -m "test: add additional edge cases"
git push origin test/add-label-validation

# 9. After merge, cleanup
git checkout master
git pull upstream master
git branch -d test/add-label-validation
git push origin --delete test/add-label-validation
```

---

## Resources

- Official contributing guide: `CONTRIBUTING.md`
- Slack channel: #kruize-autotune
- GitHub Issues: https://github.com/kruize/autotune/issues
- Learn about PRs: https://www.freecodecamp.org/news/how-to-make-your-first-pull-request-on-github-3/

## Next Steps

1. Pick a simple issue from GitHub
2. Follow this guide step-by-step
3. Submit your first PR
4. Respond to feedback
5. Celebrate when it merges!
6. Move on to more complex contributions!

Good luck with your first PR! 🚀
